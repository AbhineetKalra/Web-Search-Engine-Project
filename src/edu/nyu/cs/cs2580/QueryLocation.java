package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is [
 *          "new york city"], the presence of the phrase "new york city" must be
 *          recorded here and be used in indexing and ranking.
 */
public class QueryLocation extends Query {

    public Vector<String> phrase_tokens = new Vector<String>();
    public Vector<String> location_tokens = new Vector<String>();
    public Vector<String> corrected_tokens = new Vector<String>();
    public double locLatitude;
    public double locLongitude;
    public String incorrectQuery;
    public String location = "your location";
    public String _sortType = "DISTANCE";

    public QueryLocation(String query) {
		super(query);
    }

    public QueryLocation(String query, String lat, String lng, String srtType) {
		super(query);
		_sortType = srtType;
		try {
			locLatitude = Double.parseDouble(lat);
			locLongitude = Double.parseDouble(lng);
		} catch (Exception ex) {
			ex.printStackTrace();
		}		
    }

    @Override
    public void processQuery() throws IOException {
		if (_query == null) {
		    return;
		}

		Parser parse = new Parser();
	
		Scanner s1 = new Scanner(parse.Stem(_query));
		while (s1.hasNext()) {
		    String token = s1.next();
		    _tokens.add(token);
		}
		s1.close();
	
		AutoCorrect corrector = new AutoCorrect();
		Scanner s2 = new Scanner(parse.Stem(_query));
		boolean gotCorrected = false;
		while (s2.hasNext()) {
		    String token = s2.next();
		    String correctedToken = corrector.correctString(token);
		    //System.out.println("Corrected -->" + correctedToken);
		    if(!correctedToken.equalsIgnoreCase(token)){
		    	gotCorrected = true;
		    }
		    corrected_tokens.add(correctedToken);
		}
		s2.close();

		if(gotCorrected){
			Vector<String> temp = new Vector<String>();
			//temp = corrected_tokens;
			//corrected_tokens = _tokens;
			//_tokens = temp;
			incorrectQuery = _query;
			//_query = corrected_tokens.toString();
			_query = String.join(" ", corrected_tokens);
		}else{
			incorrectQuery = "";
		}

		

		Pattern p = Pattern.compile("\"([^\"]*)\"");
		Matcher m = p.matcher(_query);
		while (m.find()) {
		    phrase_tokens.add(parse.Stem(m.group(1)));
		}
	
		int containsLocationSpecifier = 0;
		String locToken = "";
		String[] locationSpecifier = {"near","in","around"};
		for(int i =0; i < locationSpecifier.length; i++)
	    {
	        if(_query.toLowerCase().contains(" " + locationSpecifier[i] + " "))
	        {
	        	containsLocationSpecifier = 1;
	        	locToken = locationSpecifier[i];
	        }
	    }
		if(containsLocationSpecifier == 1){
			String token = _query.substring(_query.toLowerCase().lastIndexOf(" "+locToken + " ") + locToken.length() + 2);
			location_tokens.add(token.trim());
			_query = _query.substring(0,_query.toLowerCase().lastIndexOf(" " + locToken + " "));
			location = String.join(" ",location_tokens);
			
		}else{
			//Tagger nerLocation = new Tagger();
			//location_tokens = nerLocation.nerTagger(_query);
			
			Scanner locFile = new Scanner(new File("data/locations.txt"));
			Set<String> locations = new HashSet<>();
			while (locFile.hasNext()) {
				locations.add(locFile.nextLine().trim().toLowerCase());
			}
			System.out.println("Total locations Read : " + locations.size());
			// Assuming user usually puts location is first or last
			if(_tokens.size() > 1){
				Set<String> foundLocations = new HashSet<String>();
				for(int i= 1; i < _tokens.size();i++){
					for(int j = 0; j < _tokens.size() && i + j < _tokens.size();j++){
						String miniToken = "";
						for(int k = j; k <= (i + j);k++){
							miniToken += _tokens.get(k) + " ";
						}
						miniToken = miniToken.trim().toLowerCase();
						if(locations.contains(miniToken)){
							System.out.println("Found location : " + miniToken);
							//location_tokens.add(miniToken.trim());
							foundLocations.add(miniToken);
							//_query = _query.replace(miniToken, "").trim();
						}
					}
				}
				
				// Add first and last token to add borderline cases
				if(locations.contains(_tokens.get(0))){
					foundLocations.add(_tokens.get(0));
				}
				if(locations.contains(_tokens.get(_tokens.size() - 1))){
					foundLocations.add(_tokens.get(_tokens.size() - 1));
				}
				
				if(foundLocations.size() > 0){
					String mostLikelyLocation = "";
					int maxLength = 0, len = 0;
					for(String loc : foundLocations){
						len = loc.split(" ").length;
						if(len > maxLength){
							mostLikelyLocation = loc;
							maxLength = len;
						}
					}
					location_tokens.add(mostLikelyLocation.trim());
					location = String.join(" ",location_tokens);
					_query = _query.replace(mostLikelyLocation, "").trim();
					if(_query.isEmpty()){
						_query = location;
					}
				}
			}
			
		}
		
		
		// refresh tokens
		_tokens = new Vector<String>();
		s1 = new Scanner(parse.Stem(_query));
		while (s1.hasNext()) {
		    String token = s1.next();
		    _tokens.add(token);
		}
		s1.close();
		// Temp
		System.out.println("Corrected Query-->" + _query);
		System.out.println("Corrected Tokens-->" + _tokens);
		System.out.println("Incorrect Query-->" + incorrectQuery);
		geolocation();
		
		parse = null;
    }
    
