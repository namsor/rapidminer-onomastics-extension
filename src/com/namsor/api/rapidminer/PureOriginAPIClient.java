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

public class PureOriginAPIClient implements OriginAPI {
	private static final String PRIMARY_API_ADDRESS = NamSorAPI.API_PREFIX
			+ "onomastics/api/json/origin/";
	private static final String PRIMARY_API_ADDRESS_BATCH = NamSorAPI.API_PREFIX
			+ "onomastics/api/json/originList";

	private final String pureGenderAPIAddress;
	private final String pureGenderAPIAddressBatch;

	private static final String ATTR_XChannelSecret = "X-Channel-Secret";
	private static final String ATTR_XChannelUser = "X-Channel-User";
	private static final String ATTR_XClientVersion = "X-Client-Version";

	private final String APIChannel;
	private final String APIKey;

	private Gson gson = new GsonBuilder().create();

	public PureOriginAPIClient(final String APIChannel, final String APIKey) {
		this.APIChannel = APIChannel;
		this.APIKey = APIKey;
		pureGenderAPIAddress = PRIMARY_API_ADDRESS;
		pureGenderAPIAddressBatch = PRIMARY_API_ADDRESS_BATCH;
	}

	public PureOriginAPIClient() {
		this.APIChannel = null;
		this.APIKey = null;
		pureGenderAPIAddress = PRIMARY_API_ADDRESS;
		pureGenderAPIAddressBatch = PRIMARY_API_ADDRESS_BATCH;
	}

	@Override
	public GeoriginResponse origin(String firstName, String lastName)
			throws OriginAPIException {

		if (firstName == null) {
			firstName = "";
		}
		if (lastName == null) {
			lastName = "";
		}

		String url = null;
		StringWriter resp = new StringWriter();
		try {

			url = pureGenderAPIAddress
					+ URLEncoder.encode(firstName.replace('.', ' ').trim(),
							"UTF-8")
					+ "/"
					+ URLEncoder.encode(lastName.replace('.', ' ').trim(),
							"UTF-8");
			URL api = new URL(url);

			HttpURLConnection myURLConnection = (HttpURLConnection) api
					.openConnection();
			myURLConnection.setRequestMethod("GET");
			myURLConnection.setRequestProperty("Accept-Charset", "UTF-8");
			if (APIChannel != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelUser,
						APIChannel);
			}
			if (APIKey != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelSecret, APIKey);
			}
			myURLConnection.setRequestProperty(ATTR_XClientVersion,
					SoftwareVersion.ATTVALUE_ClientAppVersion);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					myURLConnection.getInputStream(), "UTF-8"));
			String inputLine = in.readLine();
			while (inputLine != null) {
				resp.append(inputLine);
				inputLine = in.readLine();
			}
			in.close();
			GeoriginResponse result = gson.fromJson(resp.toString(),
					GeoriginResponse.class);
			return result;
		} catch (Exception e) {
			throw new OriginAPIException(e);
		}
	}

	@Override
	public boolean allowsBatchAPI() {
		return (APIChannel != null && APIKey != null
				&& !APIChannel.trim().isEmpty() && !APIKey.trim().isEmpty());
	}

	@Override
	public GeoriginBatchRequest originBatch(GeoriginBatchRequest req)
			throws OriginAPIException {
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
			if (APIChannel != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelUser,
						APIChannel);
			}
			if (APIKey != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelSecret, APIKey);
			}
			myURLConnection.setRequestProperty(ATTR_XClientVersion,
					SoftwareVersion.ATTVALUE_ClientAppVersion);

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
			GeoriginBatchRequest result = gson.fromJson(resp.toString(),
					GeoriginBatchRequest.class);
			return result;
		} catch (Exception e) {
			throw new OriginAPIException(e);
		}
	}

}
