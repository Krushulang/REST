import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import static java.lang.Thread.sleep;

/**
 *
 * Prog2 - REST
 * Professor Dimpsey
 * CSS 436 - Cloud Computing
 *
 * This program exposes and uses RESTful APIs to provide information about a specified city.
 * The program will take the users argument and pass it to various methods that will
 * retrieve the city's location in latitude and longitude, the temperature in fahrenheit,
 * the pollen severity
 * It then will find parks or pharmacies in the area depending on the results of the previous
 * queries.
 * This program also uses Gson to parse the Json that is returned from the HTTP GET calls that are made.
 *
 *
 * @author Krishna Langille
 * @version 1.0
 * January 18, 2023
 *
 */
public class MyCity {
    //retry times that start at 1 that exponentially grow if no response
    public static long locRetry = 1;
    public static long weaRetry = 1;
    public static long polRetry = 1;
    public static long phaRetry = 1;
    public static long actRetry = 1;

    /**
     * This is the main method that calls all other methods used to retrieve information from the APIs.
     * @param args - This is what is inputted into the command line.
     *             Should be a name of a city
     * @throws IOException - thrown in case of errors making an HTTP connection within methods called.
     * @throws InterruptedException - thrown because sleep is used in called methods.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        //this is used to make sure the user provides the proper number of arguments
        if(args.length != 1){
            System.out.println("No input given or invalid input, please retry with city name argument \nExiting...");
            System.exit(0);
        }

        //assigns the city to a variable
        String cityName = args[0];

        //this section calls getLocation to retrieve the latitude and longitude
        String[] coords = getLocation(cityName).split(",");
        float lat = Float.parseFloat(coords[0]);
        float lon = Float.parseFloat(coords[1]);
        System.out.println("For " + cityName + ", the latitude is: " + lat + " and the longitude is: " + lon);

        //this section calls getWeather to retrieve the temperature
        int temp = getWeather(lat, lon);
        System.out.println("The temperature is currently: " + temp + " in Fahrenheit\n");

        //this section calls getPollen to retrieve whether pollen is in the area
        boolean pollen = getPollen(lat, lon);

        //this section calls getActivity to find parks nearby as long as it is warm and there is no pollen
        if(!pollen && temp > 69){
            System.out.println("It is a great day! No pollen in sight and the temperature is just right. Here are some parks to visit in the area: \n");
            getActivity(lat, lon);
        }
        //this section calls getPharmacy to find pharmacies nearby as long as it is cold or there is pollen
        else{
            System.out.println("The temperature or pollen must not be in your favor today. Maybe head to one of these pharmacies for medication: \n");
            getPharmacy(lat, lon);
        }

        //program finished
        System.out.println("Calls finished, now exiting...\nHave a great day!");
    }

    /**
     * This method uses the openweather RESTful API.
     * It takes in a latitude and longitude to find what the temperature is in a given location.
     * Returns a temperature in fahrenheit.
     * @param latitude - float value that represents the latitude.
     * @param longitude - float value that represents the longitude.
     * @return - an integer that represents the temperature.
     * @throws IOException - for HTTP calls.
     * @throws InterruptedException - for sleep.
     */
    public static int getWeather(float latitude, float longitude) throws IOException, InterruptedException{
        //exponential retry
        if(weaRetry > 30){
            System.out.println("Couldn't connect at this time, please try again later");
            System.exit(0);
        }
        sleep(weaRetry * 1000);

        //openweather api HTTP url
        String stringUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=d6f4f80fa29ba1eb0d5bfa64c2f98cab";

        URL url = new URL(stringUrl);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        //switch case that determines how the status code is handled
        switch(statusCode/100){
            case 1:
            case 2:
                //Nothing to be done here
                break;
            case 3:
                //this properly redirects the connection to the proper URL
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                System.out.println("Redirecting: " + stringUrl + " to: " + redirectUrl);
                break;
            case 4:
                //this flips the error bit and returns since 400 level codes are bad requests on the server side
                System.out.println("Error visiting this location. Data may not exist, try again with a different city");
            case 5:
                //we retry this instance since there is a temporary issue that may be with the client
                System.out.println("Retrying connection...");
                weaRetry *= 2;
                getWeather(latitude, longitude);
                break;
            default:
                break;
        }

        //this section isolates the json that is needed to calculate the temperature
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Gson json = new Gson();
        String read = reader.readLine();
        JsonObject obj = json.fromJson(read, JsonObject.class);
        JsonObject main = obj.get("main").getAsJsonObject();

        //json is converted to int value
        int tempK = main.get("temp").getAsInt();

        //kelvin to fahrenheit conversion
        return (int) ((tempK - 273.15) * 9/5 + 32);
    }

