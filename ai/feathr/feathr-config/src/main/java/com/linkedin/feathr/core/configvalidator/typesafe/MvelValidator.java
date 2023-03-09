package com.linkedin.feathr.core.configvalidator.typesafe;

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKey;
import com.linkedin.feathr.core.config.producer.anchors.ExpressionBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.ExtractorBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExpr;
import com.linkedin.feathr.core.config.producer.derivations.SimpleDerivationConfig;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * package private class to validate MVEL expression
 */
class MvelValidator {

  private static final MvelValidator MVEL_VALIDATOR = new MvelValidator();
  private MvelValidator() {

  }

  static MvelValidator getInstance() {
    return MVEL_VALIDATOR;
  }

  /**
   * validate MVEL expressions in FeatureDef config
   * @param featureDefConfig the {@link FeatureDefConfig} object
   * @return validation result in the format of {@link ValidationResult}
   */
  ValidationResult validate(FeatureDefConfig featureDefConfig) {
    // mapping from feature/anchor name to its MVEL expression
    Map<String, List<String>> invalidMvels = getPossibleInvalidMvelsUsingIn(featureDefConfig);
    if (!invalidMvels.isEmpty()) {
      Set<String> invalidMvelInfoSet = invalidMvels.entrySet().stream()
          .map(e -> String.join(": ", e.getKey(), "[", String.join(", ", e.getValue()), "]"))
          .collect(Collectors.toSet());
      String warnMsg = String.join("", "For MVEL expression, if you are using `in` expression, ",
          "there should be parenthesis around it. Based on a heuristic check, the following anchors/features have invalid MVEL ",
          "definitions containing `in` keyword: \n", String.join("\n", invalidMvelInfoSet));
      return new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.WARN, warnMsg);
    }
    return new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.VALID);
  }

  /**
   * heuristic check to find all invalid MVEL expression using "in"
   * @param featureDefConfig the {@link FeatureDefConfig} object
   * @return mapping of feature name to its invalid MVEL expression
   */
  Map<String, List<String>> getPossibleInvalidMvelsUsingIn(FeatureDefConfig featureDefConfig) {
    Map<String, List<String>> invalidFeatureMvels = getFeatureMvels(featureDefConfig).entrySet().stream()
        .filter(e -> !heuristicProjectionExprCheck(e.getValue())) // get all heuristically invalid MVEL expressions
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

    Map<String, List<String>> invalidAnchorKeyMvels = getAnchorKeyMvels(featureDefConfig).entrySet().stream()
        .filter(e -> !e.getValue().stream().allMatch(this::heuristicProjectionExprCheck))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return Stream.concat(invalidFeatureMvels.entrySet().stream(), invalidAnchorKeyMvels.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * collect all features whose definition is based on MVEL
   * @return mapping of feature name to its MVEL expression
   */
  @VisibleForTesting
  Map<String, String> getFeatureMvels(FeatureDefConfig featureDefConfig) {
    Map<String, String> featureNameToMvel = new HashMap<>();

    // get MVEL expression from each anchor
    BiConsumer<String, AnchorConfig> consumeAnchor = (anchorName, anchorConfig) -> {
      for (Map.Entry<String, FeatureConfig> entry : anchorConfig.getFeatures().entrySet()) {
        FeatureConfig featureConfig = entry.getValue();
        String featureName = entry.getKey();
        if (featureConfig instanceof ExtractorBasedFeatureConfig) {
          featureNameToMvel.put(featureName, ((ExtractorBasedFeatureConfig) featureConfig).getFeatureName());
        } else if (featureConfig instanceof ExpressionBasedFeatureConfig) {
          ExpressionBasedFeatureConfig expressionBasedFeatureConfig = (ExpressionBasedFeatureConfig) featureConfig;
          if (expressionBasedFeatureConfig.getExprType() == ExprType.MVEL) {
            featureNameToMvel.put(featureName, expressionBasedFeatureConfig.getFeatureExpr());
          }
        } else if (featureConfig instanceof TimeWindowFeatureConfig) {
          TimeWindowFeatureConfig timeWindowFeatureConfig = (TimeWindowFeatureConfig) featureConfig;
          if (timeWindowFeatureConfig.getColumnExprType() == ExprType.MVEL) {
            featureNameToMvel.put(featureName, timeWindowFeatureConfig.getColumnExpr());
          }
        } // for the rest FeatureConfig types, do nothing
      }
    };

    featureDefConfig.getAnchorsConfig().ifPresent(anchorsConfig ->
        anchorsConfig.getAnchors().forEach(consumeAnchor)
    );

    // get MVEL expression from each derivation
    BiConsumer<String, DerivationConfig> consumeDerivation = (featureName, derivationConfig) -> {
      // SimpleDerivationConfig can have MVEL and SQL expr type
      if (derivationConfig instanceof SimpleDerivationConfig) {
        SimpleDerivationConfig simpleDerivationConfig = ((SimpleDerivationConfig) derivationConfig);
        if (simpleDerivationConfig.getFeatureTypedExpr().getExprType() == ExprType.MVEL) {
          featureNameToMvel.put(featureName, simpleDerivationConfig.getFeatureTypedExpr().getExpr());
        }
      } else if (derivationConfig instanceof DerivationConfigWithExpr) {
        DerivationConfigWithExpr derivationConfigWithExpr = (DerivationConfigWithExpr) derivationConfig;
        if (derivationConfigWithExpr.getTypedDefinition().getExprType() == ExprType.MVEL) {
          featureNameToMvel.put(featureName, derivationConfigWithExpr.getTypedDefinition().getExpr());
        }
      } // for the rest DerivationConfig types, do nothing
    };

    featureDefConfig.getDerivationsConfig().ifPresent(derivationsConfig ->
        derivationsConfig.getDerivations().forEach(consumeDerivation)
    );
    return featureNameToMvel;
  }

  /**
   * get MVEL expressions used in anchor level
   * for now, just key definition in type {@link AnchorConfigWithKey}
   * @param featureDefConfig
   * @return
   */
  Map<String, List<String>> getAnchorKeyMvels(FeatureDefConfig featureDefConfig) {
    Map<String, List<String>> anchorNameToMvel = new HashMap<>();

    // get MVEL expression from each anchor
    BiConsumer<String, AnchorConfig> consumeAnchor = (anchorName, anchorConfig) -> {
      // if anchor keys are MVEL expressions,
      if (anchorConfig instanceof AnchorConfigWithKey) {
        AnchorConfigWithKey anchorConfigWithKey = (AnchorConfigWithKey) anchorConfig;
        if (anchorConfigWithKey.getTypedKey().getKeyExprType() == ExprType.MVEL) {
          anchorNameToMvel.put(anchorName, anchorConfigWithKey.getKey());
        }
      }
    };

    featureDefConfig.getAnchorsConfig().ifPresent(anchorsConfig ->
        anchorsConfig.getAnchors().forEach(consumeAnchor)
    );

    return anchorNameToMvel;
  }

  /**
   * heuristic check if a given MVEL projection expression(http://mvel.documentnode.com/#projections-and-folds) is valid
   *
   * When inspecting very complex object models inside collections, MVEL requires parentheses around the
   * projection expression. If missing the parentheses, sometimes it
   *  won't throw exception. Instead, it will only return wrong results.
   *
   * Without a fully-built MVEL syntax and semantic analyzer, we can only perform some heuristic check here.
   * The heuristic strategy is to first search for the “in” keyword,
   * and then try to locate the parentheses around the keyword.
   * The check is based on the observation that if there are multiple `in`, then these `in` are nested
   * Specifically, the following checks are performed:
   * 1. check if parenthesis are balanced
   * 2. for each `in`, check if there is a parentheses pair around it, and there can not be other `in` within the pair
   *    If the pair is used to match a `in`, it can not be used to match other `in`
   *
   * Some valid examples are:
   * - "(parent.name in users)"
   * - "(name in (familyMembers in users))"
   *
   * Some invalid examples are:
   * - "parent.name in users"
   * - "(name in familyMembers in users)"
   * - "(some expression) familyMembers in users"
   * @param mvelExpr the MVEL expression
   * @return heuristic result of whether the MVEL projection expression is valid
   */
  boolean heuristicProjectionExprCheck(String mvelExpr) {
    String inKeyword = " in "; // make sure it is a single word

    // find all "in" occurrences backward
    List<Integer> reversedInPosList = new ArrayList<>();
    int index = mvelExpr.lastIndexOf(inKeyword);
    while (index >= 0) {
      reversedInPosList.add(index);
      index = mvelExpr.lastIndexOf(inKeyword, index - 1);
    }

    // if no "in" keyword, return true
    if (reversedInPosList.isEmpty()) {
      return true;
    }

    /*
     * check if parentheses is balanced
     */
    List<Integer> sortedLeftParenthesis = new LinkedList<>();
    Stack<Integer> stack = new Stack<>(); // use stack to make sure the parenthesis is balanced
    for (int pos = 0; pos < mvelExpr.length(); pos++) {
      if (mvelExpr.charAt(pos) == '(') {
        stack.push(pos); // record the left parenthesis position
      } else if (mvelExpr.charAt(pos) == ')') {
        if (stack.isEmpty()) {
          return false; // unbalanced parenthesis
        }
        int leftPos = stack.pop();
        /* record the parenthesis pair positions
         * do not record if it is pair on the left side of the first "in", or on the right side of the last "in"
         */
        if (pos < reversedInPosList.get(reversedInPosList.size() - 1) || leftPos > reversedInPosList.get(0)) {
          continue;
        }
        sortedLeftParenthesis.add(leftPos);
      }
    }

    // quick check if there are enough parenthesis pairs
    return reversedInPosList.size() <= sortedLeftParenthesis.size();

    /* TODO As heuristic check, the current one above is enough for existing cases. But we can add more strict check,
     *   to cover more extreme case, if we discover any in the future. Here just document the idea, as it is expensive
     *   to perform the check, but we might be dealing with non-existing use cases.
     *
     * Based on the observation that for projection with nested "in", the inner "in" expression is always on the right side,
     *  we check all "in" keywords from right to left.
     * For each "in", find the right most "(" on its left. There should be no other "in" keyword between the pair of parentheses,
     * and the "in" should be within the parentheses pair.
     * If yes, remove the pair of parentheses as it is matched for the specific "in" keyword, and can not be used for
     * other "in" keyword.
     * If no, or if there are not enough pair of parentheses, return invalid
     */
  }
}
