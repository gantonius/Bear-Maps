import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {

    private static double ROOT_ULLAT = 37.892195547244356;
    private static double ROOT_ULLON = -122.2998046875;
    private static double ROOT_LRLAT = 37.82280243352756;
    private static double ROOT_LRLON = -122.2119140625;

    public Rasterer() {
        // YOUR CODE HERE
    }
    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        Map<String, Object> results = new HashMap<>();
        System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
                + "your browser.");
        double ullon = params.get("ullon");
        double lrlon = params.get("lrlon");
        double w = params.get("w");
        double h = params.get("h");
        double ullat = params.get("ullat");
        double lrlat = params.get("lrlat");
        double lonDPP = (lrlon - ullon) / w;
        double feetPerPixel = lonDPP * 288200;
        if (!checkValid(ullon, ullat, lrlon, lrlat)) {
            results.put("depth", null);
            results.put("raster_ul_lon", null);
            results.put("raster_lr_lon", null);
            results.put("raster_ul_lat", null);
            results.put("raster_lr_lon", null);
            results.put("render_grid", null);
            results.put("query_success", false);
            return results;
        }
        double[] cutCoord = cutCoordinates(ullon, ullat, lrlon, lrlat);
        ullon = cutCoord[0];
        lrlon = cutCoord[1];
        ullat = cutCoord[2];
        lrlat = cutCoord[3];
        int depth = 0;
        double resDepth;
        while (depth < 7) {
            resDepth = widthAndHeightTileForDepth(depth)[0] / 256.0;
            if (resDepth < lonDPP) {
                break;
            }
            depth += 1;
        }
        int[] howManyFromLeftBottom = howManyAway(depth, ullon, ullat, lrlon, lrlat);
        // in term of file name
        int[] startingPoint = convertRegCoordToFileSystem(howManyFromLeftBottom[0],
                howManyFromLeftBottom[1], depth);
        double[] tileLonLatCoord = widthAndHeightTileForDepth(depth);
        double startingPointCoordX = ROOT_ULLON + (howManyFromLeftBottom[0] * tileLonLatCoord[0]);
        double startingPointCoordY = ROOT_LRLAT + (howManyFromLeftBottom[1] * tileLonLatCoord[1]);

        double[] startingPointCoord = {startingPointCoordX, startingPointCoordY};

        int[] howManyAway = howManyXAndYToGo(depth, startingPointCoord[0], startingPointCoord[1],
                lrlon, ullat, tileLonLatCoord[0], tileLonLatCoord[1]);

        double coordsToReturnULLON = startingPointCoord[0];
        double coordsToReturnULLAT = startingPointCoord[1]
                + ((howManyAway[1]) * tileLonLatCoord[1]);
        double coordsToReturnLRLON = startingPointCoord[0]
                + ((howManyAway[0]) * tileLonLatCoord[0]);
        double coordsToReturnLRLAT = startingPointCoord[1];

        int[] topLeft = {startingPoint[0], startingPoint[1] - howManyAway[1] + 1};


        String[][] grid = new String[howManyAway[1]][howManyAway[0]];

        int count = 0;

        for (int i = 0; i < howManyAway[1]; i++) {
            for (int j = 0; j < howManyAway[0]; j++) {
                grid[i][j] = "d" + depth + "_x" + (topLeft[0] + j) + "_y"
                        +  (topLeft[1] + i) + ".png";
            }
        }

        results.put("depth", depth);
        results.put("raster_ul_lon", coordsToReturnULLON);
        results.put("raster_lr_lon", coordsToReturnLRLON);
        results.put("raster_ul_lat", coordsToReturnULLAT);
        results.put("raster_lr_lat", coordsToReturnLRLAT);
        results.put("render_grid", grid);
        results.put("query_success", true);

        return results;
    }

    private int getDepth(double resolution) {
        double depthExact = (-(Math.log(resolution / 49.0)) / Math.log(2.0)) + 1;
        if (depthExact > 7) {
            return 7;
        }
        return (int) Math.ceil(depthExact);
    }

    private double[][] fourPointsCoordinates(double ullon, double ullat,
                                             double lrlon, double lrlat) {
        double lllon = ullon;
        double lllat = lrlat;
        double urlon = lrlon;
        double urlat = ullat;

        // format = top-left ([0][0] & [1][0]), top-right ([0][1] & [1][1]),
        // bottom-left ([0][2] & [1][2]), bottom-right ([0][3] & [1][3])
        double[][] coords = {{ullon, urlon, lllon, lrlon}, {ullat, urlat, lllat, lrlat}};
        return coords;
    }

    private double[] widthAndHeight(double ullon, double ullat, double lrlon, double lrlat) {
        double[] coords = {Math.abs(ullon - lrlon), (ullat - lrlat)};
        // format = width, height
        return coords;
    }

    // width and depth of tile is the same (256 x 256 pixels)
    private double widthAndDepthOfTileInFeet(int depth) {
        return 256 * (49 / (Math.pow(2.0, depth)));
    }

    // width and depth of tile is the same (256 x 256 pixels)
    // returns in long lat coord
    private double[] widthAndHeightTileForDepth(int depth) {
        //int num = (int) Math.pow(2, depth);
        double num = Math.pow(2, depth);
        double[] coords = {Math.abs(ROOT_ULLON - ROOT_LRLON) / num,
                Math.abs(ROOT_ULLAT - ROOT_LRLAT) / num};
        // format = width, height
        return coords;
    }

    private int[] howManyAway(int depth, double pointULLON, double pointULLAT,
                              double pointLRLON, double pointLRLAT) {

        double[][] pointOfInterest = fourPointsCoordinates(pointULLON, pointULLAT,
                pointLRLON, pointLRLAT);
        double bottomLeftLonX = pointOfInterest[0][2];
        double bottomLeftLatY = pointOfInterest[1][2];

        double[] tileWidthHeight = widthAndHeightTileForDepth(depth);
        double tileWidth = tileWidthHeight[0];
        double tileHeight = tileWidthHeight[1];

        double bottomLeftLonXBase0 = bottomLeftLonX - ROOT_ULLON;
        double bottomLeftLatYBase0 = bottomLeftLatY - ROOT_LRLAT;

        int tilesHorizontal = (int) Math.floor(bottomLeftLonXBase0 / tileWidth);
        int tilesVertical = (int) Math.floor(bottomLeftLatYBase0 / tileHeight);

        int[] toReturn = {tilesHorizontal, tilesVertical};

        // format x (long), y (lat)
        return toReturn;
    }

    private int[] convertRegCoordToFileSystem(int x, int y, int depth) {
        int canvasSizeY = (int) Math.pow(2, depth) - 1;
        //int canvasSizeY = canvasSizeX;
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }

        int[] toReturn = {x, canvasSizeY - y};

        return toReturn;
    }

    private int[] howManyXAndYToGo(int depth, double initLON, double initLAT,
                                   double finishLON, double finishLAT,
                                   double tileWidthCoord, double tileHeightCoord) {

        int numHor = (int) Math.ceil((finishLON - initLON) / tileWidthCoord);
        int numVer = (int) Math.ceil((finishLAT - initLAT) / tileHeightCoord);
        int[] toReturn = {numHor, numVer};

        // format x (long), y (lat)
        return toReturn;
    }

    private boolean checkValid(double pointULLON, double pointULLAT,
                               double pointLRLON, double pointLRLAT) {
        // check horizontal
        if ((pointULLON > ROOT_LRLON) || (pointLRLON < ROOT_ULLON)
                || (pointLRLAT > ROOT_ULLAT) || (pointULLAT < ROOT_LRLAT)) {
            return false;
        }
        return true;
    }

    private double[] cutCoordinates(double pointULLON, double pointULLAT,
                                    double pointLRLON, double pointLRLAT) {

        if (pointULLON < ROOT_ULLON) {
            pointULLON = ROOT_ULLON;
        }
        if (pointLRLON > ROOT_LRLON) {
            pointLRLON = ROOT_LRLON;
        }
        if (pointULLAT > ROOT_ULLAT) {
            pointULLAT = ROOT_ULLAT;
        }
        if (pointLRLAT < ROOT_LRLAT) {
            pointLRLAT = ROOT_LRLAT;
        }

        double[] toReturn = {pointULLON, pointLRLON, pointULLAT, pointLRLAT};

        return toReturn;
    }



    // for own testing
    /*public static void main(String[] args) {
        Rasterer temp = new Rasterer();
        Map<String, Double> params = new HashMap<String, Double>();
        params.put("ullon", -122.27809796131179);
        params.put("lrlon", -122.25401605993639);
        params.put("w", 566.0);
        params.put("h", 317.0);
        params.put("ullat", 37.840785807816395);
        params.put("lrlat", 37.827298241145016);
        Map<String, Object> result = temp.getMapRaster(params);
        System.out.println("raster_ul_lon: " + result.get("raster_ul_lon"));
        System.out.println("raster_lr_lon: " + result.get("raster_lr_lon"));
        System.out.println("raster_ul_lat: " + result.get("raster_ul_lat"));
        System.out.println("raster_lr_lat: " + result.get("raster_lr_lat"));
        System.out.println("depth: " + result.get("depth"));
        // test getDepth
        System.out.println(temp.getDepth(3.092050552368164));
        // test fourPointsCoordinates
        double[][] temp1 = temp.fourPointsCoordinates(-122.2998046875, 37.892195547244356,
                -122.2119140625, 37.82280243352756);
        System.out.println("ULLon" + temp1[0][0]);
        System.out.println("ULLat" + temp1[1][0]);
        System.out.println("URLon" + temp1[0][1]);
        System.out.println("URLat" + temp1[1][1]);
        System.out.println("LLLon" + temp1[0][2]);
        System.out.println("URLat" + temp1[1][2]);
        System.out.println("LRLon" + temp1[0][3]);
        System.out.println("LRLat" + temp1[1][3]);
        // test widthAndHeight
        double[] temp2 = temp.widthAndHeight(-122.2998046875, 37.892195547244356,
                -122.2119140625, 37.82280243352756);
        System.out.println("Height" + temp2[0]);
        System.out.println("Width" + temp2[1]);
        // test widthAndDepthOfTileInFeet
        System.out.println(temp.widthAndDepthOfTileInFeet(0));
        // test widthAndDepthOfTileInDecimal
        System.out.println("Width " + temp.widthAndHeightTileForDepth(0)[0]);
        System.out.println("Width " + temp.widthAndHeightTileForDepth(0)[1]);
        // test howManyAway
        int[] temp3 = temp.howManyAway(5, -122.2998046875, 37.824970968331215,
                -122.29705810546875, 37.82280243352756);
        System.out.println(temp3[0]);
        System.out.println(temp3[1]);
        System.out.println("Tiles x: "
                + temp.convertRegCoordToFileSystem(temp3[0], temp3[1], 5)[0]);
        System.out.println("Tiles y: "
                + temp.convertRegCoordToFileSystem(temp3[0], temp3[1], 5)[1]);
        int[] temp4 = temp.howManyAway(5, -122.255859375, 37.85533045558231,
                -122.25311279296875, 37.85316192077866);
        System.out.println(temp4[0]);
        System.out.println(temp4[1]);
        System.out.println("Tiles x: "
                + temp.convertRegCoordToFileSystem(temp4[0], temp4[1], 5)[0]);
        System.out.println("Tiles y: "
                + temp.convertRegCoordToFileSystem(temp4[0], temp4[1], 5)[1]);
        int[] temp6 = temp.howManyAway(1, -122.30410170759153, 37.870213571328854,
                -122.2104604264636, 37.8318576119893);
        System.out.println(temp6[0]);
        System.out.println(temp6[1]);
        System.out.println("Tiles x: "
                + temp.convertRegCoordToFileSystem(temp6[0], temp6[1], 1)[0]);
        System.out.println("Tiles y: "
                + temp.convertRegCoordToFileSystem(temp6[0], temp6[1], 1)[1]);
        // more comprehensive test
        Rasterer temporary = new Rasterer();
        double ullon = -122.241632;
        double lrlon = -122.24053;
        double ullat = 37.87655;
        double lrlat = 37.87548;
        double w = 892.0;
        double h = 875.0;
        double lonDPP = (lrlon - ullon) / w;
        double feetPerPixel = lonDPP * 288200;
        int depth = temporary.getDepth(feetPerPixel);
        int[] howManyFromLeftBottom = temporary.howManyAway(depth, ullon, ullat, lrlon, lrlat);
        // in term of file name
        int[] startingPoint = temporary.convertRegCoordToFileSystem(howManyFromLeftBottom[0],
                howManyFromLeftBottom[1], depth);
        double[] tileLonLatCoord = temporary.widthAndHeightTileForDepth(depth);
        System.out.println("Tile width: " + tileLonLatCoord[0]);
        System.out.println("Tile height: " + tileLonLatCoord[1]);
        double[] startingPointCoord = {ROOT_ULLON + (howManyFromLeftBottom[0] * tileLonLatCoord[0]),
                ROOT_LRLAT + (howManyFromLeftBottom[1] * tileLonLatCoord[1])};
        int[] howManyAway = temporary.howManyXAndYToGo(depth, startingPointCoord[0],
                startingPointCoord[1], lrlon, ullat, tileLonLatCoord[0], tileLonLatCoord[1]);
        System.out.println("X AWAY: " + howManyAway[0]);
        System.out.println("Y AWAY: " + howManyAway[1]);
    }*/




}
