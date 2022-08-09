/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.query;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class SelectClauseComposer {
    private static final String COUNT_STR = "count";
    private static final String MIN_STR = "min";
    private static final String MAX_STR = "max";
    private static final String SUM_STR = "sum";

    private final String[]                     labels;
    private final String[]                     attributes;
    private final String[]                     items;
    private final int                          countIdx;
    private final int                          sumIdx;
    private final int                          minIdx;
    private final int                          maxIdx;
    private final int                          aggCount;
    private final Map<Integer, AggregatorFlag> aggregatorFlags      = new HashMap();
    private final Set<Integer>                 isNumericAggregator  = new HashSet<>();
    private final Set<Integer>                 isPrimitiveAttr      = new HashSet<>();
    private final Map<String, String>          itemAssignmentExprs  = new LinkedHashMap<>();
    private       boolean                      isSelectNoop         = false;
    private       int                          introducedTypesCount = 0;

    public enum AggregatorFlag {
        NONE, COUNT, MIN, MAX, SUM
    }

    public SelectClauseComposer(String[] labels, String[] attributes, String[] items, int countIdx, int sumIdx, int minIdx, int maxIdx) {
        this.labels     = labels;
        this.attributes = Arrays.copyOf(attributes, attributes.length);
        this.items      = Arrays.copyOf(items, items.length);
        this.countIdx   = countIdx;
        this.sumIdx     = sumIdx;
        this.minIdx     = minIdx;
        this.maxIdx     = maxIdx;

        int aggCount = 0;

        if (countIdx != -1) {
            this.aggregatorFlags.put(countIdx, AggregatorFlag.COUNT);
            aggCount++;
        }

        if (sumIdx != -1) {
            this.aggregatorFlags.put(sumIdx, AggregatorFlag.SUM);
            aggCount++;
        }

        if (maxIdx != -1) {
            this.aggregatorFlags.put(maxIdx, AggregatorFlag.MAX);
            aggCount++;
        }

        if (minIdx != -1) {
            this.aggregatorFlags.put(minIdx, AggregatorFlag.MIN);
            aggCount++;
        }

        this.aggCount = aggCount;
    }

    public static boolean isKeyword(String s) {
        return COUNT_STR.equals(s) ||
               MIN_STR.equals(s) ||
               MAX_STR.equals(s) ||
               SUM_STR.equals(s);
    }

    public String[] getItems() {
        return items;

    }

    public boolean updateAsApplicable(int currentIndex, String propertyForClause, String qualifiedName) {
        boolean ret = false;

        if (currentIndex == getCountIdx()) {
            ret = assign(currentIndex, COUNT_STR, GremlinClause.INLINE_COUNT.get(), GremlinClause.INLINE_ASSIGNMENT);

            this.isNumericAggregator.add(currentIndex);
        } else if (currentIndex == getMinIdx()) {
            ret = assign(currentIndex, MIN_STR, propertyForClause,  GremlinClause.INLINE_ASSIGNMENT, GremlinClause.INLINE_MIN);

            this.isNumericAggregator.add(currentIndex);
        } else if (currentIndex == getMaxIdx()) {
            ret = assign(currentIndex, MAX_STR, propertyForClause, GremlinClause.INLINE_ASSIGNMENT, GremlinClause.INLINE_MAX);

            this.isNumericAggregator.add(currentIndex);
        } else if (currentIndex == getSumIdx()) {
            ret = assign(currentIndex, SUM_STR, propertyForClause, GremlinClause.INLINE_ASSIGNMENT, GremlinClause.INLINE_SUM);

            this.isNumericAggregator.add(currentIndex);
        }

        attributes[currentIndex] = qualifiedName;

        return ret;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public boolean assign(int i, String qualifiedName, GremlinClause clause) {
        items[i] = clause.get(qualifiedName, qualifiedName);

        return true;
    }

    public String[] getLabels() {
        return labels;
    }

    public boolean onlyAggregators() {
        return hasAggregators() && aggCount == items.length;
    }

    public boolean hasAggregators() {
        return aggCount > 0;
    }

    public String getLabelHeader() {
        return getJoinedQuotedStr(getLabels());
    }

    public String getItemsString() {
        return String.join(",", getItems());
    }

    public String getAssignmentExprString(){
        return (!itemAssignmentExprs.isEmpty()) ? String.join(" ", itemAssignmentExprs.values()) : StringUtils.EMPTY;
    }

    public String getItem(int i) {
        return items[i];
    }

    public String getAttribute(int i) {
        return attributes[i];
    }

    public String getLabel(int i) {
        return labels[i];
    }

    public int getAttrIndex(String attr) {
        int ret = -1;

        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].equals(attr)) {
                ret = i;

                break;
            }
        }

        return ret;
    }

    public int getCountIdx() {
        return countIdx;
    }

    public int getSumIdx() {
        return sumIdx;
    }

    public int getMaxIdx() {
        return maxIdx;
    }

    public int getMinIdx() {
        return minIdx;
    }

    public boolean isAggregatorWithArgument(int i) {
        return i == getMaxIdx() || i == getMinIdx() || i == getSumIdx();
    }

    public void incrementTypesIntroduced() {
        introducedTypesCount++;
    }

    public int getIntroducedTypesCount() {
        return introducedTypesCount;
    }

    public boolean hasMultipleReferredTypes() {
        return getIntroducedTypesCount() > 1;
    }

    public boolean hasMixedAttributes() {
        return getIntroducedTypesCount() > 0 && getPrimitiveTypeCount() > 0;
    }

    private int getPrimitiveTypeCount() {
        return isPrimitiveAttr.size();
    }

    public boolean getIsSelectNoop() {
        return this.isSelectNoop;
    }

    public void setIsSelectNoop(boolean isSelectNoop) {
        this.isSelectNoop = isSelectNoop;
    }

    public boolean isSumIdx(int idx) {
        return aggregatorFlags.get(idx) == AggregatorFlag.SUM;
    }

    public boolean isMinIdx(int idx) {
        return aggregatorFlags.get(idx) == AggregatorFlag.MIN;
    }

    public boolean isMaxIdx(int idx) {
        return aggregatorFlags.get(idx) == AggregatorFlag.MAX;
    }

    public boolean isCountIdx(int idx) {
        return aggregatorFlags.get(idx) == AggregatorFlag.COUNT;
    }

    public boolean isNumericAggregator(int idx) {
        return isNumericAggregator.contains(idx);
    }

    public boolean isPrimitiveAttribute(int idx) {
        return isPrimitiveAttr.contains(idx);
    }

    public void setIsPrimitiveAttr(int i) {
        this.isPrimitiveAttr.add(i);
    }


    private boolean assign(String item, String assignExpr) {
        itemAssignmentExprs.put(item, assignExpr);

        return true;
    }

    private boolean assign(int i, String s, String p1, GremlinClause clause) {
        items[i] = s;

        return assign(items[i], clause.get(s, p1));
    }

    private boolean assign(int i, String s, String p1, GremlinClause inline, GremlinClause clause) {
        items[i] = s;

        return assign(items[i], inline.get(s, clause.get(p1, p1)));
    }

    private String getJoinedQuotedStr(String[] elements) {
        StringJoiner joiner = new StringJoiner(",");

        Arrays.stream(elements)
                .map(x -> x.contains("'") ? "\"" + x + "\"" : "'" + x + "'")
                .forEach(joiner::add);

        return joiner.toString();
    }
}
