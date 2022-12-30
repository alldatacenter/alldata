/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest;

import io.debezium.configserver.model.FrontendConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("")
public class FrontendConfigResource {

    public static final String PROPERTY_UI_MODE = "ui.mode";
    public static final String PROPERTY_DEPLOYMENT_MODE = "deployment.mode";

    public static final String FRONTEND_CONFIG_ENDPOINT = "/config.js";

    public static final String DEFAULT_UI_MODE = FrontendConfig.UI_MODE_PROD;
    public static final String DEFAULT_DEPLOYMENT_MODE = FrontendConfig.DEPLOYMENT_MODE_DEFAULT;

    @ConfigProperty(name = PROPERTY_UI_MODE, defaultValue = DEFAULT_UI_MODE)
    String uiMode;

    @ConfigProperty(name = PROPERTY_DEPLOYMENT_MODE, defaultValue = DEFAULT_DEPLOYMENT_MODE)
    String deploymentMode;

    @Path(FRONTEND_CONFIG_ENDPOINT)
    @GET
    @Produces("application/javascript")
    public String getFrontendConfig() {
        Jsonb jsonb = JsonbBuilder.create();
        var config = new FrontendConfig(
                FrontendConfig.UIMode.getModeForValue(uiMode),
                FrontendConfig.DeploymentMode.getModeForValue(deploymentMode));
        var jsonConfig = jsonb.toJson(config);
        return "window.UI_CONFIG=" + jsonConfig + ";";
    }

}
