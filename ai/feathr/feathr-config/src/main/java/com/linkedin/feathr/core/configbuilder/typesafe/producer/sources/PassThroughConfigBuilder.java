package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.PassThroughConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import javax.lang.model.SourceVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Builds {@link PassThroughConfig} objects by delegating to child builders
 */
class PassThroughConfigBuilder {
  private final static Logger logger = LogManager.getLogger(PassThroughConfigBuilder.class);

  private PassThroughConfigBuilder() {
  }

  public static PassThroughConfig build(String sourceName, Config sourceConfig) {
    String dataModel = sourceConfig.hasPath(PassThroughConfig.DATA_MODEL)
        ? sourceConfig.getString(PassThroughConfig.DATA_MODEL)
        : null;

    if (dataModel != null && !SourceVersion.isName(dataModel)) {
      throw new ConfigBuilderException("Invalid class name for dataModel: " + dataModel);
    }

    PassThroughConfig configObj = new PassThroughConfig(sourceName, dataModel);
    logger.debug("Built PassThroughConfig object for source " + sourceName);

    return configObj;
  }
}