    /**
     * This method uses the openweather RESTful API.
     * It takes in a city name to find what the latitude and longitude are in a given city.
     * Returns a location as a string to be parsed.
     * @param cityName - the name of the city the user inputs
     * @return - a string that is the concatenated location
     * @throws IOException - for HTTP calls.
     * @throws InterruptedException - for sleep.
     */
    public static String getLocation(String cityName) throws IOException, InterruptedException {
        //exponential retry
        if(locRetry > 30){
            System.out.println("Couldn't connect at this time, please try again later");
            System.exit(0);
        }
        sleep(locRetry * 1000);

        //openweather api HTTP url
        String stringUrl = "https://api.openweathermap.org/geo/1.0/direct?q=" + cityName + "&appid=d6f4f80fa29ba1eb0d5bfa64c2f98cab";

        URL url = new URL(stringUrl);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        //switch case that determines how the status code is handled
        switch(statusCode/100){
            case 1:
            case 2:
                //Nothing to be done here
                break;
            case 3:
                //this properly redirects the connection to the proper URL
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                System.out.println("Redirecting: " + stringUrl + " to: " + redirectUrl);
                break;
            case 4:
                //this flips the error bit and returns since 400 level codes are bad requests on the server side
                System.out.println("Error visiting " + cityName + ". City may not exist, try again with proper syntax");
            case 5:
                //we retry this instance since there is a temporary issue that may be with the client
                System.out.println("Retrying connection...");
                locRetry *= 2;
                getLocation(cityName);
                break;
            default:
                break;
        }

        //this section isolates the latitude and longitude from the json that will be returned
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Gson json = new Gson();
        String read = reader.readLine();
        JsonArray data = json.fromJson(read, JsonArray.class);
        JsonObject obj = data.get(0).getAsJsonObject();

        //from json to strings
        String lat = obj.get("lat").getAsString();
        String lon = obj.get("lon").getAsString();

        return lat + "," + lon;
    }

