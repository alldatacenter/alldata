package com.linkedin.feathr.core.configbuilder.typesafe;

import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.sources.EspressoConfig;
import com.linkedin.feathr.core.config.producer.sources.HdfsConfigWithRegularData;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.configbuilder.typesafe.producer.FeatureDefFixture;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.TypesafeFixture.*;
import static com.linkedin.feathr.core.configbuilder.typesafe.producer.FeatureDefFixture.*;
import static org.testng.Assert.*;


public class TypesafeConfigBuilderTest {

  private TypesafeConfigBuilder configBuilder = new TypesafeConfigBuilder();

  @Test(description = "Tests build of FeatureDefConfig object for a syntactically valid config")
  public void testFeatureDefConfig() {
    try {
      FeatureDefConfig obsFeatureDefConfigObj = configBuilder.buildFeatureDefConfigFromString(featureDefConfigStr1);
      assertEquals(obsFeatureDefConfigObj, FeatureDefFixture.expFeatureDefConfigObj1);
    } catch (ConfigBuilderException e) {
      fail("Test failed", e);
    }
  }

  @Test(description = "Include of another config and selective overrides")
  public void includeTest() {
    String expEspressoConfigName = "MemberPreferenceData";
    String expHdfsConfigName = "member_derived_data";

    EspressoConfig expEspressoConfigObj = new EspressoConfig(expEspressoConfigName, "CareersPreferenceDB",
        "MemberPreference", "d2://EI_ESPRESSO_MT2", "key[0]");


    String path = "/eidata/derived/standardization/waterloo/members_std_data/#LATEST";
    HdfsConfigWithRegularData expHdfsConfigObj = new HdfsConfigWithRegularData(expHdfsConfigName, path, false);

    TypesafeConfigBuilder configBuilder = new TypesafeConfigBuilder();
    try {
      FeatureDefConfig config = configBuilder.buildFeatureDefConfig("dir2/features-1-ei.conf");

      assertTrue(config.getSourcesConfig().isPresent());

      Map<String, SourceConfig> sourcesConfig = config.getSourcesConfig().get().getSources();

      assertTrue(sourcesConfig.containsKey(expEspressoConfigName));
      SourceConfig obsEspressoConfigObj = sourcesConfig.get(expEspressoConfigName);
      assertEquals(obsEspressoConfigObj, expEspressoConfigObj);

      assertTrue(sourcesConfig.containsKey(expHdfsConfigName));
      SourceConfig obsHdfsConfigObj = sourcesConfig.get(expHdfsConfigName);
      assertEquals(obsHdfsConfigObj, expHdfsConfigObj);
    } catch (ConfigBuilderException e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests build of FeatureDefConfig object from single resource file")
  public void testFeatureDefConfigFromResource1() {
    try {
      FeatureDefConfig obsFeatureDef1ConfigObj = configBuilder.buildFeatureDefConfig("dir1/features-2-prod.conf");

      assertEquals(obsFeatureDef1ConfigObj, expFeatureDef1ConfigObj);

    } catch (ConfigBuilderException e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests build of FeatureDefConfig object from multiple resource files")
  public void testFeatureDefConfigFromResource2() {
    try {
      List<String> sources = Arrays.asList("dir1/features-3-prod.conf", "dir1/features-2-prod.conf");
      FeatureDefConfig obsFeatureDef2ConfigObj = configBuilder.buildFeatureDefConfig(sources);

      assertEquals(obsFeatureDef2ConfigObj, expFeatureDef2ConfigObj);

    } catch (ConfigBuilderException e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests build of FeatureDefConfig object with single configuration file specified by URL")
  public void testFeatureDefConfigFromUrl1() {
    try {
      URL url = new File("src/test/resources/dir1/features-2-prod.conf").toURI().toURL();
      FeatureDefConfig obsFeatureDef1ConfigObj = configBuilder.buildFeatureDefConfig(url);

      assertEquals(obsFeatureDef1ConfigObj, expFeatureDef1ConfigObj);

    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests build of FeatureDefConfig object with multiple configuration files specified by list of URLs")
  public void testFeatureDefConfigFromUrl2() {
    try {
      URL url1 = new File("src/test/resources/dir1/features-3-prod.conf").toURI().toURL();
      URL url2 = new File("src/test/resources/dir1/features-2-prod.conf").toURI().toURL();
      List<URL> urls = Arrays.asList(url1, url2);
      FeatureDefConfig obsFeatureDef2ConfigObj = configBuilder.buildFeatureDefConfigFromUrls(urls);

      assertEquals(obsFeatureDef2ConfigObj, expFeatureDef2ConfigObj);

    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests build of FeatureDefConfig object from a local config file specified in a manifest")
  public void testFeatureDefConfigFromManifest1() {
    try {
      FeatureDefConfig obsFeatureDef1ConfigObj = configBuilder.buildFeatureDefConfigFromManifest("config/manifest1.conf");

      assertEquals(obsFeatureDef1ConfigObj, expFeatureDef1ConfigObj);
    } catch (ConfigBuilderException e) {
      fail("Error in building config", e);
    }
  }
}
