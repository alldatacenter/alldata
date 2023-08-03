package com.linkedin.feathr.core.configdataprovider;

import com.linkedin.feathr.core.configbuilder.ConfigBuilder;
import java.io.Closeable;
import java.io.Reader;
import java.util.List;


/**
 * ConfigDataProvider abstracts aways the source of config data which may come from, for example, a resource, or a URL,
 * or as a String. Doing so allows {@link ConfigBuilder ConfigBuilder} API to
 * have a narrow surface area. Further, it also allows clients to plug in their own custom ConfigDataProviders.
 *
 * Example usage:
 * <pre>{@code
 * ConfigBuilder configBuilder = ConfigBuilder.get();
 *
 * try (ConfigDataProvider cdp = new ResourceConfigDataProvider("config/offline/myFeatures.conf")) {
 *  FeatureDef configObj = configBuilder.buildFeatureDefConfig(cdp);
 * } catch (Exception e) {
 *   // process exception
 * }
 * }</pre>
 */
public interface ConfigDataProvider extends Closeable {
  /**
   * Return the config data as a list of {@link Reader} objects. Clients should ideally provide
   * {@link java.io.BufferedReader BufferedReader} objects.
   * @return List of Readers
   */
  List<Reader> getConfigDataReaders();

  /**
   * Provides some information about config data. This information is used in logging and debugging. For example, a
   * {@link UrlConfigDataProvider} will provide a list of URLs from which the config data is obtained.
   * @return A String representing config data
   */
  String getConfigDataInfo();
}
