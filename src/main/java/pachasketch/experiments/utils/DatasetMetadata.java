package pachasketch.experiments.utils;

public class DatasetMetadata {
    String datasetName;
    int nCat;
    int nNum;

    public DatasetMetadata(String datasetName, int nCat, int nNum) {
        this.datasetName = datasetName;
        this.nCat = nCat;
        this.nNum = nNum;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public int getnCat() {
        return nCat;
    }

    public int getnNum() {
        return nNum;
    }
}
