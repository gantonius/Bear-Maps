public class FringeContent implements Comparable<FringeContent> {
    long id;
    double totalDistance;


    FringeContent(long identification, double totalDist) {
        id = identification;
        totalDistance = totalDist;
    }

    @Override
    public int compareTo(FringeContent item) {
        if (totalDistance < item.totalDistance) {
            return -1;
        }
        if (totalDistance > item.totalDistance) {
            return 1;
        }
        return 0;
    }
}