    /**
     * This method uses the breezometer RESTful API.
     * It takes in a latitude and longitude to find what the pollen content is in a given location.
     * Returns a boolean that represents if there is pollen or not.
     * @param latitude - float value that represents the latitude.
     * @param longitude - float value that represents the longitude.
     * @return - a boolean for pollen or not.
     * @throws IOException - for HTTP calls.
     * @throws InterruptedException - for sleep.
     */
    public static boolean getPollen(float latitude, float longitude) throws IOException, InterruptedException {
        //exponential retry
        if(polRetry > 30){
            System.out.println("Couldn't connect at this time, please try again later");
            System.exit(0);
        }
        sleep(polRetry * 1000);

        boolean pollen = false;

        //breezometer api HTTP url
        String stringUrl = "https://api.breezometer.com/pollen/v2/forecast/daily?lat=" + latitude + "&lon=" + longitude + "&key=fcc1b051c03b4b208da2283c996e302d&days=1";

        URL url = new URL(stringUrl);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        //switch case that determines how the status code is handled
        switch(statusCode/100){
            case 1:
            case 2:
                //Nothing to be done here
                break;
            case 3:
                //this properly redirects the connection to the proper URL
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                System.out.println("Redirecting: " + stringUrl + " to: " + redirectUrl);
                break;
            case 4:
                //this flips the error bit and returns since 400 level codes are bad requests on the server side
                System.out.println("Error visiting this location. Data may not exist, try again with a different city");
            case 5:
                //we retry this instance since there is a temporary issue that may be with the client
                System.out.println("Retrying connection...");
                polRetry *= 2;
                getPollen(latitude, longitude);
                break;
            default:
                break;
        }

        //this section isolates the json that has the pollen information for tree, grass, and weed pollen
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Gson json = new Gson();
        String read = reader.readLine();
        JsonObject obj = json.fromJson(read, JsonObject.class);
        JsonArray array = obj.get("data").getAsJsonArray();
        JsonObject data = array.get(0).getAsJsonObject();
        JsonObject types = data.get("types").getAsJsonObject();

        //sometimes some areas don't provide all 3 pollen counts, so this is necessary to prevent unexpected results
        JsonObject grass = null;
        JsonObject tree = null;
        JsonObject weed = null;
        if(types.get("tree") != null){
            grass = types.get("grass").getAsJsonObject();
        }
        if(types.get("grass") != null){
            tree = types.get("tree").getAsJsonObject();
        }
        if(types.get("weed") != null){
            weed = types.get("weed").getAsJsonObject();
        }

        //this section prints out the appropriate statement for each pollen type
        if(grass != null && grass.get("in_season").getAsBoolean()){
            pollen = true;
            JsonObject index = grass.get("index").getAsJsonObject();
            System.out.println("Grass pollen is in season with a " + index.get("category") + " rating of severity for the area");
        }
        else{
            System.out.println("Grass pollen is out of season");
        }
        if(tree != null && tree.get("in_season").getAsBoolean()){
            pollen = true;
            JsonObject index = tree.get("index").getAsJsonObject();
            System.out.println("Tree pollen is in season with a " + index.get("category") + " rating of severity for the area");
        }
        else{
            System.out.println("Tree pollen is out of season");
        }
        if(weed != null && weed.get("in_season").getAsBoolean()){
            pollen = true;
            JsonObject index = weed.get("index").getAsJsonObject();
            System.out.println("Weed pollen is in season with a " + index.get("category") + " rating of severity for the area\n");
        }
        else{
            System.out.println("Weed pollen is out of season\n");
        }

        return pollen;
    }

    /**
     * This method uses the googlemaps RESTful API.
     * It takes in a latitude and longitude to find what pharmacies are nearby in a given location.
     * Prints them out instead of returning to main.
     * @param latitude - float value that represents the latitude.
     * @param longitude - float value that represents the longitude.
     * @throws IOException - for HTTP calls.
     * @throws InterruptedException - for sleep.
     */
    public static void getPharmacy(float latitude, float longitude) throws IOException, InterruptedException{
        //exponential retry
        if(phaRetry > 30){
            System.out.println("Couldn't connect at this time, please try again later");
            System.exit(0);
        }
        sleep(phaRetry * 1000);

        //googlemaps api HTTP url
        String stringUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude + "&radius=5000&type=pharmacy&key=AIzaSyA00ozw5jC60mvnmAW9LvWX6lF9scpkSCY";

        URL url = new URL(stringUrl);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        //switch case that determines how the status code is handled
        switch(statusCode/100){
            case 1:
            case 2:
                //Nothing to be done here
                break;
            case 3:
                //this properly redirects the connection to the proper URL
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                System.out.println("Redirecting: " + stringUrl + " to: " + redirectUrl);
                break;
            case 4:
                //this flips the error bit and returns since 400 level codes are bad requests on the server side
                System.out.println("Error visiting this location. Data may not exist, try again with a different city");
            case 5:
                //we retry this instance since there is a temporary issue that may be with the client
                System.out.println("Retrying connection...");
                phaRetry *= 2;
                getPharmacy(latitude, longitude);
                break;
            default:
                break;
        }

        //this section isolates the json that gives the names and locations of pharmacies nearby
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Gson json = new Gson();
        String line;
        String data = "";
        while ((line = reader.readLine()) != null) {
            data += line;
        }
        JsonObject obj = json.fromJson(data, JsonObject.class);
        JsonArray results = obj.get("results").getAsJsonArray();

        //if there is somehow nothing nearby
        if(results.size() == 0){
            System.out.println("No pharmacies in this area's vicinity");
        }
        //if there are many places that are nearby
        else if(results.size() > 2){
            for(int i = 0; i < 3; i++){
                System.out.println("Name: " + results.get(i).getAsJsonObject().get("name"));
                System.out.println("Location: " + results.get(i).getAsJsonObject().get("vicinity") + "\n");
            }
        }
        //if there aren't many places nearby
        else{
            for(int i = 0; i < results.size(); i++){
                System.out.println("Name: " + results.get(i).getAsJsonObject().get("name"));
                System.out.println("Location: " + results.get(i).getAsJsonObject().get("vicinity") + "\n");
            }
        }
    }

