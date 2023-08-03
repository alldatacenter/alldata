package com.linkedin.feathr.core.configbuilder.typesafe;

import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilder;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.configbuilder.typesafe.consumer.JoinConfigBuilder;
import com.linkedin.feathr.core.configbuilder.typesafe.producer.FeatureDefConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProviderException;
import com.linkedin.feathr.core.configdataprovider.ManifestConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.ReaderConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.StringConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.UrlConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.typesafe.TypesafeConfigValidator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigSyntax;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.feathr.core.config.ConfigType.*;
import static com.linkedin.feathr.core.configvalidator.ValidationStatus.*;


/**
 * Builds Frame Feature Config and Frame Join Config using the Typesafe (Lightbend) Config library.
 *
 * @author djaising
 */
public class TypesafeConfigBuilder implements ConfigBuilder {

  private final static Logger logger = LoggerFactory.getLogger(TypesafeConfigBuilder.class);

  // Used while parsing a config string in HOCON format
  private ConfigParseOptions _parseOptions;

  // Used when rendering the parsed config to JSON string (which is then used in validation)
  private ConfigRenderOptions _renderOptions;


  /**
   * Default constructor. Builds parsing and rendering options.
   */
  public TypesafeConfigBuilder() {
    _parseOptions = ConfigParseOptions.defaults()
        .setSyntax(ConfigSyntax.CONF)   // HOCON document
        .setAllowMissing(false);

    _renderOptions = ConfigRenderOptions.defaults()
        .setComments(false)
        .setOriginComments(false)
        .setFormatted(true)
        .setJson(true);
  }

  /*
   * Methods for building FeatureDef Config
   */


  @Override
  public FeatureDefConfig buildFeatureDefConfig(ConfigDataProvider configDataProvider) {
    Objects.requireNonNull(configDataProvider, "ConfigDataProvider object can't be null");

    FeatureDefConfig configObj;

    try {
      List<Reader> readers = configDataProvider.getConfigDataReaders();
      configObj = doBuildFeatureDefConfig(readers);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building FeatureDefConfig object", e);
    }
    logger.info("Built FeatureDefConfig from " + configDataProvider.getConfigDataInfo());

    return configObj;
  }

  @Override
  public List<FeatureDefConfig> buildFeatureDefConfigList(ConfigDataProvider configDataProvider) {
    Objects.requireNonNull(configDataProvider, "ConfigDataProvider object can't be null");
    List<FeatureDefConfig> featureDefConfigList = new ArrayList<>();

    try {
      List<Reader> readers = configDataProvider.getConfigDataReaders();
      for (Reader reader : readers) {
        List<Reader> singletonReaderList = Collections.singletonList(reader);
        FeatureDefConfig configObj = doBuildFeatureDefConfig(singletonReaderList);
        featureDefConfigList.add(configObj);
      }
    } catch (ConfigBuilderException e) {
      throw new ConfigBuilderException("Error in building FeatureDefConfig object", e);
    }
    if (featureDefConfigList.isEmpty()) {
      logger.warn("No FeatureDefConfigs were built after entering buildFeatureDefConfigList(). ConfigDataProvider Info:"
          + configDataProvider.getConfigDataInfo());
    } else {
      logger.info("Built FeatureDefConfig from " + configDataProvider.getConfigDataInfo());
    }
    return featureDefConfigList;
  }


  @Deprecated
  @Override
  public FeatureDefConfig buildFeatureDefConfigFromUrls(List<URL> urls) {
    /*
     * Delegate the config building to buildFeatureDefConfig(ConfigDataProvider configDataProvider) method
     */
    try (ConfigDataProvider cdp = new UrlConfigDataProvider(urls)) {
      return buildFeatureDefConfig(cdp);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building FeatureDefConfig object", e);
    }
  }

  @Deprecated
  @Override
  public FeatureDefConfig buildFeatureDefConfig(URL url) {
    return buildFeatureDefConfigFromUrls(Collections.singletonList(url));
  }

  @Deprecated
  @Override
  public FeatureDefConfig buildFeatureDefConfig(List<String> resourceNames) {
    /*
     * Delegate the config building to buildFeatureDefConfig(ConfigDataProvider configDataProvider) method
     */
    try (ConfigDataProvider cdp = new ResourceConfigDataProvider(resourceNames)) {
      return buildFeatureDefConfig(cdp);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building FeatureDefConfig object", e);
    }
  }

  @Deprecated
  @Override
  public FeatureDefConfig buildFeatureDefConfig(String resourceName) {
    return buildFeatureDefConfig(Collections.singletonList(resourceName));
  }

  @Deprecated
  @Override
  public FeatureDefConfig buildFeatureDefConfigFromString(String configStr) {
    /*
     * Delegate the config building to buildFeatureDefConfig(ConfigDataProvider configDataProvider) method
     */
    try (ConfigDataProvider cdp = new StringConfigDataProvider(configStr)) {
      return buildFeatureDefConfig(cdp);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building FeatureDefConfig object", e);
    }
  }

  @Deprecated
  @Override
  public FeatureDefConfig buildFeatureDefConfig(Reader reader) {
    /*
     * Delegate the config building to buildFeatureDefConfig(ConfigDataProvider configDataProvider) method
     */
    try (ConfigDataProvider cdp = new ReaderConfigDataProvider(reader)) {
      return buildFeatureDefConfig(cdp);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building FeatureDefConfig object", e);
    }
  }

