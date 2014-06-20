package com.namsor.api.rapidminer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.awt.windows.ThemeReader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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
 * - Extract Gender is an operator to infer the gender from international names.
 * GendRE Genderizer API already covers many languages/cultures/geographies
 * (USA, Europe, Cyrillic Russia/CIS, Hebrew, Arabic/the Arab World, Chinese,
 * ...). Possible usage include: gender studies, public policies monitoring
 * (gender equality, etc.), customer intelligence... The online API is available
 * with a Free, Freemium and Premium model, with the Free model covering ~80% of
 * general needs.
 * 
 * @author ELC201203
 * 
 */
public class ExtractGenderOperator extends Operator {
	private static final Random RND = new Random();

	public static final String API_CHANNEL_SECRET = "api_key";
	public static final String API_CHANNEL_USER = "api_channel";
	private static final String API_IS_FREE_VALUE = "-use free version-";

	public static final String MASHAPE_CHANNEL_USER = "mashape.com";
	public static final String MASHAPE_CHANNEL_REGISTRATION_URL = "https://www.mashape.com/namsor/gendre-infer-gender-from-world-names";
	public static final String MASHAPE_CHANNEL_REGISTRATION_GET_APIKEY = "get_freemium_api_key";
	public static final String MASHAPE_CHANNEL_REGISTRATION_GET_APIKEY_MSG = "Get a Freemium API Key on Mashape.com";

	private static final String ATTRIBUTE_THRESHOLD = "threshold";
	private static final Double ATTRIBUTE_THRESHOLD_DEFAULT = .1d;

	private static final String ATTRIBUTE_META_PREFIX_INPUT = "attribute_";
	private static final String ATTRIBUTE_FN = "first_name";
	private static final String ATTRIBUTE_LN = "last_name";
	private static final String ATTRIBUTE_COUNTRY = "country";
	private static final String ATTRIBUTE_BATCHID = "batch_id";

	private static final String ATTRIBUTE_META_PREFIX_OUTPUT = "result_";
	private static final String ATTRIBUTE_GENDERSCALE = "scale";
	private static final String ATTRIBUTE_GENDER = "gender";
	private static final String ATTRIBUTE_COUNTRY_DEFAULT = "country_default";

	private static final String PARAMETER_USE_COUNTRY = "use_country";

	private static final String INPUTSET_NAME = "example set input";
	private static final String OUTPUTSET_NAME = "example set output";
	private InputPort inputSet = getInputPorts().createPort(INPUTSET_NAME);
	private OutputPort outputSet = getOutputPorts().createPort(OUTPUTSET_NAME);

	private static final String GENDER_VALUE_MALE = "Male";
	private static final String GENDER_VALUE_FEMALE = "Female";
	private static final String GENDER_VALUE_UNKNOWN = "Unknown";

