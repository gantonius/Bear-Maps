import java.util.ArrayList;

public class Node {
    long id;
    double lat;
    double lon;
    ArrayList<Long> connectedTo;

    public Node(long identification, double latitude, double longitude) {
        id = identification;
        lat = latitude;
        lon = longitude;
        connectedTo = new ArrayList<>();
    }

    public void addAdjacent(long adjID) {
        connectedTo.add(adjID);
    }
}
