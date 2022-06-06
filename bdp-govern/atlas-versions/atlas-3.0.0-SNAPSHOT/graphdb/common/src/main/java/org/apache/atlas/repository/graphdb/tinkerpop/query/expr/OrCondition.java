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
package org.apache.atlas.repository.graphdb.tinkerpop.query.expr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents an OrCondition that has one or more AndConditions as it children.  The OrCondition
 * matches vertices that meet the criteria in at least one of its children.  The overall query result is
 * computed by executing the TitanGraphQuery queries that correspond to each AndCondition individually and
 * then unioning their results together. This is needed because the native Titan query mechanism does not
 * natively support 'OR' conditions.  When we execute the query, we accomplish the 'OR' by executing all of the
 * individual queries and unioning the results together.
 *
 */
public class OrCondition {

    private List<AndCondition> children;

    public OrCondition() {
        this(true);
    }

    private OrCondition(List<AndCondition> children) {
        this.children = children;
    }

    public OrCondition(boolean addInitialTerm) {
        this.children = new ArrayList<>();
        if (addInitialTerm) {
            children.add(new AndCondition());
        }
    }

    /**
    /**
     * Updates this OrCondition in place so that it  matches vertices that satisfy the current
     * OrCondition AND that match the specified predicate.
     *
     * @param other
     */
    public void andWith(QueryPredicate predicate) {

        for (AndCondition child : children) {
            child.andWith(predicate);
        }
    }

    public List<AndCondition> getAndTerms() {
        return children;
    }

    /**
     * Updates this OrCondition in place so that it matches vertices that satisfy the current
     * OrCondition AND that satisfy the provided OrCondition.
     *
     * @param other
     */
    public void andWith(OrCondition other) {

        //Because Titan does not natively support Or conditions in Graph Queries,
        //we need to expand out the condition so it is in the form of a single OrCondition
        //that contains only AndConditions.  We do this by following the rules of boolean
        //algebra.  As an example, suppose the current condition is ((a=1 and b=2) or (c=3 and d=4)).
        //Suppose "other" is ((e=5 and f=6) or (g=7 or h=8)).  The overall condition, after applying the
        //"and" is:
        //
        //((a=1 and b=2) or (c=3 and d=4)) and ((e=5 and f=6) or (g=7 and h=8))
        //
        //This needs to be expanded out to remove the nested or clauses.  The result of this expansion is:
        //
        //(a=1 and b=2 and e=5 and f=6) or
        //(a=1 and b=2 and g=7 and h=8) or
        //(c=3 and d=4 and e=5 and f=6) or
        //(c=3 and d=4 and g=7 and h=8)

        //The logic below does this expansion, in a generalized way.  It loops through the existing AndConditions
        //and, in a nested loop, through the AndConditions in "other".  For each of those combinations,
        //it creates a new AndCondition that combines the two AndConditions together.  These combined
        //AndConditions become the new set of AndConditions in this OrCondition.

        List<AndCondition> expandedExpressionChildren = new ArrayList<>();
        for (AndCondition otherExprTerm : other.getAndTerms()) {
            for (AndCondition currentExpr : children) {
                AndCondition currentAndConditionCopy = currentExpr.copy();
                currentAndConditionCopy.andWith(otherExprTerm.getTerms());
                expandedExpressionChildren.add(currentAndConditionCopy);
            }
        }
        children = expandedExpressionChildren;
    }

    /**
     * Updates this OrCondition in place so that it matches vertices that satisfy the current
     * OrCondition OR that satisfy the provided OrCondition.
     *
     * @param other
     */
    public void orWith(OrCondition other) {
        children.addAll(other.getAndTerms());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OrCondition [andExprs=");
        Iterator<AndCondition> it = children.iterator();
        while (it.hasNext()) {
            AndCondition andExpr = it.next();
            builder.append(andExpr.toString());
            if (it.hasNext()) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

}