	public ExtractGenderOperator(OperatorDescription description) {
		super(description);
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
								ATTRIBUTE_GENDERSCALE, Ontology.REAL);
						idMD.setValueRange(new Range(-1, +1), SetRelation.EQUAL);
						emd.addAttribute(idMD);
					}
					{
						AttributeMetaData idMD = new AttributeMetaData(
								ATTRIBUTE_GENDER, Ontology.STRING);
						Set<String> valueSet = new HashSet();
						valueSet.add(GENDER_VALUE_FEMALE);
						valueSet.add(GENDER_VALUE_MALE);
						valueSet.add(GENDER_VALUE_UNKNOWN);
						idMD.setValueSet(valueSet, SetRelation.EQUAL);
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
	 * @param someString
	 * @return
	 */
	private static final String cleanup(String someString) {
		if(someString==null) {
			return null;
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
		String iso2AttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_COUNTRY,
				ATTRIBUTE_COUNTRY);
		String batchIdAttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_BATCHID,
				ATTRIBUTE_BATCHID);

		// output attribute names
		String genderAttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_GENDER,
				ATTRIBUTE_GENDER);
		String genderScaleAttributeName = getParameterAsString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_GENDERSCALE,
				ATTRIBUTE_GENDERSCALE);

		Attribute fnAttribute = attributes.get(fnAttributeName);
		if (fnAttribute == null) {
			throw new UserError(this, 111, fnAttributeName);
		}
		Attribute lnAttribute = attributes.get(lnAttributeName);
		if (lnAttribute == null) {
			throw new UserError(this, 111, lnAttributeName);
		}
		Attribute iso2Attribute = attributes.get(iso2AttributeName);
		Attribute batchIdAttribute = attributes.get(batchIdAttributeName);

		String APIKey = getParameterAsString(API_CHANNEL_SECRET);
		String APIChannel = getParameterAsString(API_CHANNEL_USER);
		String countryDefault_ = getParameterAsString(ATTRIBUTE_COUNTRY_DEFAULT);
		String countryDefault = CountryISO.countryIso2(countryDefault_);
		double threshold = getParameterAsDouble(ATTRIBUTE_THRESHOLD);

		Attribute genderScaleAttribute = AttributeFactory.createAttribute(
				genderScaleAttributeName, Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(genderScaleAttribute);
		attributes.addRegular(genderScaleAttribute);

		Attribute genderAttribute = AttributeFactory.createAttribute(
				genderAttributeName, Ontology.STRING);
		exampleSet.getExampleTable().addAttribute(genderAttribute);
		attributes.addRegular(genderAttribute);
		// for progress monitoring
		long startProcessing = System.currentTimeMillis();
		int tobeProcessed = exampleSet.size();
		int countProcessed = 0;
		int pctDone = 0;
		for (Example example : exampleSet) {
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
			String iso2 = null;
			if (iso2Attribute != null) {
				iso2 = example.getValueAsString(iso2Attribute);
				iso2 = cleanup(iso2);
			}
			String batchId = null;
			if (batchIdAttribute != null) {
				batchId = example.getValueAsString(batchIdAttribute);
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
			double genderScale = 0d;
			if (firstName != null && lastName != null
					&& !firstName.trim().isEmpty()
					&& !lastName.trim().isEmpty()) {
				try {
					if (APIKey != null
							&& !APIKey.trim().equals(API_IS_FREE_VALUE)
							&& APIChannel != null
							&& APIChannel.trim().toLowerCase()
									.startsWith(MASHAPE_CHANNEL_USER)) {
						// use Mashape API
						genderScale = GenderAPIClient.getInstance()
								.genderizeMashape(APIKey, firstName, lastName,
										iso2);
						Logger.getLogger(getClass().getName()).log(
								Level.FINE,
								"GendRE API Freemium (Mashape) " + firstName
										+ "/" + lastName + "/" + iso2 + "="
										+ genderScale);
					} else if (APIKey != null
							&& !APIKey.trim().equals(API_IS_FREE_VALUE)
							&& APIChannel != null
							&& !APIChannel.trim().isEmpty() && APIKey != null
							&& !APIKey.trim().isEmpty()) {
						// use Premium API
						genderScale = GenderAPIClient.getInstance()
								.genderizePremium(APIChannel, APIKey, batchId,
										firstName, lastName, iso2);
						Logger.getLogger(getClass().getName()).log(
								Level.FINE,
								"GendRE API Premium " + firstName + "/"
										+ lastName + "/" + iso2 + "="
										+ genderScale);
					} else {
						// if (APIKey == null || APIKey.trim().isEmpty() ||
						// APIKey.trim().equals(API_IS_FREE_VALUE) ) {
						// use Free API
						genderScale = GenderAPIClient.getInstance()
								.genderizeFree(firstName, lastName, iso2);
						Logger.getLogger(getClass().getName()).log(
								Level.FINE,
								"GendRE API Free " + firstName + "/" + lastName
										+ "/" + iso2 + "=" + genderScale);
					}
				} catch (GenderAPIException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							"GenderAPI error : " + e.getMessage(), e);
					throw new UserError(this, e, 108, e.getMessage());
				}
			}
			String gender = "Unknown";
			if (genderScale > threshold) {
				gender = "Female";
			} else if (genderScale < -threshold) {
				gender = "Male";
			}
			example.setValue(genderScaleAttribute, genderScale);
			example.setValue(genderAttribute, gender);
		}
		outputSet.deliver(exampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterTypeAttribute first_name = new ParameterTypeAttribute(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_FN,
				"Input attribute name for First Name (Given Name)", inputSet,
				false, false);
		types.add(first_name);

		ParameterTypeAttribute last_name = new ParameterTypeAttribute(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_LN,
				"Input attribute name for Last Name (Family Name)", inputSet,
				false, false);
		types.add(last_name);

		ParameterTypeAttribute batch_id = new ParameterTypeAttribute(
				ATTRIBUTE_META_PREFIX_INPUT + ATTRIBUTE_BATCHID,
				"Input attribute name for Batch ID", inputSet, true, true);
		types.add(batch_id);

		ParameterTypeString gender_scale = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_GENDERSCALE,
				"Output attribute name for Gender Scale",
				ATTRIBUTE_GENDERSCALE, false);
		gender_scale.setExpert(true);
		types.add(gender_scale);

		ParameterTypeString gender = new ParameterTypeString(
				ATTRIBUTE_META_PREFIX_OUTPUT + ATTRIBUTE_GENDER,
				"Output attribute name for Gender", ATTRIBUTE_GENDER, false);
		gender.setExpert(true);
		types.add(gender);

		ParameterTypeDouble threshold = new ParameterTypeDouble(
				ATTRIBUTE_THRESHOLD,
				"This parameter defines the threshold for considering the gender Unknown. Default: 0.10",
				0, +1, ATTRIBUTE_THRESHOLD_DEFAULT, false);
		threshold.setExpert(true);
		types.add(threshold);

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

		types.add(new ParameterTypeString(
				API_CHANNEL_SECRET,
				"GendRE API is free to use with certain restrictions. For commercial subscribers, please insert the API Key.",
				API_IS_FREE_VALUE, false));

		types.add(new ParameterTypeString(
				API_CHANNEL_USER,
				"GendRE API is free to use with certain restrictions. For commercial subscribers, please insert the API Key domain.",
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
				GendreAPIPreviewCreator.class, previewListener);
		getAPIKey.setExpert(false);
		types.add(getAPIKey);

		return types;
	}

}
