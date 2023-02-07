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
package org.apache.drill.exec.expr.fn.impl;

import io.netty.buffer.DrillBuf;

/** SQL Pattern Contains implementation */
public final class SqlPatternContainsMatcher extends AbstractSqlPatternMatcher {
  private final MatcherFcn matcherFcn;

  public SqlPatternContainsMatcher(String patternString) {
    super(patternString);

    // Pattern matching is 1) a CPU intensive operation and 2) pattern and input dependent. The conclusion is
    // that there is no single implementation that can do it all well. So, we use multiple implementations
    // chosen based on the pattern length.
    if (patternLength == 0) {
      matcherFcn = new MatcherZero();
    } else if (patternLength == 1) {
      matcherFcn = new MatcherOne();
    } else if (patternLength == 2) {
      matcherFcn = new MatcherTwo();
    } else if (patternLength == 3) {
      matcherFcn = new MatcherThree();
    } else if (patternLength < 10) {
      matcherFcn = new MatcherN();
    } else {
      matcherFcn = new BoyerMooreMatcher();
    }
  }

  @Override
  public int match(int start, int end, DrillBuf drillBuf) {
    return matcherFcn.match(start, end, drillBuf);
  }

  //--------------------------------------------------------------------------
  // Inner Data Structure
  // --------------------------------------------------------------------------

  /** Abstract matcher class to allow us pick the most efficient implementation */
  private abstract class MatcherFcn {
    protected final byte[] patternArray;

    protected MatcherFcn() {
      assert patternByteBuffer.hasArray();

      patternArray = patternByteBuffer.array();
    }

    /**
     * @return 1 if the pattern was matched; 0 otherwise
     */
    protected abstract int match(int start, int end, DrillBuf drillBuf);
  }

  /** Handles patterns with length zero */
  private final class MatcherZero extends MatcherFcn {

    private MatcherZero() {
    }

    /** {@inheritDoc} */
    @Override
    protected final int match(int start, int end, DrillBuf drillBuf) {
      return 1;
    }
  }

  /** Handles patterns with length one */
  private final class MatcherOne extends MatcherFcn {
    final byte firstPatternByte;

    private MatcherOne() {
      firstPatternByte  = patternArray[0];
    }

    /** {@inheritDoc} */
    @Override
    protected final int match(int start, int end, DrillBuf drillBuf) {
      final int lengthToProcess = end - start;

      // simplePattern string has meta characters i.e % and _ and escape characters removed.
      // so, we can just directly compare.
      for (int idx = 0; idx < lengthToProcess; idx++) {
        byte inputByte = drillBuf.getByte(start + idx);

        if (firstPatternByte != inputByte) {
          continue;
        }
        return 1;
      }
      return 0;
    }
  }

  /** Handles patterns with length two */
  private final class MatcherTwo extends MatcherFcn {
    final byte firstPatternByte;
    final byte secondPatternByte;

    private MatcherTwo() {
      firstPatternByte  = patternArray[0];
      secondPatternByte = patternArray[1];
    }

    /** {@inheritDoc} */
    @Override
    protected final int match(int start, int end, DrillBuf drillBuf) {
      final int lengthToProcess = end - start - 1;

      // simplePattern string has meta characters i.e % and _ and escape characters removed.
      // so, we can just directly compare.
      for (int idx = 0; idx < lengthToProcess; idx++) {
        final byte firstInByte = drillBuf.getByte(start + idx);

        if (firstPatternByte != firstInByte) {
          continue;
        } else {
          final byte secondInByte = drillBuf.getByte(start + idx + 1);

          if (secondInByte == secondPatternByte) {
            return 1;
          }
        }
      }
      return 0;
    }
  }

  /** Handles patterns with length three */
  private final class MatcherThree extends MatcherFcn {
    final byte firstPatternByte;
    final byte secondPatternByte;
    final byte thirdPatternByte;

    private MatcherThree() {
      firstPatternByte   = patternArray[0];
      secondPatternByte  = patternArray[1];
      thirdPatternByte   = patternArray[2];
    }

    /** {@inheritDoc} */
    @Override
    protected final int match(int start, int end, DrillBuf drillBuf) {
      final int lengthToProcess = end - start - 2;

      // simplePattern string has meta characters i.e % and _ and escape characters removed.
      // so, we can just directly compare.
      for (int idx = 0; idx < lengthToProcess; idx++) {
        final byte inputByte = drillBuf.getByte(start + idx);

        if (firstPatternByte != inputByte) {
          continue;
        } else {
          final byte secondInByte = drillBuf.getByte(start + idx + 1);
          final byte thirdInByte  = drillBuf.getByte(start + idx + 2);

          if (secondInByte == secondPatternByte && thirdInByte == thirdPatternByte) {
            return 1;
          }
        }
      }
      return 0;
    }
  }

