package com.linkedin.feathr.common;

import com.google.common.collect.Lists;
import com.linkedin.feathr.common.exception.FeathrException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;


/**
 * A dependency graph for feature anchors and feature derivations.
 *
 * Purpose 1: Given a list of features' dependencies and which features are anchored, build a graph that can determine
 *            which features are reachable and can resolve features' transitive dependencies on demand.
 * Purpose 2: Given a list of features with entity key bindings (a.k.a. key tags), provide fully expanded list of all
 *            transitive dependencies including entity key bindings. E.g. if feature2 depends on feature1, and the
 *            request was for [ (a1):feature2, (a2):feature2 ], then the expanded dependencies would be:
 *            [ (a1):feature2, (a2):feature2, (a1):feature1, (a2):feature1]
 */
@InternalApi
public class FeatureDependencyGraph {

  private Map<String, Node> _nodeMap;

  /**
   * Constructs a FeatureDependencyGraph for a given map of features' dependency relationships and a list of which features
   * are anchored.
   *
   * @param dependencyFeatures Map of derived feature names to their sets of required inputs (described as TaggedFeatureNames)
   * @param anchoredFeatures List of anchored feature names
   */
  public FeatureDependencyGraph(Map<String, Set<ErasedEntityTaggedFeature>> dependencyFeatures, Collection<String> anchoredFeatures) {
    Map<String, Node> nodes = new HashMap<>();

    anchoredFeatures.forEach(featureName -> {
      Node node = nodes.computeIfAbsent(featureName, Node::new);
      node._anchored = true;
    });

    // If a derived feature depends on some feature that isn't defined anywhere else, what should we do?
    // 1.) We could make this an error case, such that all feature names need to be defined with their types.
    // 2.) Alternatively we can just silently continue with the assumption that this is an unknown/unreachable feature.
    // Going with #2 right now, but in the future with stricter typing we may want to switch to #1.
    dependencyFeatures.forEach((featureName, inputFeatures) -> {
      Node node = nodes.computeIfAbsent(featureName, Node::new);
      node._dependencies = inputFeatures.stream()
          .map(x -> x.getFeatureName().toString())
          .distinct()
          .map(x -> nodes.computeIfAbsent(x, Node::new))
          .collect(toSet());
      node._inputs = inputFeatures;
    });

    Set<Node> visited = new HashSet<>();
    Set<Node> ancestors = new HashSet<>();
    nodes.forEach((name, node) -> {
      checkForReachabilityAndCyclicDependencies(ancestors, visited, node);
      if (!ancestors.isEmpty()) {
        throw new RuntimeException(String.format("Assertion failed, ancestor not reachable. ancestors=%s visited=%s "
            + "node=%s", ancestors, visited, node));
      }
    });

    _nodeMap = Collections.unmodifiableMap(nodes);

    // Compute the max depth of each node, which is needed to compute the execution pipeline for a set of requested
    // features where each pipeline stage represents features that can be computed simultaneously.
    //
    // Algorithm: Compute the max depth of each node for the reachable features based on the topological sort ordering of
    // the entire DAG. Since topological sort orders the nodes by their dependency ordering, finding the max depth for
    // each node is simply to find the max depth of its dependencies that came previously in the topological ordering
    Set<String> reachableFeatures = _nodeMap.keySet().stream().filter(this::isReachable).collect(toSet());
    List<String> topologicalOrdering = findSimpleDependencyOrdering(reachableFeatures);
    topologicalOrdering.forEach((name) -> {
      Node node = nodes.get(name);
      if (node._dependencies.isEmpty()) {
        node._maxDepth = 0;
      } else {
        node._maxDepth = node._dependencies.stream().mapToInt(n -> n._maxDepth).max().getAsInt() + 1;
      }
    });
  }

  // starting from one node, do a DFS, marking whether nodes are reachable. throw exception in case of a cycle.
  private static void checkForReachabilityAndCyclicDependencies(Set<Node> ancestors, Set<Node> visited, Node node) {
    if (ancestors.contains(node)) {
      throw new RuntimeException("Detected dependency cycle: " + ancestors.stream().map(x -> x._name).collect(joining("->")));
    }

    if (!visited.contains(node)) {
      // even if the node is anchored, rather than short-circuiting we still examine the dependencies (protects against dependency cycles)
      visited.add(node);
      boolean allDependenciesAreReachable = !node._dependencies.isEmpty(); // if there are dependencies, default to true and disprove below.
      ancestors.add(node);
      for (Node dependency : node._dependencies) {
        checkForReachabilityAndCyclicDependencies(ancestors, visited, dependency);
        allDependenciesAreReachable &= dependency._reachable;
      }
      ancestors.remove(node);
      node._reachable = allDependenciesAreReachable | node._anchored;
    }
  }

