package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.PinotConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.PinotConfig.*;

/**
 * Builds {@link PinotConfig} objects
 */
public class PinotConfigBuilder {
  private final static Logger logger = LogManager.getLogger(PinotConfigBuilder.class);
  private final static String QUERY_ARGUMENT_PLACEHOLDER = "?";

  private PinotConfigBuilder() {
  }

  public static PinotConfig build(String sourceName, Config sourceConfig) {
    // first validate the sourceConfig
    validate(sourceConfig);

    // construct the PinotConfig object
    String resourceName = sourceConfig.getString(RESOURCE_NAME);
    String queryTemplate = sourceConfig.getString(QUERY_TEMPLATE);
    String[] queryArguments = sourceConfig.getStringList(QUERY_ARGUMENTS).toArray(new String[]{});
    String[] queryKeyColumns = sourceConfig.getStringList(QUERY_KEY_COLUMNS).toArray(new String[]{});
    PinotConfig configObj = new PinotConfig(sourceName, resourceName, queryTemplate, queryArguments, queryKeyColumns);
    logger.debug("Built PinotConfig object for source " + sourceName);
    return configObj;
  }

  /**
   * Validate the following:
   *  1. the column names specified in queryKeyColumns need to be unique
   *  2. the count of argument placeholder("?") in queryTemplate needs to match the size of queryArguments
   *  3. the count of key based queryArguments needs to match the size of queryKeyColumns
   *  4. "?" in queryTemplate needs to be always wrapped inside an IN clause if the argument is key based
   * If validation failed, throw ConfigBuilderException.
   *
   * @param sourceConfig {@link Config}
   */
  private static void validate(Config sourceConfig) {
    List<String> queryKeyColumnList = sourceConfig.getStringList(QUERY_KEY_COLUMNS);
    if (new HashSet(queryKeyColumnList).size() != queryKeyColumnList.size()) {
      throw new ConfigBuilderException(
          String.format("Column name in queryKeyColumns [%s] need to be unique", queryKeyColumnList));
    }
    String[] queryKeyColumns = queryKeyColumnList.toArray(new String[]{});

    String queryTemplate = sourceConfig.getString(QUERY_TEMPLATE);
    String[] queryArguments = sourceConfig.getStringList(QUERY_ARGUMENTS).toArray(new String[]{});
    // the count of argument placeholder ("?") in queryTemplate needs to match the size of queryArguments
    int placeHolderCnt = StringUtils.countMatches(queryTemplate, QUERY_ARGUMENT_PLACEHOLDER);
    if (placeHolderCnt != queryArguments.length) {
      throw new ConfigBuilderException(
          String.format("Arguments count does not match between [%s] and [%s]", queryTemplate, queryArguments));
    }

    //the count of key based queryArguments needs to match the size of queryKeyColumns
    int keyBasedArgCnt = Arrays.stream(queryArguments).filter(arg -> isArgValFromKey(arg)).toArray().length;
    if (keyBasedArgCnt != queryKeyColumns.length) {
      throw new ConfigBuilderException(
          String.format("Key based arguments count does not match between [%s] and [%s]", queryArguments,
              queryKeyColumns));
    }

    // iterate through individual key based argument, and make sure the corresponding "?" in the query template is
    // wrapped inside an IN clause.
    Pattern p = Pattern.compile("\\b(?i)(in\\s*\\(\\s*\\?\\s*\\))");
    Matcher matcher = p.matcher(queryTemplate);
    int keyColumnPlaceHolderCnt = 0;
    while (matcher.find()) {
      keyColumnPlaceHolderCnt++;
    }

    //"?" in queryTemplate needs to be always wrapped inside an IN clause if the argument is key based
    if (keyColumnPlaceHolderCnt != queryKeyColumns.length) {
      throw new ConfigBuilderException(
          String.format("Please make sure the key based placeholders are always wrapped inside an IN clause [%s] [%s]", queryArguments,
              queryKeyColumns));
    }
  }

  /**
   * Check if the argument expression is key based
   * @param argExpr the argument expression
   * @return if the argument expression is key based
   */
  private static boolean isArgValFromKey(String argExpr) {
    return Pattern.compile(".*key\\[\\d.*\\].*").matcher(argExpr).find();
  }
}