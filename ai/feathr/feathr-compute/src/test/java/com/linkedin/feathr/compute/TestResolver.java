package com.linkedin.feathr.compute;

import com.google.common.collect.ImmutableMap;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.template.StringMap;
import com.linkedin.feathr.compute.converter.FeatureDefinitionsConverter;
import com.linkedin.feathr.config.FeatureDefinitionLoaderFactory;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Unit tests for [[Resolver]] and [[ComputeGraphs]] class
 */
public class TestResolver {

  @Test(description = "test simple merge of 2 compute graphs")
  public void testMergeGraphs() throws Exception {
    DataSource dataSource1 = new DataSource().setId(0).setSourceType(DataSourceType.UPDATE).setExternalSourceRef("foo");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foo:bar:1").setParameters(new StringMap(Collections.singletonMap("foo", "bar"))));
    AnyNodeArray nodeArray1 = new AnyNodeArray(AnyNode.create(dataSource1), AnyNode.create(transformation1));
    IntegerMap featureNameMap1 = new IntegerMap(Collections.singletonMap("baz", 1));
    ComputeGraph graph1 = new ComputeGraph().setNodes(nodeArray1).setFeatureNames(featureNameMap1);

    DataSource dataSource2 = new DataSource().setId(0).setSourceType(DataSourceType.UPDATE).setExternalSourceRef("bar");
    Transformation transformation2 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray((new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0))))))
        .setFunction(new TransformationFunction().setOperator("foo:baz:1"));
    Transformation transformation3 = new Transformation().setId(2)
        .setInputs(new NodeReferenceArray((new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0))))))
        .setFunction(new TransformationFunction().setOperator("foo:foo:2"));
    AnyNodeArray nodeArray2 = new AnyNodeArray(AnyNode.create(dataSource2), AnyNode.create(transformation2), AnyNode.create(transformation3));
    IntegerMap featureNameMap2 = new IntegerMap(
        ImmutableMap.of("fizz", 1, "buzz", 2));
    ComputeGraph graph2 = new ComputeGraph().setNodes(nodeArray2).setFeatureNames(featureNameMap2);

    ComputeGraph merged = ComputeGraphs.merge(Arrays.asList(graph1, graph2));
    Assert.assertEquals(merged.getNodes().size(), 5);
    Assert.assertEquals(merged.getFeatureNames().keySet().size(), 3);
  }

  @Test
  public void testMergeGraphWithFeatureDependencies() {
    External featureReference1 = new External().setId(0).setName("feature1");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"));
    AnyNodeArray nodeArray1 = new AnyNodeArray(AnyNode.create(featureReference1), AnyNode.create(transformation1));
    IntegerMap featureNameMap1 = new IntegerMap(Collections.singletonMap("apple", 1));
    ComputeGraph graph1 = new ComputeGraph().setNodes(nodeArray1).setFeatureNames(featureNameMap1);
    Assert.assertEquals(graph1.getNodes().size(), 2);
    External featureReference2 = new External().setId(0).setName("feature2");
    Transformation transformation2 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar2"));
    AnyNodeArray nodeArray2 = new AnyNodeArray(AnyNode.create(featureReference2), AnyNode.create(transformation2));
    IntegerMap featureNameMap2 = new IntegerMap(Collections.singletonMap("feature1", 1));
    ComputeGraph graph2 = new ComputeGraph().setNodes(nodeArray2).setFeatureNames(featureNameMap2);
    Assert.assertEquals(graph2.getNodes().size(), 2);
    ComputeGraph merged = ComputeGraphs.merge(Arrays.asList(graph1, graph2));
    Assert.assertEquals(merged.getNodes().size(), 3);
  }

  @Test(description = "test remove redundant nodes method")
  public void testRemoveDuplicates() throws CloneNotSupportedException {
    External featureReference1 = new External().setId(0).setName("feature1");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"));
    External featureReference2 = new External().setId(2).setName("feature1");
    Transformation transformation2 = new Transformation().setId(3)
        .setInputs(new NodeReferenceArray(new NodeReference().setId(2).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar2"));
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(featureReference1), AnyNode.create(featureReference2),
        AnyNode.create(transformation1), AnyNode.create(transformation2));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1, "banana", 3));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    Assert.assertEquals(graph.getNodes().size(), 4);
    ComputeGraph simplified = ComputeGraphs.removeRedundancies(graph);
    Assert.assertEquals(simplified.getNodes().size(), 3);
  }

  @Test(description = "test with same feature name and different keys")
  public void testResolveGraph() throws CloneNotSupportedException {
    DataSource dataSource1 =
        new DataSource().setId(0).setSourceType(DataSourceType.UPDATE).setExternalSourceRef("dataSource1");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"));
    AnyNodeArray nodeArray1 = new AnyNodeArray(AnyNode.create(dataSource1), AnyNode.create(transformation1));
    IntegerMap featureNameMap1 = new IntegerMap(Collections.singletonMap("apple", 1));
    ComputeGraph graph1 = new ComputeGraph().setNodes(nodeArray1).setFeatureNames(featureNameMap1);

    List<Resolver.FeatureRequest> requestedFeatures = Arrays.asList(
        new Resolver.FeatureRequest("apple", Collections.singletonList("viewer"), Duration.ZERO,"apple__viewer"),
        new Resolver.FeatureRequest("apple", Collections.singletonList("viewee"), Duration.ZERO, "apple__viewee"));
    ComputeGraph resolved = Resolver.create(graph1).resolveForRequest(requestedFeatures);
    Assert.assertTrue(resolved.getFeatureNames().containsKey("apple__viewer"));
    Assert.assertTrue(resolved.getFeatureNames().containsKey("apple__viewee"));
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testNonSequentialNodes() {
    External featureReference1 = new External().setId(0).setName("feature1");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"));
    External featureReference2 = new External().setId(2).setName("feature1");

    // Node id 6 is not sequential
    Transformation transformation2 = new Transformation().setId(6)
        .setInputs(new NodeReferenceArray(new NodeReference().setId(2).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar2"));
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(featureReference1), AnyNode.create(featureReference2),
        AnyNode.create(transformation1), AnyNode.create(transformation2));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1, "banana", 3));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraphs.ensureNodeIdsAreSequential(graph);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testDependenciesNotExist() {
    External featureReference1 = new External().setId(0).setName("feature1");
    Transformation transformation1 = new Transformation().setId(1)
        // node 6 does not exist
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(6).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"));
    External featureReference2 = new External().setId(2).setName("feature1");

    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(featureReference1), AnyNode.create(featureReference2),
        AnyNode.create(transformation1));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraphs.ensureNodeReferencesExist(graph);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testNoDependencyCycle() {
    External featureReference1 = new External().setId(0).setName("feature1");

    // Dependency cycle created
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"));
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(featureReference1), AnyNode.create(transformation1));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraphs.ensureNoDependencyCycles(graph);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testNoExternalReferencesToSelf() {
    External featureReference1 = new External().setId(0).setName("feature1");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("feature1");
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(featureReference1), AnyNode.create(transformation1));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("feature1", 1));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraphs.ensureNoExternalReferencesToSelf(graph);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testNoConcreteKeys() {
    External featureReference1 = new External().setId(0).setName("feature1");
    IntegerArray array = new IntegerArray();
    array.add(1);
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("feature1")
        .setConcreteKey(new ConcreteKey().setKey(array));
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(featureReference1), AnyNode.create(transformation1));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("feature1", 1));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraphs.ensureNoConcreteKeys(graph);
  }

  @Test(description = "test attaching of concrete node to dependencies of transformation node")
  public void testAddConcreteKeyToTransformationNode() throws CloneNotSupportedException {
    DataSource dataSource1 = new DataSource().setId(0).setExternalSourceRef("testPath");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("apple");
    Transformation transformation2 = new Transformation().setId(2)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("banana");
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(dataSource1), AnyNode.create(transformation1), AnyNode.create(transformation2));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1, "banana", 2));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraph simplified = ComputeGraphs.removeRedundancies(graph);
    List<String> keys = new ArrayList<>();
    keys.add("x");

    // The same concrete key should get attached to the dependencies
    ComputeGraph withConcreteKeyAttached = new Resolver(ComputeGraphs.removeRedundancies(simplified)).resolveForFeature("banana", keys, "banana");

    DataSource createdKeyNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getDataSource)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getExternalSourceRef(), "x")).collect(Collectors.toList()).get(0);
    Transformation appleNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getTransformation)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "apple")).collect(Collectors.toList()).get(0);
    Transformation bananaNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getTransformation)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "banana")).collect(Collectors.toList()).get(0);
    Assert.assertEquals(Objects.requireNonNull(appleNode.getConcreteKey()).getKey().get(0), createdKeyNode.getId());
    Assert.assertEquals(Objects.requireNonNull(bananaNode.getConcreteKey()).getKey().get(0), createdKeyNode.getId());
  }

  @Test(description = "test attaching of concrete node to dependencies of aggregation node")
  public void testAddConcreteKeyToAggregationNode() throws CloneNotSupportedException {
    DataSource dataSource1 = new DataSource().setId(0);
    Aggregation aggregation1 = new Aggregation().setId(1)
        .setInput(new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))).setFeatureName("apple");
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(dataSource1), AnyNode.create(aggregation1));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraph simplified = ComputeGraphs.removeRedundancies(graph);
    List<String> keys = new ArrayList<>();
    keys.add("x");

    // The same concrete key should get attached to the dependencies
    ComputeGraph withConcreteKeyAttached = new Resolver(ComputeGraphs.removeRedundancies(simplified)).resolveForFeature("apple", keys, "apple");

    Aggregation appleNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getAggregation)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "apple")).collect(Collectors.toList()).get(0);
    Assert.assertEquals(Objects.requireNonNull(appleNode.getConcreteKey()).getKey().get(0).intValue(), 0);
  }

  @Test(description = "test attaching of concrete node to dependencies of seq join node")
  public void testAddConcreteKeyToSeqJoinNode() throws CloneNotSupportedException {
    DataSource dataSource1 = new DataSource().setId(0).setExternalSourceRef("testpath");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("apple");
    Transformation transformation2 = new Transformation().setId(2)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("banana");
    NodeReference nr = new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)));
    Lookup.LookupKey lookupKey = new Lookup.LookupKey();
    lookupKey.setNodeReference(nr);
    Lookup.LookupKeyArray lookupKeyArray = new Lookup.LookupKeyArray();
    lookupKeyArray.add(lookupKey);
    Lookup lookupNode1 = new Lookup().setId(3).setLookupNode(2).setLookupKey(lookupKeyArray).setFeatureName("apple-banana");
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(dataSource1), AnyNode.create(transformation1),
        AnyNode.create(transformation2), AnyNode.create(lookupNode1));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1, "banana", 2,
            "apple-banana", 3));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraph simplified = ComputeGraphs.removeRedundancies(graph);
    List<String> keys = new ArrayList<>();
    keys.add("x");
    // The same concrete key should get attached to the dependencies
    ComputeGraph withConcreteKeyAttached = new Resolver(ComputeGraphs.removeRedundancies(simplified)).resolveForFeature("apple-banana", keys, "apple");

    DataSource createdKeyNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getDataSource)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getExternalSourceRef(), "x")).collect(Collectors.toList()).get(0);
    Transformation appleNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getTransformation)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "apple")).collect(Collectors.toList()).get(0);
    Transformation bananaNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getTransformation)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "banana")).collect(Collectors.toList()).get(0);
    Assert.assertEquals(Objects.requireNonNull(appleNode.getConcreteKey()).getKey().get(0), createdKeyNode.getId());

    // key of the expansion should be the transformation node of apple.
    Assert.assertEquals(Objects.requireNonNull(bananaNode.getConcreteKey()).getKey().get(0).intValue(), 2);
  }

  @Test(description = "test attaching of concrete node to dependencies of complex seq join node with multi-key")
  public void testAddConcreteKeyToComplexSeqJoinNode() throws CloneNotSupportedException {
    DataSource dataSource1 = new DataSource().setId(0).setExternalSourceRef("testpath");
    Transformation transformation1 = new Transformation().setId(1)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(0).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("apple");
    Transformation transformation2 = new Transformation().setId(2)
        .setInputs(new NodeReferenceArray(
            new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)))))
        .setFunction(new TransformationFunction().setOperator("foobar1"))
        .setFeatureName("banana");
    NodeReference nr = new NodeReference().setId(1).setKeyReference(new KeyReferenceArray(new KeyReference().setPosition(0)));
    Lookup.LookupKey lookupKey = new Lookup.LookupKey();
    lookupKey.setNodeReference(nr);
    Lookup.LookupKeyArray lookupKeyArray = new Lookup.LookupKeyArray();
    lookupKeyArray.add(lookupKey);
    Lookup lookupNode1 = new Lookup().setId(3).setLookupNode(2).setLookupKey(lookupKeyArray).setFeatureName("apple-banana");
    AnyNodeArray nodeArray = new AnyNodeArray(AnyNode.create(dataSource1), AnyNode.create(transformation1),
        AnyNode.create(transformation2), AnyNode.create(lookupNode1));
    IntegerMap featureNameMap = new IntegerMap(
        ImmutableMap.of("apple", 1, "banana", 2,
            "apple-banana", 3));
    ComputeGraph graph = new ComputeGraph().setNodes(nodeArray).setFeatureNames(featureNameMap);
    ComputeGraph simplified = ComputeGraphs.removeRedundancies(graph);
    List<String> keys = new ArrayList<>();
    keys.add("x");
    keys.add("y");
    // The same concrete key should get attached to the dependencies
    ComputeGraph withConcreteKeyAttached = new Resolver(ComputeGraphs.removeRedundancies(simplified)).resolveForFeature("apple-banana", keys, "apple");

    DataSource createdKeyNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getDataSource)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getExternalSourceRef(), "x")).collect(Collectors.toList()).get(0);
    Transformation appleNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getTransformation)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "apple")).collect(Collectors.toList()).get(0);
    Transformation bananaNode = withConcreteKeyAttached.getNodes().stream().map(AnyNode::getTransformation)
        .filter(Objects::nonNull).filter(p -> Objects.equals(p.getFeatureName(), "banana")).collect(Collectors.toList()).get(0);
    Assert.assertEquals(Objects.requireNonNull(appleNode.getConcreteKey()).getKey().get(0), createdKeyNode.getId());

    // key of the expansion should be the transformation node of apple.
    Assert.assertEquals(Objects.requireNonNull(bananaNode.getConcreteKey()).getKey().get(0), appleNode.getId());
  }
}