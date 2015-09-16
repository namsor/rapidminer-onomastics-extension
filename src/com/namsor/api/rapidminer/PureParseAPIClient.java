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
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PureParseAPIClient implements ParseAPI {
	private static final String PRIMARY_API_ADDRESS = NamSorAPI.API_PREFIX
			+ "onomastics/api/json/parse/";
	private static final String PRIMARY_API_ADDRESS_BATCH = NamSorAPI.API_PREFIX
			+ "onomastics/api/json/parseList";

	private final String pureGenderAPIAddress;
	private final String pureGenderAPIAddressBatch;

	private static final String ATTR_XChannelSecret = "X-Channel-Secret";
	private static final String ATTR_XChannelUser = "X-Channel-User";
	private static final String ATTR_XClientVersion = "X-Client-Version";

	private final String APIChannel;
	private final String APIKey;

	private Gson gson = new GsonBuilder().create();

	public PureParseAPIClient(final String APIChannel, final String APIKey) {
		this.APIChannel = APIChannel;
		this.APIKey = APIKey;
		pureGenderAPIAddress = PRIMARY_API_ADDRESS;
		pureGenderAPIAddressBatch = PRIMARY_API_ADDRESS_BATCH;
	}

	public PureParseAPIClient() {
		this.APIChannel = null;
		this.APIKey = null;
		pureGenderAPIAddress = PRIMARY_API_ADDRESS;
		pureGenderAPIAddressBatch = PRIMARY_API_ADDRESS_BATCH;
	}

	@Override
	public ParseResponse parse(String fullName)
			throws ParseAPIException {
		return parse(fullName, null);
	}	
	
	@Override
	public ParseResponse parse(String fullName, String countryIso2)
			throws ParseAPIException {

		if (fullName == null) {
			fullName = "";
		}

		String url = null;
		StringWriter resp = new StringWriter();
		try {
			if (countryIso2 == null || countryIso2.trim().isEmpty() ) {
				url = pureGenderAPIAddress
						+ URLEncoder.encode(fullName.replace('.', ' ').trim(),
								"UTF-8");
			} else {
				url = pureGenderAPIAddress
						+ URLEncoder.encode(fullName.replace('.', ' ').trim(),
								"UTF-8")
						+ "/"
						+ URLEncoder.encode(countryIso2.replace('.', ' ').trim(),
								"UTF-8");
			}
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
			ParseResponse result = gson.fromJson(resp.toString(),
					ParseResponse.class);
			return result;
		} catch (Exception e) {
			throw new ParseAPIException(e);
		}
	}

	@Override
	public boolean allowsBatchAPI() {
		return (APIChannel != null && APIKey != null
				&& !APIChannel.trim().isEmpty() && !APIKey.trim().isEmpty());
	}

	@Override
	public ParseBatchRequest parseBatch(ParseBatchRequest req)
			throws ParseAPIException {
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
			ParseBatchRequest result = gson.fromJson(resp.toString(),
					ParseBatchRequest.class);
			return result;
		} catch (Exception e) {
			String json = gson.toJson(req);
			Logger.getLogger(getClass().getName()).severe("Failed to parseBatch JSON=\n"+json);
			throw new ParseAPIException(e);
		}
	}

}
