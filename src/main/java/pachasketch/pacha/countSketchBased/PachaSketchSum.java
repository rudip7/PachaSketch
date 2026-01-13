package pachasketch.pacha.countSketchBased;

import pachasketch.Sketch;
import pachasketch.pacha.baseSketches.BloomFilter;
import pachasketch.pacha.baseSketches.CountSketch;
import pachasketch.pacha.baseSketches.Filter;
import pachasketch.pacha.baseSketches.NodeTracker;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.pacha.components.NumericalBitmap;
import pachasketch.pacha.utils.BAdicUtils;
import pachasketch.pacha.utils.PachaQueryResult;
import pachasketch.pacha.utils.QueryStats;

import java.util.*;
import java.util.stream.Collectors;

public class PachaSketchSum implements Sketch {
    private final int levels;
    private final int numDimensions;
    private final int[] catColMap;
    private final int[] numColMap;
    private final int[] bases;
    private final ADTree adTree;
    private MaterializedCombinations materialized;
    public NumericalBitmap[] numericalBitmaps;
    private int[] maxValues;
    private int[] minValues;
    private int processedElements;
    public Filter catIndex;
    public Filter numIndex;
    public Filter regionIndex;
    public CountSketch[] baseSketches;

    public int forcedAlignment = -1; // -1 means no forced alignment

    private int maxNCubes = 1_000_000; // Default value, can be adjusted based on requirements


    public PachaSketchSum(int levels, int[] catColMap, int[] numColMap,
                          int[] bases, ADTree adTree, MaterializedCombinations materialized,
                          Filter catIndex, Filter numIndex, Filter regionIndex,
                          CountSketch[] baseSketches) {
        this.levels = levels;
        this.numDimensions = catColMap.length + numColMap.length;
        this.catColMap = catColMap;
        this.numColMap = numColMap;
        assert numColMap.length == bases.length : "Number of bases must match number of numerical columns";
        this.bases = bases;

        this.adTree = adTree;
        this.materialized = materialized;
        this.numericalBitmaps = new NumericalBitmap[numColMap.length];
        for (int i = 0; i < numColMap.length; i++) {
            this.numericalBitmaps[i] = new NumericalBitmap(bases[i], 110_000);
        }

        this.maxValues = new int[numColMap.length];
        this.minValues = new int[numColMap.length];

        if (catIndex == null || (catIndex instanceof BloomFilter && ((BloomFilter) catIndex).getSize() > adTree.computeDistinctValues())){
            this.catIndex = new NodeTracker(adTree.computeDistinctValues());
        } else {
            this.catIndex = catIndex;
        }

        this.numIndex = numIndex;
        this.regionIndex = regionIndex;
        assert baseSketches.length == levels : "Number of base sketches must match number of levels";
        this.baseSketches = baseSketches;

        this.processedElements = 0;
    }

    public Object[][] getNumericalMappings(int[] element) {
        // Initialize matrices for level calculations
        int[][] cubes = new int[levels][numColMap.length];

        for (int level = 0; level < levels; level++) {
            for (int dim = 0; dim < numColMap.length; dim++) {
                double rangeSize = Math.pow(bases[dim], level);
                cubes[level][dim] = (int) Math.floor(element[dim] / rangeSize);
            }
        }

        // Expand cubes based on materialized combinations
        Object[][] expandedCubes = materialized.latticeExpand(cubes);


        // Create final mappings including levels
        int numRows = expandedCubes.length;
        Object[][] mappings = new Object[numRows + 1][numColMap.length + 1];

        // Fill in the levels and expanded cubes
        for (int i = 0; i < numRows; i++) {
            mappings[i][0] = i / (numRows / levels); // Calculate corresponding level
            System.arraycopy(expandedCubes[i], 0, mappings[i], 1, numColMap.length);
        }

        // Add the all-wildcards option as the last row
        mappings[numRows][0] = levels - 1;
        for (int j = 1; j <= numColMap.length; j++) {
            mappings[numRows][j] = "*";
        }

        return mappings;
    }

