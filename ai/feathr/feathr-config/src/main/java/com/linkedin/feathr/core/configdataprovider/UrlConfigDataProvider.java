package com.linkedin.feathr.core.configdataprovider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.utils.Utils.*;


/**
 * A Config Data Provider that obtains config data from URLs. The config data from each URL is obtained via a
 * {@link Reader} object.
 */
public class UrlConfigDataProvider extends BaseConfigDataProvider {
  private static final Logger logger = LogManager.getLogger(UrlConfigDataProvider.class);

  private final List<URL> _urls;

  public UrlConfigDataProvider(URL url) {
    this(Collections.singletonList(url));
  }

  public UrlConfigDataProvider(List<URL> urls) {
    Objects.requireNonNull(urls, "url list can't be null");
    for (URL url : urls) {
      Objects.requireNonNull(url, "url can't be null");
    }

    _urls = urls;
  }

  @Override
  public List<Reader> getConfigDataReaders() {
    for (URL url : _urls) {
      try {
        InputStream in = url.openStream();

        Reader reader = new BufferedReader(new InputStreamReader(in));
        logger.debug("Created Reader object for URL " + url);

        _readers.add(reader);
      } catch (IOException e) {
        throw new ConfigDataProviderException("Error creating a Reader from URL " + url, e);
      }
    }

    return _readers;
  }

  @Override
  public String getConfigDataInfo() {
    return "URLs: " + string(_urls);
  }

  public List<URL> getUrls() {
    return Collections.unmodifiableList(_urls);
  }
}
