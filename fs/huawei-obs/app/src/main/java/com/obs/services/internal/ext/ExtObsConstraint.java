package com.obs.services.internal.ext;

public class ExtObsConstraint {

    public static final boolean DEFAULT_RETRY_ON_CONNECTION_FAILURE_IN_OKHTTP = false;
    
    public static final int DEFAULT_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION = -1;
    
    public static final String IS_RETRY_ON_CONNECTION_FAILURE_IN_OKHTTP = 
            "httpclient.is-retry-on-connection-failure-in-okhttp";
    
    public static final String HTTP_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION = 
            "httpclient.max-retry-on-unexpected-end-exception";
}
