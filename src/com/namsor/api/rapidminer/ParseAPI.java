package com.namsor.api.rapidminer;

public interface ParseAPI extends NamSorAPI {

	ParseResponse parse(String fullName) throws ParseAPIException;
	
	ParseResponse parse(String fullName, String countryIso2) throws ParseAPIException;
	
	boolean allowsBatchAPI();

	ParseBatchRequest parseBatch(ParseBatchRequest req) throws ParseAPIException;
	
}
