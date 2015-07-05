package com.namsor.api.rapidminer;

public interface NamSorAPI {
	String API_CHANNEL_SECRET = "api_key";
	String API_CHANNEL_USER = "api_channel";

	String MASHAPE_CHANNEL_USER = "mashape.com";
	
	String NAMSOR_CHANNEL_USER = "namsor.com";
	String NAMSOR_CHANNEL_REGISTRATION_URL = "https://api.namsor.com/";
	String NAMSOR_CHANNEL_REGISTRATION_GET_APIKEY = "get_freemium_api_key";
	String NAMSOR_CHANNEL_REGISTRATION_GET_APIKEY_MSG = "Get a Freemium API Key on NamSor.com";

	String API_MSG = "Get an API Key on "+NAMSOR_CHANNEL_USER+" then set "+
			API_CHANNEL_SECRET+"=<your api key> and "+
			API_CHANNEL_USER+"=<your api channel/user>";

	boolean TEST_MODE = false;
	String API_PREFIX = (TEST_MODE?"http://localhost:8084/":"https://api.namsor.com/");
}