    public void update(String[] element, int increment) {
        // Extract categorical and numerical values
        List<String> catValues = new ArrayList<>(catColMap.length);
        int[] numValues = new int[numColMap.length];

        for (int i = 0; i < catColMap.length; i++) {
            catValues.add(element[catColMap[i]]);
        }
        for (int i = 0; i < numColMap.length; i++) {
            numValues[i] = Integer.parseInt(element[numColMap[i]]);
        }

        // Update max and min values for numerical dimensions
        for (int i = 0; i < numColMap.length; i++) {
            int maxLevelIdx = (int) Math.floor(numValues[i] / Math.pow(bases[i], levels));
            int maxLevelMin = maxLevelIdx * (int) Math.pow(bases[i], levels);
            int maxLevelMax = (maxLevelIdx + 1) * (int) Math.pow(bases[i], levels) - 1;

            maxValues[i] = Math.max(maxLevelMax, maxValues[i]);
            minValues[i] = Math.min(maxLevelMin, minValues[i]);

            numericalBitmaps[i].update(numValues[i]);
        }

        // Get categorical mappings from ADTree
        List<List<String>> catMappings = adTree.getMapping(catValues);

        // Get numerical mappings
        Object[][] numMappings = getNumericalMappings(numValues);

        List<String> keysCatMappings = catMappings.stream()
                .map(row -> String.join(", ", row))
                .collect(Collectors.toList());

        Map<Integer, List<String>> keysNumMappings = new HashMap<>(levels);
        for (int i = 0; i < numMappings.length; i++) {
            Integer level = (Integer) numMappings[i][0];
            String regionKey = Arrays.stream(numMappings[i])
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            keysNumMappings.computeIfAbsent(level, k -> new ArrayList<>()).add(regionKey);
        }

        // Built cross product between numerical and categorical mappings
        Map<Integer, List<String>> mappedRegions = regionsCrossProduct(keysCatMappings, keysNumMappings);

        // Update indices and base sketches
        catIndex.updateBatch(keysCatMappings);
        for (List<String> regions : keysNumMappings.values()) {
            numIndex.updateBatch(regions);
        }

        for(int level : mappedRegions.keySet()){
            List<String> regions = mappedRegions.get(level);
            regionIndex.updateBatch(regions);
            baseSketches[level].updateBatch(regions, increment);
        }

        processedElements++;
    }

    public int[][] minimalSpatialBAdicCover(int[] numDimensions, int[][] numPredicates, int reducedToLevel) {
        int[] coverBases = new int[numDimensions.length];
        for (int i = 0; i < numDimensions.length; i++) {
            coverBases[i] = bases[numDimensions[i]];
        }

        List<List<int[]>> minimalBAdicCovers = new ArrayList<>();

        // Calculate minimal b-adic covers for each dimension
        for (int i = 0; i < numPredicates.length; i++) {
            List<int[]> coverRanges = BAdicUtils.minimalBAdicCover(
                coverBases[i],
                numPredicates[i][0],
                numPredicates[i][1]
            );
            // Filter empty ranges
            List<int[]> prunedRanges = numericalBitmaps[numDimensions[i]].pruneBAdicArray(coverRanges);
            minimalBAdicCovers.add(prunedRanges);
        }



        // Cache for downgraded ranges
        Map<String, int[]> cachedPrunedRanges = new HashMap<>();
        List<Integer> levels = new ArrayList<>();
        List<int[]> indices = new ArrayList<>();
        int partialNCubes = 0;

        // Process all combinations
        for (int[][] combination : generateCombinations(minimalBAdicCovers)) {
            int minLevel = getMinLevel(combination);
            minLevel = Math.min(minLevel, this.levels - 1);

            List<int[]> downgraded = new ArrayList<>();
            boolean skipCombination = false;

            // Downgrade each dimension
            for (int i = 0; i < combination.length; i++) {
                String cacheKey = String.format("%d_%d_%d_%d", i, combination[i][0], combination[i][1], minLevel);
                int[] downgradedIndices;

                if (cachedPrunedRanges.containsKey(cacheKey)) {
                    downgradedIndices = cachedPrunedRanges.get(cacheKey);
                } else {
                    int[] rawDowngradedIndices = BAdicUtils.downgradeBAdicRangeIndices(
                        coverBases[i],
                        combination[i][0],
                        combination[i][1],
                        minLevel
                    );
                    downgradedIndices = numericalBitmaps[numDimensions[i]].pruneBAdicIndices(minLevel, rawDowngradedIndices);
                    cachedPrunedRanges.put(cacheKey, downgradedIndices);
                }

                if (downgradedIndices.length >= 1) {
                    downgraded.add(downgradedIndices);
                } else {
                    skipCombination = true;
                    break;
                }
            }

            if (skipCombination) {
                continue;
            }

            // Calculate partial number of cubes
            int product = 1;
            for (int[] indices1 : downgraded) {
                product *= indices1.length;
            }
            partialNCubes += product;

            // Check if we need to reduce levels
            if (partialNCubes > maxNCubes || partialNCubes < 0) { // Handle potential overflow
                if (reducedToLevel == -1) {
                    reducedToLevel = calculateMedianLevel(minimalBAdicCovers);
//                    reducedToLevel = calculateNewAlignedLevel(minimalBAdicCovers);
                } else if (reducedToLevel < this.levels - 1) {
                    reducedToLevel++;
                } else {
                    // TODO: Use this output as it returned all wildcards at max level
                    return null;
                }
                forcedAlignment = reducedToLevel;
                return minimalSpatialBAdicCover(numDimensions, alignNumPredicates(numPredicates, coverBases, reducedToLevel), reducedToLevel);
            }

            // Add valid combinations
            int[][] cartesianProduct = generateCartesianProduct(downgraded);
            for (int[] productIndices : cartesianProduct) {
                levels.add(minLevel);
                indices.add(productIndices);
            }
        }

        // Combine results
        // TODO: When all regions are pruned by the bitmap then return null
        int[][] result = new int[levels.size()][indices.get(0).length + 1];
        for (int i = 0; i < levels.size(); i++) {
            result[i][0] = levels.get(i);
            System.arraycopy(indices.get(i), 0, result[i], 1, indices.get(i).length);
        }

        return result;
    }


