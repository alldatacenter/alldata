package com.linkedin.feathr.offline.logical

import com.linkedin.feathr.common
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException, FeathrException}
import com.linkedin.feathr.common.{FeatureDependencyGraph, JoiningFeatureParams}
import com.linkedin.feathr.offline.{ErasedEntityTaggedFeature, FeatureName, JoinStage, KeyTagIdTuple}
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.derived.DerivedFeature
import org.apache.logging.log4j.LogManager

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.convert.wrapAll._

/**
 * Multi-stage join planner is an implementation of Logical Planner in Feathr which analyzes the requested features,
 * derives the required features, groups them to different categories and in sequence within each category.
 * Checkout MultiStageJoinPlan for details.
 */
private[offline] class MultiStageJoinPlanner extends LogicalPlanner[MultiStageJoinPlan] {
  private val log = LogManager.getLogger(getClass)

  /**
   * Analyzes the key tagged features requested by feature join job or feature generation job
   * generates a join plan and returns [[MultiStageJoinPlan]].
   *
   * @param featureGroups [[FeatureGroups]] object.
   * @param keyTaggedFeatures requested features.
   * @return analyzed results, includes join stages, feature grouping, etc.
   */
  def getLogicalPlan(featureGroups: FeatureGroups, keyTaggedFeatures: Seq[JoiningFeatureParams]): MultiStageJoinPlan = {
    /*
     * Rewrite requested feature list in terms of integer key-tags. preserve integer-name mapping in 'keyTagIntsToStrings'
     * keyTagIntsToStrings is used to extract keys from observation data
     * Integer index in allRequestedFeatures is used to infer keys in feature dataset
     */
    val (allRequestedFeatures, keyTagIntsToStrings) = convertKeyTagsToIntegerIndexes(keyTaggedFeatures)
    log.info(s"allRequestedFeatures: $allRequestedFeatures, keyTagIntsToStrings: $keyTagIntsToStrings")

    // Resolve feature dependencies
    val allRequiredFeatures = getDependencyOrdering(featureGroups.allAnchoredFeatures, featureGroups.allDerivedFeatures, allRequestedFeatures)
    log.info(s"allRequiredFeatures: $allRequiredFeatures")

    // Plan the join stages required to resolve all these features
    val (windowAggFeatureStages, joinStages, postJoinDerivedFeatures) = getJoinStages(featureGroups, allRequiredFeatures)
    log.info(s"joinStages: $joinStages")

    // Separate sliding window aggregation features from other features, for special processing later
    val (requiredWindowAggFeatures, requiredNonWindowAggFeatures) = allRequiredFeatures
      .partition(taggedFeatureName => featureGroups.allWindowAggFeatures.contains(taggedFeatureName.getFeatureName))

    // Extract SeqJoin features here; SeqJoin features will be specifically processed during the join
    val (requestedSeqJoinFeatures, _) = allRequestedFeatures.partition(f => featureGroups.allSeqJoinFeatures.contains(f.getFeatureName))

    MultiStageJoinPlan(
      windowAggFeatureStages,
      joinStages,
      postJoinDerivedFeatures,
      requiredWindowAggFeatures,
      requiredNonWindowAggFeatures,
      requestedSeqJoinFeatures,
      keyTagIntsToStrings,
      allRequiredFeatures,
      allRequestedFeatures)
  }

  /**
   * Generate a list of join stages from the linear ordering of the feature dependency graph.
   *
   * Example input: [0:A, 1:A, 1:B, 0:C, (0,1):D]
   * Output Join Stages: [ ([0], [A, C]), ([1], [A, B]), ([0,1], [D]) ]
   *
   * @param featureGroups [[FeatureGroups]] which will be generated only using the feature def configs.
   * @param requiredFeatures a list of required feature names, as TaggedFeature.
   * @return 3 groups of stages:
   *         stages for window aggregation features,
   *         stages for regular non aggregation features,
   *         stages for post-join derived features.
   */
  def getJoinStages(featureGroups: FeatureGroups, requiredFeatures: Seq[common.ErasedEntityTaggedFeature]):
  (Seq[JoinStage], Seq[JoinStage], Seq[common.ErasedEntityTaggedFeature]) = {
    val allWindowAggFeatures = featureGroups.allWindowAggFeatures
    val allAnchoredFeatures = featureGroups.allAnchoredFeatures
    val allPassthroughFeatures = featureGroups.allPassthroughFeatures
    val allDerivedFeatures = featureGroups.allDerivedFeatures

    val windowAggFeaturesOrdered = requiredFeatures.filter(taggedFeature => allWindowAggFeatures.contains(taggedFeature.getFeatureName))

    // All required basic anchored features, basic anchored features are non-SWA features and non-passthrough features
    val requiredBasicAnchoredFeatures = requiredFeatures
      .filter(_.getBinding.nonEmpty) // Filter out only sequential join dependent expansion feature
      .filter(
        taggedFeature =>
          allAnchoredFeatures.contains(taggedFeature.getFeatureName)
            && (!allWindowAggFeatures.contains(taggedFeature.getFeatureName))
            && (!allPassthroughFeatures.contains(taggedFeature.getFeatureName)))

    val derivedFeaturesOrdered = requiredFeatures.filter(taggedFeature => allDerivedFeatures.contains(taggedFeature.getFeatureName))

    val windowAggFeatureStage: Seq[JoinStage] = windowAggFeaturesOrdered
      .groupBy(_.getBinding.map(_.toInt).toSeq) // convert binding (java Integer) to scala int
      .mapValues(_.map(_.getFeatureName).toIndexedSeq)
      .toSeq

    val joinStages = new mutable.HashMap[KeyTagIdTuple, Seq[FeatureName]]
    // ErasedEntityTaggedFeature.getBinding returns a java list of integers, need to convert to Seq[Int]
    joinStages ++= requiredBasicAnchoredFeatures
      .groupBy(_.getBinding.map(_.asInstanceOf[Int]))
      .mapValues(_.map(_.getFeatureName).toIndexedSeq)
    /*
     * optimization: for any derived features that depend ONLY on features joined in a single stage,
     *               compute those derived features WHILE doing the join.
     *               all the rest of the derived features can be done in a single stage at the end.
     */
    val postJoinDerivedFeatures = new mutable.ArrayBuffer[common.ErasedEntityTaggedFeature]

    derivedFeaturesOrdered.foreach(taggedFeatureName => {
      val ErasedEntityTaggedFeature(keyTag, featureRefStr) = taggedFeatureName
      joinStages.get(keyTag) match {
        case Some(featureNamesJoinedInThisStage) =>
          val dependencies = getDependenciesForTaggedFeature(allAnchoredFeatures, allDerivedFeatures, taggedFeatureName)
          if (dependencies.forall(x =>
                keyTag.equals(x.getBinding.map(_.toInt))
                  && featureNamesJoinedInThisStage.contains(x.getFeatureName))) {
            joinStages.put(keyTag, joinStages.getOrElse(keyTag, Nil) :+ featureRefStr)
          } else {
            postJoinDerivedFeatures.append(taggedFeatureName)
          }
        case None =>
          postJoinDerivedFeatures.append(taggedFeatureName)
      }
    })

    // how do we know which join stage to run first?
    // IDEA: run join stages with fewer features first.
    //       ideally this would reduce the shuffle write size.
    val adjustedJoinStages = joinStages.mapValues(_.toIndexedSeq).toIndexedSeq.filter(_._1.nonEmpty).sortBy(_._2.size)
    (windowAggFeatureStage, adjustedJoinStages, postJoinDerivedFeatures)
  }

  /**
   * Notes about the dependency graph for key-tagged features (different from the dependency graph for untagged features)
   *
   * A Key-tagged feature (aka subject-ascribed feature) is like viewer:a_x or viewee:a_x,
   * while a "raw" untagged feature is something like just plain a_x.
   *
   * A Derived feature (such as "the difference between viewer's and viewee's connection count") might depend on a single
   * raw feature (a's connection count) but more than one tagged-feature (viewer:a_x and viewee:)
   *
   * Assume A is anchored.
   *
   *     x:A                  y:A
   *     / \                 / \
   *    /   \               /   \
   *  x:B   x:C           y:B   y:C
   *        \           /
   *         \         /
   *          \       /
   *           \     /
   *            \   /
   *           (x,y):D
   * Note: in the implementation, x and y are actually integers, e.g, use (0,1) to represent (viewerid, vieweeid)
   * Feature Graph (resolve for D):
   * D -> B, C
   * C -> A
   * B -> A
   * Ordered Plan: [A], [B, C, D]    (Note: currently plan is consist of two parts, first [] contains anchored feature (A),
   * second [] contains derived feature B,C,D. features in same [] are calculated in sequential order
   * However, ideally, this could be represented as [A], [B,C], [D], where features in same [] can
   * be calculated in parallel, features in different [] should be calculated in sequential order,
   * or even better, record all dependencies, but make no sequential order representation, a feature
   * begin to calculate whenever all dependencies are met.
   *
   * Subject-Feature Graph (resolve for `(x,y):D`): (resolve happens in a bottom-up manner, as bottom-features are the required features)
   * (x,y):D -> x:C, y:B
   * x:C -> x:A
   * y:B -> y:A
   * Ordered Plan: [x:A, y:A], [y:B, x:C, (x,y):D]
   * "Join A for key `x` and join A for key `y`. Then compute these derived features in this order."
   *
   * Note: "Feature Dump" isn't possible here in general cases. You need ids for the join, right?
   * We could probably support feature dump for single-key cases.
   *
   *
   * Furthermore: Need to be careful about whether different anchors come from the same source.
   *
   *
   * (source1)---- x:E --- x:A ---------- y:A     y:F ----(source2)
   *                       / \            / \
   *                      /   \          /   \
   *                    x:B   x:C      y:B   y:C
   *                            \      /
   *                             \    /
   *                              \  /
   *                             (x,y):D
   *
   * Request: [ x:E, y:F, (x,y):D ]
   * Plan:
   * source1: DataFrame
   * source2: DataFrame
   *
   * 0       [x:E, x:A] <- [ source1(A.extractor, E.extractor) ]
   * 1  [x:E, x:A, x:C] <- [ source1(A.extractor, E.extractor)(C.derivation) ]
   *
   * 2       [y:A, y:F] <- [ source1(A.extractor), source2(F.extractor) ]  // merging unresolved keys allowed if they're the same key?
   * 3  [y:A, y:F, y:B] <- [ source1(A.extractor)(B.derivation), source2(F.extractor) ]
   *
   * [ x:source1(A.extractor, E.extractor)(C.derivation) ], y:source1(A.extractor)(B.derivation), source2(F.extractor) ](D.derivation)
   *
   * @param allAnchoredFeatures Map from anchor feature name to [[FeatureAnchorWithSource]]
   * @param allDerivedFeatures  Map from derived feature to [[DerivedFeature]]
   * @param requestedFeatures
   * @return
   */
  def getDependencyOrdering(allAnchoredFeatures: Map[String, FeatureAnchorWithSource], allDerivedFeatures: Map[String, DerivedFeature],
    requestedFeatures: Seq[common.ErasedEntityTaggedFeature]): Seq[common.ErasedEntityTaggedFeature] = {
    // input: [ (0,1):D ]
    val ordering = constructFeatureDependencyGraph(allDerivedFeatures, allAnchoredFeatures)
      .getPlan(requestedFeatures.map(_.getFeatureName).distinct)
      .toIndexedSeq
    /*
     * ordering: [A, C, B, D]
     * Now for each feature name or featureRefStr in the ordering, what tags do i need?
     * I need the feature for a given tag if:
     * 1. it is requested explicitly, or
     * 2. it is required by a derived feature (possibly transitively)
     */

    // "scratch" is a mutable map tracking which key-tag configurations we need for each feature.
    // We start out by populating it with the key tags that were requested directly
    val scratch = new mutable.HashMap[String, mutable.Set[Seq[Int]]] with mutable.MultiMap[String, Seq[Int]]
    requestedFeatures.foreach { case ErasedEntityTaggedFeature(keyTag, featureRefStr) => scratch.addBinding(featureRefStr, keyTag) }
    /*
     * scratch: { D -> [(0,1)] }
     * after first iteration { D -> [(0,1)],
     *                         B -> [(1)],
     *                         C -> [(0)] }
     * after next iteration  { D -> [(0,1)],
     *                         B -> [(1)],
     *                         C -> [(0)],
     *                         A -> [(1)] }
     * after next iteration  { D -> [(0,1)],
     *                         B -> [(1)],
     *                         C -> [(0)],
     *                         A -> [(1), (0)] }
     * Walk the list of feature names or featureRefStr in reverse. This is because the ordering produced by `dependencyGraph` has each
     * element's dependencies to its left.
     */
    ordering.reverseIterator.foreach(featureRefStr => {
      if (!scratch.contains(featureRefStr)) {
        // Scratch always contains features that were either directly requested or that are depended on by derived features
        // we've already processed.
        throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, s"Unknown feature $featureRefStr in an internal data structure. Likely a bug.")
      }

      val rawDependencies = getDependenciesForFeature(allAnchoredFeatures, allDerivedFeatures, featureRefStr)
      /*
       * Key tag integers have different meanings depending on scope.
       *
       *   anchors: {
       *     ... feature1, feature2, feature3 ...
       *   }
       *
       *   derivations: {
       *     feature4: {
       *       keys: [aA, aB]
       *       inputs: { a: { key: aA, feature: feature1 },
       *                 b: { key: aB, feature: feature1 } }
       *       definition: "(a + b)/(a * b)"
       *     }
       *
       *     feature5: {
       *       keys: [activity, viewer, viewee]
       *       inputs: { arg0: { key: [viewer, viewee], feature: feature4 },
       *                 arg1: { key: activity, feature: feature5 } }
       *       definition: "arg0 * arg1"
       *     }
       *
       * Feature4's raw dependencies are: [0:feature1, 1:feature1] where 0 is aA and 1 is aB
       * Feature5's raw dependencies are: [(1,2):feature4, 0:feature5] where 0 is activity, 1 is viewer, and 2 is viewee
       *
       * As shown in the example, the key tags have different meanings in different contexts. It is analogous to how
       * in Java and most other programming languages, variable/parameter names are specific within the scope of the function
       * in which tye are defined. When building the dependency ordering, it is important for us to always resolve the
       * key tags of the derived feature in terms of the root scope's key tags. In Feathr-offline the root scope is the
       * joinConfig.
       *
       * The process of resolving the key tags in terms of the parent scope is kind of like resolving a function's parameters
       * in terms of the caller's variables/namespace. That is why they are referred to below as "caller keyTag" and "callee keyTag".
       */
      val callerKeyTags = scratch(featureRefStr)
      if (callerKeyTags.nonEmpty) {
        for (callerKeyTag <- callerKeyTags; ErasedEntityTaggedFeature(calleeKeyTag, dependencyName) <- rawDependencies) {
          if (calleeKeyTag.isEmpty) {
            scratch.addBinding(dependencyName, calleeKeyTag)
          } else {
            scratch.addBinding(dependencyName, calleeKeyTag.map(callerKeyTag))
          }
        }
      }
    })
    /*
     * scratch: { D -> [(0,1)],
     *            B -> [1],
     *            C -> [0],
     *            A -> [0, 1] }
     * WATCH OUT: does flatMap respect ordering?
     */
    ordering.flatMap(x => scratch(x).map(ErasedEntityTaggedFeature(_, x)))
    // return: [0:A, 1:A, 1:B, 0:C, (0,1):D]
  }

  /**
   * Utility method that converts [[JoiningFeatureParams]] to [[ErasedEntityTaggedFeature]].
   */
  def convertKeyTagsToIntegerIndexes(requestFeatures: Seq[JoiningFeatureParams]): (Seq[common.ErasedEntityTaggedFeature], Seq[String]) = {
    // Seq[String] behaves like a Map[Int, String]
    val allKeyTagStrings = requestFeatures.flatMap(joiningFeatureParams => joiningFeatureParams.keyTags).distinct.toIndexedSeq
    val keyTagStringIndex: Map[String, Int] = allKeyTagStrings.zipWithIndex.toMap

    // If a feature alias and time delay is specified, then we will treat it as a new feature (as it needs to computed). So, we
    // will create an EETF with feature alias name. Otherwise, the EETF will be associated with the feature name itself.
    val taggedFeatureNames = requestFeatures.map {
      joiningFeatureParams =>
        if (joiningFeatureParams.featureAlias.isDefined && joiningFeatureParams.timeDelay.isDefined) {
          ErasedEntityTaggedFeature(joiningFeatureParams.keyTags.map(keyTagStringIndex).toIndexedSeq, joiningFeatureParams.featureAlias.get)
        } else {
          ErasedEntityTaggedFeature(joiningFeatureParams.keyTags.map(keyTagStringIndex).toIndexedSeq, joiningFeatureParams.featureName)
        }
    }.toIndexedSeq
    (taggedFeatureNames, allKeyTagStrings)
  }

  /**
   * This method uses the feathr common library to build the feature dependency graph.
   * @param derivedFeatures   a map of derived feature name or featureRefStr to its deserialized representation of definition.
   * @param anchoredFeatures  a map of anchored feature name or featureRefStr to its deserialized representation of definition.
   * @return FeatureDependencyGraph instance.
   */
  private def constructFeatureDependencyGraph(
      derivedFeatures: Map[String, DerivedFeature],
      anchoredFeatures: Map[String, FeatureAnchorWithSource]): FeatureDependencyGraph = {
    val dependencyFeaturesMap = derivedFeatures.map {
      case (featureName, derivedFeature) =>
        (featureName, derivedFeature.consumedFeatureNames.toSet.asJava)
    }.asJava
    val anchoredFeaturesSet = anchoredFeatures.keySet.asJava
    new FeatureDependencyGraph(dependencyFeaturesMap, anchoredFeaturesSet)
  }

  /**
   * This method returns all dependent features as [[common.ErasedEntityTaggedFeature]] for input feature.
   * If the feature is anchored feature then return an empty sequence.
   * If the feature is derived feature then return dependent features from feature definition.
   * @param allAnchoredFeatures Map from anchor feature name to [[FeatureAnchorWithSource]]
   * @param allDerivedFeatures  Map from derived feature to [[DerivedFeature]]
   * @param featureName feature name or featureRefStr
   * @return dependent features as Seq[ErasedEntityTaggedFeature].
   */
  def getDependenciesForFeature(allAnchoredFeatures: Map[String, FeatureAnchorWithSource], allDerivedFeatures: Map[String, DerivedFeature],
    featureName: String): Seq[common.ErasedEntityTaggedFeature] = {
    if (allAnchoredFeatures.contains(featureName)) {
      Seq.empty[common.ErasedEntityTaggedFeature]
    } else if (allDerivedFeatures.contains(featureName)) {
      allDerivedFeatures(featureName).consumedFeatureNames
    } else {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Feature $featureName is not defined in the config.")
    }
  }

  /**
   * This method returns dependent features as [[common.ErasedEntityTaggedFeature]] for a tagged input feature.
   * The method determines the featureRef string of the input feature, to get the dependencies and then
   * updates the tags with that of the input.
   */
  private def getDependenciesForTaggedFeature(allAnchoredFeatures: Map[String, FeatureAnchorWithSource], allDerivedFeatures: Map[String, DerivedFeature],
    erasedEntityTaggedFeature: common.ErasedEntityTaggedFeature): Seq[common.ErasedEntityTaggedFeature] = {
    // ErasedEntityTaggedFeature.getBinding returns a java list of integers, need to convert to Seq[Int]
    getDependenciesForFeature(allAnchoredFeatures, allDerivedFeatures, erasedEntityTaggedFeature.getFeatureName).map {
      case ErasedEntityTaggedFeature(calleeKeyTag, dependencyName) =>
        ErasedEntityTaggedFeature(calleeKeyTag.map(erasedEntityTaggedFeature.getBinding.map(_.asInstanceOf[Int])), dependencyName)
    }
  }
}

/**
 * Companion object for Logical Planner.
 */
private[offline] object MultiStageJoinPlanner {
  def apply(): MultiStageJoinPlanner = new MultiStageJoinPlanner
}
