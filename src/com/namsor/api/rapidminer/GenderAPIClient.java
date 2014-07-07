package com.namsor.api.rapidminer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

public class GenderAPIClient {

	private static final long batchRequestId = System.currentTimeMillis();
    private static final String ATTR_XMashapeAuthorization="X-Mashape-Authorization";
    private static final String ATTR_XChannelSecret="X-Channel-Secret";
    private static final String ATTR_XChannelUser="X-Channel-User";
	private static final String ATTR_XBatchRequest = "X-BatchRequest-Id";
	private static final String ATTR_XClientVersion = "X-Client-Version";

	private static final String MASHAPE_API_ADDRESS = "https://namsor-gendre.p.mashape.com/gendre/";

	private static final String ATTVALUE_ClientAppVersion = "RapidMinerExt_v0.0.3";

	private static final String API_ADDRESS = "http://api.namsor.com/onomastics/api/gendre/";

	private static final GenderAPIClient instance = new GenderAPIClient();

	public static GenderAPIClient getInstance() {
		return instance;
	}

	public Double genderizeMashape(final String mashapeAPIKey,
			final String firstName, String lastName, String iso2)
			throws GenderAPIException {
		if (firstName == null || firstName.trim().isEmpty() || lastName == null
				|| lastName.trim().isEmpty()) {
			return 0d;
		}
		try {
			String url = null;
			if (iso2 == null) {
				url = MASHAPE_API_ADDRESS
						+ URLEncoder.encode(firstName.replace('.', ' ').trim(),
								"UTF-8")
						+ "/"
						+ URLEncoder.encode(lastName.replace('.', ' ').trim(),
								"UTF-8");
			} else {
				url = MASHAPE_API_ADDRESS
						+ URLEncoder.encode(firstName.replace('.', ' ').trim(),
								"UTF-8")
						+ "/"
						+ URLEncoder.encode(lastName.replace('.', ' ').trim(),
								"UTF-8") + "/" + iso2;
			}
			HttpResponse<String> request = Unirest.get(url)
					.header(ATTR_XMashapeAuthorization, mashapeAPIKey)
					.asString();
			double genderScale = Double.parseDouble(request.getBody());
			return genderScale;
		} catch (Exception e) {
			throw new GenderAPIException(e);
		}
	}

	/**
	 * Predict Gender from a Personal name
	 * 
	 * @param firstName
	 *            The given name
	 * @param lastName
	 *            The family name
	 * @return Double in range -1 (male) .. +1 (female)
	 */
	public Double genderizeFree(final String firstName, String lastName,
			String iso2) throws GenderAPIException {
		return genderize(null, null, ""+batchRequestId, firstName, lastName, iso2);
	}

	/**
	 * Predict Gender from a Personal name
	 * 
	 * @param APIChannel
	 *            The API Channel
	 * @param APIKey
	 *            The API Key
	 * @param firstName
	 *            The given name
	 * @param lastName
	 *            The family name
	 * @return Double in range -1 (male) .. +1 (female)
	 */
	public Double genderizePremium(final String APIChannel, final String APIKey, final String batchId, final String firstName, String lastName,
			String iso2) throws GenderAPIException {
		return genderize(APIChannel, APIKey, batchId, firstName, lastName, iso2);
	}
	
	private Double genderize(final String APIChannel, final String APIKey, final String batchId, final String firstName, String lastName,
				String iso2) throws GenderAPIException {
		
		if (firstName == null || firstName.trim().isEmpty() || lastName == null
				|| lastName.trim().isEmpty()) {
			return 0d;
		}

		String url = null;
		StringWriter resp = new StringWriter();
		try {

			if (iso2 == null) {
				url = API_ADDRESS
						+ URLEncoder.encode(firstName.replace('.', ' ').trim(),
								"UTF-8")
						+ "/"
						+ URLEncoder.encode(lastName.replace('.', ' ').trim(),
								"UTF-8");
			} else {
				url = API_ADDRESS
						+ URLEncoder.encode(firstName.replace('.', ' ').trim(),
								"UTF-8")
						+ "/"
						+ URLEncoder.encode(lastName.replace('.', ' ').trim(),
								"UTF-8") + "/" + iso2;
			}
			URL api = new URL(url);

			HttpURLConnection myURLConnection = (HttpURLConnection) api
					.openConnection();
			myURLConnection.setRequestMethod("GET");
			myURLConnection.setRequestProperty("Accept-Charset", "UTF-8");
			if( APIChannel != null ) {
				myURLConnection.setRequestProperty(ATTR_XChannelUser, APIChannel);				
			}
			if( APIKey != null ) {
				myURLConnection.setRequestProperty(ATTR_XChannelSecret, APIKey);				
			}
			if( batchId == null ) {
				myURLConnection.setRequestProperty(ATTR_XBatchRequest, batchId);				
			} else {
				myURLConnection.setRequestProperty(ATTR_XBatchRequest, ""+batchRequestId);								
			}
			myURLConnection.setRequestProperty(ATTR_XClientVersion,
					ATTVALUE_ClientAppVersion);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					myURLConnection.getInputStream(), "UTF-8"));
			String inputLine = in.readLine();
			while (inputLine != null) {
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
