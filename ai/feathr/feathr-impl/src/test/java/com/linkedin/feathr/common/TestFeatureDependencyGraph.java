package com.linkedin.feathr.common;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestFeatureDependencyGraph extends TestNGSuite {

  /*
    Description of test scenario:

      Notation:
        D(x,y) means "feature D for entity (x,y)" where D is a dual-entity a.k.a. dual-key feature

      Definitions:

        A(x) = anchored
        B(x) = derived feature that depends on A(x)
        C(x) = derived feature that depends on A(x)
        D(x,y) = derived feature that depends on A(x) and B(y)
        E(x) = anchored
        F(x) = anchored
        G(x,y) = derived feature that depends on D(y,x) and E(y) (note the reversed ordering of keys passed to D)
        H(x) = not anchored (missing)
        I(x) = derived feature that depends on H(x)

      Feature-Level dependency graph (dependency direction is bottom to top)

          anchor    anchor    anchor
            |         |         |
            |         |         |
            A         E         F       H (not anchored, not reachable)
           / \        |                 |
          /   \       |                 |
         B     C     /                  I (derived feature, not anchored, not reachable)
          \   /     /
           \ /     /
            D     /
             \   /
              \ /
               G

      Example Request: [ E(x), F(y), G(x,y) ]

           E(x)    E(y)     A(y)        A(x)    F(y)
                      \       \          /
                       \       \        /
                        \      C(y)   B(x)
                         \       \    /
                          \       \  /
                           \     D(y,x)
                            \      /
                             \    /
                              \  /
                             G(x,y)

      (Expected) Expanded Request: [ E(x), E(y), A(y), A(x), F(y), C(y), B(x), D(y,x), G(x,y) ]

      We have a mix of legacy feature names and the currently used ones. Example - "feathr-A-1-0" and "feathr-B-1-0" are the featureRef
      names, and C, D, E, F, G, H, I are the legacy names. This is to ensure both would work in the featureDependencyGraph.

   */

  private FeatureDependencyGraph _featureDependencyGraph;

  @BeforeClass
  public void setup() {
    // Construct dependency graph described above
    _featureDependencyGraph = new FeatureDependencyGraph(ImmutableMap.of(
        "G", setOf(dependency("D", 1, 0), dependency("E", 1)), // G(arg0, arg1) requires D(arg1, arg0) and E(arg1)
        "B", setOf(dependency("A", 0)), // B(arg0) requires feathr-A-1-0(arg0)
        "C", setOf(dependency("A", 0)), // C(arg0) requires feathr-A-1-0(arg0)
        "D", setOf(dependency("C", 0), dependency("B", 1)), // D(arg0, arg1) requires C(arg0) and B(arg1)
        "I", setOf(dependency("H", 0)) // I(arg0) requires H(arg0)
    ), Arrays.asList("A", "E", "F"));
  }

  @Test
  public void testGetOrderedPlanForRequest() {
    // Input: Request: E(x), F(y), G(x,y)
    List<TaggedFeatureName> request = listOf(
        tfn("E", "x"),
        tfn("F", "y"),
        tfn("G", "x", "y")
    );
    List<TaggedFeatureName> plan = _featureDependencyGraph.getOrderedPlanForRequest(request);
    // Expected Output: Ordered Plan: [ E(x), E(y), A(y), A(x), F(y), C(y), B(x), D(y,x), G(x,y) ]
    // Note: The exact ordering above is not required. All that is required is that for each element in the list,
    //       all of its dependencies, if any, appear to the left of it.
    // tfn takes in a featureName, example which is just A in feathr-A-1-0, while tfu takes in the entire featureRefString.
    Assert.assertEquals(new HashSet<>(plan), Stream.of(
        tfn("E", "x"),
        tfn("E", "y"),
        tfn("A", "y"),
        tfn("A", "x"),
        tfn("F", "y"),
        tfn("C", "y"),
        tfn("B", "x"),
        tfn("D", "y", "x"),
        tfn("G", "x", "y")
    ).collect(Collectors.toSet()));

    /*
      Confirm the following ordering constraints.
      Nodes must appear earlier in the list than their children in the tree.

        E(x)    E(y)     A(y)       A(x)    F(y)
                  \       \          /
                   \       \        /
                    \      C(y)   B(x)
                     \       \    /
                      \       \  /
                       \     D(y,x)
                        \      /
                         \    /
                          \  /
                         G(x,y)
     */
    checkOrder(plan, tfn("E", "y"), tfn("G", "x", "y"));
    checkOrder(plan, tfn("D", "y", "x"), tfn("G", "x", "y"));
    checkOrder(plan, tfn("C", "y"), tfn("D", "y", "x"));
    checkOrder(plan, tfn("B", "x"), tfn("D", "y", "x"));
    checkOrder(plan, tfn("A", "y"), tfn("C", "y"));
    checkOrder(plan, tfn("A", "x"), tfn("B", "x"));
  }

  @Test
  public void testGetComputationPipeline() {
    // Input: Request: E(x), F(y), G(x,y)
    List<TaggedFeatureName> request = listOf(
        tfu("E", "x"),
        tfu("F", "y"),
        tfu("G", "x", "y")
    );

    List<Set<TaggedFeatureName>> pipeline = _featureDependencyGraph.getComputationPipeline(request);

    /*
      Confirm the pipeline to have 4 stages as shown as below.  The ordering of a single stage does not matter
        E(x)    E(y)     A(y)       A(x)    F(y)
                  \       \          /
                   \       \        /
                    \      C(y)   B(x)
                     \       \    /
                      \       \  /
                       \     D(y,x)
                        \      /
                         \    /
                          \  /
                         G(x,y)
     */
    assertEquals(pipeline.size(), 4);

    assertEquals(pipeline.get(0), setOf(tfu("E", "x"), tfu("E", "y"), tfu("A", "x"), tfu("A", "y"), tfu("F", "y"),
        tfu("E", "x")));
    assertEquals(pipeline.get(1), setOf(tfu("C", "y"), tfu("B", "x")));
    assertEquals(pipeline.get(2), setOf(tfu("D", "y", "x")));
    assertEquals(pipeline.get(3), setOf(tfu("G", "x", "y")));
  }

  @DataProvider
  private static Object[][] testAnchoredOnlyPipelineTestCases() {
    return new Object[][]{
        { listOf("A", "B"), listOf("C->A", "D->B", "E->D")},
        { listOf("A", "B"), listOf("C->A", "D->B", "E->D")},
        { listOf("A"), listOf("B->A", "C->B", "D->C")}
    };
  }
  @Test(description = "verifies that when only anchored features are requested, there is only one computation stage",
      dataProvider = "testAnchoredOnlyPipelineTestCases")
  public void testAnchoredOnlyPipeline(List<String> anchoredFeatures, List<String> dependencies) {
    Set<TaggedFeatureName> requestedAnchoredFeatures = anchoredFeatures.stream().map(f -> tfu(f, "0")).collect(Collectors.toSet());

    FeatureDependencyGraph dependencyGraph = createDependencyGraph(anchoredFeatures, dependencies);
    List<Set<TaggedFeatureName>> pipeline = dependencyGraph.getComputationPipeline(requestedAnchoredFeatures);
    assertEquals(pipeline.size(), 1);
    pipeline.get(0).containsAll(requestedAnchoredFeatures);
  }

  @Test
  public void testGetPipelineWithEmptyFeatureRequest() {
    FeatureDependencyGraph dependencyGraph = createDependencyGraph(listOf("A", "B", "C"), listOf("D->A", "E->B"));
    assertTrue(dependencyGraph.getComputationPipeline(listOf()).isEmpty());
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*Feature Z can't be resolved in the dependency graph.*")
  public void testGetPipelineWithUnknownFeature() {
    FeatureDependencyGraph dependencyGraph = createDependencyGraph(listOf("A", "B", "C"), listOf("D->A", "E->B"));
    dependencyGraph.getComputationPipeline(listOf(tfu("Z", "0")));
  }

  @Test
  public void testGetOrderedPlanForFeatureUrns() {
    // Input: Request: E(x), F(y), G(x,y)
    List<TaggedFeatureName> request = listOf(
        tfu("E", "x"),
        tfu("F", "y"),
        tfu("G", "x", "y")
    );
    List<TaggedFeatureName> plan = _featureDependencyGraph.getOrderedPlanForFeatureUrns(request);
    // Expected Output: Ordered Plan: [ E(x), E(y), A(y), A(x), F(y), C(y), B(x), D(y,x), G(x,y) ]
    // Note: The exact ordering above is not required. All that is required is that for each element in the list,
    //       all of its dependencies, if any, appear to the left of it.
    Assert.assertEquals(new HashSet<>(plan), Stream.of(
        tfu("E", "x"),
        tfu("E", "y"),
        tfu("A", "y"),
        tfu("A", "x"),
        tfu("F", "y"),
        tfu("C", "y"),
        tfu("B", "x"),
        tfu("D", "y", "x"),
        tfu("G", "x", "y")
    ).collect(Collectors.toSet()));

    /*
      Confirm the following ordering constraints.
      Nodes must appear earlier in the list than their children in the tree.

        E(x)    E(y)     A(y)       A(x)    F(y)
                  \       \          /
                   \       \        /
                    \      C(y)   B(x)
                     \       \    /
                      \       \  /
                       \     D(y,x)
                        \      /
                         \    /
                          \  /
                         G(x,y)
     */
    checkOrder(plan, tfu("E", "y"), tfu("G", "x", "y"));
    checkOrder(plan, tfu("D", "y", "x"), tfu("G", "x", "y"));
    checkOrder(plan, tfu("C", "y"), tfu("D", "y", "x"));
    checkOrder(plan, tfu("B", "x"), tfu("D", "y", "x"));
    checkOrder(plan, tfu("A", "y"), tfu("C", "y"));
    checkOrder(plan, tfu("A", "x"), tfu("B", "x"));
  }

  @Test
  public void testGetPlan() {
    /*
        anchor
          |
          |
          A
         / \
        /   \
       B     C
        \   /
         \ /
          D

        Requesting D would require A, B, C.
     */
    List<String> plan = _featureDependencyGraph.getPlan(Collections.singleton("D"));

    assertEquals(new HashSet<>(plan), Stream.of("A", "B", "C", "D").collect(Collectors.toSet()));
    checkOrder(plan, "A", "B");
    checkOrder(plan, "A", "C");
    checkOrder(plan, "B", "D");
    checkOrder(plan, "C", "D");
  }


  /**
   * Ensures that 'first' and 'second' both appear in 'list', and that 'first' comes before 'second'
   */
  private static <T> void checkOrder(List<T> list, T first, T second) {
    int firstPos = list.indexOf(first);
    int secondPos = list.indexOf(second);
    assertTrue(firstPos >= 0);
    assertTrue(secondPos > firstPos);
  }

  private static ErasedEntityTaggedFeature dependency(String name, Integer ... argBindings) {
    return new ErasedEntityTaggedFeature(Stream.of(argBindings).collect(Collectors.toList()), name);
  }

  /***
   * Constructs a new {@link FeatureDependencyGraph} using a simplified string based expression
   *
   * @param anchoredFeatures list of anchored feature names
   * @param dependencies string presentation of the dependencies in the form of {featureName -> dependentFeature}
   */
  private static FeatureDependencyGraph createDependencyGraph(Collection<String> anchoredFeatures, List<String> dependencies) {
    Map<String, Set<ErasedEntityTaggedFeature>> dependencyGraphString = new HashMap<>();

    for (String dependency : dependencies) {
      String[] split = dependency.split("->");
      String sourceFeature = split[0];
      String dependentFeature = split[1];
      dependencyGraphString.computeIfAbsent(sourceFeature, x -> new HashSet<>())
          .add(new ErasedEntityTaggedFeature(listOf(0), dependentFeature));
    }

    return new FeatureDependencyGraph(dependencyGraphString, anchoredFeatures);
  }

  @SafeVarargs
  private static <T> Set<T> setOf(T ... args) {
    return Stream.of(args).collect(Collectors.toSet());
  }

  @SafeVarargs
  private static <T> List<T> listOf(T ... args) {
    return Arrays.asList(args);
  }

  private static TaggedFeatureName tfn(String featureName, String ... keyPart) {
    return new TaggedFeatureName(Arrays.asList(keyPart), featureName);
  }

  private static TaggedFeatureName tfu(String featureRefStr, String ... keyPart) {
    return new TaggedFeatureName(Arrays.asList(keyPart), featureRefStr);
  }
}
