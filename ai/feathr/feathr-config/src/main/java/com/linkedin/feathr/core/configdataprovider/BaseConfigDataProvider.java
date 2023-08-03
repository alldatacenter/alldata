package com.linkedin.feathr.core.configdataprovider;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * A base class for {@link ConfigDataProvider} that concrete classes should extend rather than implementing
 * ConfigDataProvider directly. It implements the {@link java.io.Closeable#close()} method that concrete classes typically
 * shouldn't have to worry about.
 */
public abstract class BaseConfigDataProvider implements ConfigDataProvider {
  private static final Logger logger = LogManager.getLogger(BaseConfigDataProvider.class);

  protected List<Reader> _readers;

  public BaseConfigDataProvider() {
    _readers = new ArrayList<>();
  }

  @Override
  public void close() {
    try {
      for (Reader reader : _readers) {
        reader.close();
      }
    } catch (IOException e) {
      logger.warn("Unable to close a reader");
    }
    logger.debug("Closed " + _readers.size() + " readers");

    _readers.clear();
  }
}
