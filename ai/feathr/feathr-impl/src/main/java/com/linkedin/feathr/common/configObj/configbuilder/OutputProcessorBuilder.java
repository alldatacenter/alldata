package com.linkedin.feathr.common.configObj.configbuilder;


import com.linkedin.feathr.common.configObj.generation.OutputProcessorConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Output processor config object builder, e.g., HDFS, Redis processor
 */
public class OutputProcessorBuilder {
    private final static Logger logger = LogManager.getLogger(OutputProcessorBuilder.class);
    private static final String PARAMS = "params";
    private static final String NAME = "name";

    private OutputProcessorBuilder() {
    }

    /**
     * build output processor from config object
     */
    public static OutputProcessorConfig build(Config config) {
        String name = config.getString(NAME);
        Config params = config.getConfig(PARAMS);
        logger.trace("Built OperationalConfig object");
        return new OutputProcessorConfig(name, params);
    }

    /**
     * build output processor from all the class as
     * This is typically used to rebuild a new config object from the existing one when there's
     * need to modify/pass in extra parameters
     */
    public static OutputProcessorConfig build(String name, Config params) {
        return new OutputProcessorConfig(name, params);
    }
}
