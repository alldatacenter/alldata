package com.linkedin.feathr.core.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class MvelInputsResolverTest {
  MvelInputsResolver _mvelInputsResolver = MvelInputsResolver.getInstance();

  @DataProvider
  public Object[][] testGetInputFeaturesDataProvider() {
    return new Object[][]{
        // Tests simple alias syntax
        {"featureA", Collections.singletonList("featureA")},
        // Tests Mvel expresion with multiple input features with no import
        {"featureA + featureB", Arrays.asList("featureA", "featureB")},
        // Test fully-qualified existing class that starts with com will work
        {"com.linkedin.frame.core.utils.Object.apply(featureA,  featureB ) ; ",
            Arrays.asList("featureA", "featureB")},
        // Test fully-qualified existing class that starts with org will work
        {"org.linkedin.frame.core.utils.Object.apply(featureA,  featureB ) ; ",
            Arrays.asList("featureA", "featureB")},
        // Test fully-qualified existing class that starts with java will work
        {"java.lang.Object.apply(featureA,  featureB ) ; ",
            Arrays.asList("featureA", "featureB")},
        // Tests Mvel expresion with additional whitespaces
        {" import   com.linkedin.frame.core.utils.MemberJobFunctionToYoeExtractor ;  MemberJobFunctionToYoeExtractor.apply(featureA,  featureB ) ; ",
            Arrays.asList("featureA", "featureB")},
        // Test Mvel with built-in frame functions
        {"getTerms(careers_job_applicants_90d).size()", Collections.singletonList("careers_job_applicants_90d")},
        // Test Mvel with complex projections
        {"if (isNonZero(waterloo_member_location)) {([$.getKey.substring(11) : $.getValue] in waterloo_member_location.getValue().entrySet() if $.getKey.startsWith('geo_region='))}",
            Collections.singletonList("waterloo_member_location")},
        // Test mvel with null
        {"isPresent(waterloo_member_location) ? Math.abs(waterloo_member_location) : null",
            Collections.singletonList("waterloo_member_location")},
        // Test mvel with numbers
        {"isPresent(waterloo_member_location) ? waterloo_member_location : 0.0",
            Collections.singletonList("waterloo_member_location")},
        // Tests Mvel expresion with multiple input features with multiple imports
        {"import com.linkedin.frame.core.utils.MemberJobFunctionToYoeExtractor; MemberJobFunctionToYoeExtractor.apply(featureA, featureB);",
            Arrays.asList("featureA", "featureB")},
        // Tests Mvel expresion with multiple input features with multiple imports
        {"import com.linkedin.frame.stz.ExtractorA; import com.linkedin.frame.stz.ExtractorB; ExtractorA.test(featureA) + ExtractorB.apply(featureB, featureC);",
            Arrays.asList("featureA", "featureB", "featureC")},
        // Tests Mvel expresion with multiple input features and constant, with single imports
        {"import com.linkedin.frame.stz.Extractor; Extractor.test(featureA, featureB, 100L, 'a_constant_string');",
            Arrays.asList("featureA", "featureB")}};
  }

  @Test(dataProvider = "testGetInputFeaturesDataProvider")
  public void testGetInputFeatures(String input, List<String> expected) {
    List<String> inputFeatures = _mvelInputsResolver.getInputFeatures(input);
    assertEquals(inputFeatures, expected);
  }
}
