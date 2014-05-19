package com.namsor.api.rapidminer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class GenderAPIClient {
	
	private static final long batchRequestId = System.currentTimeMillis();
	private static final String ATTR_XBatchRequest = "X-BatchRequest-Id"; 
	private static final String ATTR_XClientVersion = "X-Client-Version"; 

	private static final String ATTVALUE_ClientAppVersion = "RapidMinerExt_v0.0.1"; 
	
    private static final String API_ADDRESS = "http://api.onomatic.com/onomastics/api/gendre/";
    //private static final String API_ADDRESS = "http://localhost:8084/onomastics/api/gendre/";

    private static final GenderAPIClient instance = new GenderAPIClient();
    
    public static GenderAPIClient getInstance() {
    	return instance;
    }
    
    /**
     * Predict Gender from a Personal name
     *
     * @param firstName The given name
     * @param lastName The family name
     * @return Double in range -1 (male) .. +1 (female)
     */
    public Double genderize(final String firstName, String lastName, String iso2) throws GenderAPIException {
        if( firstName == null || firstName.trim().isEmpty() || 
                lastName == null || lastName.trim().isEmpty()
                ) {
            return 0d;
        }
        
        String url = null;
        StringWriter resp = new StringWriter();
        try {
            
            if (iso2 == null) {
                url = API_ADDRESS
                        + URLEncoder.encode(firstName.replace('.', ' ').trim(), "UTF-8") + "/"
                        + URLEncoder.encode(lastName.replace('.', ' ').trim(), "UTF-8");
            } else {
                url = API_ADDRESS
                        + URLEncoder.encode(firstName.replace('.', ' ').trim(), "UTF-8") + "/"
                        + URLEncoder.encode(lastName.replace('.', ' ').trim(), "UTF-8") + "/" + iso2;
            }
        	URL api = new URL(url);

            HttpURLConnection myURLConnection = (HttpURLConnection)api.openConnection();
            myURLConnection.setRequestMethod("GET");
            myURLConnection.setRequestProperty("Accept-Charset", "UTF-8");
            myURLConnection.setRequestProperty(ATTR_XBatchRequest, ""+batchRequestId);
            myURLConnection.setRequestProperty(ATTR_XClientVersion, ATTVALUE_ClientAppVersion);
            
            BufferedReader in = new BufferedReader(
            		new InputStreamReader(myURLConnection.getInputStream(),"UTF-8"));
            String inputLine = in.readLine();
            while ( inputLine != null) {
            	resp.append(inputLine);
            	inputLine = in.readLine();
            }
            in.close();        	
            Double result = Double.parseDouble(resp.toString());
            return result;
        } catch (Exception e) {
        	throw new GenderAPIException(e);
        }
    }
}