  /**
   * Returns whether a given feature name is present in the dependency graph
   * @param feature
   * @return whether a given feature name is present in the dependency graph
   */
  public boolean isDeclared(String feature) {
    return _nodeMap.containsKey(feature);
  }

  /**
   * Returns whether a given feature is reachable
   * @param feature
   * @return isReachable Boolean
   */
  @Deprecated
  public boolean isReachable(String feature) {
    Node node = _nodeMap.get(feature);
    return (node != null && node._reachable);
  }

  /**
   * Returns whether a given feature is reachable
   * @param feature
   * @return A Pair of Boolean and String. Boolean indicates if it's reachable and the String indicates the error
   * message if not reachable.
   */
  public Pair<Boolean, String> isReachableWithErrorMessage(String feature) {
    Node node = _nodeMap.get(feature);
    String errorMessage = "";

    if (node == null) {
      errorMessage = String.format(
          "Trying to find feature %s in the dependency graph but didn't find any matched feature node. ", feature);
    }
    if (node != null && !node._reachable) {
      errorMessage = String.format(
          "Trying to find dependencies of feature %s in the dependency graph but failed. Please check its dependencies.%n", feature);
    }
    return new Pair<>((node != null && node._reachable), errorMessage);
  }

  /**
   * Given a set of input features, perform a DFS and produce a ordered list of feature dependencies for the
   * input features
   */
  private List<String> findSimpleDependencyOrdering(Collection<String> features) {
    LinkedHashSet<String> plan = new LinkedHashSet<>();
    features.forEach(feature -> findDependencyOrderingRecursive(feature, plan));
    return new ArrayList<>(plan);
  }

  // Note: Assumes feature is reachable. This assumption is safe to make for our recursive calls, because:
  //       1. We already verified all originally requested features are reachable.
  //       2. Therefore, all their dependency features are also reachable, else our dependency tree is invalid.
  //       3. We only make recursive calls for dependency features.
  private void findDependencyOrderingRecursive(String feature, LinkedHashSet<String> plan) {
    Pair<Boolean, String> reachableWithError = isReachableWithErrorMessage(feature);
    checkReachable(reachableWithError, feature);

    // relying heavily on LinkedHashSet's documented functionality, we use "plan" both to store the "visited" nodes in DFS search,
    // as well as the valid ordering of dependencies such that no element depends on another element coming after it in the list.
    Node node = _nodeMap.get(feature);
    if (!node._anchored && isReachable(node._name)) {
      node._dependencies.forEach(dependency -> findDependencyOrderingRecursive(dependency._name, plan));
    }
    plan.add(feature);
  }

  /**
   * Construct a plan for procuring a group of features.
   *
   * For a given group of features, what are all the features (transitive dependencies) that need to be procured in order
   * to derive them? This function returns a complete, ordered list of features sufficient to derive the given group of
   * features when evaluated in-order.
   *
   * @param features
   * @return Ordered list of feature names that can be resolved in-sequence to produce a superset of the given group of features.
   *         The returned list is NOT just a re-ordering of the input features, and may contain other features that weren't
   *         specifically requested but are required as dependencies. The returned list will always contain all of the features
   *         provided in the input.
   */
  public List<String> getPlan(Collection<String> features) {
    for (String feature : features) {
      Pair<Boolean, String> reachableWithError = isReachableWithErrorMessage(feature);
      checkReachable(reachableWithError, feature);
    }
    return findSimpleDependencyOrdering(features);
  }

  private void checkReachable(Pair<Boolean, String> reachableWithError, String feature) {
    if (!reachableWithError.fst) {
      throw new IllegalArgumentException("Requirement failed. Feature " + feature + " can't be resolved in the dependency graph.");
    }
  }

  /**
   * Use {@link #getOrderedPlanForFeatureUrns} instead.
   */
  @Deprecated
  public List<TaggedFeatureName> getOrderedPlanForRequest(Collection<TaggedFeatureName> request) {
    List<TaggedFeatureName> planWithStringTags = getOrderedPlanForFeatureUrns(request)
        .stream()
        .collect(Collectors.toList());
    return planWithStringTags;
  }

