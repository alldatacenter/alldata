package com.linkedin.feathr.core.configdataprovider;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * A Config Data Provider that obtains config data from config string. The config data from each string is obtained
 * via a {@link Reader} object.
 */
public class StringConfigDataProvider extends BaseConfigDataProvider {
  private static final Logger logger = LogManager.getLogger(StringConfigDataProvider.class);

  private final List<String> _configStringList;

  public StringConfigDataProvider(String configString) {
    this(Collections.singletonList(configString));
  }

  public StringConfigDataProvider(List<String> configStringList) {
    Objects.requireNonNull(configStringList, "List of config strings can't be null");
    for (String configString : configStringList) {
      Objects.requireNonNull(configString, "Config string can't be null");
    }
    _configStringList = configStringList;
  }

  @Override
  public List<Reader> getConfigDataReaders() {
    _readers = _configStringList.stream().map(StringReader::new).map(BufferedReader::new).collect(Collectors.toList());
    logger.debug("Created Reader object(s) for config string(s)");

    return _readers;
  }

  @Override
  public String getConfigDataInfo() {
    String firstConfigString = _configStringList.get(0);
    int endIdx = Math.min(256, firstConfigString.length());
    String substring = firstConfigString.substring(0, endIdx).trim().replace("\n", " ");

    return "Config strings: \"" + substring + "...\"";
  }
}