  /** Handles patterns with arbitrary length */
  private final class MatcherN extends MatcherFcn {
    final byte firstPatternByte;

    private MatcherN() {
      firstPatternByte = patternArray[0];
    }

    /** {@inheritDoc} */
    @Override
    protected final int match(int start, int end, DrillBuf drillBuf) {
      final int lengthToProcess = end - start - patternLength + 1;
      int patternIndex          = 0;

      // simplePattern string has meta characters i.e % and _ and escape characters removed.
      // so, we can just directly compare.
      for (int idx = 0; idx < lengthToProcess; idx++) {
        final byte inputByte = drillBuf.getByte(start + idx);

        if (firstPatternByte == inputByte) {
          for (patternIndex = 1; patternIndex < patternLength; ++patternIndex) {
            final byte currInByte   = drillBuf.getByte(start + idx + patternIndex);
            final byte currPattByte = patternArray[patternIndex];

            if (currInByte != currPattByte) {
              break;
            }
          }

          if (patternIndex == patternLength) {
            return 1;
          }
        }
      }
      return 0;
    }
  }

  /**
   * Boyer-Moore matcher algorithm; excellent for large patterns and for prefix patterns which appear
   * frequently in the input.
   */
  private final class BoyerMooreMatcher extends MatcherFcn {
    private final int[] offsetTable;
    private final int[] characterTable;

    private BoyerMooreMatcher() {
      super();

      this.offsetTable    = makeOffsetTable();
      this.characterTable = makeCharTable();
    }

    /** {@inheritDoc} */
    @Override
    protected int match(int start, int end, DrillBuf drillBuf)  {
      final int inputLength = end - start;

      for (int idx1 = patternLength - 1, idx2; idx1 < inputLength;) {
        for (idx2 = patternLength - 1; patternArray[idx2] == drillBuf.getByte(start + idx1); --idx1, --idx2) {
          if (idx2 == 0) {
            return 1;
          }
        }
        // idx1 += pattern.length - idx2; // For naive method
        idx1 += Math.max(offsetTable[patternLength - 1 - idx2], characterTable[drillBuf.getByte(start + idx1) & 0xFF]);
      }
      return 0;
    }

    /** Build the jump table based on the mismatched character information **/
    private int[] makeCharTable() {
      final int TABLE_SIZE = 256; // This implementation is based on byte comparison
      int[] resultTable    = new int[TABLE_SIZE];

      for (int idx = 0; idx < resultTable.length; ++idx) {
        resultTable[idx] = patternLength;
      }

      for (int idx = 0; idx < patternLength - 1; ++idx) {
        final int patternValue    = ((int) patternArray[idx]) & 0xFF;
        resultTable[patternValue] = patternLength - 1 - idx;
      }

      return resultTable;
    }

    /** Builds the scan offset based on which mismatch occurs. **/
    private int[] makeOffsetTable() {
      int[] resultTable      = new int[patternLength];
      int lastPrefixPosition = patternLength;

      for (int idx = patternLength - 1; idx >= 0; --idx) {
        if (isPrefix(idx + 1)) {
          lastPrefixPosition = idx + 1;
        }
        resultTable[patternLength - 1 - idx] = lastPrefixPosition - idx + patternLength - 1;
      }

      for (int idx = 0; idx < patternLength - 1; ++idx) {
        int suffixLen          = suffixLength(idx);
        resultTable[suffixLen] = patternLength - 1 - idx + suffixLen;
      }

      return resultTable;
    }

    /** Checks whether needle[pos:end] is a prefix of pattern **/
    private boolean isPrefix(int pos) {
      for (int idx1 = pos, idx2 = 0; idx1 < patternLength; ++idx1, ++idx2) {
        if (patternArray[idx1] != patternArray[idx2]) {
          return false;
        }
      }
      return true;
    }

    /** Computes the maximum length of the substring ends at "pos" and is a suffix **/
    private int suffixLength(int pos) {
      int result = 0;
      for (int idx1 = pos, idx2 = patternLength - 1; idx1 >= 0 && patternArray[idx1] == patternArray[idx2]; --idx1, --idx2) {
        result += 1;
      }
      return result;
    }
  }

}
