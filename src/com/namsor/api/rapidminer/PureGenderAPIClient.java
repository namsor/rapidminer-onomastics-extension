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

public class PureGenderAPIClient implements GenderAPI {
	private static final String PRIMARY_API_ADDRESS = "http://api.namsor.com/onomastics/api/gendre/";
	private static final String PRIMARY_API_ADDRESS_BATCH = "http://api.namsor.com/onomastics/api/json/gendreList";

	private static final String SECONDARY_API_ADDRESS = "http://api2.namsor.com/onomastics/api/gendre/";
	private static final String SECONDARY_API_ADDRESS_BATCH = "http://api2.namsor.com/onomastics/api/json/gendreList";

	// primary=true, secondary=false
	private static final boolean PRIMARY_OR_SECONDARY = true;

	private final String pureGenderAPIAddress;
	private final String pureGenderAPIAddressBatch;

	private static final String ATTR_XChannelSecret = "X-Channel-Secret";
	private static final String ATTR_XChannelUser = "X-Channel-User";
	private static final String ATTR_XBatchRequest = "X-BatchRequest-Id";
	private static final String ATTR_XClientVersion = "X-Client-Version";

	private final String APIChannel;
	private final String APIKey;

	private Gson gson = new GsonBuilder().create();

	public PureGenderAPIClient(final String APIChannel, final String APIKey) {
		this.APIChannel = APIChannel;
		this.APIKey = APIKey;
		if (PRIMARY_OR_SECONDARY) {
			pureGenderAPIAddress = PRIMARY_API_ADDRESS;
			pureGenderAPIAddressBatch = PRIMARY_API_ADDRESS_BATCH;
		} else {
			pureGenderAPIAddress = SECONDARY_API_ADDRESS;
			pureGenderAPIAddressBatch = SECONDARY_API_ADDRESS_BATCH;
		}
	}

	public PureGenderAPIClient() {
		this.APIChannel = null;
		this.APIKey = null;
		if (PRIMARY_OR_SECONDARY) {
			pureGenderAPIAddress = PRIMARY_API_ADDRESS;
			pureGenderAPIAddressBatch = PRIMARY_API_ADDRESS_BATCH;
		} else {
			pureGenderAPIAddress = SECONDARY_API_ADDRESS;
			pureGenderAPIAddressBatch = SECONDARY_API_ADDRESS_BATCH;
		}
	}

	private Double genderize(final String APIChannel, final String APIKey,
			final String batchId, final String firstName, String lastName,
			String iso2) throws GenderAPIException {

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
			if (APIChannel != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelUser,
						APIChannel);
			}
			if (APIKey != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelSecret, APIKey);
			}
			if (batchId != null) {
				myURLConnection.setRequestProperty(ATTR_XBatchRequest, batchId);
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
			Double result = Double.parseDouble(resp.toString());
			return result;
		} catch (Exception e) {
			throw new GenderAPIException(e);
		}
	}

	@Override
	public Double genderize(String firstName, String lastName, String iso2,
			String batchId) throws GenderAPIException {
		return genderize(APIChannel, APIKey, batchId, firstName, lastName, iso2);
	}

	@Override
	public boolean allowsBatchAPI() {
		return (APIChannel != null && APIKey != null
				&& !APIChannel.trim().isEmpty() && !APIKey.trim().isEmpty());
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
			if (APIChannel != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelUser,
						APIChannel);
			}
			if (APIKey != null) {
				myURLConnection.setRequestProperty(ATTR_XChannelSecret, APIKey);
			}
			if (batchId != null) {
				myURLConnection.setRequestProperty(ATTR_XBatchRequest, batchId);
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
			in.close();
			GenderBatchRequest result = gson.fromJson(resp.toString(),
					GenderBatchRequest.class);
			return result;
		} catch (Exception e) {
			throw new GenderAPIException(e);
		}
	}

}
