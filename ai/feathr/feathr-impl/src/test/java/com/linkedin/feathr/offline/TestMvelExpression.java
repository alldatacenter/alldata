package com.linkedin.feathr.offline;

import com.linkedin.feathr.offline.mvel.MvelContext;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.mvel2.*;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Test MVEL expression evaluator
 */
public class TestMvelExpression extends TestNGSuite {
  @Test(description = "test mvel expression foo.bar on a map field of GenericRecord, where foo is the map field "
      + "and bar is the target key in the map")
  public void testMVELExpressionOnMap() {
    String schemaFile =
        "{ \"name\" : \"a\", \"type\" : \"record\", \"fields\" : [" + "   { \"name\" : \"testMap\", " + "     \"type\" : {"
            + "          \"type\" : \"map\", \"values\" : \"string\"" + "     } " + "   } " + "  ] " + "}";
    Schema schema = Schema.parse(schemaFile);
    GenericRecord firstRecord = new GenericData.Record(schema);
    // the map value for the first record, note that it does NOT have the target key 'testKey'
    Map<String, String> firstRecordValue = new HashMap();
    // the map value for the second record, note that it DOES have the target key 'testKey'
    firstRecordValue.put("testKey", "testMapValue1");
    firstRecord.put("testMap", firstRecordValue);

    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
    // test case 1: simple expression
    Serializable compiledExpression = MVEL.compileExpression("testMap.testKey");
    // test case 2: Bug reproduced
    Serializable compiledExpression2 = MVEL.compileExpression("testMap.testKey != null? testMap.testKey : null;\n");
    // test case 3: Working fine, the only difference between case 2 and 3 is changing the first testMap.testKey to testMap['testKey']
    Serializable compiledExpression3 = MVEL.compileExpression("testMap['testKey'] != null? testMap.testKey : null;\n");
    VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    PropertyHandlerFactory.registerPropertyHandler(GenericRecord.class, MvelContext.GenericRecordPropertyHandler.INSTANCE);

    // Evaluate the first record using the compiled MVEL expression, this is the direct root cause of the issue
    assertEquals(MVEL.executeExpression(compiledExpression, firstRecord, factory, String.class), "testMapValue1");
    assertEquals(MVEL.executeExpression(compiledExpression2, firstRecord, factory, String.class), "testMapValue1");
    assertEquals(MVEL.executeExpression(compiledExpression3, firstRecord, factory, String.class), "testMapValue1");

    Map<String, String> secondRecordValue = new HashMap();
    // the map value for the second record, note that it DOES have the target key 'testKey'
    secondRecordValue.put("testKey", "testMapValue");
    GenericRecord secondRecord = new GenericData.Record(schema);
    secondRecord.put("testMap", secondRecordValue);

    // This assert will fail, and MVEL library is on version < 2.4.7, there were 3 ways to fix this:
    // 1. Comment out the previous MVEL.executeExpression()
    // 2. Recompile the MVEL expression here, but this will bring performance issue (as we should not compile for each record)
    // 3. Change the elCtx to ctx in ThisValueAccessor
    assertEquals(MVEL.executeExpression(compiledExpression, secondRecord, factory, String.class), "testMapValue");
    assertEquals(MVEL.executeExpression(compiledExpression2, secondRecord, factory, String.class), "testMapValue");
    assertEquals(MVEL.executeExpression(compiledExpression3, secondRecord, factory, String.class), "testMapValue");
  }

