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

public enum GremlinClause {
    AS("as('%s')"),
    DEDUP("dedup()"),
    G("g"),
    GROUP_BY("group().by('%s')"),
    HAS("has('%s', %s)"),
    HAS_OPERATOR("has('%s', %s(%s))"),
    HAS_NOT_OPERATOR("or(__.has('%s', neq(%s)), __.hasNot('%s'))"),
    HAS_PROPERTY("has('%s')"),
    WHERE("where(%s)"),
    HAS_NOT_PROPERTY("hasNot('%s')"),
    HAS_TYPE("has('__typeName', '%s')"),
    HAS_TYPE_WITHIN("has('__typeName', within(%s))"),
    HAS_WITHIN("has('%s', within(%s))"),
    IN("in('%s')"),
    OR("or(%s)"),
    AND("and(%s)"),
    NESTED_START("__"),
    NESTED_HAS_OPERATOR("has('%s', %s(%s))"),
    LIMIT("limit(%s)"),
    ORDER_BY("order().by('%s')"),
    ORDER_BY_DESC("order().by('%s', desc)"),
    OUT("out('%s')"),
    RANGE("range(%s, %s + %s)"),
    SELECT("select('%s')"),
    TO_LIST("toList()"),
    STRING_CONTAINS("has('%s', org.janusgraph.core.attribute.Text.textRegex(%s))"),
    TEXT_CONTAINS("has('%s', org.janusgraph.core.attribute.Text.textContainsRegex(%s))"),
    TRAIT("outE('classifiedAs').has('__name', within('%s')).outV()"),
    ANY_TRAIT("or(has('__traitNames'), has('__propagatedTraitNames'))"),
    NO_TRAIT("and(hasNot('__traitNames'), hasNot('__propagatedTraitNames'))"),
    SELECT_NOOP_FN("def f(r){ r }; "),
    SELECT_FN("def f(r){ t=[[%s]]; %s r.each({t.add([%s])}); t.unique(); }; "),
    SELECT_ONLY_AGG_FN("def f(r){ t=[[%s]]; %s t.add([%s]); t;}; "),
    SELECT_ONLY_AGG_GRP_FN("def f(l){ t=[[%s]]; l.get(0).each({k,r -> L:{ %s t.add([%s]); } }); t; }; "),
    // Optional sorting required here
    SELECT_MULTI_ATTR_GRP_FN("def f(l){ h=[[%s]]; t=[]; l.get(0).each({k,r -> L:{ %s r.each({t.add([%s])}) } }); h.plus(t.unique()%s); }; "),
    INLINE_ASSIGNMENT("def %s=%s;"),
    INLINE_LIST_RANGE("[%s..<%s]"),
    INLINE_COUNT("r.size()"),
    INLINE_SUM("r.sum({it.value('%s')})"),
    INLINE_MAX("r.max({it.value('%s')}).value('%s')"),
    INLINE_MIN("r.min({it.value('%s')}).value('%s')"),
    INLINE_GET_PROPERTY("it.property('%s').isPresent() ? it.value('%s') : \"\""),
    INLINE_TRANSFORM_CALL("f(%s)"),
    INLINE_DEFAULT_SORT(".sort()"),
    INLINE_SORT_DESC(".sort{a,b -> b <=> a}"),
    INLINE_DEFAULT_TUPLE_SORT(".sort{a,b -> a[0] <=> b[0]}"),
    // idx of the tuple field to be sorted on
    INLINE_TUPLE_SORT_ASC(".sort{a,b -> a[%s] <=> b[%s]}"),
    INLINE_TUPLE_SORT_DESC(".sort{a,b -> b[%s] <=> a[%s]}"),
    TERM("where(in('r:AtlasGlossarySemanticAssignment').has('AtlasGlossaryTerm.%s', '%s'))"),

    V("V()"),
    VALUE_MAP("valueMap(%s)");

    private final String template;

    GremlinClause(String template) {
        this.template = template;
    }

    String get() {
        return template;
    }

    String get(String... args) {
        return (args == null || args.length == 0) ? template : String.format(template, args);
    }
}
