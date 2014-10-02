package com.namsor.api.rapidminer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RegisteredGenderAPIClient implements GenderAPI {
	
	private static final String PRIMARY_API_ADDRESS = "https://namsor-gendre.p.mashape.com/gendre/";
	private static final String PRIMARY_API_ADDRESS_BATCH = "https://namsor-gendre.p.mashape.com/json/gendreList";

	private static final String SECONDARY_API_ADDRESS = "https://namsor-gendre-bak.p.mashape.com/gendre/";
	private static final String SECONDARY_API_ADDRESS_BATCH = "https://namsor-gendre-bak.p.mashape.com/json/gendreList";

	// primary=true, secondary=false
	private static final boolean PRIMARY_OR_SECONDARY = true;
    private static final String ATTR_XMashapeAuthorization="X-Mashape-Authorization";

	private final String pureGenderAPIAddress;
	private final String pureGenderAPIAddressBatch;

    private final String mashapeAPIKey;

	private Gson gson = new GsonBuilder().create();

	public RegisteredGenderAPIClient(String mashapeAPIKey) {
		this.mashapeAPIKey = mashapeAPIKey;
		if (PRIMARY_OR_SECONDARY) {
			pureGenderAPIAddress = PRIMARY_API_ADDRESS;
			pureGenderAPIAddressBatch = PRIMARY_API_ADDRESS_BATCH;
		} else {
			pureGenderAPIAddress = SECONDARY_API_ADDRESS;
			pureGenderAPIAddressBatch = SECONDARY_API_ADDRESS_BATCH;
		}
	}


	@Override
	public Double genderize(String firstName, String lastName, String iso2,
			String batchId) throws GenderAPIException {

		if (firstName == null || firstName.trim().isEmpty() || lastName == null
				|| lastName.trim().isEmpty()) {
			return 0d;
		}

		String url = null;
		StringWriter resp = new StringWriter();
		try {

			if (iso2 == null) {
				url = pureGenderAPIAddress
						+ URLEncoder.encode(firstName.replace('.', ' ').trim(),
								"UTF-8")
						+ "/"
						+ URLEncoder.encode(lastName.replace('.', ' ').trim(),
								"UTF-8");
			} else {
				url = pureGenderAPIAddress
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
			if (mashapeAPIKey != null) {
				myURLConnection.setRequestProperty(ATTR_XMashapeAuthorization,
						mashapeAPIKey);
			}
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

	@Override
	public boolean allowsBatchAPI() {
		return (mashapeAPIKey != null && !mashapeAPIKey.trim().isEmpty());
	}

	@Override
	public GenderBatchRequest genderizeBatch(String batchId,
			GenderBatchRequest req) throws GenderAPIException {
		try {
			String url = pureGenderAPIAddressBatch;

			URL api = new URL(url);

			HttpURLConnection myURLConnection = (HttpURLConnection) api
					.openConnection();
			myURLConnection.setDoInput(true);
			myURLConnection.setDoOutput(true);
			myURLConnection.setUseCaches(false);

			myURLConnection.setRequestMethod("POST");
			myURLConnection.setRequestProperty("Accept-Charset", "UTF-8");
			myURLConnection.setRequestProperty("Content-Encoding", "gzip");
			// myURLConnection.setConnectTimeout(1000);
			if (mashapeAPIKey != null) {
				myURLConnection.setRequestProperty(ATTR_XMashapeAuthorization,
						mashapeAPIKey);
			}
			OutputStream outStream = myURLConnection.getOutputStream();
			Writer out = new OutputStreamWriter(outStream, "UTF-8");
			gson.toJson(req, out);
			out.close();

			InputStream stream = myURLConnection.getInputStream();
			// long fM = System.currentTimeMillis();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					stream, "UTF-8"));
			String inputLine = in.readLine();
			StringWriter resp = new StringWriter();
			while (inputLine != null) {
				resp.append(inputLine);
				inputLine = in.readLine();
			}
			in.close();
			GenderBatchRequest result = gson.fromJson(resp.toString(),
					GenderBatchRequest.class);
			return result;
		} catch (Exception e) {
			throw new GenderAPIException(e);
		}
	}

}
