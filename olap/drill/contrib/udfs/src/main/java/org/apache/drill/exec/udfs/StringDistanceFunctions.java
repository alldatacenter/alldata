/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.udfs;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

public class StringDistanceFunctions {

  /**
   * This function calculates the cosine distance between two strings.
   * Usage:  SELECT cosine_distance( string1, string2 ) AS cosine_distance FROM...
   */
  @FunctionTemplate(name = "cosine_distance", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class CosineDistanceFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput1;

    @Param
    VarCharHolder rawInput2;

    @Workspace
    org.apache.commons.text.similarity.CosineDistance d;

    @Output
    Float8Holder out;

    @Override
    public void setup() {
      d = new org.apache.commons.text.similarity.CosineDistance();
    }

    @Override
    public void eval() {

      String input1 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput1.start, rawInput1.end, rawInput1.buffer);
      String input2 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput2.start, rawInput2.end, rawInput2.buffer);


      out.value = d.apply(input1, input2);
    }
  }

  /**
   * This function calculates the cosine distance between two strings.
   * A matching algorithm that is similar to the searching algorithms implemented in editors such
   * as Sublime Text, TextMate, Atom and others.
   * <p>
   * One point is given for every matched character. Subsequent matches yield two bonus points. A higher score
   * indicates a higher similarity.
   * <p>
   * <p>
   * Usage:  SELECT fuzzy_score( string1, string2 ) AS fuzzy_score FROM...
   */
  @FunctionTemplate(name = "fuzzy_score", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class FuzzyScoreFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput1;

    @Param
    VarCharHolder rawInput2;

    @Output
    Float8Holder out;

    @Workspace
    org.apache.commons.text.similarity.FuzzyScore d;

    @Override
    public void setup() {
      d = new org.apache.commons.text.similarity.FuzzyScore(java.util.Locale.ENGLISH);
    }

    @Override
    public void eval() {

      String input1 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput1.start, rawInput1.end, rawInput1.buffer);
      String input2 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput2.start, rawInput2.end, rawInput2.buffer);

      out.value = d.fuzzyScore(input1, input2);
    }
  }

  /**
   * The hamming distance between two strings of equal length is the number of
   * positions at which the corresponding symbols are different.
   * <p>
   * For further explanation about the Hamming Distance, take a look at its
   * Wikipedia page at http://en.wikipedia.org/wiki/Hamming_distance.
   * <p>
   * Usage:  SELECT hamming_distance( string1, string2 ) FROM...
   */
  @FunctionTemplate(name = "hamming_distance", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class HammingDistanceFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput1;

    @Param
    VarCharHolder rawInput2;

    @Output
    Float8Holder out;

    @Workspace
    org.apache.commons.text.similarity.HammingDistance d;

    @Override
    public void setup() {
      d = new org.apache.commons.text.similarity.HammingDistance();
    }

    @Override
    public void eval() {

      String input1 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput1.start, rawInput1.end, rawInput1.buffer);
      String input2 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput2.start, rawInput2.end, rawInput2.buffer);

      out.value = d.apply(input1, input2);
    }
  }


  /**
   * Measures the Jaccard distance of two sets of character sequence. Jaccard
   * distance is the dissimilarity between two sets. It is the complementary of
   * Jaccard similarity.
   * <p>
   * For further explanation about Jaccard Distance, refer
   * https://en.wikipedia.org/wiki/Jaccard_index
   * <p>
   * Usage:  SELECT jaccard_distance( string1, string2 ) FROM ...
   */
  @FunctionTemplate(name = "jaccard_distance", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class JaccardDistanceFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput1;

    @Param
    VarCharHolder rawInput2;

    @Output
    Float8Holder out;

    @Workspace
    org.apache.commons.text.similarity.JaccardDistance d;

    @Override
    public void setup() {
      d = new org.apache.commons.text.similarity.JaccardDistance();
    }

    @Override
    public void eval() {

      String input1 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput1.start, rawInput1.end, rawInput1.buffer);
      String input2 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput2.start, rawInput2.end, rawInput2.buffer);

      out.value = d.apply(input1, input2);
    }
  }

  /**
   * A similarity algorithm indicating the percentage of matched characters between two character sequences.
   * <p>
   * The Jaro measure is the weighted sum of percentage of matched characters
   * from each file and transposed characters. Winkler increased this measure
   * for matching initial characters.
   * <p>
   * This implementation is based on the Jaro Winkler similarity algorithm
   * from https://en.wikipedia.org/wiki/Jaro–Winkler_distance
   * <p>
   * Usage: SELECT jaro_distance( string1, string2 ) FROM...
   */
  @FunctionTemplate(name = "jaro_distance", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class JaroDistanceFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput1;

    @Param
    VarCharHolder rawInput2;

    @Output
    Float8Holder out;

    @Workspace
    org.apache.commons.text.similarity.JaroWinklerDistance d;

    @Override
    public void setup() {
      d = new org.apache.commons.text.similarity.JaroWinklerDistance();
    }

    @Override
    public void eval() {

      String input1 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput1.start, rawInput1.end, rawInput1.buffer);
      String input2 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput2.start, rawInput2.end, rawInput2.buffer);

      out.value = d.apply(input1, input2);
    }
  }

  /**
   * An algorithm for measuring the difference between two character sequences.
   * <p>
   * This is the number of changes needed to change one sequence into another,
   * where each change is a single character modification (deletion, insertion
   * or substitution).
   * <p>
   * Usage: SELECT levenshtein_distance( string1, string2 ) FROM...
   */
  @FunctionTemplate(name = "levenshtein_distance", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class LevenstheinDistanceFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput1;

    @Param
    VarCharHolder rawInput2;

    @Output
    Float8Holder out;

    @Workspace
    org.apache.commons.text.similarity.LevenshteinDistance d;

    @Override
    public void setup() {
      d = new org.apache.commons.text.similarity.LevenshteinDistance();
    }

    @Override
    public void eval() {

      String input1 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput1.start, rawInput1.end, rawInput1.buffer);
      String input2 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput2.start, rawInput2.end, rawInput2.buffer);

      out.value = d.apply(input1, input2);
    }
  }

  /**
   * The Longest common subsequence algorithm returns the length of the longest subsequence that two strings have in common.
   * Two strings that are entirely different, return a value of 0, and two strings that return a value of the
   * commonly shared length implies that the strings are completely the same in value and position.
   * Note: Generally this algorithm is fairly inefficient, as for length m, n of the input
   * CharSequence's left and right respectively, the runtime of the algorithm is O(m*n).
   * <p>
   * This implementation is based on the Longest Commons Substring algorithm from https://en.wikipedia.org/wiki/Longest_common_subsequence_problem.
   * <p>
   * Usage:  SELECT longest_common_substring_distance( string1, string2 ) FROM...
   */
  @FunctionTemplate(name = "longest_common_substring_distance", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class LongestCommonSubstringDistanceFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput1;

    @Param
    VarCharHolder rawInput2;

    @Output
    Float8Holder out;

    @Workspace
    org.apache.commons.text.similarity.LongestCommonSubsequenceDistance d;

    @Override
    public void setup() {
      d = new org.apache.commons.text.similarity.LongestCommonSubsequenceDistance();
    }

    @Override
    public void eval() {

      String input1 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput1.start, rawInput1.end, rawInput1.buffer);
      String input2 = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput2.start, rawInput2.end, rawInput2.buffer);

      out.value = d.apply(input1, input2);
    }
  }

}
