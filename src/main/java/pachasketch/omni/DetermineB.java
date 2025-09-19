package pachasketch.omni;

public class DetermineB {
    private final long ram;
    private int depth;
    private int width;
    private double delta;
    int nCat;
    int nNum;
    int dyadicLevels;

    public DetermineB(long ram) {
        this.ram = ram;
    }

    public int determineB(int depth, int width, double delta, int nCat, int nNum, int dyadicLevels) {
        this.depth = depth;
        this.width = width;
        this.delta = delta;

        this.nCat = nCat;
        this.nNum = nNum;
        this.dyadicLevels = dyadicLevels;

        return search(0, 2, 0);
    }
    int highB = Integer.MAX_VALUE;
    public int search(int prevB, int B, int iters) {
        if (B <= 1) {
            return B;
        }
        if (iters > 100){
            if (compRam(prevB) < ram) {
                return prevB;
            } else {
                throw new RuntimeException("Too many iterations");
            }
        }
        iters++;
        double usedM = compRam(B);

        if (ram*0.99 <= usedM && usedM <= ram) {
            System.out.println("Found B: " + B + " usedM: " + usedM / 8/ 1024/1024 + " ram: " + ram + " iters: " + iters);
            return B;
        } else if (usedM < ram*0.99) {
            if (highB == Integer.MAX_VALUE)
                return search(B, B*2, iters);
            else
                return search(B, Math.min(highB, B*2), iters);
        } else {
            highB = B;
            return search(prevB, (int) Math.ceil((double) (B + prevB)/2), iters);
        }
    }

    public double compRam(int B) {
        int smallb = (int) Math.ceil(Math.log(4*Math.pow(B, (double) 5/2)/delta));

        double memCatAttrs = (double) depth * width * (B * (smallb + 3 * 32 + 1) + 32) * nCat;
        double memNumAttrs = this.dyadicLevels * depth * width*(B * (smallb + 3 * 32 + 1) + 32) * nNum;

        return memCatAttrs + memNumAttrs;
    }


}
