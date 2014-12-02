package com.namsor.api.rapidminer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import sun.awt.windows.ThemeReader;

import com.rapidminer.Process;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.gui.wizards.PreviewCreator;
import com.rapidminer.gui.wizards.PreviewListener;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypePreview;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

/**
 * Onomastics is a set of operators to extract information from personal names.
 * - Extract Origin is an operator to infer the likely country of origin of a
 * name.
 * 
 * @author ELC201203
 * 
 */
public class ExtractOriginOperator extends Operator {
	private static final Random RND = new Random();

	public static final String API_CHANNEL_SECRET = "api_key";
	public static final String API_CHANNEL_USER = "api_channel";
	private static final String API_IS_FREE_VALUE = "-get your freemium API Key-";

	public static final String MASHAPE_CHANNEL_USER = "mashape.com";
	public static final String MASHAPE_CHANNEL_REGISTRATION_URL = "https://www.mashape.com/namsor/origin";
	public static final String MASHAPE_CHANNEL_REGISTRATION_GET_APIKEY = "get_freemium_api_key";
	public static final String MASHAPE_CHANNEL_REGISTRATION_GET_APIKEY_MSG = "Get a Freemium API Key on Mashape.com";

	private static final String ATTRIBUTE_THRESHOLD = "threshold";
	private static final Double ATTRIBUTE_THRESHOLD_DEFAULT = .1d;

	private static final String ATTRIBUTE_META_PREFIX_INPUT = "attribute_";
	private static final String ATTRIBUTE_FN = "first_name";
	private static final String ATTRIBUTE_LN = "last_name";

	private static final String ATTRIBUTE_META_PREFIX_OUTPUT = "result_attribute_";
	private static final String ATTRIBUTE_SCORE = "score";
	private static final String ATTRIBUTE_SCOREROUNDED = "scoreRounded";

	private static final String ATTRIBUTE_SCRIPT = "script";
	private static final String ATTRIBUTE_COUNTRY = "country";
	private static final String ATTRIBUTE_COUNTRYALT = "countryAlt";
	private static final String ATTRIBUTE_COUNTRYFIRSTNAME = "countryFirstName";
	private static final String ATTRIBUTE_COUNTRYLASTNAME = "countryLastName";
	private static final String ATTRIBUTE_SCOREFIRSTNAME = "scoreFirstName";
	private static final String ATTRIBUTE_SCORELASTNAME = "scoreLastName";
	private static final String ATTRIBUTE_SUBREGION = "subRegion";
	private static final String ATTRIBUTE_REGION = "region";
	private static final String ATTRIBUTE_TOPREGION = "topRegion";
	private static final String ATTRIBUTE_COUNTRYNAME = "countryName";
	private static final String[] STR_ATTRIBUTES = { ATTRIBUTE_SCRIPT, // script";
			ATTRIBUTE_COUNTRY, // country";
			ATTRIBUTE_COUNTRYALT, // countryAlt";
			ATTRIBUTE_COUNTRYFIRSTNAME, // countryFirstName";
			ATTRIBUTE_COUNTRYLASTNAME, // countryLastName";
			ATTRIBUTE_SUBREGION, // subRegion";
			ATTRIBUTE_REGION, // region";
			ATTRIBUTE_TOPREGION, // topRegion";
			ATTRIBUTE_COUNTRYNAME, // countryName";
	};

	private static final String MSG_Output_attribute_name_for = "Output attribute name for "; 
	private static final String INPUTSET_NAME = "example set input";
	private static final String OUTPUTSET_NAME = "example set output";
	private InputPort inputSet = getInputPorts().createPort(INPUTSET_NAME);
	private OutputPort outputSet = getOutputPorts().createPort(OUTPUTSET_NAME);

	private static final int BATCH_REQUEST_SIZE = 1000;
	private static final int CACHE_maxEntriesLocalHeap = 100000;
	private static final String CACHE_name = "originCache";
	private final Cache cache;

