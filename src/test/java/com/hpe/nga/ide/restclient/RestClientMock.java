package com.hpe.nga.ide.restclient;

public class RestClientMock extends RestClient {
	// ONLY FOR TEST
	public void disconnect(){
		super.disconnect();
		authorized = true;
		cookiesManager.getCookieStore().removeAll();
	}
}
