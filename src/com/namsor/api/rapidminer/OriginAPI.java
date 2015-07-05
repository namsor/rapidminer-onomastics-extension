package com.namsor.api.rapidminer;

public interface OriginAPI extends NamSorAPI {

	GeoriginResponse origin(String firstName, String lastName) throws OriginAPIException;
	
	boolean allowsBatchAPI();

	GeoriginBatchRequest originBatch(GeoriginBatchRequest req) throws OriginAPIException;
	
}
