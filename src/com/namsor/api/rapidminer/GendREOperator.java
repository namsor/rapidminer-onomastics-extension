package com.namsor.api.rapidminer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.awt.windows.ThemeReader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;

/**
 * Onomastics is a set of operators to extract information from personal names.
 * - GendRE Genderizer is an operator to infer the gender from international
 * names. GendRE already covers many languages/cultures/geographies (USA,
 * Europe, Cyrillic Russia/CIS, Hebrew, Arabic/the Arab World, Chinese, ...).
 * Possible usage include: gender studies, public policies monitoring (gender
 * equality, etc.), customer intelligence... The online API is available with a
 * Free, Freemium and Premium model, with the Free model covering ~80% of
 * general needs.
 * 
 * @author ELC201203
 * 
 */
public class GendREOperator extends Operator {
	private static final boolean MOCKUP = false;
	private static final Random RND = new Random();

	private static final String API_CHANNEL_SECRET = "APIKey";
	private static final String API_CHANNEL_USER = "APIChannel";

	private static final String MASHAPE_CHANNEL_USER = "mashape.com";

	private static final String ATTRIBUTE_THRESHOLD = "threshold";
	private static final Double ATTRIBUTE_THRESHOLD_DEFAULT = .1d;
	private static final String ATTRIBUTE_FN = "firstName";
	private static final String ATTRIBUTE_LN = "lastName";
	private static final String ATTRIBUTE_ISO2 = "countryIso2";
	private static final String ATTRIBUTE_BATCHID = "batchId";

	private static final String ATTRIBUTE_GENDERSCALE = "genderScale";
	private static final String ATTRIBUTE_GENDER = "gender";
	private static final String DEFAULT_COUNTRY_ISO2 = "defaultCountryIso2";
	private InputPort inputSet = getInputPorts().createPort("nameSet");
	private OutputPort outputSet = getOutputPorts().createPort(
			"nameAndGenderSet");

	public GendREOperator(OperatorDescription description) {
		super(description);
		inputSet.addPrecondition(new ExampleSetPrecondition(inputSet,
				new String[] { ATTRIBUTE_FN }, Ontology.ATTRIBUTE_VALUE));
		inputSet.addPrecondition(new ExampleSetPrecondition(inputSet,
				new String[] { ATTRIBUTE_LN }, Ontology.ATTRIBUTE_VALUE));
		// getTransformer().addPassThroughRule(inputSet, outputSet);
	}

	@Override
	public void doWork() throws OperatorException {

		ExampleSet exampleSet = inputSet.getData();
		Attributes attributes = exampleSet.getAttributes();
		Attribute fnAttribute = attributes.get(ATTRIBUTE_FN);
		Attribute lnAttribute = attributes.get(ATTRIBUTE_LN);
		Attribute iso2Attribute = attributes.get(ATTRIBUTE_ISO2);
		Attribute batchIdAttribute = attributes.get(ATTRIBUTE_BATCHID);

		String APIKey = getParameterAsString(API_CHANNEL_SECRET);
		String APIChannel = getParameterAsString(API_CHANNEL_USER);
		String defaultISO2 = getParameterAsString(DEFAULT_COUNTRY_ISO2);
		double threshold = getParameterAsDouble(ATTRIBUTE_THRESHOLD);

		Attribute genderScaleAttribute = AttributeFactory.createAttribute(
				ATTRIBUTE_GENDERSCALE, Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(genderScaleAttribute);
		attributes.addRegular(genderScaleAttribute);

		Attribute genderAttribute = AttributeFactory.createAttribute(
				ATTRIBUTE_GENDER, Ontology.STRING);
		exampleSet.getExampleTable().addAttribute(genderAttribute);
		attributes.addRegular(genderAttribute);

		for (Example example : exampleSet) {
			String firstName = example.getValueAsString(fnAttribute);
			String lastName = example.getValueAsString(lnAttribute);
			String iso2 = null;
			if (iso2Attribute != null) {
				iso2 = example.getValueAsString(iso2Attribute);
			}
			String batchId = null;
			if (batchIdAttribute != null) {
				batchId = example.getValueAsString(batchIdAttribute);
			}
			if (iso2 != null && iso2.trim().length() == 2) {
				// real value
			} else if (defaultISO2 != null && defaultISO2.trim().length() == 2) {
				iso2 = defaultISO2.trim();
			} else {
				// invalid value, set to null
				iso2 = null;
			}
			double genderScale = 0d;
			if (MOCKUP) {
				genderScale = RND.nextDouble() * 2 - 1;
			} else {
				try {
					if (APIKey == null || APIKey.trim().isEmpty()) {
						// use Free API
						genderScale = GenderAPIClient.getInstance()
								.genderizeFree(firstName, lastName, iso2);
						Logger.getLogger(getClass().getName()).log(
								Level.FINE,
								"GendRE API Free " + firstName + "/" + lastName
										+ "/" + iso2 + "=" + genderScale);
					} else if (APIChannel != null
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
					} else if (APIChannel != null
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
					}
				} catch (GenderAPIException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							"GenderAPI error : " + e.getMessage(), e);
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
		types.add(new ParameterTypeString(
				API_CHANNEL_SECRET,
				"Insert your API Key here for maximum precision and application support (Free version is used otherwise).",
				""));
		types.add(new ParameterTypeString(API_CHANNEL_USER,
				"Indicate the API Key domain.", ""));
		types.add(new ParameterTypeString(
				DEFAULT_COUNTRY_ISO2,
				"This parameter defines the default Country ISO2 to use as Locale. Leave blank to use the global context (World). ",
				""));
		types.add(new ParameterTypeDouble(
				ATTRIBUTE_THRESHOLD,
				"This parameter defines the threshold for considering the gender Unknown. Default: 0.10",
				0, +1, ATTRIBUTE_THRESHOLD_DEFAULT));
		return types;
	}

}