    public List<int[][]> generateCombinations(List<List<int[]>> ranges) {
        List<int[][]> result = new ArrayList<>();
        if (ranges.isEmpty()) return result;
        for (List<int[]> dimRanges : ranges) {
            if (dimRanges.isEmpty()) return result; // No valid combinations if any range is empty
        }

        int n = ranges.size();
        int[] indices = new int[n];
        int[][] current = new int[n][2];

        while (true) {
            // Build current combination
            for (int i = 0; i < n; i++) {
                current[i] = ranges.get(i).get(indices[i]);
            }
            result.add(Arrays.copyOf(current, current.length));

            // Increment indices like an odometer
            int pos = n - 1;
            while (pos >= 0 && ++indices[pos] >= ranges.get(pos).size()) {
                indices[pos] = 0;
                pos--;
            }
            if (pos < 0) break; // done
        }

        return result;
    }


    private int getMinLevel(int[][] combination) {
        int minLevel = Integer.MAX_VALUE;
        for (int[] pair : combination) {
            minLevel = Math.min(minLevel, pair[0]);
        }
        return minLevel;
    }

    private int calculateMedianLevel(List<List<int[]>> minimalBAdicCovers) {
        List<Integer> maxLevels = new ArrayList<>();
        for (List<int[]> cover : minimalBAdicCovers) {
            int maxLevel = cover.stream()
                .mapToInt(arr -> arr[0])
                .max()
                .orElse(0);
            maxLevels.add(maxLevel);
        }

        Collections.sort(maxLevels);
        int middle = maxLevels.size() / 2;
        return maxLevels.get(middle);
    }

    private int calculateNewAlignedLevel(List<List<int[]>> minimalBAdicCovers) {
        int minLevel = Integer.MAX_VALUE;
        for (List<int[]> cover : minimalBAdicCovers) {
            int coverMinLevel = cover.stream()
                    .mapToInt(arr -> arr[0])
                    .min()
                    .orElse(0);
            if (coverMinLevel < minLevel) {
                minLevel = coverMinLevel;
            }
        }
        return minLevel+1;
    }

//    private int[][] alignNumPredicates(int[][] numPredicates, int[] coverBases, int targetLevel) {
//        int[][] aligned = new int[numPredicates.length][2];
//        for (int i = 0; i < numPredicates.length; i++) {
//            int scale = (int) Math.pow(coverBases[i], targetLevel);
//            aligned[i][0] = (numPredicates[i][0] / scale) * scale;
//            aligned[i][1] = ((numPredicates[i][1] / scale) + 1) * scale - 1;
//        }
//        return aligned;
//    }

