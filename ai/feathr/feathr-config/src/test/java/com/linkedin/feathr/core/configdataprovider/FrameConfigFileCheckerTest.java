package com.linkedin.feathr.core.configdataprovider;

import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.configbuilder.typesafe.FrameConfigFileChecker;
import java.net.URL;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Unit tests for {@link FrameConfigFileChecker}
 */
public class FrameConfigFileCheckerTest {
  private static ClassLoader _classLoader;

  @BeforeClass
  public static void init() {
    _classLoader = Thread.currentThread().getContextClassLoader();
  }

  @Test(description = "Test that a txt file should throw exception.", expectedExceptions = ConfigBuilderException.class)
  public void testTxtFile() {
    URL url = _classLoader.getResource("Foo.txt");

    boolean configFile = FrameConfigFileChecker.isConfigFile(url);
    assertTrue(configFile);
  }

  @Test(description = "An invalid Frame feature config file should return false.")
  public void testInvalidConfigFile() {
    URL url = _classLoader.getResource("PresentationsSchemaTestCases.conf");

    boolean configFile = FrameConfigFileChecker.isConfigFile(url);
    assertFalse(configFile);
  }

  @Test(description = "An valid Frame config file with invalid syntax should return true.")
  public void testValidConfigFileWithInvalidSyntax() {
    URL url = _classLoader.getResource("validFrameConfigWithInvalidSyntax.conf");

    boolean configFile = FrameConfigFileChecker.isConfigFile(url);
    assertTrue(configFile);
  }
}
