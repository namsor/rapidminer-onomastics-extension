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
import java.util.prefs.Preferences;

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
 * - Parse Name is an operator to infer the likely structure (firstName, lastName order or conversely) of a name.
 * 
 * @author ELC201203
 * 
 */
public class ParseNameOperator extends Operator {
	private static final Random RND = new Random();

	private static final String API_IS_FREE_VALUE = "-get your freemium API Key-";

	private static final String PARAMETER_USE_COUNTRY = "use_country";

	private static final String ATTRIBUTE_META_PREFIX_INPUT = "attribute_";
	private static final String ATTRIBUTE_FULLNAME = "full_name";
	private static final String ATTRIBUTE_COUNTRY = "country";
	private static final String ATTRIBUTE_COUNTRY_DEFAULT = "country_default";
	private static final String ATTRIBUTE_PARSENAME_TIP = "parsename_tip";
	private static final String ATTRIBUTE_PARSENAME_TIPDEFAULT = "-use default-";
	private static final String[] ATTRIBUTE_PARSENAME_TIPS = {
		ATTRIBUTE_PARSENAME_TIPDEFAULT,
		"ORDER_FNLN",  // firstName first
		"ORDER_LNFN",  // lastName first
		"ORDER_FNLN_OR_LNFN", // make best guess name by name
		"ORDER_FNLN_OR_LNFN_ALLSAME" // make best guess and apply to ALL the names (if most names are FNLN, then ALL names will be considered FNLN)
	};

	private static final String ATTRIBUTE_META_PREFIX_OUTPUT = "result_attribute_";
	private static final String ATTRIBUTE_FN = "first_name";
	private static final String ATTRIBUTE_LN = "last_name";
	private static final String ATTRIBUTE_MID = "mid_name";
	private static final String ATTRIBUTE_TITLE = "title";
	private static final String ATTRIBUTE_NAMEFORMAT = "name_format";
	private static final String ATTRIBUTE_SCORE = "score_parse";
	
	private static final String[] STR_ATTRIBUTES = { 
			ATTRIBUTE_FN, 
			ATTRIBUTE_LN, 
			ATTRIBUTE_MID,
			ATTRIBUTE_TITLE,
			ATTRIBUTE_NAMEFORMAT
	};

	private static final String MSG_Output_attribute_name_for = "Output attribute name for "; 
	private static final String INPUTSET_NAME = "example set input";
	private static final String OUTPUTSET_NAME = "example set output";
	private InputPort inputSet = getInputPorts().createPort(INPUTSET_NAME);
	private OutputPort outputSet = getOutputPorts().createPort(OUTPUTSET_NAME);

	private static final int BATCH_REQUEST_SIZE = 1000;
	private static final int CACHE_maxEntriesLocalHeap = 100000;
	private static final String CACHE_name = "parse";
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

