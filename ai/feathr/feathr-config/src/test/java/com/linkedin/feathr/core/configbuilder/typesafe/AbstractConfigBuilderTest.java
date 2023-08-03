package com.linkedin.feathr.core.configbuilder.typesafe;

import com.linkedin.feathr.core.config.ConfigObj;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import nl.jqno.equalsverifier.EqualsVerifier;

import static com.linkedin.feathr.core.utils.Utils.*;
import static org.testng.Assert.*;


public abstract class AbstractConfigBuilderTest {

  public void testConfigBuilder(String configStr, BiFunction<String, Config, ConfigObj> configBuilder,
      ConfigObj expConfigObj) {
    ConfigInfo configInfo = getKeyAndConfig(configStr);
    ConfigObj obsConfigObj = configBuilder.apply(configInfo.configName, configInfo.config);
    assertEquals(obsConfigObj, expConfigObj);
  }

  public void testConfigBuilder(String configStr, Function<Config, ConfigObj> configBuilder, ConfigObj expConfigObj) {
    ConfigInfo configInfo = getKeyAndConfig(configStr);
    ConfigObj obsConfigObj = configBuilder.apply(configInfo.config);
    assertEquals(obsConfigObj, expConfigObj);
  }

  @FunctionalInterface
  public interface ConfigListToConfigObjBuilder extends Function<List<? extends Config>, ConfigObj> {}

  public void testConfigBuilder(String configStr, ConfigListToConfigObjBuilder configBuilder, ConfigObj expConfigObj) {
    Config fullConfig = ConfigFactory.parseString(configStr);
    String configName = fullConfig.root().keySet().iterator().next();
    List<? extends Config> configList = fullConfig.getConfigList(quote(configName));

    ConfigObj obsConfigObj = configBuilder.apply(configList);
    assertEquals(obsConfigObj, expConfigObj);
  }

  public ConfigObj buildConfig(String configStr, BiFunction<String, Config, ConfigObj> configBuilder) {
    ConfigInfo configInfo = getKeyAndConfig(configStr);
    return configBuilder.apply(configInfo.configName, configInfo.config);
  }

  public void testEqualsAndHashCode(Class clazz, String... ignoredFields) {
    EqualsVerifier.forClass(clazz)
        .usingGetClass()
        .withIgnoredFields(ignoredFields)
        .verify();
  }

  private class ConfigInfo{
    final String configName;
    final Config config;

    ConfigInfo(String configName, Config config) {
      this.configName = configName;
      this.config = config;
    }
  }

  private ConfigInfo getKeyAndConfig(String configStr) {
    Config fullConfig = ConfigFactory.parseString(configStr);
    String configName = fullConfig.root().keySet().iterator().next();
    Config config = fullConfig.getConfig(quote(configName));
    return new ConfigInfo(configName, config);
  }
}
