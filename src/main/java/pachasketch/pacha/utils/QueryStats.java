package pachasketch.pacha.utils;

public record QueryStats(
        int relevantNodes,
        int catRegions,
        int bAdicCubes,
        int numRegions,
        int candidateRegions,
        int queryRegions,
        int[] queriesPerLevel
) {
    public static QueryStats empty() {
        return new QueryStats(0, 0, 0, 0, 0, 0, new int[0]);
    }

    public static QueryStats from(int relevantNodes, int catRegions, int bAdicCubes, int numRegions,
                                  int candidateRegions, int queryRegions, int[] queriesPerLevel, boolean debug) {
        if (debug) {
            System.out.println("Categorical regions: " + relevantNodes);
            System.out.println("Indexed categorical regions: " + catRegions);
            System.out.println("Numerical regions: " + bAdicCubes);
            System.out.println("Indexed numerical regions: " + numRegions);
            System.out.println("Candidate regions: " + candidateRegions);
            System.out.println("Query regions: " + queryRegions);
            for (int i = 0; i < queriesPerLevel.length; i++) {
                if (queriesPerLevel[i] > 0) {
                    System.out.println("Level " + i + " queries: " + queriesPerLevel[i]);
                }
            }
        }

        return new QueryStats(relevantNodes, catRegions, bAdicCubes, numRegions, candidateRegions, queryRegions, queriesPerLevel);
    }
}
