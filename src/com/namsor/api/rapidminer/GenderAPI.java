package com.namsor.api.rapidminer;

public interface GenderAPI {

	Double genderize(String firstName, String lastName, String iso2, String batchId) throws GenderAPIException;
	
	boolean allowsBatchAPI();

	GenderBatchRequest genderizeBatch(String batchId, GenderBatchRequest req) throws GenderAPIException;
	
}