    private int[][] alignNumPredicates(int[][] numPredicates, int[] coverBases, int targetLevel) {
        int[][] aligned = new int[numPredicates.length][2];
        for (int i = 0; i < numPredicates.length; i++) {
            double scale = Math.pow(coverBases[i], targetLevel);
            int indexLow = (int) Math.round(numPredicates[i][0] / scale);
            int indexHigh = (int) Math.round(numPredicates[i][1] / scale);
            if (indexLow == indexHigh) {
                indexLow = (int) Math.floor(numPredicates[i][0] / scale);
                indexHigh = (int) Math.ceil(numPredicates[i][1] / scale);
            }
            aligned[i][0] = (int) (indexLow * scale);
            aligned[i][1] = (int) (indexHigh * scale - 1);
        }
        return aligned;
    }

    private int[][] generateCartesianProduct(List<int[]> arrays) {
        int totalSize = 1;
        for (int[] array : arrays) {
            totalSize *= array.length;
        }

        int[][] result = new int[totalSize][arrays.size()];
        int repeat = 1;

        for (int i = arrays.size() - 1; i >= 0; i--) {
            int[] currentArray = arrays.get(i);
            int currentLength = currentArray.length;
            int index = 0;

            for (int j = 0; j < totalSize; j++) {
                result[j][i] = currentArray[index];
                if ((j + 1) % repeat == 0) {
                    index = (index + 1) % currentLength;
                }
            }
            repeat *= currentLength;
        }

        return result;
    }

    public PachaQueryResult getSubQueries(List<Object> query, boolean detailed, boolean debug) {
        if (query.size() != numDimensions) {
            throw new IllegalArgumentException("Query must have the same number of dimensions as the sketch. Expected: " + numDimensions + ", got: " + query.size());
        }
        forcedAlignment = -1; // Reset forced alignment for each query

        // Process categorical predicates
        List<Set<String>> catPredicates = new ArrayList<>(catColMap.length);
        for(int idx : catColMap) {
            Object catPredicate = query.get(idx);
            if (catPredicate instanceof List<?>) {
                catPredicates.add(new HashSet<>(((List<?>) catPredicate).stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet())));
            } else if (catPredicate instanceof String && catPredicate.equals("*")) {
                catPredicates.add(new HashSet<>(Collections.singleton("*")));
            } else {
                throw new IllegalArgumentException("Query predicate at index " + idx + " expected to be a list or '*'.");
            }
        }

        // Process numerical predicates
        List<int[]> numPredicates = new ArrayList<>(numColMap.length);
        List<Integer> numDimensions = new ArrayList<>();
        int numIdx = 0;
        for(int idx : numColMap) {
            Object numPredicate = query.get(idx);
            if ((numPredicate instanceof List<?> bounds) && bounds.size() == 2) {
                int lower = ((Number) bounds.get(0)).intValue();
                int upper = ((Number) bounds.get(1)).intValue();
                if (lower > upper) {
                    throw new IllegalArgumentException("Lower bound cannot be greater than upper bound.");
                }
                numPredicates.add(new int[]{lower, upper});
                numDimensions.add(numIdx);
            } else if (numPredicate instanceof String && numPredicate.equals("*")) {
                numPredicates.add(new int[]{minValues[numIdx], maxValues[numIdx]});
            } else {
                throw new IllegalArgumentException("Query predicate at index " + idx +
                        " expected to be a numerical list of size 2 with [lower_bound, upper_bound] or '*'.");
            }
            numIdx++;
        }

        // Get categorical regions from ADTree
        List<List<String>> relevantNodes = adTree.getRelevantNodes(catPredicates, true);

        // Get numerical regions based on the minimal b-adic spatial cover
        boolean[] dimIndices = materialized.findBestMatch(numDimensions);
        numDimensions.clear();
        int dimCount = 0;
        for (int i = 0; i < dimIndices.length; i++) {
            if (dimIndices[i]) {
                numDimensions.add(i);
                dimCount++;
            }
        }
        int[][] bAdicCubes;
        if (numDimensions.isEmpty()){
            bAdicCubes = new int[][]{{levels - 1}};
        } else {

            int[][] matSpaceNumPredicates = new int[dimCount][2];
            int index = 0;
            for (int i = 0; i < numPredicates.size(); i++) {
                if (dimIndices[i]) {
                    matSpaceNumPredicates[index] = numPredicates.get(i);
                    index++;
                }
            }

            bAdicCubes = minimalSpatialBAdicCover(numDimensions.stream().mapToInt(Integer::intValue).toArray(), matSpaceNumPredicates, -1);
        }

        // Prune empty categorical regions
        List<String> keysCatRegions = relevantNodes.stream()
        .map(row -> String.join(", ", row))
        .collect(Collectors.toList());
        List<String> catRegions = catIndex.filterBatch(keysCatRegions);

