package com.linkedin.feathr.core.configbuilder;

import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configbuilder.typesafe.TypesafeConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import java.io.Reader;
import java.net.URL;
import java.util.List;


/**
 * Interface for building {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} and
 * {@link com.linkedin.feathr.core.config.consumer.JoinConfig JoinConfig}. Instance of a class implementing this
 * interface can be obtained from the static factory method.
 *
 * @author djaising
 */
public interface ConfigBuilder {

  /**
   * Factory method for getting an instance of ConfigBuilder
   * @return ConfigBuilder object
   */
  static ConfigBuilder get() {
    return new TypesafeConfigBuilder();
  }

  /**
   * Builds a {@link FeatureDefConfig} by specifying a {@link ConfigDataProvider} that provides FeatureDef config data
   * @param provider ConfigDataProvider
   * @return FeatureDefConfig
   * @throws ConfigBuilderException
   */
  FeatureDefConfig buildFeatureDefConfig(ConfigDataProvider provider);

  /**
   * Builds several {@link FeatureDefConfig}s by specifying a {@link ConfigDataProvider} that provides FeatureDef config
   * data. This method will not merge {@link FeatureDefConfig}s shared across different configs. Instead, it will construct
   * individual configs for each resource provided within the {@link ConfigDataProvider}.
   * @param provider ConfigDataProvider
   * @return {@link List<FeatureDefConfig>}
   * @throws ConfigBuilderException
   */
  List<FeatureDefConfig> buildFeatureDefConfigList(ConfigDataProvider provider);

  /**
   * Builds a {@link JoinConfig} by specifying a {@link ConfigDataProvider} that provides Join config data
   * @param provider ConfigDataProvider
   * @return JoinConfig
   * @throws ConfigBuilderException
   */
  JoinConfig buildJoinConfig(ConfigDataProvider provider);

  /*
   * Deprecated methods for building Frame FeatureDef Config
   */

  /**
   * Builds a single Frame FeatureDef Config from a list of configuration files referenced by URLs.
   *
   * @param urls List of {@link java.net.URL URLs} for configuration files
   * @return {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildFeatureDefConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.UrlConfigDataProvider UrlConfigDataProvider} can be used as a
   * {@link ConfigDataProvider}
   */
  @Deprecated
  FeatureDefConfig buildFeatureDefConfigFromUrls(List<URL> urls);

  /**
   * Builds a Frame FeatureDef Config from a configuration file referenced by URL.
   *
   * @param url {@link java.net.URL URL} for the config file
   * @return {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildFeatureDefConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.UrlConfigDataProvider UrlConfigDataProvider} can be used as a
   * {@link ConfigDataProvider}
   */
  @Deprecated
  FeatureDefConfig buildFeatureDefConfig(URL url);

  /**
   * Builds a single Frame FeatureDef Config from a list of configuration files on the classpath.
   * @param resourceNames Names of the config files
   * @return {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildFeatureDefConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider ResourceConfigDataProvider} can be
   * used as a {@link ConfigDataProvider}
   */
  @Deprecated
  FeatureDefConfig buildFeatureDefConfig(List<String> resourceNames);

  /**
   * Builds a Frame FeatureDef Config from a configuration file on the classpath
   * @param resourceName Name of the config file on the classpath
   * @return {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildFeatureDefConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider ResourceConfigDataProvider} can be
   * used as a {@link ConfigDataProvider}
   */
  @Deprecated
  FeatureDefConfig buildFeatureDefConfig(String resourceName);

  /**
   * Builds a Frame FeatureDef Config from a configuration string
   * @param configStr configuration expressed in a string
   * @return {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildFeatureDefConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.StringConfigDataProvider StringConfigDataProvider}
   * can be used as a {@link ConfigDataProvider}
   */
  @Deprecated
  FeatureDefConfig buildFeatureDefConfigFromString(String configStr);

  /**
   * Builds a Frame FeatureDef Config from a java.io.Reader
   * @param in A java.io.Reader instance
   * @return {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildFeatureDefConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.ReaderConfigDataProvider ReaderConfigDataProvider}
   * can be used as a {@link ConfigDataProvider}
   */
  @Deprecated
  FeatureDefConfig buildFeatureDefConfig(Reader in);

  /**
   * Builds a Frame FeatureDef Config from a config manifest specified as a resource
   * @param manifestResourceName
   * @return {@link com.linkedin.feathr.core.config.producer.FeatureDefConfig FeatureDefConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildFeatureDefConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.ManifestConfigDataProvider ManifestConfigDataProvider}
   * can be used as a {@link ConfigDataProvider}
   */
  @Deprecated
  FeatureDefConfig buildFeatureDefConfigFromManifest(String manifestResourceName);


  /*
   * Deprecated methods for building Frame Join Config
   */

  /**
   * Build a Join Config from a configuration accessed via a URL
   * @param url A java.net.URL
   * @return {@link com.linkedin.feathr.core.config.consumer.JoinConfig JoinConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildJoinConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.UrlConfigDataProvider UrlConfigDataProvider} can be used as
   * a {@link ConfigDataProvider}
   */
  @Deprecated
  JoinConfig buildJoinConfig(URL url);

  /**
   * Build a Join Config from a configuration file on the classpath
   * @param resourceName Name of the configuration file expressed as a resource
   * @return {@link com.linkedin.feathr.core.config.consumer.JoinConfig JoinConfig} config object
   * @throws ConfigBuilderException
   * @deprecated Use {@link #buildJoinConfig(ConfigDataProvider)} where
   * {@link com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider ResourceConfigDataProvider} can be
   * used as a {@link ConfigDataProvider}
   */
  @Deprecated
  JoinConfig buildJoinConfig(String resourceName);
}

