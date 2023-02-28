/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.db.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述一组后缀范围。
 * <pre>
 *   NamePattern = Name | ( Prefix SuffixExpr )
 *   SuffixExpr = "[" NameSuffix *( "," NameSuffix ) "]"
 *   NameSuffix = Pattern | NameRange
 *   NameRange = Min "-" Max
 * </pre>
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年8月26日
 */
public final class NameRange {

    static final long[] pow10 = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L };

    private int zeroPadding;

    private long min, max;

    static boolean numericCheck(String input) {
        final int len = input.length();
        if (len == 0) {
            // Empty input!
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(input.charAt(i)))
                return false;
        }
        return true;
    }

    public final List<String> list() {
        return iterate(new StringBuilder(), new ArrayList<String>());
    }

    static String paddingZero(final long number, final int zeroPadding) {
        final long pow = pow10[zeroPadding];
        if (number < pow) {
            return String.valueOf(pow + number).substring(1);
        }
        // 数值超出补 0 长度, 输出原始值
        return String.valueOf(number);
    }

    public NameRange(String numeric) {
        this.zeroPadding = (numeric.charAt(0) == '0') ? numeric.length() : 0;
        this.min = Long.parseLong(numeric);
        this.max = min;
    }

    public NameRange(final long min, final long max, final int zeroPadding) {
        if (zeroPadding > 18) {
            throw new IllegalArgumentException("ZeroPadding must less than 18, but given " + zeroPadding);
        }
        this.zeroPadding = zeroPadding;
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public int getZeroPadding() {
        return zeroPadding;
    }

    private boolean merge(long min, long max, int zeroPadding) {
        if ((max < this.min - 1) || (min > this.max + 1)) {
            // 数值不在范围内
            return false;
        }
        if (zeroPadding > 0) {
            if (this.zeroPadding == 0) {
                if (Long.toString(this.min).length() != zeroPadding || Long.toString(this.max).length() != zeroPadding) {
                    // 数值长度不相同
                    return false;
                }
                this.zeroPadding = zeroPadding;
            } else if (this.zeroPadding != zeroPadding) {
                // 补 0 长度不相同
                return false;
            }
        } else if (zeroPadding == 0) {
            if (this.zeroPadding != 0) {
                if (Long.toString(min).length() != this.zeroPadding || Long.toString(max).length() != this.zeroPadding) {
                    // 数值长度不相同
                    return false;
                }
            }
        }
        if (min < this.min)
            this.min = min;
        if (max > this.max)
            this.max = max;
        return true;
    }

    public boolean put(long number, int zeroPadding) {
        return merge(number, number, zeroPadding);
    }

    public boolean merge(NameRange nameRange) {
        return merge(nameRange.min, nameRange.max, nameRange.zeroPadding);
    }

    public boolean contains(String name) {
        if (!numericCheck(name)) {
            // 内容不是数值
            return false;
        }
        if (zeroPadding != 0 && name.length() != zeroPadding) {
            // 数值不是固定长度
            return false;
        }
        final long number = Long.parseLong(name);
        return (number >= min) && (number <= max);
    }

    protected List<String> iterate(StringBuilder buf, List<String> list) {
        final int len = buf.length();
        for (long number = min; number <= max; number++) {
            if (zeroPadding != 0) {
                buf.append(paddingZero(number, zeroPadding));
            } else {
                buf.append(number);
            }
            list.add(buf.toString());
            buf.setLength(len);
        }
        return list;
    }

    /**
     * NameSuffix = Pattern | NameRange
     *
     * NameRange = Min "-" Max
     */
    public static NameRange loadInput(String input) {
        final int len = input.length();
        final int minusIndex = input.indexOf('-');
        if (minusIndex < 0) {
            if (numericCheck(input)) {
                final long min = Long.parseLong(input);
                final int zeroPadding = (input.charAt(0) == '0') ? len : 0;
                return new NameRange(min, min, zeroPadding);
            }
            // Range 不是数值, 解析失败
            throw new IllegalArgumentException("Range not number: " + input);
        } else if (minusIndex < 1 || minusIndex + 1 >= len) {
            // Range 前后内容不全, 解析失败
            throw new IllegalArgumentException("Range not complete: " + input);
        }
        String min = input.substring(0, minusIndex);
        String max = input.substring(minusIndex + 1);
        if (!numericCheck(min) || !numericCheck(max)) {
            // min, max 其中一个不是数值, 解析失败
            throw new // NL
            IllegalArgumentException("Range min/max not number: " + input);
        }
        if (min.charAt(0) == '0' || max.charAt(0) == '0') {
            if (min.length() == max.length()) {
                return new // NL
                NameRange(Long.parseLong(min), Long.parseLong(max), min.length());
            }
        }
        // Range 前后长度不相等, 不需要补 0
        return new NameRange(Long.parseLong(min), Long.parseLong(max), 0);
    }

    protected StringBuilder buildString(StringBuilder buf) {
        if (max > min) {
            if (zeroPadding != 0) {
                buf.append(paddingZero(min, zeroPadding));
                buf.append('-');
                buf.append(paddingZero(max, zeroPadding));
            } else {
                buf.append(min);
                buf.append('-');
                buf.append(max);
            }
        } else {
            if (zeroPadding != 0) {
                buf.append(paddingZero(min, zeroPadding));
            } else {
                buf.append(min);
            }
        }
        return buf;
    }

    public static void main(String[] args) {
        NameRange nameRange = NameRange.loadInput("032-063");
    // System.out.println("Range: " + nameRange);
    // System.out.println(" contains 037: " + nameRange.contains("037"));
    // System.out.println(" contains 121: " + nameRange.contains("121"));
    // System.out.println(" contains 56: " + nameRange.contains("56"));
    // System.out.println("list: " +
    // Arrays.toString(nameRange.list().toArray()));
    // System.out.println("0112: " + NameRange.loadInput("0112"));
    // System.out.println("12: " + NameRange.loadInput("12"));
    // System.out.println();
    //
    // NameRange mergeRange = NameRange.loadInput("064-127");
    // System.out.println("Range merge: " + mergeRange);
    // System.out.println(" Return: " // NL
    // + nameRange.merge(mergeRange) + ", " + nameRange);
    // System.out.println();
    //
    // NameRange simpleRange = NameRange.loadInput("128");
    // System.out.println("Simple merge: " + simpleRange);
    // System.out.println(" Return: " // NL
    // + nameRange.merge(simpleRange) + ", " + nameRange);
    // System.out.println();
    }
}