        // Prune empty numerical regions

        Map<Integer, List<String>> keysNumRegions = new HashMap<>(levels);
        if (bAdicCubes == null) {
            keysNumRegions.put(levels - 1, Collections.singletonList(levels - 1 + ", *".repeat(numColMap.length)));
        } else {
            for (int[] bAdicCube : bAdicCubes) {
                StringBuilder regionKey = new StringBuilder(String.valueOf(bAdicCube[0]));
                int cubeIndex = 1;
                for (int j = 0; j < numColMap.length; j++) {
                    if (dimIndices[j]) {
                        regionKey.append(", ").append(bAdicCube[cubeIndex]);
                        cubeIndex++;
                    } else {
                        regionKey.append(", *");
                    }
                }
                keysNumRegions.computeIfAbsent(bAdicCube[0], k -> new ArrayList<>()).add(regionKey.toString());
            }
        }

        Map<Integer, List<String>> numRegions = new HashMap<>(keysNumRegions.size());
        int nNumRegions = 0;
        for (Integer level : keysNumRegions.keySet()) {
            List<String> regions = keysNumRegions.get(level);
            List<String> filteredRegions = numIndex.filterBatch(regions);
            if (!filteredRegions.isEmpty()) {
                numRegions.put(level, filteredRegions);
                nNumRegions += filteredRegions.size();
            }
        }

        if (catRegions.size() * nNumRegions > maxNCubes) {
            if (debug) {
                System.out.println("Too many candidate regions, skipping query.");
            }
            return null;
        }

        // Generate Cartesian product of categorical and numerical regions
        Map<Integer, List<String>> candidateRegions = regionsCrossProduct(catRegions, numRegions);

        // Prune candidate regions by the region index
        Map<Integer, List<String>> queryRegions = new HashMap<>(candidateRegions.size());
        int nCandidateRegions = 0;
        int nQueryRegions = 0;
        for (Integer level : candidateRegions.keySet()) {
            List<String> regions = candidateRegions.get(level);
            nCandidateRegions += regions.size();
            List<String> filteredRegions = regionIndex.filterBatch(regions);
            if (!filteredRegions.isEmpty()) {
                queryRegions.put(level, filteredRegions);
                nQueryRegions += filteredRegions.size();
            }
        }

        QueryStats stats = null;
        if (detailed || debug) {
            int[] queriesPerLevel = new int[levels];
            for (Integer level : queryRegions.keySet()) {
                queriesPerLevel[level] = queryRegions.get(level).size();
            }
            stats = QueryStats.from(
                    relevantNodes.size(),
                    catRegions.size(),
                    bAdicCubes != null ? bAdicCubes.length : 1,
                    nNumRegions,
                    nCandidateRegions,
                    nQueryRegions,
                    queriesPerLevel,
                    debug);
        }

