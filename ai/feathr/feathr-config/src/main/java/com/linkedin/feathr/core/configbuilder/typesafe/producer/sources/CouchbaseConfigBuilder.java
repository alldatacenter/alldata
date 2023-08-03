package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.CouchbaseConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.CouchbaseConfig.*;


/**
 * Builds {@link CouchbaseConfig} objects
 */
class CouchbaseConfigBuilder {
  private final static Logger logger = LogManager.getLogger(CouchbaseConfigBuilder.class);

  private CouchbaseConfigBuilder() {
  }

  public static CouchbaseConfig build(String sourceName, Config sourceConfig) {
    String bucketName = sourceConfig.getString(BUCKET_NAME);
    String keyExpr = sourceConfig.getString(KEY_EXPR);
    String documentModel = sourceConfig.getString(DOCUMENT_MODEL);

    CouchbaseConfig configObj = new CouchbaseConfig(sourceName, bucketName, keyExpr, documentModel);
    logger.debug("Built CouchbaseConfig object for source " + sourceName);

    return configObj;
  }
}