  /**
   * Returns an ordered list of features including the requested features and its dependencies that represents their
   * execution order.
   *
   * For example, if the feature dependency is A->B, B->C and (A,C) -> D.  Then one possible execution order would be:
   * A, B, C, D
   */
  public List<TaggedFeatureName> getOrderedPlanForFeatureUrns(Collection<TaggedFeatureName> request) {
    List<String> keyList = request.stream()
        .flatMap(x -> x.getKeyTag().stream())
        .distinct()
        .collect(toList());
    List<ErasedEntityTaggedFeature> erased = request.stream()
        .map(x -> TaggedFeatureUtils.eraseStringTags(x, keyList))
        .collect(toList());

    List<ErasedEntityTaggedFeature> plan = getOrderedPlanWithIntegerKeys(erased);

    List<TaggedFeatureName> planWithStringTags =
        plan.stream().map(x -> TaggedFeatureUtils.getTaggedFeatureNameFromStringTags(x, keyList)).collect(toList());
    return planWithStringTags;
  }

  /**
   * Return a computation pipeline (a collection of stages that  can be computed simultaneously) for the requested
   * features by examining their dependencies and grouping them based on their depth.  For each stage, all features will
   * have their direct dependencies resolved from the previous stages so they can be computed simultaneously
   *
   * For example, if the feature dependencies are as follows and features E and F are requested
   * A -> C
   * B -> D
   * (C,D) -> E
   * F
   *
   * Then the result would be returned in the form of an ordered list: [[A,B, F],[C,D],[E]] which represents
   * stage 1: A, B, F
   * stage 2: C, D
   * stage 3: E
   *
   * Disclaimer: the pipeline based approach provides one way to optimize the feature execution and their dependencies but
   * it is by no means the most optimal. For example, in the example above, if A is very slow compared to B and F, then
   * computation of C will be blocked until A is ready.  The optimal solution where each feature is computed in isolation
   * all the way from its root dependency.
   *
   * @return a sorted list of features represents the stages for feature execution
   */
  public List<Set<TaggedFeatureName>> getComputationPipeline(Collection<TaggedFeatureName> requestedFeatures) {
    List<TaggedFeatureName> orderedPlanForFeatureUrns = getOrderedPlanForFeatureUrns(requestedFeatures);
    // Group the features by their node depth in a sorted manner
    TreeMap<Integer, Set<TaggedFeatureName>> featureStages = new TreeMap<>();
    for (TaggedFeatureName featureUrn : orderedPlanForFeatureUrns) {
      Node currentFeatureNode = _nodeMap.get(featureUrn.getFeatureName().toString());
      featureStages.computeIfAbsent(currentFeatureNode._maxDepth, k -> new HashSet<>()).add(featureUrn);
    }
    return new ArrayList<>(featureStages.values());
  }