        return new PachaQueryResult(
                queryRegions,
                detailed ? stats : null,
                forcedAlignment);
    }

    public int query(List<Object> query){
        return (int) query(query, false, false).estimate();
    }

    public PachaQueryResult query(List<Object> query, boolean detailed, boolean debug){
        if (query.size() != numDimensions) {
            throw new IllegalArgumentException("Query must have the same number of dimensions as the sketch. Expected: " + numDimensions + ", got: " + query.size());
        }
        boolean allWildcards = true;
        for (Object predicate : query) {
            if (!(predicate instanceof String && predicate.equals("*"))) {
                allWildcards = false;
                break;
            }
        }
        if (allWildcards) {
            return new PachaQueryResult(processedElements);
        }

        PachaQueryResult queryResult = getSubQueries(query, detailed, debug);
        if (queryResult == null) {
            return new PachaQueryResult(0); // No valid regions found
        }

        int estimate = 0;
        Map<Integer, List<String>> queryRegions = queryResult.regions();
        for (Integer level : queryRegions.keySet()){
            List<String> regions = queryRegions.get(level);
            estimate += baseSketches[level].queryBatch(regions);
        }

        if(debug){
            System.out.println("Estimate: " + estimate);
        }

        queryResult.setEstimate(estimate);

        return queryResult;
    }

    private Map<Integer, List<String>> regionsCrossProduct(List<String> catRegions, Map<Integer, List<String>> numRegions) {
        Map<Integer, List<String>> result = new HashMap<>();
        for(Integer level : numRegions.keySet()) {
            List<String> numRegionList = numRegions.get(level);
            List<String> combinedRegions = new ArrayList<>(catRegions.size() * numRegionList.size());
            for (String catRegion : catRegions) {
                for (String numRegion : numRegionList) {
                    combinedRegions.add(catRegion + ", " + numRegion);
                }
            }
            result.put(level, combinedRegions);
        }
        return result;
    }

    public void limitToSingleCombination() {
        this.materialized = MaterializedCombinations.createSingleCombination(materialized.getAttributeNames());
    }

    public void extendToAllCombinations() {
        this.materialized = MaterializedCombinations.createAllCombinations(materialized.getAttributeNames());
    }

    public double getSizeInMB(){
        double size = 0.0;
        for (NumericalBitmap bitmap : numericalBitmaps) {
            size += bitmap.getSizeInMB();
        }
        size += catIndex.getSizeInMB();
        size += numIndex.getSizeInMB();
        size += regionIndex.getSizeInMB();
        for (CountSketch cms : baseSketches) {
            size += cms.getSizeInMB();
        }
        return size;
    }

    public int getLevels() {
        return levels;
    }

    public int getNumDimensions() {
        return numDimensions;
    }

    public int[] getCatColMap() {
        return catColMap;
    }

    public int[] getNumColMap() {
        return numColMap;
    }

    public int[] getBases() {
        return bases;
    }

    public ADTree getAdTree() {
        return adTree;
    }

    public MaterializedCombinations getMaterializedCombinations() {
        return materialized;
    }

    public int getProcessedElements() {
        return processedElements;
    }

    public void setMaxNCubes(int maxNCubes) {
        this.maxNCubes = maxNCubes;
    }

    public void setBases(int[] newBases){
        if (newBases.length != bases.length){
            throw new IllegalArgumentException("New bases array must have the same length as the current bases array.");
        }
        if (processedElements > 0 ){
            throw new IllegalStateException("Cannot change bases after processing elements.");
        }
        for (int i = 0; i < numericalBitmaps.length; i++) {
            if (this.bases[i] != newBases[i]){
                this.numericalBitmaps[i] = new NumericalBitmap(newBases[i], 110_000);
            }
        }
        System.arraycopy(newBases, 0, this.bases, 0, newBases.length);
    }

//    public String toJson() {
//        Gson gson = new Gson();
//        JsonObject jsonObject = new JsonObject();
//
//        jsonObject.addProperty("levels", levels);
//        jsonObject.addProperty("numDimensions", numDimensions);
//        jsonObject.add("catColMap", gson.toJsonTree(catColMap));
//        jsonObject.add("numColMap", gson.toJsonTree(numColMap));
//        jsonObject.add("bases", gson.toJsonTree(bases));
//        jsonObject.add("adTree", gson.toJsonTree(adTree.toJson()));
//        jsonObject.add("materialized", gson.toJsonTree(materialized.toJson()));
//        jsonObject.add("numericalBitmaps", gson.toJsonTree(
//                Arrays.stream(numericalBitmaps).map(NumericalBitmap::toJson).toArray()));
//        jsonObject.add("catIndex", gson.toJsonTree(catIndex.toJson()));
//        jsonObject.add("numIndex", gson.toJsonTree(numIndex.toJson()));
//        jsonObject.add("regionIndex", gson.toJsonTree(regionIndex.toJson()));
//        jsonObject.add("baseSketches", gson.toJsonTree(
//                Arrays.stream(baseSketches).map(CountSketch::toJson).toArray()));
//        jsonObject.addProperty("maxNCubes", maxNCubes);
//        jsonObject.addProperty("processedElements", processedElements);
//
//        return gson.toJson(jsonObject);
//    }

