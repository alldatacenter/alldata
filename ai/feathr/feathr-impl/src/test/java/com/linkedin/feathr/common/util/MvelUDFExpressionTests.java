package com.linkedin.feathr.common.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.linkedin.feathr.common.AlienMvelContextUDFs;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Unit tests for {@link MvelContextUDFs} using expressions
 */
public class MvelUDFExpressionTests extends TestNGSuite {

  private static final ParserConfiguration PARSER_CONFIG = new ParserConfiguration();
  private ParserContext parserContext;

  @BeforeClass
  public void setup() {
    MvelContextUDFs.registerUDFs(MvelContextUDFs.class, PARSER_CONFIG);
    MvelContextUDFs.registerUDFs(AlienMvelContextUDFs.class, PARSER_CONFIG);
    parserContext = new ParserContext(PARSER_CONFIG);
  }

  @Test
  public void testToUpperCaseExt() {
    String getTopTerm = "toUpperCaseExt('hello')";
    Serializable compiledExpression = MVEL.compileExpression(getTopTerm, parserContext);
    String res = (String) (MVEL.executeExpression(compiledExpression, ""));
    Assert.assertEquals(res, "HELLO");
  }

  @Test
  public void testGetTopTerm() {
    String getTopTerm = "getTopTerm([\"two\":2.0, \"one\":1.0])";
    Serializable compiledExpression = MVEL.compileExpression(getTopTerm, parserContext);
    String res = (String) (MVEL.executeExpression(compiledExpression, ""));
    Assert.assertEquals(res, "two");
  }

  @Test
  public void testGetTopTermNullInput() {
    String getTopTerm = "getTopTerm(null)";
    Serializable compiledExpression = MVEL.compileExpression(getTopTerm, parserContext);
    String res = (String) (MVEL.executeExpression(compiledExpression, ""));
    Assert.assertNull(res);
  }

  @Test
  public void testGetTopKTerms() {
    String getTopKTerm = "getTopKTerms([\"three\":3.0, \"two\":2.0, \"one\":1.0], 2)";
    Serializable compiledExpression = MVEL.compileExpression(getTopKTerm, parserContext);
    List<String> res = (List<String>) (MVEL.executeExpression(compiledExpression, ""));
    Assert.assertEquals(res, new ArrayList<String>() {{
      add("three");
      add("two");
    }});
  }

  @Test
  public void testGetTopKTermsNegative() {
    String getTopKTerm = "getTopKTerms([\"three\":3.0, \"two\":2.0, \"one\":1.0], -2)";
    Serializable compiledExpression = MVEL.compileExpression(getTopKTerm, parserContext);
    List<String> res = (List<String>) (MVEL.executeExpression(compiledExpression, ""));
    Assert.assertEquals(res, new ArrayList<String>() {{
      add("one");
      add("two");
    }});
  }

  @Test
  public void testGetTopKTermsZero() {
    String getTopKTerm = "getTopKTerms([\"three\":3.0, \"two\":2.0, \"one\":1.0], 0)";
    Serializable compiledExpression = MVEL.compileExpression(getTopKTerm, parserContext);
    List<String> res = (List<String>) (MVEL.executeExpression(compiledExpression, ""));
    Assert.assertEquals(res, new ArrayList<String>());
  }

  @DataProvider
  public Object[][] toNumericCases() {
    return new Object[][]{
        new Object[]{ "toNumeric(10.0f);", 10.0f },
        new Object[]{ "toNumeric(1000);", 1000.0f },
        new Object[]{ "toNumeric(\"5\");", 5.0f}, // numeric strings should be accepted
        new Object[]{ "toNumeric(\"100.50d\");", 100.50f}, // numeric strings should be accepted
        new Object[]{ "toNumeric({\"100.50d\": 1.0f});", 100.50f}, // raw numeric term vectors are accepted
        new Object[]{ "import com.linkedin.feathr.common.FeatureValue; toNumeric(FeatureValue.createNumeric(100f));", 100f },
    };
  }
  @Test(dataProvider = "toNumericCases")
  public void testToNumeric(String mvelExpression, Object expectedOutput) {
    Object result = MVEL.executeExpression(MVEL.compileExpression(mvelExpression, parserContext), "");
    Assert.assertEquals(result, expectedOutput);
  }

  @DataProvider
  public Object[][] toNumericErrorCases() {
    return new Object[][]{
        new Object[]{ "toNumeric(\"Not A Number\");"}, // invalid number strings are not accepted
    };
  }
  @Test(dataProvider = "toNumericErrorCases", expectedExceptions = RuntimeException.class)
  public void testToNumericErrors(String mvelExpression) {
    MVEL.executeExpression(MVEL.compileExpression(mvelExpression, parserContext), "");
  }

  @DataProvider
  public Object[][] toBooleanCases() {
    return new Object[][]{
        new Object[]{ "toBoolean(true);", true},
        new Object[]{ "toBoolean(false);", false },
        new Object[]{ "import com.linkedin.feathr.common.FeatureValue; toBoolean(FeatureValue.createBoolean(true));", true },
        new Object[]{ "import com.linkedin.feathr.common.FeatureValue; toBoolean(FeatureValue.createBoolean(false));", false },
    };
  }
  @Test(dataProvider = "toBooleanCases")
  public void testToBoolean(String mvelExpression, Object expectedOutput) {
    Object result = MVEL.executeExpression(MVEL.compileExpression(mvelExpression, parserContext), "");
    Assert.assertEquals(result, expectedOutput);
  }
}
