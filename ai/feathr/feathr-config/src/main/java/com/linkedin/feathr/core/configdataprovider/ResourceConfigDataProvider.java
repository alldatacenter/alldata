package com.linkedin.feathr.core.configdataprovider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.utils.Utils.*;


/**
 * A Config Data Provider that obtains config data from resource files, that is, config files that are on the
 * classpath. The config data from each resource is obtained via a {@link Reader} object. Optionally we can pass
 * in a custom {@link ClassLoader} object when resources need to be loaded from a specific or isolated namespace.
 */
public class ResourceConfigDataProvider extends BaseConfigDataProvider {
  private static final Logger logger = LogManager.getLogger(ResourceConfigDataProvider.class);

  private final List<String> _resourceNames;
  private final ClassLoader _classLoader;

  public ResourceConfigDataProvider(String resourceName) {
    this(Collections.singletonList(resourceName), null);
  }

  public ResourceConfigDataProvider(String resourceName, ClassLoader classLoader) {
    this(Collections.singletonList(resourceName), classLoader);
  }

  public ResourceConfigDataProvider(List<String> resourceNames) {
    this(resourceNames, null);
  }

  public ResourceConfigDataProvider(List<String> resourceNames, ClassLoader classLoader) {
    Objects.requireNonNull(resourceNames, "List of resource names can't be null");
    for (String resName : resourceNames) {
      Objects.requireNonNull(resName, "Resource name can't be null");
    }
    _resourceNames = resourceNames;
    // Use the invoking thread's context class loader when custom class loader is not provided
    _classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
  }

  @Override
  public List<Reader> getConfigDataReaders() {
    for (String resName : _resourceNames) {
      InputStream in = _classLoader.getResourceAsStream(resName);
      if (in == null) {
        throw new ConfigDataProviderException("Resource " + resName + " can't be obtained as an input stream");
      }

      Reader reader = new BufferedReader(new InputStreamReader(in));
      logger.debug("Created Reader object for resource " + resName);

      _readers.add(reader);
    }

    return _readers;
  }

  @Override
  public String getConfigDataInfo() {
    return "Resources: " + string(_resourceNames) + " Classloader: " + _classLoader;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceConfigDataProvider that = (ResourceConfigDataProvider) o;
    return _resourceNames.equals(that._resourceNames) && _classLoader.equals(that._classLoader);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_resourceNames, _classLoader);
  }
}