    public static boolean stringContainsItemFromList(String inputString, String[] items)
    {
        for(int i =0; i < items.length; i++)
        {
            if(inputString.contains(items[i]))
            {
                return true;
            }
        }
        return false;
    }


    public void geolocation() throws IOException {

	if (location_tokens.size() > 0) {
	    // https://maps.googleapis.com/maps/api/geocode/json?address=santa%20clara%20california&key=AIzaSyDw8jrrP1-l4ctHt2PnKT1ohiIpcS_CoRY
	    StringBuilder googleURL = new StringBuilder();
	    googleURL.append(
		    "https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyDw8jrrP1-l4ctHt2PnKT1ohiIpcS_CoRY&address=");
	    googleURL.append(location_tokens.get(0).replace(" ","%20"));
	    for (int i = 1; i < location_tokens.size(); i++) {
	    	googleURL.append("%20");
	    	googleURL.append(location_tokens.get(i).replace(" ","%20"));
	    }
	    System.out.println(googleURL);

	    URL objURL = new URL(googleURL.toString());
	    HttpURLConnection con = (HttpURLConnection) objURL.openConnection();

	    con.setRequestMethod("GET");
	    con.setRequestProperty("User-Agent",
		    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");

	    // int responseCode = con.getResponseCode();

	    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    String inputLine;
	    StringBuffer response = new StringBuffer();

	    while ((inputLine = in.readLine()) != null) {
		response.append(inputLine);
	    }
	    in.close();

	    // System.out.println(response.toString());

	    JSONParser jsonParser = new JSONParser();
	    Object obj;
	    try {
			obj = jsonParser.parse(response.toString());
			JSONObject jsonObject = (JSONObject) obj;
			JSONArray jsonArray = (JSONArray) jsonObject.get("results");
			JSONObject obj1 = (JSONObject) jsonArray.get(0);
			JSONObject obj2 = (JSONObject) obj1.get("geometry");
			JSONObject obj3 = (JSONObject) obj2.get("location");
			locLongitude = (Double) obj3.get("lng");
			locLatitude = (Double) obj3.get("lat");
			// System.out.print(locLatitude + " " + locLongitude);
	    } catch (ParseException e) {
	    	e.printStackTrace();
	    }
	} else {
	    // TODO when get current location of user
	}
    }

    public static void main(String args[]) throws IOException {
	QueryLocation qLocation = new QueryLocation("Santa Clara, California");
	qLocation.processQuery();
	qLocation.geolocation();
    }
}