	public ParseNameOperator(OperatorDescription description) {
		super(description);
		cache = getOrCreateCache();
		inputSet.addPrecondition(new ExampleSetPrecondition(inputSet,
				new String[] { ATTRIBUTE_FULLNAME }, Ontology.ATTRIBUTE_VALUE));
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
		String fullNameAttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_FULLNAME, ATTRIBUTE_FULLNAME);
		String iso2AttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_COUNTRY,
				ATTRIBUTE_COUNTRY);

		Attribute fullNameAttribute = attributes.get(fullNameAttributeName);
		if (fullNameAttribute == null) {
			throw new UserError(this, 111, fullNameAttributeName);
		}
		Attribute iso2Attribute = attributes.get(iso2AttributeName);

		String APIKey = getParameterAsString(NamSorAPI.API_CHANNEL_SECRET);
		String APIChannel = getParameterAsString(NamSorAPI.API_CHANNEL_USER);

		String countryDefault_ = getParameterAsString(ATTRIBUTE_COUNTRY_DEFAULT);
		String countryDefault = CountryISO.countryIso2(countryDefault_);

		String parseNameTip_ = getParameterAsString(ATTRIBUTE_PARSENAME_TIP);
		if( parseNameTip_.equals(ATTRIBUTE_PARSENAME_TIPDEFAULT)) {
			parseNameTip_=null;
		}
		// output attribute names

		Attribute originScoreAttribute = AttributeFactory.createAttribute(
				getParameterAsString(ATTRIBUTE_META_PREFIX_OUTPUT
						+ ATTRIBUTE_SCORE, ATTRIBUTE_SCORE), Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(originScoreAttribute);
		attributes.addRegular(originScoreAttribute);

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
		ParseAPI api = null;
		if (APIKey != null
				&& !APIKey.trim().equals(API_IS_FREE_VALUE)
				&& APIChannel != null
				&& APIChannel.trim().toLowerCase()
						.startsWith(NamSorAPI.MASHAPE_CHANNEL_USER)) {
			// save pref
			Preferences prefs = Preferences.userRoot().node(NamSorAPI.class.getName());
			prefs.put(NamSorAPI.API_CHANNEL_SECRET, APIKey);
			prefs.put(NamSorAPI.API_CHANNEL_USER, APIChannel);
			// use Mashape API
			throw new UserError(this, "namsor.apikey");
			//api = new RegisteredParseAPIClient(APIKey);
		} else if (APIKey != null && !APIKey.trim().equals(API_IS_FREE_VALUE)
				&& APIChannel != null && !APIChannel.trim().isEmpty()
				&& APIKey != null && !APIKey.trim().isEmpty()) {
			// save pref
			Preferences prefs = Preferences.userRoot().node(NamSorAPI.class.getName());
			prefs.put(NamSorAPI.API_CHANNEL_SECRET, APIKey);
			prefs.put(NamSorAPI.API_CHANNEL_USER, APIChannel);
			// use Premium API
			api = new PureParseAPIClient(APIChannel, APIKey);
		} else {
			// if (APIKey == null || APIKey.trim().isEmpty() ||
			// APIKey.trim().equals(API_IS_FREE_VALUE) ) {
			// API is required
			throw new UserError(this, "namsor.apikey");
		}
		// for progress monitoring
		long startProcessing = System.currentTimeMillis();
		int tobeProcessed = exampleSet.size();
		int countProcessed = 0;
		int pctDone = 0;

		String batchIdDefault = "" + System.currentTimeMillis();
		// support batch mode: this is the buffer
		List<ParseResponse> namesBuffer = new ArrayList(BATCH_REQUEST_SIZE);
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

			String fullName = example.getValueAsString(fullNameAttribute);
			fullName = cleanup(fullName);
			String iso2 = null;
			if (iso2Attribute != null) {
				iso2 = example.getValueAsString(iso2Attribute);
				iso2 = cleanup(iso2);
			}
			if (iso2 != null && iso2.trim().length() == 2) {
				// real value
			} else if (countryDefault != null
					&& countryDefault.trim().length() == 2) {
				iso2 = countryDefault.trim();
			} else {
				// invalid value, set to null
				iso2 = null;
			}
			if (fullName != null 
					&& !fullName.trim().isEmpty()
					) {
				// try cache first
				String key = fullName + "/" + iso2;
				Element element = null;
				// don't use cache is a specific method is specified
				if( parseNameTip_ == null ) {
					element = getCache().get(key);
				}
				if (element != null) {
					ParseResponse origin = (ParseResponse) element
							.getObjectValue();
					example.setValue(originScoreAttribute, origin.getScore());
					example.setValue(strAttribute.get(ATTRIBUTE_FN),
							origin.getFirstName());
					example.setValue(strAttribute.get(ATTRIBUTE_LN),
							origin.getLastName());
					example.setValue(strAttribute.get(ATTRIBUTE_MID),
							origin.getMidName());
					example.setValue(strAttribute.get(ATTRIBUTE_TITLE),
							origin.getTitle());
					example.setValue(strAttribute.get(ATTRIBUTE_NAMEFORMAT),
							origin.getNameFormat());
				} else {
					String reqId = batchIdDefault + "-" + rowId;
					ParseResponse param = new ParseResponse();
					param.setFullName(fullName);
					param.setCountryIso2(iso2);
					param.setId(reqId);
					namesBuffer.add(param);
					bufferMapping.put(reqId, example);
					if (namesBuffer.size() >= BATCH_REQUEST_SIZE) {
						// flush buffer
						parse(api, namesBuffer, bufferMapping, batchIdDefault,
								strAttribute, originScoreAttribute,parseNameTip_, countryDefault);
					}
				}
			}
		}
		// final flush buffer
		parse(api, namesBuffer, bufferMapping, batchIdDefault,
								strAttribute, originScoreAttribute, parseNameTip_, countryDefault);
		outputSet.deliver(exampleSet);
	}


	private static final int MIN_NAMES_TO_USE_BATCH_API = 10;

	private void parse(ParseAPI api, List<ParseResponse> namesBuffer,
			Map<String, Example> bufferMapping, String batchId, 
			Map<String, Attribute> strAttribute,
			Attribute originScoreAttribute, String parseNameTip, String countryDefault)
			throws UserError {
		if (api.allowsBatchAPI()
				&& namesBuffer.size() > MIN_NAMES_TO_USE_BATCH_API) {
			ParseResponse[] a1 = new ParseResponse[namesBuffer.size()];
			ParseResponse[] a2 = (ParseResponse[]) namesBuffer
					.toArray(a1);
			ParseBatchRequest req = new ParseBatchRequest();
			req.setNames(a2);
			if( parseNameTip!=null) {
				req.setNameFormatTip(parseNameTip);
			}
			if( countryDefault!=null && !countryDefault.isEmpty()) {
				req.setCountryIso2Default(countryDefault);
			}
			try {
				ParseBatchRequest resp = api.parseBatch(req);
				for (ParseResponse genderResponse : resp.getNames()) {
					// update cache
					String key = genderResponse.getFullName() + "/"
							+ genderResponse.getCountryIso2();
					getCache().put(new Element(key, genderResponse));

					String reqId = genderResponse.getId();
					Example example = bufferMapping.get(reqId);

					ParseResponse origin = genderResponse;
					example.setValue(originScoreAttribute, origin.getScore());
					example.setValue(strAttribute.get(ATTRIBUTE_FN),
							origin.getFirstName());
					example.setValue(strAttribute.get(ATTRIBUTE_LN),
							origin.getLastName());
					example.setValue(strAttribute.get(ATTRIBUTE_MID),
							origin.getMidName());
					example.setValue(strAttribute.get(ATTRIBUTE_TITLE),
							origin.getTitle());
					example.setValue(strAttribute.get(ATTRIBUTE_NAMEFORMAT),
							origin.getNameFormat());
				}
			} catch (ParseAPIException e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE,
						"OriginAPI error : " + e.getMessage(), e);
				throw new UserError(this, e, 108, e.getMessage());
			}
		} else {
			for (ParseResponse genderResponse : namesBuffer) {
				ParseResponse genderScale = null;
				String reqId = genderResponse.getId();
				try {
					genderScale = api.parse(genderResponse.getFullName(),
							genderResponse.getCountryIso2());
					// update cache
					String key = genderResponse.getFullName() + "/"
							+ genderResponse.getCountryIso2();
					getCache().put(new Element(key, genderScale));

					Logger.getLogger(getClass().getName()).log(
							Level.FINE,
							"ParseAPI " + genderResponse.getFullName() + "/"
									+ genderResponse.getCountryIso2() + " = "
									+ genderScale);
				} catch (ParseAPIException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							"ParseAPI error : " + e.getMessage(), e);
					throw new UserError(this, e, 108, e.getMessage());
				}
				Example example = bufferMapping.get(reqId);
					ParseResponse origin = genderScale;
					example.setValue(originScoreAttribute, origin.getScore());
					example.setValue(strAttribute.get(ATTRIBUTE_FN),
							origin.getFirstName());
					example.setValue(strAttribute.get(ATTRIBUTE_LN),
							origin.getLastName());
					example.setValue(strAttribute.get(ATTRIBUTE_MID),
							origin.getMidName());
					example.setValue(strAttribute.get(ATTRIBUTE_TITLE),
							origin.getTitle());
					example.setValue(strAttribute.get(ATTRIBUTE_NAMEFORMAT),
							origin.getNameFormat());
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
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_FULLNAME,
				MSG_Output_attribute_name_for+"Full Name", inputSet,
				false, false);
		types.add(first_name);
		}
		
		types.add(new ParameterTypeBoolean(PARAMETER_USE_COUNTRY,
				"Indicates if country hints should be used.", false, false));

		ParameterTypeAttribute country = new ParameterTypeAttribute(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_COUNTRY,
				"Input attribute name for Country (2-letters ISO2 code)",
				inputSet, true, // optional
				false);
		country.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_USE_COUNTRY, false, true));
		types.add(country);

		ParameterTypeStringCategory countryDefault = new ParameterTypeStringCategory(
				ATTRIBUTE_COUNTRY_DEFAULT,
				"This parameter to refine the default country to use, it not already specified in data input.",
				CountryISO.countryNames(), CountryISO.COUNTRIES_ALL, false);
		countryDefault.setExpert(false);
		countryDefault
				.registerDependencyCondition(new BooleanParameterCondition(
						this, PARAMETER_USE_COUNTRY, false, true));
		types.add(countryDefault);

		ParameterTypeStringCategory parseNameTip = new ParameterTypeStringCategory(
				ATTRIBUTE_PARSENAME_TIP,
				"This parameter to refine the how lists of names should be handled, assuming all names are in firstName, lastName order; or the opposite; or best guess one by one; or best guess assuming all have the same order.",
				ATTRIBUTE_PARSENAME_TIPS, ATTRIBUTE_PARSENAME_TIPDEFAULT, false);
		parseNameTip.setExpert(false);
		types.add(parseNameTip);
		
		Preferences prefs = Preferences.userRoot().node(NamSorAPI.class.getName());
		String apiChannelSecret = prefs.get(NamSorAPI.API_CHANNEL_SECRET, API_IS_FREE_VALUE);
		String apiChannelUser = prefs.get(NamSorAPI.API_CHANNEL_USER, API_IS_FREE_VALUE);
		
		types.add(new ParameterTypeString(
				NamSorAPI.API_CHANNEL_SECRET,
				"API is freemium require registration, please insert the API Key.",
				apiChannelSecret, false));

		types.add(new ParameterTypeString(
				NamSorAPI.API_CHANNEL_USER,
				"API is freemium require registration, please insert the API Key.",
				apiChannelUser, false));

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
				NamSorAPI.NAMSOR_CHANNEL_REGISTRATION_GET_APIKEY,
				NamSorAPI.NAMSOR_CHANNEL_REGISTRATION_GET_APIKEY_MSG,
				OriginAPIPreviewCreator.class, previewListener);
		getAPIKey.setExpert(false);
		types.add(getAPIKey);
		
		{
		ParameterTypeString score = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_SCORE,
				MSG_Output_attribute_name_for+"Score", ATTRIBUTE_SCORE,
				false);
		score.setExpert(true);
		types.add(score);
		}
		{
		ParameterTypeString countryAlt = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_FN,
				MSG_Output_attribute_name_for+"FirstName", ATTRIBUTE_FN, false);
		countryAlt.setExpert(true);
		types.add(countryAlt);
		}
		{
		ParameterTypeString script = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_LN,
				MSG_Output_attribute_name_for+"LastName", ATTRIBUTE_LN,
				false);
		script.setExpert(true);
		types.add(script);
		}
		{
		ParameterTypeString countryFirstName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_MID,
				MSG_Output_attribute_name_for+"MidName", ATTRIBUTE_MID,
				false);
		countryFirstName.setExpert(true);
		types.add(countryFirstName);
		}
		{
		ParameterTypeString countryLastName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_TITLE,
				MSG_Output_attribute_name_for+"Title", ATTRIBUTE_TITLE,
				false);
		countryLastName.setExpert(true);
		types.add(countryLastName);
		}
		{
		ParameterTypeString scoreFirstName = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_NAMEFORMAT,
				MSG_Output_attribute_name_for+"NameFormat", ATTRIBUTE_NAMEFORMAT,
				false);
		scoreFirstName.setExpert(true);
		types.add(scoreFirstName);
		}
		return types;
	}

	private Cache getCache() {
		return cache;
	}

}