//    public void saveAsJson(String filePath) throws IOException {
//        try (Writer writer = new FileWriter(filePath);
//             JsonWriter jsonWriter = new JsonWriter(writer)) {
//            jsonWriter.beginObject();
//            Gson gson = new Gson();
//
//            jsonWriter.name("levels").value(levels);
//            jsonWriter.name("numDimensions").value(numDimensions);
//            jsonWriter.name("catColMap");
//            gson.toJson(catColMap, int[].class, jsonWriter);
//            jsonWriter.name("numColMap");
//            gson.toJson(numColMap, int[].class, jsonWriter);
//            jsonWriter.name("bases");
//            gson.toJson(bases, int[].class, jsonWriter);
//            jsonWriter.name("adTree");
//            gson.toJson(adTree.toJson(), String.class, jsonWriter);
//            jsonWriter.name("materialized");
//            gson.toJson(materialized.toJson(), String.class, jsonWriter);
//            jsonWriter.name("numericalBitmaps");
//            gson.toJson(Arrays.stream(numericalBitmaps).map(NumericalBitmap::toJson).toArray(), Object[].class, jsonWriter);
//            jsonWriter.name("catIndex");
//            gson.toJson(catIndex.toJson(), String.class, jsonWriter);
//            jsonWriter.name("numIndex");
//            gson.toJson(numIndex.toJson(), String.class, jsonWriter);
//            jsonWriter.name("regionIndex");
//            gson.toJson(regionIndex.toJson(), String.class, jsonWriter);
//            jsonWriter.name("baseSketches");
//            gson.toJson(Arrays.stream(baseSketches).map(CountMinSketch::toJson).toArray(), Object[].class, jsonWriter);
//            jsonWriter.name("maxNCubes").value(maxNCubes);
//            jsonWriter.name("processedElements").value(processedElements);
//
//            jsonWriter.endObject();
//        }
//    }

//    public static PachaSketchSum fromJson(String json) {
//        Gson gson = new Gson();
//        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
//
//        int levels = jsonObject.get("levels").getAsInt();
//        int[] catColMap = gson.fromJson(jsonObject.get("catColMap"), int[].class);
//        int[] numColMap = gson.fromJson(jsonObject.get("numColMap"), int[].class);
//        int[] bases = gson.fromJson(jsonObject.get("bases"), int[].class);
//        ADTree adTree = ADTree.fromJson(jsonObject.get("adTree").getAsString());
//        MaterializedCombinations materialized = MaterializedCombinations.fromJson(jsonObject.get("materialized").toString());
//        BloomFilter catIndex = BloomFilter.fromJson(jsonObject.get("catIndex").getAsString());
//        BloomFilter numIndex = BloomFilter.fromJson(jsonObject.get("numIndex").getAsString());
//        BloomFilter regionIndex = BloomFilter.fromJson(jsonObject.get("regionIndex").getAsString());
//
//        JsonArray baseSketchesJson = (JsonArray) jsonObject.get("baseSketches");
//        CountMinSketch[] baseSketches = new CountMinSketch[baseSketchesJson.size()];
//        for (int i = 0; i < baseSketchesJson.size(); i++) {
//            String cmsJson = baseSketchesJson.get(i).getAsString();
//            baseSketches[i] = CountMinSketch.fromJson(cmsJson);
//        }
//        int maxNCubes = jsonObject.get("maxNCubes").getAsInt();
//        PachaSketchSum pachaSketch = new PachaSketchSum(levels, catColMap, numColMap, bases, adTree, materialized,
//                catIndex, numIndex, regionIndex, baseSketches);
//        pachaSketch.maxNCubes = maxNCubes;
//        pachaSketch.processedElements = jsonObject.get("processedElements").getAsInt();
//
//        JsonArray numericalBitmapsJson = (JsonArray) jsonObject.get("numericalBitmaps");
//        for (int i = 0; i < numericalBitmapsJson.size(); i++) {
//            String nbJson = numericalBitmapsJson.get(i).getAsString();
//            pachaSketch.numericalBitmaps[i] = NumericalBitmap.fromJson(nbJson);
//        }
//
//        return pachaSketch;
//    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PachaSketchSum other = (PachaSketchSum) obj;
        return levels == other.levels &&
               numDimensions == other.numDimensions &&
               maxNCubes == other.maxNCubes &&
               processedElements == other.processedElements &&
               Arrays.equals(catColMap, other.catColMap) &&
               Arrays.equals(numColMap, other.numColMap) &&
               Arrays.equals(bases, other.bases) &&
               Arrays.equals(numericalBitmaps, other.numericalBitmaps) &&
               Arrays.equals(baseSketches, other.baseSketches) &&
               Objects.equals(adTree, other.adTree) &&
               Objects.equals(materialized, other.materialized) &&
               Objects.equals(catIndex, other.catIndex) &&
               Objects.equals(numIndex, other.numIndex) &&
               Objects.equals(regionIndex, other.regionIndex) &&
               Arrays.equals(maxValues, other.maxValues) &&
               Arrays.equals(minValues, other.minValues);
    }

}