    /**
     * This method uses the googlemaps RESTful API.
     * It takes in a latitude and longitude to find what parks are nearby in a given location.
     * Prints them out instead of returning to main.
     * @param latitude - float value that represents the latitude.
     * @param longitude - float value that represents the longitude.
     * @throws IOException - for HTTP calls.
     * @throws InterruptedException - for sleep.
     */
    public static void getActivity(float latitude, float longitude) throws IOException, InterruptedException{
        //exponential retry
        if(actRetry > 30){
            System.out.println("Couldn't connect at this time, please try again later");
            System.exit(0);
        }
        sleep(actRetry * 1000);

        //googlemaps api HTTP url
        String stringUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude + "&radius=5000&type=park&key=AIzaSyA00ozw5jC60mvnmAW9LvWX6lF9scpkSCY";

        URL url = new URL(stringUrl);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        //switch case that determines how the status code is handled
        switch(statusCode/100){
            case 1:
            case 2:
                //Nothing to be done here
                break;
            case 3:
                //this properly redirects the connection to the proper URL
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                System.out.println("Redirecting: " + stringUrl + " to: " + redirectUrl);
                break;
            case 4:
                //this flips the error bit and returns since 400 level codes are bad requests on the server side
                System.out.println("Error visiting this location. Data may not exist, try again with a different city");
            case 5:
                //we retry this instance since there is a temporary issue that may be with the client
                System.out.println("Retrying connection...");
                actRetry *= 2;
                getActivity(latitude, longitude);
                break;
            default:
                break;
        }

        //this section isolates the json that gives the names and locations of parks nearby
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Gson json = new Gson();
        String line;
        String data = "";
        while ((line = reader.readLine()) != null) {
            data += line;
        }
        JsonObject obj = json.fromJson(data, JsonObject.class);
        JsonArray results = obj.get("results").getAsJsonArray();

        //if there is somehow nothing nearby
        if(results.size() == 0){
            System.out.println("No parks in this area's vicinity");
        }
        //if there are many places that are nearby
        else if(results.size() > 2){
            for(int i = 0; i < 3; i++){
                System.out.println("Name: " + results.get(i).getAsJsonObject().get("name"));
                System.out.println("Location: " + results.get(i).getAsJsonObject().get("vicinity") + "\n");
            }
        }
        //if there aren't many places nearby
        else{
            for(int i = 0; i < results.size(); i++){
                System.out.println("Name: " + results.get(i).getAsJsonObject().get("name"));
                System.out.println("Location: " + results.get(i).getAsJsonObject().get("vicinity") + "\n");
            }
        }
    }
}

