package com.linkedin.feathr.compute;

import com.linkedin.data.template.StringMap;
import com.linkedin.feathr.compute.converter.FeatureDefinitionsConverter;
import com.linkedin.feathr.config.FeatureDefinitionLoaderFactory;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider;
import java.util.Objects;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;

 /**
   * Unit tests for [[FeatureDefinitionsConverter]] class
   */
  public class TestFeatureDefinitionsConverter {
    @Test(description = "Test simple swa")
    public void testSimplesSwa() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("swa.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 2);
      Assert.assertEquals(output.getNodes().stream().map(AnyNode::isAggregation).filter(i -> i).count(), 1);
      Aggregation aggregationNode = output.getNodes().stream().map(AnyNode::getAggregation).filter(Objects::nonNull).collect(
          Collectors.toList()).get(0);
      Assert.assertEquals(aggregationNode.getFeatureName(), "memberEmbedding");
      // concrete key should not be set yet, as there is no join config
      Assert.assertEquals(aggregationNode.getConcreteKey(), null);
      StringMap aggParams = aggregationNode.getFunction().getParameters();
      Assert.assertEquals(aggParams.get("aggregation_type"), "LATEST");
      Assert.assertEquals(aggParams.get("window_size"), "PT72H");
      Assert.assertEquals(aggParams.get("window_unit"), "DAY");
      Assert.assertEquals(aggParams.get("target_column"), "embedding");
    }

    @Test(description = "Test anchored feature")
    public void testAnchoredFeature() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("anchoredFeature.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 2);
      Assert.assertEquals(output.getNodes().stream().map(AnyNode::isTransformation).filter(i -> i).count(), 1);
      Transformation transformationNode = output.getNodes().stream().map(AnyNode::getTransformation).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(transformationNode.getFeatureName(), "waterloo_member_yearBorn");
      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(transformationNode.getConcreteKey());
      Assert.assertEquals(transformationNode.getFunction().getOperator(), "feathr:anchor_mvel:0");
      StringMap aggParams = transformationNode.getFunction().getParameters();
      Assert.assertEquals(aggParams.get("expression"), "yearBorn");
      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "seqJoin/member.avro.json");
    }


    @Test(description = "Test seq join feature")
    public void testSeqJoinFeature() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("seqJoinFeature.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 5);
      Assert.assertEquals(output.getNodes().stream().map(AnyNode::isLookup).filter(i -> i).count(), 1);
      Lookup lookupNode = output.getNodes().stream().map(AnyNode::getLookup).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(lookupNode.getFeatureName(), "seq_join_industry_names");

      // base feature
      int baseNodeId = output.getFeatureNames().get("MemberIndustryId");

      // expansion feature
      int expansionNodeId = output.getFeatureNames().get("MemberIndustryName");

      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(lookupNode.getConcreteKey());
      Assert.assertEquals(lookupNode.getAggregation(), "UNION");
      Assert.assertEquals(lookupNode.getLookupKey().get(0).getNodeReference().getId().intValue(), baseNodeId);

      // MemberIndustryId has only one key, and the same key is re-used.
      Assert.assertEquals(lookupNode.getLookupKey().get(0).getNodeReference().getKeyReference().size(), 1);
      Assert.assertEquals(lookupNode.getLookupKey().get(0).getNodeReference().getKeyReference().get(0).getPosition().intValue(), 0);
      Assert.assertEquals(lookupNode.getLookupNode().intValue(), expansionNodeId);

      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "seqJoin/member.avro.json");
    }


    @Test(description = "Test a simple mvel derived feature")
    public void testMvelDerivedFeature() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("mvelDerivedFeature.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 3);
      Transformation derivedFeatureNode = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "B")).collect(Collectors.toList()).get(0);

      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(derivedFeatureNode.getConcreteKey());
      Assert.assertEquals(derivedFeatureNode.getFunction().getOperator(), "feathr:derived_mvel:0");
      Assert.assertEquals(derivedFeatureNode.getFunction().getParameters().get("parameterNames"), "AA");
      Assert.assertEquals(derivedFeatureNode.getFunction().getParameters().get("expression"), "AA*2");

      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "%s");
    }


    @Test(description = "Test a complex derived feature")
    public void testComplexDerivedFeature() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("complexDerivedFeature.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 6);
      Transformation derivedFeatureNode = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "C")).collect(Collectors.toList()).get(0);

      // input features
      int inputFeature1 = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "arg1")).collect(Collectors.toList()).get(0).getId();
      int inputFeature2 = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "arg2")).collect(Collectors.toList()).get(0).getId();

      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(derivedFeatureNode.getConcreteKey());
      Assert.assertEquals(derivedFeatureNode.getFunction().getOperator(), "feathr:extract_from_tuple:0");
      Assert.assertEquals(derivedFeatureNode.getInputs().size(), 2);
      Assert.assertTrue(derivedFeatureNode.getInputs().stream().map(NodeReference::getId).collect(Collectors.toList()).contains(inputFeature1));
      Assert.assertTrue(derivedFeatureNode.getInputs().stream().map(NodeReference::getId).collect(Collectors.toList()).contains(inputFeature2));
      Assert.assertEquals(Objects.requireNonNull(derivedFeatureNode.getFunction().getParameters()).get("expression"),
          "arg1 + arg2");

      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "%s");
    }

    @Test(description = "Test an anchored feature with source object")
    public void testAnchorWithSourceObject() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("anchoredFeature2.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 2);
      Transformation anchoredFeatureNode = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "f1")).collect(Collectors.toList()).get(0);

      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(anchoredFeatureNode.getConcreteKey());
      Assert.assertEquals(anchoredFeatureNode.getFunction().getOperator(), "feathr:anchor_mvel:0");

      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "slidingWindowAgg/localSWAAnchorTestFeatureData/daily");
      Assert.assertEquals(dataSourceNode.getKeyExpression(), "\"x\"");
    }

    @Test(description = "Test an anchored feature with key extractor")
    public void testAnchorWithKeyExtractor() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("anchorWithKeyExtractor.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 2);
      Transformation anchoredFeatureNode = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "cohortActorFeature_base")).collect(Collectors.toList()).get(0);

      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(anchoredFeatureNode.getConcreteKey());
      Assert.assertEquals(anchoredFeatureNode.getFunction().getOperator(), "feathr:anchor_spark_sql_feature_extractor:0");

      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "seqJoin/cohortActorFeatures.avro.json");
    }

    @Test(description = "Test a complex derived feature with udf")
    public void testDerivedWithUdf() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("derivedFeatureWithClass.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 4);
      Transformation derivedFeatureNode = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "C")).collect(Collectors.toList()).get(0);

      // input features
      int inputFeature1 = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "AA")).collect(Collectors.toList()).get(0).getId();
      int inputFeature2 = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "BB")).collect(Collectors.toList()).get(0).getId();

      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(derivedFeatureNode.getConcreteKey());
      Assert.assertEquals(derivedFeatureNode.getFunction().getOperator(), "feathr:derived_java_udf_feature_extractor:0");
      Assert.assertEquals(derivedFeatureNode.getInputs().size(), 2);
      Assert.assertTrue(derivedFeatureNode.getInputs().stream().map(NodeReference::getId).collect(Collectors.toList()).contains(inputFeature1));
      Assert.assertTrue(derivedFeatureNode.getInputs().stream().map(NodeReference::getId).collect(Collectors.toList()).contains(inputFeature2));
      Assert.assertEquals(Objects.requireNonNull(derivedFeatureNode.getFunction().getParameters()).get("class"),
          "com.linkedin.feathr.offline.anchored.anchorExtractor.TestxGenericSparkFeatureDataExtractor2");

      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "%s");
    }

    @Test(description = "Test a derived feature with mvel expression")
    public void testDerivedWithMvel() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("mvelDerivedFeature.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 3);
      Transformation derivedFeatureNode = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "B")).collect(Collectors.toList()).get(0);

      // input features
      int inputFeature1 = output.getNodes().stream().map(AnyNode::getTransformation)
          .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "AA")).collect(Collectors.toList()).get(0).getId();

      // concrete key should not be set yet, as there is no join config
      Assert.assertNull(derivedFeatureNode.getConcreteKey());
      Assert.assertEquals(derivedFeatureNode.getFunction().getOperator(), "feathr:derived_mvel:0");
      Assert.assertEquals(derivedFeatureNode.getInputs().size(), 1);
      Assert.assertTrue(derivedFeatureNode.getInputs().stream().map(NodeReference::getId).collect(Collectors.toList()).contains(inputFeature1));
      Assert.assertEquals(Objects.requireNonNull(derivedFeatureNode.getFunction().getParameters()).get("expression"),
          "AA*2");

      DataSource dataSourceNode = output.getNodes().stream().map(AnyNode::getDataSource).filter(Objects::nonNull).collect(Collectors.toList()).get(0);
      Assert.assertEquals(dataSourceNode.getExternalSourceRef(), "%s");
    }

    @Test(description = "Test a combination of swa features with key extractors")
    public void testSwaWithKeyExtractors() throws CloneNotSupportedException {
      FeatureDefConfig features = FeatureDefinitionLoaderFactory.getInstance()
          .loadAllFeatureDefinitions(new ResourceConfigDataProvider("swaWithExtractor.conf"));
      ComputeGraph output = new FeatureDefinitionsConverter().convert(features);
      Assert.assertEquals(output.getNodes().size(), 11);
      Assert.assertEquals(output.getNodes().stream().map(AnyNode::isAggregation).filter(i -> i).count(), 5);
      Aggregation aggregationNode = output.getNodes().stream().map(AnyNode::getAggregation).filter(Objects::nonNull)
          .filter(p -> Objects.equals(p.getFeatureName(), "f3")).collect(Collectors.toList()).get(0);
      Assert.assertEquals(aggregationNode.getFeatureName(), "f3");
      // concrete key should not be set yet, as there is no join config
      Assert.assertEquals(aggregationNode.getConcreteKey(), null);
      StringMap aggParams = aggregationNode.getFunction().getParameters();
      Assert.assertEquals(aggParams.get("aggregation_type"), "SUM");
      Assert.assertEquals(aggParams.get("window_size"), "PT72H");
      Assert.assertEquals(aggParams.get("window_unit"), "DAY");
      Assert.assertEquals(aggParams.get("target_column"), "aggregationWindow");
    }
  }
