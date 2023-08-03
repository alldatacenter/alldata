package com.linkedin.feathr.offline.evaluator

/**
 * The classes that implement this trait represent a computation stage of
 * processing features. These computations take in a context T that represents
 * a context required for processing and returns result of type R
 */
private[offline] trait StageEvaluator[T, R] {

  /**
   * Evaluates input features.
   * @param features features of the stage.
   * @param keyTags  keyTags for the stage.
   * @param context  context necessary for processing.
   * return evaluated output
   */
  def evaluate(features: Seq[String], keyTags: Seq[Int], context: T): R
}
