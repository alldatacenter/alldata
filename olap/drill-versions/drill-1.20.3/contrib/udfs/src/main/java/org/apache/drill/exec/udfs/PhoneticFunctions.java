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

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.VarCharHolder;

import javax.inject.Inject;

public class PhoneticFunctions {

  /**
   * The Caverphone function is a phonetic matching function.   This is an algorithm created by the Caversham Project at the University of Otago. It implements the Caverphone 1.0 algorithm.
   * <p>
   * <p>
   * Usage:  SELECT caverphone1( string ) FROM...
   */
  @FunctionTemplate(name = "caverphone1", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class Caverphone1Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.Caverphone1().encode(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * The Caverphone function is a phonetic matching function.   This is an algorithm created by the Caversham Project at the University of Otago. It implements the Caverphone 2.0 algorithm.
   * <p>
   * Usage: SELECT caverphone2( string ) FROM...
   */
  @FunctionTemplate(name = "caverphone2", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class Caverphone2Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.Caverphone2().encode(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * Encodes a string into a Cologne Phonetic value.
   * Implements the Kölner Phonetik (Cologne Phonetic) algorithm issued by Hans Joachim Postel in 1969.
   * <p>
   * The Kölner Phonetik is a phonetic algorithm which is optimized for the German language.
   * It is related to the well-known soundex algorithm.
   * <p>
   * Usage:  SELECT cologne_phonetic( string ) FROM...
   */
  @FunctionTemplate(name = "cologne_phonetic", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class ColognePhoneticFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.ColognePhonetic().encode(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * Encodes a string into a Daitch-Mokotoff Soundex value.
   * The Daitch-Mokotoff Soundex algorithm is a refinement of the Russel and American Soundex algorithms,
   * yielding greater accuracy in matching especially Slavish and Yiddish surnames with similar pronunciation
   * but differences in spelling.
   * <p>
   * The main differences compared to the other soundex variants are:
   * coded names are 6 digits long
   * the initial character of the name is coded
   * rules to encoded multi-character n-grams
   * multiple possible encodings for the same name (branching)
   * <p>
   * Usage:  SELECT dm_soundex( string ) FROM...
   */
  @FunctionTemplate(name = "dm_soundex", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class DaitchMokotoffFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.DaitchMokotoffSoundex().encode(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * Match Rating Approach Phonetic Algorithm Developed by Western Airlines in 1977.
   * Usage:  SELECT match_rating_encoder( string ) FROM...
   */
  @FunctionTemplate(name = "match_rating_encoder", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class MatchRatingFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.MatchRatingApproachEncoder().encode(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * The New York State Identification and Intelligence System Phonetic Code, commonly known as NYSIIS, is a phonetic algorithm devised in 1970 as part of the New York State Identification and Intelligence System (now a part of the New York State Division of Criminal Justice Services). It features an accuracy increase of 2.7% over the traditional Soundex algorithm.
   * Encodes a string into a NYSIIS value. NYSIIS is an encoding used to relate similar names, but can also be used as a general purpose scheme to find word with similar phonemes.
   * <p>
   * Usage: SELECT nysiis(string) FROM...
   */
  @FunctionTemplate(name = "nysiis", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class NYSIISFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.Nysiis().encode(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }
  }

  /**
   * Encodes a string into a Refined Soundex value. Soundex is an encoding used to relate similar names, but can also be used as a general purpose scheme to find word with similar phonemes.
   * <p>
   * Usage:  SELECT refined_soundex( string ) FROM...
   */
  @FunctionTemplate(name = "refined_soundex", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class RefinedSoundexFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.RefinedSoundex().encode(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * Encodes a string into a Soundex value. Soundex is an encoding used to relate similar names, but can also be used as a general purpose scheme to find word with similar phonemes.
   * <p>
   * Usage:  SELECT soundex( string ) FROM...
   */
  @FunctionTemplate(name = "soundex", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class SoundexFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.Soundex().soundex(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }
  }

  /**
   * Implements the Metaphone phonetic algorithm (https://en.wikipedia.org/wiki/Metaphone),
   * and calculates a given string's Metaphone value.
   * <p>
   * Usage: SELECT metaphone( string ) FROM...
   */
  @FunctionTemplate(name = "metaphone", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class MetaphoneFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.Metaphone().metaphone(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * Implements the Double Metaphone phonetic algorithm (https://en.wikipedia.org/wiki/Metaphone),
   * and calculates a given string's Double Metaphone value.
   * <p>
   * Usage: SELECT double_metaphone( string ) FROM...
   */
  @FunctionTemplate(name = "double_metaphone", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class DoubleMetaphoneFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = new org.apache.commons.codec.language.DoubleMetaphone().doubleMetaphone(input);
      outputString = outputString == null ? "" : outputString;

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }
  }
}