  private List<ErasedEntityTaggedFeature> getOrderedPlanWithIntegerKeys(Collection<ErasedEntityTaggedFeature> requested) {
    /*
      Consider a case where there are four features, A, B, C, and D.
       - A is anchored; and B, C, and D are derived
       - A, B, and C are a features; D is a a-a feature (dual key feature)

      The derived features B, C, and D are defined as follows:
          k:B = f1(k:A)
          k:C = f2(k:A)
          (k1,k2):D = f3(k1:C, k2:B)

      Now assume there's a request for:
        (x,y):D
      for some entities x and y (think viewer and viewee)

      The fully expanded dependency graph for (x,y):D is:

            x:A                  y:A
             / \                 / \
            /   \               /   \
          x:B   x:C           y:B   y:C
                  \           /
                   \         /
                    \       /
                     \     /
                      \   /
                     (x,y):D

      The desired "ordered plan" to produce (x,y):D is:
        [x:A, y:A, y:B, x:C, (x,y):D]
      There are other valid orderings as well, such as:
        [x:A, x:C, y:A, y:B, (x,y):D]

      The ordering is meaningful because we can resolve the features from first to last ensuring all dependencies will
      be available at the right time. (Because (x,y):D depends on all the other elements in the ordered list, it appears
      at the end.)

      The inline comments below reflect the example case where (0,1):D is being requested.

      */
    // [EXAMPLE] input: [ (0,1):D ]
    List<String> distinctFeatureNames = requested.stream().map(name -> name.getFeatureName().toString()).distinct().collect(toList());
    List<String> ordering = findSimpleDependencyOrdering(distinctFeatureNames);
    // [EXAMPLE] ordering: [A, B, C, D]

    // "scratch" is a mutable map tracking which key-tag configurations we need for each feature.
    // We start out by populating it with the key tags that were requested directly
    Map<String, Set<List<Integer>>> scratch = new HashMap<>();
    requested.forEach(tfn -> scratch.computeIfAbsent(tfn.getFeatureName().toString(), x -> new HashSet<>()).add(tfn.getBinding()));
    // [EXAMPLE] scratch: { D -> [(0,1)] }

    // after first iteration { D -> [(0,1)],
    //                         B -> [(1)],
    //                         C -> [(0)] }

    // after next iteration { D -> [(0,1)],
    //                        B -> [(1)],
    //                        C -> [(0)],
    //                        A -> [(1)] }

    // after next iteration { D -> [(0,1)],
    //                        B -> [(1)],
    //                        C -> [(0)],
    //                        A -> [(1), (0)] }

    Lists.reverse(ordering).forEach(featureName -> {
      if (!scratch.containsKey(featureName)) {
        // Scratch always contains features that were either directly requested or that are depended on by derived features
        // we've already processed.
        throw new RuntimeException("dependency graph " + scratch.toString() + " doesn't has this feature " + featureName);
      }
      Set<ErasedEntityTaggedFeature> dependencies = _nodeMap.get(featureName)._inputs;

      // Key tag integers have different meanings depending on scope.
      //
      //   anchors: {
      //     ... feature1, feature2, feature3 ...
      //   }
      //
      //   derivations: {
      //     feature4: {
      //       keys: [aA, aB]
      //       inputs: { a: { key: aA, feature: feature1 },
      //                 b: { key: aB, feature: feature1 } }
      //       definition: "(a + b)/(a * b)"
      //     }
      //
      //     feature5: {
      //       keys: [activity, viewer, viewee]
      //       inputs: { arg0: { key: [viewer, viewee], feature: feature4 },
      //                 arg1: { key: activity, feature: feature5 } }
      //       definition: "arg0 * arg1"
      //     }
      //
      // Feature4's raw dependencies are: [0:feature1, 1:feature1] where 0 is aA and 1 is aB
      // Feature5's raw dependencies are: [(1,2):feature4, 0:feature5] where 0 is activity, 1 is viewer, and 2 is viewee
      //
      // As shown in the example, the key tags have different meanings in different contexts. It is analogous to how
      // in Java and most other programming languages, variable/parameter names are specific within the scope of the function
      // in which they are defined. When building the dependency ordering, it is important for us to always resolve the
      // key tags of the derived feature in terms of the root scope's key tags.
      //
      // The process of resolving the key tags in terms of the parent scope is kind of like resolving a function's parameters
      // in terms of the caller's variables/namespace. That is why they are referred to below as "caller keyTag" and "callee keyTag".
      Set<List<Integer>> keyArgumentGroups = scratch.get(featureName);
      for (List<Integer> keyArguments : keyArgumentGroups) {
        for (ErasedEntityTaggedFeature dependency : dependencies) {
          List<Integer> dependencyKeyParameters = dependency.getBinding();
          String dependencyFeatureName = dependency.getFeatureName().toString();

          List<Integer> substitutedKeyParameters = dependencyKeyParameters.stream()
              .map(keyArguments::get)
              .collect(toList());

          scratch.computeIfAbsent(dependencyFeatureName, x -> new HashSet<>())
              .add(substitutedKeyParameters);
        }
      }
    });
    // scratch: { D -> [(0,1)],
    //            B -> [1],
    //            C -> [0],
    //            A -> [0, 1] }

    // flatMap should respect existing ordering
    List<ErasedEntityTaggedFeature> result = ordering.stream()
        .flatMap(featureName ->
            scratch.get(featureName).stream()
                .map(keyTag -> new ErasedEntityTaggedFeature(keyTag, featureName)))
        .collect(toList());
    return result;
  }

  private static class Node implements Serializable {
    Node(String name) {
      _name = name;
    }

    String _name;
    Set<Node> _dependencies = Collections.emptySet();
    Set<ErasedEntityTaggedFeature> _inputs = Collections.emptySet();
    boolean _anchored = false;
    boolean _reachable = false; // should we cache the compute plan, instead of just "that it's reachable"?
    // The maximum depth of a node represents the length of the longest path from anchored features nodes to the current
    // node. 0 implies there are no dependencies and -1 means it is unreachable
    int _maxDepth = -1;

    @Override
    public String toString() {
      return _name + " [reachable=" + _reachable + ", anchored=" + _anchored + ", depth=" + _maxDepth + "] -> " + _inputs;
    }
  }

  @Override
  public String toString() {
    return _nodeMap.values().toString();
  }

  private static class Pair<T, T1> {
    public T fst;
    public T1 snd;
    public Pair(T first, T1 second) {
      fst = first;
      snd = second;
    }
  }
}
