package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.utils.Utils;
import com.linkedin.feathr.core.config.producer.sources.RestliConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValueType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.RestliConfig.*;

/**
 * Builds {@link RestliConfig} objects
 */
class RestliConfigBuilder {
  private final static Logger logger = LogManager.getLogger(RestliConfigBuilder.class);

  private RestliConfigBuilder() {
  }

  public static RestliConfig build(String sourceName, Config sourceConfig) {
    String resourceName = sourceConfig.hasPath(RESOURCE_NAME) ? sourceConfig.getString(RESOURCE_NAME)
        : sourceConfig.getString(RESOUCE_NAME); // TODO: we'll fix this.

    Map<String, Object> reqParams = sourceConfig.hasPath(REQ_PARAMS) ? buildReqParams(sourceConfig) : null;

    PathSpec pathSpec = sourceConfig.hasPath(PATH_SPEC) ? buildPathSpec(sourceConfig) : null;

    String keyExpr = null;
    String finder = null;

    if (sourceConfig.hasPath(KEY_EXPR)) {
      keyExpr = sourceConfig.getString(KEY_EXPR);
    } else if (sourceConfig.hasPath(ENTITY_TYPE)) {
      /*
       * TODO: We'll remove entity type
       * "restEntityType" is deprecated. Until we remove it, a restEntityType can be converted to a keyExpr
       * (which is a MVEL expression). For example, if restEntityType: member, the resulting key expression
       * will be: "toUrn(\"member\", key[0])"
       */
      String entityType = sourceConfig.getString(ENTITY_TYPE);
      keyExpr = String.format("toUrn(\"%s\", key[0])", entityType);
    }

    if (sourceConfig.hasPath(FINDER)) {
      finder = sourceConfig.getString(FINDER);
    }

    if (StringUtils.isAllBlank(finder, keyExpr)) {
      throw new ConfigBuilderException("Rest.li config cannot have both blank \"keyExpr\" and \"finder\" fields");
    }

    RestliConfig configObj = new RestliConfig(sourceName, resourceName, keyExpr, reqParams, pathSpec, finder);

    logger.debug("Built RestliConfig object for source " + sourceName);

    return configObj;
  }

  private static Map<String, Object> buildReqParams(Config sourceConfig) {
    Config reqParamsConfig = sourceConfig.getConfig(REQ_PARAMS);
    ConfigObject reqParamsConfigObj = reqParamsConfig.root();
    Set<String> reqParamsKeys = reqParamsConfigObj.keySet();
    logger.debug("reqParamsKeys: " + Utils.string(reqParamsKeys));

    BiConsumer<Map<String, Object>, String> accumulator = (acc, key) -> {
      ConfigValueType configValueType = reqParamsConfig.getValue(key).valueType();

      switch (configValueType) {
        case STRING:
          acc.put(key, reqParamsConfig.getString(key));
          break;

        case OBJECT:
          Config paramConfig = reqParamsConfig.getConfig(key);
          String keyWord = paramConfig.root().keySet().iterator().next();

          switch (keyWord) {
            case JSON:
              ConfigValueType valueType = paramConfig.getValue(JSON).valueType();
              Config config;
              if (valueType == ConfigValueType.OBJECT) {
                config = paramConfig.getConfig(JSON);
              } else {
                /*
                 * Assumed to be string which contains a config, so parse it
                 * Note: this notation should not be allowed, HOCON notation should be used to specify the object.
                 * Due to this, the code has become bloated.
                 */
                config = ConfigFactory.parseString(paramConfig.getString(JSON));
              }
              DataMap dataMap = buildDataMap(config);
              acc.put(key, dataMap);
              break;

            case JSON_ARRAY:
              ConfigValueType jsonArrayValueType = paramConfig.getValue(JSON_ARRAY).valueType();
              Config jsonArrayConfig;
              if (jsonArrayValueType == ConfigValueType.OBJECT) {
                jsonArrayConfig = paramConfig.getConfig(JSON_ARRAY);
              } else {
                /*
                 * Assumed to be string which contains a config, so parse it
                 * Note: this notation should not be allowed, HOCON notation should be used to specify the object.
                 * Due to this, the code has become bloated.
                 */
                jsonArrayConfig = ConfigFactory.parseString(paramConfig.getString(JSON_ARRAY));
              }
              DataList dataList = buildDataList(jsonArrayConfig);
              acc.put(key, dataList);
              break;

            case MVEL_KEY:
              String mvelExpr = paramConfig.getString(MVEL_KEY);
              // when the param is an MVEL expression, store it as a DataMap={"mvel"-> EXPR} instead of just a raw string
              // to differentiate it from the case where it is truly just a static String
              DataMap mvelDataMap = new DataMap();
              mvelDataMap.put(MVEL_KEY, mvelExpr);
              acc.put(key, mvelDataMap);
              break;

            case FILE:
              StringBuilder warnSb = new StringBuilder();
              warnSb.append("Handling of keyword ").append(FILE).append(" in ").append(REQ_PARAMS)
                  .append(" is not yet implemented");
              logger.warn(warnSb.toString());
              break;

            default:
              StringBuilder errSb = new StringBuilder();
              errSb.append("Unsupported key ").append(keyWord).append(". Keys in ").append(REQ_PARAMS)
                  .append(" object must be one of ").append(JSON).append(", ").append(JSON_ARRAY).append(", ")
                  .append(MVEL_KEY).append(", or ").append(FILE);
              throw new ConfigBuilderException(errSb.toString());
          }
          break;

        default:
          throw new ConfigBuilderException("Expected value type 'String' or 'Object'; found " + configValueType);

      }
    };

    return reqParamsKeys.stream().collect(HashMap::new, accumulator, Map::putAll);
  }

  /*
   * jsonConfig refers to the value part of key 'json':
   * json: { // }
   */
  private static DataMap buildDataMap(Config jsonConfig) {
    Set<String> keys = jsonConfig.root().keySet();
    Map<String, String> map = keys.stream().collect(Collectors.toMap(Function.identity(), jsonConfig::getString));
    return new DataMap(map);
  }

  /*
   * jsonArrayConfig refers to the value part of key 'jsonArray':
   * jsonArray: { array: [ // ] }
   */
  private static DataList buildDataList(Config jsonArrayConfig) {
    List<? extends Config> listOfConfigs = jsonArrayConfig.getConfigList(JSON_ARRAY_ARRAY);
    List<DataMap> listOfDataMaps = listOfConfigs.stream().map(config -> {
      Set<String> keys = config.root().keySet();
      // TODO simplify converting from DataList to DataMap
      Map<String, String> dm = keys.stream().collect(Collectors.toMap(Function.identity(), k -> config.getString(k)));
      return new DataMap(dm);
    }).collect(Collectors.toList());

    return new DataList(listOfDataMaps);
  }

  private static PathSpec buildPathSpec(Config sourceConfig) {
    PathSpec pathSpec;
    ConfigValueType configValueType = sourceConfig.getValue(PATH_SPEC).valueType();
    switch (configValueType) {
      case STRING:
        String pathSpecStr = sourceConfig.getString(PATH_SPEC);
        pathSpec = new PathSpec(pathSpecStr);
        break;

      case LIST:
        List<String> pathSpecList = sourceConfig.getStringList(PATH_SPEC);
        String[] pathSpecArray = new String[pathSpecList.size()];
        pathSpecArray = pathSpecList.toArray(pathSpecArray);
        pathSpec = new PathSpec(pathSpecArray);
        break;

      default:
        throw new ConfigBuilderException(PATH_SPEC + " must be of 'String' or 'List', got " + configValueType);
    }

    return pathSpec;
  }

}
