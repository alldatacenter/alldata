package com.linkedin.feathr.core.config.producer.common;

import com.linkedin.feathr.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;

/**
 * The util class to extract key list.
 */
public class KeyListExtractor {
  private static final KeyListExtractor INSTANCE = new KeyListExtractor();
  private static final String KEY_PATH = "MOCK_KEY_EXPR_PATH";
  private static final String HOCON_PREFIX = "{ ";
  private static final String HOCON_SUFFIX = " }";
  private static final String HOCON_DELIM = " : ";

  public static KeyListExtractor getInstance() {
    return INSTANCE;
  }

  private KeyListExtractor() {
    // singleton constructor
  }

  /**
   * This function extract a List of key String from HOCON representation of key field in Frame config.
   * @param keyExpression key expression in HOCON format
   */
  public List<String> extractFromHocon(String keyExpression) {
    // keyExpression is in HOCON ConfigValue format, which is not yet a valid HOCON Config string that can be parsed
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(HOCON_PREFIX).append(KEY_PATH).append(HOCON_DELIM).append(keyExpression).append(HOCON_SUFFIX);
    String hoconFullString = stringBuilder.toString();
    Config config = ConfigFactory.parseString(hoconFullString);
    return ConfigUtils.getStringList(config, KEY_PATH);
  }
}
