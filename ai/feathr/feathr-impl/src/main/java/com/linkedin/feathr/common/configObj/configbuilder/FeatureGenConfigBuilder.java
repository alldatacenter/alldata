package com.linkedin.feathr.common.configObj.configbuilder;

import com.linkedin.feathr.common.configObj.generation.FeatureGenConfig;
import com.linkedin.feathr.common.configObj.generation.OperationalConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


/**
 * Feature generation config builder
 */
public class FeatureGenConfigBuilder {
    private final static Logger logger = LogManager.getLogger(FeatureGenConfigBuilder.class);
    private final static String OPERATIONAL = "operational";
    private final static String FEATURES = "features";

    private FeatureGenConfigBuilder() {
    }

    /**
     * config represents the object part in:
     * {@code operational : { ... } }
     */
    public static FeatureGenConfig build(Config config) {
        OperationalConfig operationalConfig = OperationalConfigBuilder.build(config.getConfig(OPERATIONAL));
        List<String>  features = config.getStringList(FEATURES);
        FeatureGenConfig featureGenConfig = new FeatureGenConfig(operationalConfig, features);
        logger.trace("Built FeatureGenConfig object");
        return featureGenConfig;
    }
}