  @Test
  public void demonstrateCompatibilityCapabilityForDifferentFeatureValueTypes() throws Exception {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.addImport("createFeatureValue1", getClass().getMethod("createFeatureValue1", String.class));
    parserConfiguration.addImport("createFeatureValue2", getClass().getMethod("createFeatureValue2", String.class));
    parserConfiguration.addImport("processFeatureValue1", getClass().getMethod("processFeatureValue1", FeatureValue1.class));
    parserConfiguration.addImport("processFeatureValue2", getClass().getMethod("processFeatureValue2", FeatureValue2.class));
    ParserContext parserContext = new ParserContext(parserConfiguration);

    ConversionHandler featureValue2ConversionHandler = new ConversionHandler() {
      @Override
      public Object convertFrom(Object in) {
        String contents = ((FeatureValue1) in).getContents1();
        return new FeatureValue2(contents);
      }

      @Override
      public boolean canConvertFrom(Class cls) {
        return FeatureValue1.class.equals(cls);
      }
    };

    ConversionHandler featureValue1ConversionHandler = new ConversionHandler() {
      @Override
      public Object convertFrom(Object in) {
        String contents = ((FeatureValue2) in).getContents2();
        return new FeatureValue1(contents);
      }

      @Override
      public boolean canConvertFrom(Class cls) {
        return FeatureValue2.class.equals(cls);
      }
    };

    // Use MVEL DataConversion API to enable magical coercion between FeatureValue1 and FeatureValue2
    DataConversion.addConversionHandler(FeatureValue1.class, featureValue1ConversionHandler);
    DataConversion.addConversionHandler(FeatureValue2.class, featureValue2ConversionHandler);

    {
      // Basic sanity test: ensure UDF to make FeatureValue1 works
      String expression = "createFeatureValue1('foooo')";
      Serializable compiledExpression = MVEL.compileExpression(expression, parserContext);
      Object output = MVEL.executeExpression(compiledExpression);
      System.out.println(output);
      Assert.assertEquals(((FeatureValue1) output).getContents1(), "foooo");
    }
    {
      // Call a UDF that produces FeatureValue1, and feed it to a different UDF that consumes FeatureValue2
      String expression = "processFeatureValue2(createFeatureValue1('foooo'))";
      Serializable compiledExpression = MVEL.compileExpression(expression, parserContext);
      Object output = MVEL.executeExpression(compiledExpression);
      System.out.println(output);
      Assert.assertEquals(output, "foooo");
    }
    {
      // Run an MVEL expression that produces FeatureValue1, and invoke MVEL in a way that converts to FeatureValue2 without
      // the calling code needing to know about the existence of FeatureValue1.
      String expression = "createFeatureValue1('foooo')";
      Serializable compiledExpression = MVEL.compileExpression(expression, parserContext);
      Object output = maybeCoerceMvelOutput(MVEL.executeExpression(compiledExpression), FeatureValue2.class);
      System.out.println(output);
      Assert.assertEquals(output.getClass(), FeatureValue2.class);
      Assert.assertEquals(((FeatureValue2) output).getContents2(), "foooo");
    }
    {
      // Run an MVEL expression that produces some other output type, e.g. int. Ensure that "regular" behavior is
      // followed, and that we don't accidentally choke by trying to convert it to FeatureValue2.
      String expression = "123";
      Serializable compiledExpression = MVEL.compileExpression(expression, parserContext);
      Object output = maybeCoerceMvelOutput(MVEL.executeExpression(compiledExpression), FeatureValue2.class);
      System.out.println(output);
      Assert.assertEquals(output, 123);
    }
  }

  public static Object maybeCoerceMvelOutput(Object data, Class<?> maybeTarget) {
    if (DataConversion.canConvert(maybeTarget, data.getClass())) {
      return DataConversion.convert(data, maybeTarget);
    } else {
      return data;
    }
  }

  public static FeatureValue1 createFeatureValue1(String contents) {
    return new FeatureValue1(contents);
  }
  public static FeatureValue2 createFeatureValue2(String contents) {
    return new FeatureValue2(contents);
  }
  public static String processFeatureValue1(FeatureValue1 data) {
    return data.getContents1();
  }
  public static String processFeatureValue2(FeatureValue2 data) {
    return data.getContents2();
  }

  public static class FeatureValue1 {
    private final String contents;

    public FeatureValue1(String contents) {
      this.contents = contents;
    }

    public String getContents1() {
      return contents;
    }

    @Override
    public String toString() {
      return "FeatureValue1{contents='" + contents + "'}";
    }
  }

  public static class FeatureValue2 {
    private final String contents;

    public FeatureValue2(String contents) {
      this.contents = contents;
    }

    public String getContents2() {
      return contents;
    }

    @Override
    public String toString() {
      return "FeatureValue2{contents='" + contents + "'}";
    }
  }
}