  /*
   * Builds the FeatureDefConfig object from a manifest file that is specified as a resource.
   * An example file is shown below:
   *
   * manifest: [
   *   {
   *     jar: local
   *     conf: [config/online/feature-prod.conf]
   *   },
   *   {
   *     jar: frame-feature-waterloo-online-1.1.4.jar
   *     conf: [config/online/prod/feature-prod.conf]
   *   }
   * ]
   */
  @Deprecated
  @Override
  public FeatureDefConfig buildFeatureDefConfigFromManifest(String manifestResourceName) {
    /*
     * Delegate the config building to buildFeatureDefConfig(ConfigDataProvider configDataProvider) method
     */
    try (ConfigDataProvider cdp = new ManifestConfigDataProvider(manifestResourceName)) {
      return buildFeatureDefConfig(cdp);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building FeatureDefConfig object from manifest resource "
          + manifestResourceName, e);
    }
  }

  /*
   * Methods for building Frame Join Config
   */

  @Override
  public JoinConfig buildJoinConfig(ConfigDataProvider configDataProvider) {
    Objects.requireNonNull(configDataProvider, "ConfigDataProvider object can't be null");

    JoinConfig configObj;

    try {
      List<Reader> readers = configDataProvider.getConfigDataReaders();
      if (readers.size() != 1) {
        throw new ConfigDataProviderException("Expected number of Join configs = 1, found " + readers.size());
      }
      configObj = doBuildJoinConfig(readers.get(0));
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building JoinConfig object", e);
    }
    logger.info("Built JoinConfig from " + configDataProvider.getConfigDataInfo());

    return configObj;
  }

  @Deprecated
  @Override
  public JoinConfig buildJoinConfig(URL url) {
    /*
     * Delegate the config building to buildJoinConfig(ConfigDataProvider configDataProvider) method
     */
    try (ConfigDataProvider cdp = new UrlConfigDataProvider(url)) {
      return buildJoinConfig(cdp);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building JoinConfig object from URL " + url, e);
    }
  }

  @Deprecated
  @Override
  public JoinConfig buildJoinConfig(String resourceName) {
    /*
     * Delegate the config building to buildJoinConfig(ConfigDataProvider configDataProvider) method
     */
    try (ConfigDataProvider cdp = new ResourceConfigDataProvider(resourceName)) {
      return buildJoinConfig(cdp);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building JoinConfig object from resource " + resourceName, e);
    }
  }

  /*
   * This method is intended to be used internally by other packages, for example, by TypesafeConfigValidator in
   * configvalidator package.
   */
  public Config buildTypesafeConfig(ConfigType configType, ConfigDataProvider configDataProvider) {
    List<Reader> readers = configDataProvider.getConfigDataReaders();

    Config config;

    switch (configType) {
      case FeatureDef:
        config = buildMergedConfig(readers);
        break;

      case Join:
      case Presentation:
        if (readers.size() != 1) {
          throw new ConfigDataProviderException("Expected number of " + configType + " configs = 1, found " + readers.size());
        }
        config = ConfigFactory.parseReader(readers.get(0), _parseOptions);
        break;

      default:
        throw new ConfigBuilderException("Unsupported config type " + configType);
    }
    logger.debug(configType + " config: \n" + config.root().render(_renderOptions.setJson(false)));

    return config;
  }

  private FeatureDefConfig doBuildFeatureDefConfig(List<Reader> readers) {
    Config mergedConfig = buildMergedConfig(readers);
    logger.debug("FeatureDef config: \n" + mergedConfig.root().render(_renderOptions.setJson(false)));

    validate(mergedConfig, FeatureDef);

    return FeatureDefConfigBuilder.build(mergedConfig);
  }

  private Config buildMergedConfig(List<Reader> readers) {
    /*
     * Merge configs into a single config. Objects with the same key are merged to form a single object, duplicate
     * values are merged according to the 'left' config value overriding 'the right' config value. If the keys don't
     * overlap, they are retained in the merged config with their respective values.
     * For more details and examples, see the relevant sections in HOCON spec:
     * Duplicate keys and object merging:
     *    https://github.com/lightbend/config/blob/master/HOCON.md#duplicate-keys-and-object-merging
     * Config object merging and file merging:
     *    https://github.com/lightbend/config/blob/master/HOCON.md#config-object-merging-and-file-merging
     */
    Config emptyConfig = ConfigFactory.empty();

    // TODO: Need to decide when to do substitution resolution. After each file parse, or after the merge.
    return readers.stream()
        .map(r -> ConfigFactory.parseReader(r, _parseOptions))
        .map(Config::resolve)
        .reduce(emptyConfig, Config::withFallback);
  }

  private JoinConfig doBuildJoinConfig(Reader reader) {
    Config config = ConfigFactory.parseReader(reader, _parseOptions);
    logger.debug("Join config: \n" + config.root().render(_renderOptions.setJson(false)));

    validate(config, Join);

    return JoinConfigBuilder.build(config);
  }

  /*
   * Validates the syntax of the config. Delegates the task to a validator.
   */
  private void validate(Config config, ConfigType configType) {
    TypesafeConfigValidator validator = new TypesafeConfigValidator();

    ValidationResult validationResult = validator.validateSyntax(configType, config);
    logger.debug("Performed syntax validation for " + configType + " config. Result: " + validationResult);

    if (validationResult.getValidationStatus() == INVALID) {
      String errMsg = validationResult.getDetails().orElse(configType + " config syntax validation failed");

      if (validationResult.getCause().isPresent()) {
        throw new ConfigBuilderException(errMsg, validationResult.getCause().get());
      } else {
        throw new ConfigBuilderException(errMsg);
      }
    }
  }
}
