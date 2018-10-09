public class Storage {
    long id;
    double totalDistance;
    long parent;

    public Storage(long identification) {
        id = identification;
        totalDistance = Double.POSITIVE_INFINITY;
        parent = -100;
    }

    public Storage(long identification, double totalDist, long par) {
        id = identification;
        totalDistance = totalDist;
        parent = par;
    }

}