	private Cache getOrCreateCache() {
		// Create a singleton CacheManager using defaults
		CacheManager manager = CacheManager.create();
		// Create a Cache specifying its configuration
		Cache c = manager.getCache(CACHE_name);
		if (c == null) {
			c = new Cache(new CacheConfiguration(CACHE_name,
					CACHE_maxEntriesLocalHeap)
					.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
					.eternal(true)
					.persistence(
							new PersistenceConfiguration()
									.strategy(Strategy.LOCALTEMPSWAP)));
			manager.addCache(c);
		}
		return c;
	}

	public ExtractOriginOperator(OperatorDescription description) {
		super(description);
		cache = getOrCreateCache();
		inputSet.addPrecondition(new ExampleSetPrecondition(inputSet,
				new String[] { ATTRIBUTE_FN }, Ontology.ATTRIBUTE_VALUE));
		inputSet.addPrecondition(new ExampleSetPrecondition(inputSet,
				new String[] { ATTRIBUTE_LN }, Ontology.ATTRIBUTE_VALUE));
		// getTransformer().addPassThroughRule(inputSet, outputSet);
		getTransformer().addRule(new MDTransformationRule() {
			@Override
			public void transformMD() {
				MetaData metaData = inputSet.getMetaData();
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData
							.clone();
					{
						AttributeMetaData idMD = new AttributeMetaData(
								ATTRIBUTE_SCORE, Ontology.REAL);
						// idMD.setValueRange(new Range(-1, +1),
						// SetRelation.EQUAL);
						emd.addAttribute(idMD);
					}
					{
						AttributeMetaData idMD = new AttributeMetaData(
								ATTRIBUTE_SCOREROUNDED, Ontology.INTEGER);
						emd.addAttribute(idMD);
					}
					for (String attr : STR_ATTRIBUTES) {
						AttributeMetaData idMD = new AttributeMetaData(attr,
								Ontology.STRING);
						emd.addAttribute(idMD);
					}
					outputSet.deliverMD(emd);
				}
			}
		});

	}

	private String getParameterAsString(String key, String defaultValue)
			throws UndefinedParameterError {
		String value = getParameterAsString(key);
		if (value == null || value.trim().isEmpty()) {
			value = defaultValue;
		}
		return value;
	}

	/**
	 * Fix any char that would cause the URL to be incorrect
	 * 
	 * @param someString
	 * @return
	 */
	private static final String cleanup(String someString) {
		if (someString == null) {
			return "";
		}
		return someString.replace('\\', ' ').replace('/', ' ');
	}

	@Override
	public void doWork() throws OperatorException {

		ExampleSet exampleSet = inputSet.getData();
		Attributes attributes = exampleSet.getAttributes();

		// input attribute names
		String fnAttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_FN, ATTRIBUTE_FN);
		String lnAttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_LN, ATTRIBUTE_LN);

		Attribute fnAttribute = attributes.get(fnAttributeName);
		if (fnAttribute == null) {
			throw new UserError(this, 111, fnAttributeName);
		}
		Attribute lnAttribute = attributes.get(lnAttributeName);
		if (lnAttribute == null) {
			throw new UserError(this, 111, lnAttributeName);
		}

		String APIKey = getParameterAsString(API_CHANNEL_SECRET);
		String APIChannel = getParameterAsString(API_CHANNEL_USER);

		// output attribute names

		Attribute originScoreAttribute = AttributeFactory.createAttribute(
				getParameterAsString(ATTRIBUTE_META_PREFIX_OUTPUT
						+ ATTRIBUTE_SCORE, ATTRIBUTE_SCORE), Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(originScoreAttribute);
		attributes.addRegular(originScoreAttribute);

		Attribute originScoreFirstNameAttribute = AttributeFactory
				.createAttribute(
						getParameterAsString(ATTRIBUTE_META_PREFIX_OUTPUT
								+ ATTRIBUTE_SCOREFIRSTNAME,
								ATTRIBUTE_SCOREFIRSTNAME), Ontology.REAL);
		exampleSet.getExampleTable()
				.addAttribute(originScoreFirstNameAttribute);
		attributes.addRegular(originScoreFirstNameAttribute);

		Attribute originScoreLastNameAttribute = AttributeFactory
				.createAttribute(
						getParameterAsString(ATTRIBUTE_META_PREFIX_OUTPUT
								+ ATTRIBUTE_SCORELASTNAME,
								ATTRIBUTE_SCORELASTNAME), Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(originScoreLastNameAttribute);
		attributes.addRegular(originScoreLastNameAttribute);

		Attribute originScoreRoundedAttribute = AttributeFactory
				.createAttribute(
						getParameterAsString(ATTRIBUTE_META_PREFIX_OUTPUT
								+ ATTRIBUTE_SCOREROUNDED,
								ATTRIBUTE_SCOREROUNDED), Ontology.INTEGER);
		exampleSet.getExampleTable().addAttribute(originScoreRoundedAttribute);
		attributes.addRegular(originScoreRoundedAttribute);

		Map<String, Attribute> strAttribute = new HashMap();
		for (String attr : STR_ATTRIBUTES) {
			Attribute originStrAttribute = AttributeFactory.createAttribute(
					getParameterAsString(ATTRIBUTE_META_PREFIX_OUTPUT + attr,
							attr), Ontology.STRING);
			exampleSet.getExampleTable().addAttribute(originStrAttribute);
			attributes.addRegular(originStrAttribute);
			strAttribute.put(attr, originStrAttribute);
		}
		// create API
		OriginAPI api = null;
		if (APIKey != null
				&& !APIKey.trim().equals(API_IS_FREE_VALUE)
				&& APIChannel != null
				&& APIChannel.trim().toLowerCase()
						.startsWith(MASHAPE_CHANNEL_USER)) {
			// use Mashape API
			api = new RegisteredOriginAPIClient(APIKey);
		} else if (APIKey != null && !APIKey.trim().equals(API_IS_FREE_VALUE)
				&& APIChannel != null && !APIChannel.trim().isEmpty()
				&& APIKey != null && !APIKey.trim().isEmpty()) {
			// use Premium API
			api = new PureOriginAPIClient(APIChannel, APIKey);
		} else {
			// if (APIKey == null || APIKey.trim().isEmpty() ||
			// APIKey.trim().equals(API_IS_FREE_VALUE) ) {
			// API is required
			throw new UserError(this, new OriginAPIException("Please, enter an APIKey"), "APIKey");
		}
		// for progress monitoring
		long startProcessing = System.currentTimeMillis();
		int tobeProcessed = exampleSet.size();
		int countProcessed = 0;
		int pctDone = 0;

		String batchIdDefault = "" + System.currentTimeMillis();
		// support batch mode: this is the buffer
		List<GeoriginResponse> namesBuffer = new ArrayList(BATCH_REQUEST_SIZE);
		Map<String, Example> bufferMapping = new HashMap();

		int rowId = 0;
		for (Example example : exampleSet) {
			rowId++;
			int pct = (int) (countProcessed * 1f / tobeProcessed);
			long currentTime = System.currentTimeMillis();
			long ttc = (long) (countProcessed
					/ (currentTime * 1d - startProcessing) * (tobeProcessed - countProcessed));
			if (pct != pctDone || countProcessed == (2 ^ 2)
					|| countProcessed == (2 ^ 3) || countProcessed == (2 ^ 4)
					|| countProcessed == (2 ^ 5) || countProcessed == (2 ^ 6)
					|| countProcessed == (2 ^ 7) || countProcessed == (2 ^ 8)
					|| countProcessed == (2 ^ 9)
					|| countProcessed % (2 ^ 10) == 0) {
				pctDone = pct;
				String logMsg = pctDone + "% done: " + countProcessed + "/"
						+ tobeProcessed + " " + (ttc / 1000)
						+ " seconds remaining.";
				Logger.getLogger(getClass().getName()).log(Level.FINE, logMsg);
			}

			String firstName = example.getValueAsString(fnAttribute);
			firstName = cleanup(firstName);
			String lastName = example.getValueAsString(lnAttribute);
			lastName = cleanup(lastName);

			if (firstName != null && lastName != null
					&& !firstName.trim().isEmpty()
					&& !lastName.trim().isEmpty()) {
				// try cache first
				String key = firstName + "/" + lastName;
				Element element = getCache().get(key);
				if (element != null) {
					GeoriginResponse origin = (GeoriginResponse) element
							.getObjectValue();
					example.setValue(originScoreAttribute, origin.getScore());
					example.setValue(originScoreFirstNameAttribute,
							origin.getScoreFirstName());
					example.setValue(originScoreLastNameAttribute,
							origin.getScoreLastName());
					example.setValue(originScoreRoundedAttribute,
							Math.round(origin.getScore()));
					example.setValue(strAttribute.get("country"),
							origin.getCountry());
					example.setValue(strAttribute.get("countryAlt"),
							origin.getCountryAlt());
					example.setValue(strAttribute.get("countryFirstName"),
							origin.getCountryFirstName());
					example.setValue(strAttribute.get("countryLastName"),
							origin.getCountryLastName());
					example.setValue(strAttribute.get("countryName"),
							origin.getCountryName());
					example.setValue(strAttribute.get("region"),
							origin.getRegion());
					example.setValue(strAttribute.get("script"),
							origin.getScript());
					example.setValue(strAttribute.get("subRegion"),
							origin.getSubRegion());
					example.setValue(strAttribute.get("topRegion"),
							origin.getTopRegion());
				} else {
					String reqId = batchIdDefault + "-" + rowId;
					GeoriginResponse param = new GeoriginResponse();
					param.setFirstName(firstName);
					param.setLastName(lastName);
					param.setId(reqId);
					namesBuffer.add(param);
					bufferMapping.put(reqId, example);
					if (namesBuffer.size() >= BATCH_REQUEST_SIZE) {
						// flush buffer
						origin(api, namesBuffer, bufferMapping, batchIdDefault,
								strAttribute, originScoreAttribute,
								originScoreRoundedAttribute,
								originScoreFirstNameAttribute,
								originScoreLastNameAttribute);
					}
				}
			}
		}
		// final flush buffer
		origin(api, namesBuffer, bufferMapping, batchIdDefault,
								strAttribute, originScoreAttribute,
								originScoreRoundedAttribute,
								originScoreFirstNameAttribute,
								originScoreLastNameAttribute);
		outputSet.deliver(exampleSet);
	}


	private static final int MIN_NAMES_TO_USE_BATCH_API = 10;

	private void origin(OriginAPI api, List<GeoriginResponse> namesBuffer,
			Map<String, Example> bufferMapping, String batchId, 
			Map<String, Attribute> strAttribute,
			Attribute originScoreAttribute,
			Attribute originScoreRoundedAttribute,
			Attribute originScoreFirstNameAttribute,
			Attribute originScoreLastNameAttribute)
			throws UserError {
		if (api.allowsBatchAPI()
				&& namesBuffer.size() > MIN_NAMES_TO_USE_BATCH_API) {
			GeoriginResponse[] a1 = new GeoriginResponse[namesBuffer.size()];
			GeoriginResponse[] a2 = (GeoriginResponse[]) namesBuffer
					.toArray(a1);
			GeoriginBatchRequest req = new GeoriginBatchRequest();
			req.setNames(a2);
			try {
				GeoriginBatchRequest resp = api.originBatch(req);
				for (GeoriginResponse genderResponse : resp.getNames()) {
					// update cache
					String key = genderResponse.getFirstName() + "/"
							+ genderResponse.getLastName();
					getCache().put(new Element(key, genderResponse));

					String reqId = genderResponse.getId();
					Example example = bufferMapping.get(reqId);

					GeoriginResponse origin = genderResponse;
					example.setValue(originScoreAttribute, origin.getScore());
					example.setValue(originScoreFirstNameAttribute,
							origin.getScoreFirstName());
					example.setValue(originScoreLastNameAttribute,
							origin.getScoreLastName());
					example.setValue(originScoreRoundedAttribute,
							Math.round(origin.getScore()));
					example.setValue(strAttribute.get("country"),
							origin.getCountry());
					example.setValue(strAttribute.get("countryAlt"),
							origin.getCountryAlt());
					example.setValue(strAttribute.get("countryFirstName"),
							origin.getCountryFirstName());
					example.setValue(strAttribute.get("countryLastName"),
							origin.getCountryLastName());
					example.setValue(strAttribute.get("countryName"),
							origin.getCountryName());
					example.setValue(strAttribute.get("region"),
							origin.getRegion());
					example.setValue(strAttribute.get("script"),
							origin.getScript());
					example.setValue(strAttribute.get("subRegion"),
							origin.getSubRegion());
					example.setValue(strAttribute.get("topRegion"),
							origin.getTopRegion());
				}
			} catch (OriginAPIException e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE,
						"OriginAPI error : " + e.getMessage(), e);
				throw new UserError(this, e, 108, e.getMessage());
			}
		} else {
			for (GeoriginResponse genderResponse : namesBuffer) {
				GeoriginResponse genderScale = null;
				String reqId = genderResponse.getId();
				try {
					genderScale = api.origin(genderResponse.getFirstName(),
							genderResponse.getLastName());
					// update cache
					String key = genderResponse.getFirstName() + "/"
							+ genderResponse.getLastName();
					getCache().put(new Element(key, genderScale));

					Logger.getLogger(getClass().getName()).log(
							Level.FINE,
							"OriginAPI " + genderResponse.getFirstName() + "/"
									+ genderResponse.getLastName() + " = "
									+ genderScale);
				} catch (OriginAPIException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							"OriginAPI error : " + e.getMessage(), e);
					throw new UserError(this, e, 108, e.getMessage());
				}
				Example example = bufferMapping.get(reqId);
					GeoriginResponse origin = genderScale;
					example.setValue(originScoreAttribute, origin.getScore());
					example.setValue(originScoreFirstNameAttribute,
							origin.getScoreFirstName());
					example.setValue(originScoreLastNameAttribute,
							origin.getScoreLastName());
					example.setValue(originScoreRoundedAttribute,
							Math.round(origin.getScore()));
					example.setValue(strAttribute.get("country"),
							origin.getCountry());
					example.setValue(strAttribute.get("countryAlt"),
							origin.getCountryAlt());
					example.setValue(strAttribute.get("countryFirstName"),
							origin.getCountryFirstName());
					example.setValue(strAttribute.get("countryLastName"),
							origin.getCountryLastName());
					example.setValue(strAttribute.get("countryName"),
							origin.getCountryName());
					example.setValue(strAttribute.get("region"),
							origin.getRegion());
					example.setValue(strAttribute.get("script"),
							origin.getScript());
					example.setValue(strAttribute.get("subRegion"),
							origin.getSubRegion());
					example.setValue(strAttribute.get("topRegion"),
							origin.getTopRegion());
			}
		}
		// clear buffer
		namesBuffer.clear();
		bufferMapping.clear();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		{
		ParameterTypeAttribute first_name = new ParameterTypeAttribute(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_FN,
				MSG_Output_attribute_name_for+"First Name (Given Name)", inputSet,
				false, false);
		types.add(first_name);
		}
		{
		ParameterTypeAttribute last_name = new ParameterTypeAttribute(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_LN,
				MSG_Output_attribute_name_for+"Last Name (Family Name)", inputSet,
				false, false);
		types.add(last_name);
		}
		
		types.add(new ParameterTypeString(
				API_CHANNEL_SECRET,
				"API is freemium require registration, please insert the API Key.",
				API_IS_FREE_VALUE, false));

		types.add(new ParameterTypeString(
				API_CHANNEL_USER,
				"API is freemium require registration, please insert the API Key.",
				API_IS_FREE_VALUE, false));

		PreviewListener previewListener = new PreviewListener() {

			@Override
			public Parameters getParameters() {
				return null;
			}

			@Override
			public ParameterHandler getParameterHandler() {
				return null;
			}

			@Override
			public Process getProcess() {
				return null;
			}

		};

		ParameterTypePreview getAPIKey = new ParameterTypePreview(
				MASHAPE_CHANNEL_REGISTRATION_GET_APIKEY,
				MASHAPE_CHANNEL_REGISTRATION_GET_APIKEY_MSG,
				OriginAPIPreviewCreator.class, previewListener);
		getAPIKey.setExpert(false);
		types.add(getAPIKey);
		
		{
		ParameterTypeString country = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_COUNTRY,
				MSG_Output_attribute_name_for+"Country", ATTRIBUTE_COUNTRY, false);
		country.setExpert(true);
		types.add(country);
		}
		{
		ParameterTypeString score = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_SCORE,
				MSG_Output_attribute_name_for+"Score", ATTRIBUTE_SCORE,
				false);
		score.setExpert(true);
		types.add(score);
		}
		{
		ParameterTypeString scoreRounded = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_SCOREROUNDED,
				MSG_Output_attribute_name_for+"Score (Rounded)", ATTRIBUTE_SCOREROUNDED,
				false);
		scoreRounded.setExpert(true);
		types.add(scoreRounded);
		}
		{
		ParameterTypeString countryAlt = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_COUNTRYALT,
				MSG_Output_attribute_name_for+"Country", ATTRIBUTE_COUNTRYALT, false);
		countryAlt.setExpert(true);
		types.add(countryAlt);
		}
		{
		ParameterTypeString script = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_SCRIPT,
				MSG_Output_attribute_name_for+"Script", ATTRIBUTE_SCRIPT,
				false);
		script.setExpert(true);
		types.add(script);
		}
		{
		ParameterTypeString countryFirstName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_COUNTRYFIRSTNAME,
				MSG_Output_attribute_name_for+"Country (FirstName)", ATTRIBUTE_COUNTRYFIRSTNAME,
				false);
		countryFirstName.setExpert(true);
		types.add(countryFirstName);
		}
		{
		ParameterTypeString countryLastName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_COUNTRYLASTNAME,
				MSG_Output_attribute_name_for+"Country (LastName)", ATTRIBUTE_COUNTRYLASTNAME,
				false);
		countryLastName.setExpert(true);
		types.add(countryLastName);
		}
		{
		ParameterTypeString scoreFirstName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_SCOREFIRSTNAME,
				MSG_Output_attribute_name_for+"Score (FirstName)", ATTRIBUTE_SCOREFIRSTNAME,
				false);
		scoreFirstName.setExpert(true);
		types.add(scoreFirstName);
		}
		{
		ParameterTypeString scoreLastName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_SCORELASTNAME,
				MSG_Output_attribute_name_for+"Score (LastName)", ATTRIBUTE_SCORELASTNAME,
				false);
		scoreLastName.setExpert(true);
		types.add(scoreLastName);
		}
		{
		ParameterTypeString subRegion = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_SUBREGION,
				MSG_Output_attribute_name_for+"Sub-region", ATTRIBUTE_SUBREGION,
				false);
		subRegion.setExpert(true);
		types.add(subRegion);
		}
		{
		ParameterTypeString region = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_REGION,
				MSG_Output_attribute_name_for+"Region", ATTRIBUTE_REGION,
				false);
		region.setExpert(true);
		types.add(region);
		}
		{
		ParameterTypeString countryName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_COUNTRYNAME,
				MSG_Output_attribute_name_for+"Full country name", ATTRIBUTE_COUNTRYNAME,
				false);
		countryName.setExpert(true);
		types.add(countryName);
		}				

		return types;
	}

	private Cache getCache() {
		return cache;
	}

}
