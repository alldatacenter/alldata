package com.webank.wedatasphere.streamis.dss.appconn.constraints;

import org.apache.linkis.common.conf.CommonVars;

/**
 * use Constraints class to manage the constant value
 */
public class Constraints {

    // AppConn name
    public static final String STREAMIS_APPCONN_NAME = CommonVars.apply("wds.dss.appconn.streamis.name", "Streamis").getValue();

    public static final String STREAMIS_SERVER_VERSION = CommonVars.apply("wds.dss.appconn.streamis.server.version", "v1").getValue();

    public static final String API_REQUEST_PREFIX = CommonVars.apply("wds.dss.appconn.streamis.api.request-prefix", "/api/rest_j/"+STREAMIS_SERVER_VERSION+"/streamis/project").getValue();


}
