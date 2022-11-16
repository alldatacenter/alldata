/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import io.debezium.config.EnumeratedValue;
import io.debezium.configserver.rest.FrontendConfigResource;

import javax.json.bind.annotation.JsonbProperty;

public class FrontendConfig {

    public static final String UI_MODE_DEV = "dev";
    public static final String UI_MODE_PROD = "prod";

    public static final String DEPLOYMENT_MODE_DEFAULT = "default";
    public static final String DEPLOYMENT_MODE_DISABLE_VALIDATION = "validation_disabled";

    @JsonbProperty("mode")
    public final String uiMode;

    @JsonbProperty(FrontendConfigResource.PROPERTY_DEPLOYMENT_MODE)
    public final String deploymentMode;

    public final FrontendConfigArtifacts artifacts;

    public enum UIMode implements EnumeratedValue {
        DEV(UI_MODE_DEV),
        PROD(UI_MODE_PROD);

        private final String value;

        UIMode(String value) {
            this.value = value.toLowerCase();
        }

        public static UIMode getModeForValue(String value) {
            return UIMode.valueOf(value.toUpperCase());
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public enum DeploymentMode implements EnumeratedValue {
        DEFAULT(DEPLOYMENT_MODE_DEFAULT),
        VALIDATION_DISABLED(DEPLOYMENT_MODE_DISABLE_VALIDATION);

        private final String value;

        DeploymentMode(String value) {
            this.value = value.toLowerCase();
        }

        public static DeploymentMode getModeForValue(String value) {
            switch (value.toLowerCase()) {
                case DEPLOYMENT_MODE_DEFAULT:
                    return DeploymentMode.DEFAULT;
                case DEPLOYMENT_MODE_DISABLE_VALIDATION:
                    return DeploymentMode.VALIDATION_DISABLED;
                default:
                    throw new RuntimeException("Invalid deployment mode selected: \"" + value
                            + "\". Choose one of: " + DEPLOYMENT_MODE_DEFAULT + ", " + DEPLOYMENT_MODE_DISABLE_VALIDATION);
            }
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public FrontendConfig(UIMode uiMode, DeploymentMode deploymentMode) {
        this("rest", uiMode, deploymentMode);
    }

    public FrontendConfig(String artifactType, UIMode uiMode, DeploymentMode deploymentMode) {
        this.artifacts = new FrontendConfigArtifacts(artifactType);
        this.uiMode = uiMode.getValue();
        this.deploymentMode = deploymentMode.getValue();
    }

}
