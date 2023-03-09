package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.PinotConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.utils.Utils.*;


public class PinotConfigBuilderTest {
  static final String pinotSourceName = "pinotTestSource";
  static final String resourceName = "recentMemberActionsPinotQuery";
  static final String queryTemplate = "SELECT verb, object, verbAttributes, timeStampSec FROM RecentMemberActions WHERE actorId IN (?)";
  static final String[] queryArguments = new String[]{"key[0]"};
  static final String[] queryKeyColumns = new String[]{"actorId"};

  static final PinotConfig expectedPinotConfig = new PinotConfig(pinotSourceName, resourceName, queryTemplate, queryArguments, queryKeyColumns);

  static final String goodPinotSourceConfigStr =
      String.join("\n", "pinotTestSource {",
          "  type: PINOT",
          "  resourceName : \"recentMemberActionsPinotQuery\"",
          "  queryTemplate : \"SELECT verb, object, verbAttributes, timeStampSec FROM RecentMemberActions WHERE actorId IN (?)\"",
          "  queryArguments : [\"key[0]\"]",
          "  queryKeyColumns: [\"actorId\"]",
          "}");

  // placeholder for key expression is not wrapped inside IN clause
  static final String badPinotSourceConfigStr1 =
      String.join("\n", "pinotTestSource {",
          "  type: PINOT",
          "  resourceName : \"recentMemberActionsPinotQuery\"",
          "  queryTemplate : \"SELECT verb, object, verbAttributes, timeStampSec FROM RecentMemberActions WHERE actorId = ?\"",
          "  queryArguments : [\"key[0]\"]",
          "  queryKeyColumns: [\"actorId\"]",
          "}");

  // queryArgument count does not match the place holder count in queryTemplate
  static final String badPinotSourceConfigStr2 =
      String.join("\n", "pinotTestSource {",
          "  type: PINOT",
          "  resourceName : \"recentMemberActionsPinotQuery\"",
          "  queryTemplate : \"SELECT verb, object, verbAttributes, timeStampSec FROM RecentMemberActions WHERE actorId IN (?)\"",
          "  queryArguments : [\"key[0]\", \"key[1]\"]",
          "  queryKeyColumns: [\"actorId\"]",
          "}");

  // column names in queryKeyColumns are not unique
  static final String badPinotSourceConfigStr3 =
      String.join("\n", "pinotTestSource {",
          "  type: PINOT",
          "  resourceName : \"recentMemberActionsPinotQuery\"",
          "  queryTemplate : \"SELECT verb, object, verbAttributes, timeStampSec FROM RecentMemberActions WHERE actorId IN (?) AND object IN (?)\"",
          "  queryArguments : [\"key[0]\", \"key[1]\"]",
          "  queryKeyColumns: [\"actorId\", \"actorId\"]",
          "}");

  @DataProvider()
  public Object[][] dataProviderPinotConfigStr() {
    return new Object[][]{
        {badPinotSourceConfigStr1},
        {badPinotSourceConfigStr2},
        {badPinotSourceConfigStr3}
    };
  }

  @Test
  public void pinotGoodConfigTest() {
    Config fullConfig = ConfigFactory.parseString(goodPinotSourceConfigStr);
    String configName = fullConfig.root().keySet().iterator().next();
    Config config = fullConfig.getConfig(quote(configName));

    Assert.assertEquals(PinotConfigBuilder.build("pinotTestSource", config), expectedPinotConfig);
  }

  @Test(description = "Tests Pinot config validation", dataProvider = "dataProviderPinotConfigStr",
      expectedExceptions = ConfigBuilderException.class)
  public void pinotConfigTest(String sourceConfigStr) {
    Config fullConfig = ConfigFactory.parseString(sourceConfigStr);
    String configName = fullConfig.root().keySet().iterator().next();
    Config config = fullConfig.getConfig(quote(configName));
    PinotConfigBuilder.build("pinotTestSource", config);
  }
}