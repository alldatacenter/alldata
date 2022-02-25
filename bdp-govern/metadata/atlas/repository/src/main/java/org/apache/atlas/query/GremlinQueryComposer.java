/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.query;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.glossary.GlossaryUtils;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasBuiltInTypes;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.atlas.model.discovery.SearchParameters.ALL_CLASSIFICATIONS;
import static org.apache.atlas.model.discovery.SearchParameters.NO_CLASSIFICATIONS;
import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection.IN;
import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection.OUT;

public class GremlinQueryComposer {
    private static final String ISO8601_FORMAT              = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String ISO8601_DATE_FORMAT         = "yyyy-MM-dd";
    private static final String REGEX_ALPHA_NUMERIC_PATTERN = "[a-zA-Z0-9]+";
    private static final String EMPTY_STRING                = "";
    private static final int    DEFAULT_QUERY_RESULT_LIMIT  = 25;
    private static final int    DEFAULT_QUERY_RESULT_OFFSET = 0;

    private static final ThreadLocal<DateFormat[]> DSL_DATE_FORMAT = ThreadLocal.withInitial(() -> {
        final String[]     formats = { ISO8601_FORMAT, ISO8601_DATE_FORMAT };
        final DateFormat[] dfs     = new DateFormat[formats.length];

        for (int i = 0; i < formats.length; i++) {
            dfs[i] = new SimpleDateFormat(formats[i]);

            dfs[i].setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        return dfs;
    });

    private final GremlinClauseList      queryClauses                = new GremlinClauseList();
    private final Set<String>            attributesProcessed         = new HashSet<>();
    private final Lookup                 lookup;
    private final AtlasDSL.QueryMetadata queryMetadata;
    private final int                    providedLimit;
    private final int                    providedOffset;
    private final Context                context;
    private final GremlinQueryComposer   parent;
    private       boolean                hasTrait                    = false;

    public GremlinQueryComposer(Lookup registryLookup, Context context, AtlasDSL.QueryMetadata qmd, int limit, int offset, GremlinQueryComposer parent) {
        this.lookup         = registryLookup;
        this.context        = context;
        this.queryMetadata  = qmd;
        this.providedLimit  = limit;
        this.providedOffset = offset;
        this.parent         = parent;

        init();
    }

    public GremlinQueryComposer(Lookup registryLookup, AtlasDSL.QueryMetadata qmd, int limit, int offset) {
        this(registryLookup, new Context(registryLookup), qmd, limit, offset, null);
    }

    public GremlinQueryComposer(AtlasTypeRegistry typeRegistry, AtlasDSL.QueryMetadata qmd, int limit, int offset) {
        this(new RegistryBasedLookup(typeRegistry), qmd, limit, offset);
    }

    @VisibleForTesting
    GremlinQueryComposer(Lookup lookup, Context context, final AtlasDSL.QueryMetadata qmd) {
        this(lookup, context, qmd, DEFAULT_QUERY_RESULT_LIMIT, DEFAULT_QUERY_RESULT_OFFSET, null);
    }

    public void addFrom(String typeName) {
        IdentifierHelper.Info typeInfo = createInfo(typeName);

        if (context.shouldRegister(typeInfo.get())) {
            context.registerActive(typeInfo.get());

            IdentifierHelper.Info ia = createInfo(typeInfo.get());

            if (ia.isTrait()) {
                String traitName = ia.get();

                if (traitName.equals(ALL_CLASSIFICATIONS)) {
                    addTrait(GremlinClause.ANY_TRAIT, ia);
                } else if (traitName.equals(NO_CLASSIFICATIONS)) {
                    addTrait(GremlinClause.NO_TRAIT, ia);
                } else {
                    addTrait(GremlinClause.TRAIT, ia);
                }
            } else {
                if (ia.hasSubtypes()) {
                    add(GremlinClause.HAS_TYPE_WITHIN, ia.getSubTypes());
                } else {
                    add(GremlinClause.HAS_TYPE, ia);
                }
            }
        } else {
            IdentifierHelper.Info ia = createInfo(typeInfo.get());
            introduceType(ia);
        }
    }

    public void addFromProperty(String typeName, String attribute) {
        if (!isNestedQuery()) {
            addFrom(typeName);
        }

        add(GremlinClause.HAS_PROPERTY, createInfo(attribute));
    }

    public void addIsA(String typeName, String traitName) {
        if (!isNestedQuery()) {
            addFrom(typeName);
        }

        IdentifierHelper.Info traitInfo = createInfo(traitName);

        if (StringUtils.equals(traitName, ALL_CLASSIFICATIONS)) {
            addTrait(GremlinClause.ANY_TRAIT, traitInfo);
        } else if (StringUtils.equals(traitName, NO_CLASSIFICATIONS)) {
            addTrait(GremlinClause.NO_TRAIT, traitInfo);
        } else {
            addTrait(GremlinClause.TRAIT, traitInfo);
        }
    }

    public void addHasTerm(String typeName, String termName) {
        String   qualifiedAttributeSeperator = String.valueOf(GlossaryUtils.invalidNameChars[0]);
        String[] terms                       = termName.split(qualifiedAttributeSeperator);
        String   attributeToSearch;

        if (terms.length > 1) {
            attributeToSearch = GlossaryUtils.QUALIFIED_NAME_ATTR;;
        } else {
            termName = terms[0];
            attributeToSearch = GlossaryUtils.NAME;;
        }

        add(GremlinClause.TERM, attributeToSearch, IdentifierHelper.removeQuotes(termName));
    }

    public void addWhere(String lhs, String operator, String rhs) {
        String                currentType = context.getActiveTypeName();

        //in case if trait type is registered and lhs has trait attributes
        if (currentType != null && lookup.isTraitType(currentType)) {
            context.setActiveTypeToUnknown();
        }

        IdentifierHelper.Info org         = null;
        IdentifierHelper.Info lhsI        = createInfo(lhs);
        boolean rhsIsNotDateOrNumOrBool   = false;

        if (!lhsI.isPrimitive()) {
            introduceType(lhsI);

            org  = lhsI;
            lhsI = createInfo(lhs);

            lhsI.setTypeName(org.getTypeName());

            if (org.isTrait()) {
                setHasTrait();
            }
        }

        if (!context.validator.isValidQualifiedName(lhsI.getQualifiedName(), lhsI.getRaw())) {
            return;
        }

        if (lhsI.isDate()) {
            rhs = parseDate(rhs);
        } else if (lhsI.isNumeric()) {
            if(!StringUtils.equals(lhsI.getAttributeName(), Constants.IS_INCOMPLETE_PROPERTY_KEY)) {
                rhs = parseNumber(rhs, this.context);
            }
        } else if (!IdentifierHelper.isTrueOrFalse(rhs)) {
            rhsIsNotDateOrNumOrBool = true;
        }

        rhs = addQuotesIfNecessary(lhsI, rhs);

        SearchParameters.Operator op = SearchParameters.Operator.fromString(operator);

        if (StringUtils.equals(lhsI.getAttributeName(), Constants.IS_INCOMPLETE_PROPERTY_KEY)) {
            addForIsIncompleteClause(lhsI, op, rhs);
        } else {
            if (op == SearchParameters.Operator.LIKE) {
                final AtlasStructType.AtlasAttribute             attribute = context.getActiveEntityType().getAttribute(lhsI.getAttributeName());
                final AtlasStructDef.AtlasAttributeDef.IndexType indexType = attribute.getAttributeDef().getIndexType();

                if (indexType == AtlasStructDef.AtlasAttributeDef.IndexType.STRING || !containsNumberAndLettersOnly(rhs)) {
                    String escapeRhs = IdentifierHelper.escapeCharacters(IdentifierHelper.getFixedRegEx(rhs));
                    add(GremlinClause.STRING_CONTAINS, getPropertyForClause(lhsI), escapeRhs);
                } else {
                    add(GremlinClause.TEXT_CONTAINS, getPropertyForClause(lhsI), IdentifierHelper.getFixedRegEx(rhs));
                }
            } else if (op == SearchParameters.Operator.IN) {
                add(GremlinClause.HAS_OPERATOR, getPropertyForClause(lhsI), "within", rhs);
            } else if (op == SearchParameters.Operator.NEQ && rhsIsNotDateOrNumOrBool) {
                String propertyName = getPropertyForClause(lhsI);

                add(GremlinClause.HAS_NOT_OPERATOR, propertyName, rhs, propertyName);
            } else {
                Object normalizedRhs = getNormalizedAttrVal(lhsI, IdentifierHelper.removeQuotes(rhs));

                addWithNormalizedValue(GremlinClause.HAS_OPERATOR, getPropertyForClause(lhsI), op.getSymbols()[1], normalizedRhs, rhs);
            }
        }

        // record that the attribute has been processed so that the select clause doesn't add a attr presence check
        attributesProcessed.add(lhsI.getQualifiedName());

        if (org != null && org.isReferredType()) {
            add(GremlinClause.DEDUP);

            if (org.getEdgeDirection() != null) {
                GremlinClause gremlinClauseForEdgeLabel = org.getEdgeDirection().equals(IN) ? GremlinClause.OUT : GremlinClause.IN;

                add(gremlinClauseForEdgeLabel, org.getEdgeLabel());
            } else {
                add(GremlinClause.OUT, org.getEdgeLabel());
            }

            context.registerActive(currentType);
        }
    }

    private void setHasTrait() {
        this.hasTrait = true;
    }

    private void addForIsIncompleteClause(IdentifierHelper.Info lhsI,SearchParameters.Operator op, String rhs ) {
        GremlinClause clause = GremlinClause.HAS_OPERATOR;

        rhs = rhs.replace("'", "").replace("\"", "");

        switch (op) {
            case EQ:
                if (IdentifierHelper.isCompleteValue(rhs)) {
                    clause = GremlinClause.HAS_NOT_PROPERTY;
                } else if (IdentifierHelper.isInCompleteValue(rhs)) {
                    rhs = Constants.INCOMPLETE_ENTITY_VALUE.toString();
                }
                break;

            case NEQ:
                if (IdentifierHelper.isCompleteValue(rhs)) {
                    op  = SearchParameters.Operator.EQ;
                    rhs = Constants.INCOMPLETE_ENTITY_VALUE.toString();
                } else if (IdentifierHelper.isInCompleteValue(rhs)) {
                    clause = GremlinClause.HAS_NOT_PROPERTY;
                }
                break;
        }

        Object normalizedRhs = getNormalizedAttrVal(lhsI, IdentifierHelper.removeQuotes(rhs));

        addWithNormalizedValue(clause, getPropertyForClause(lhsI), op.getSymbols()[1], normalizedRhs, rhs);
    }

    private Object getNormalizedAttrVal(IdentifierHelper.Info attrInfo, String attrVal) {
        Object          ret        = attrVal;
        AtlasEntityType entityType = context.getActiveEntityType();

        if (entityType != null && StringUtils.isNotEmpty(attrVal)) {
            String    attrName      = attrInfo.getAttributeName();
            AtlasType attributeType = entityType.getAttributeType(attrName);

            if (attributeType != null) {
                Object normalizedValue = attributeType.getNormalizedValue(attrVal);

                if (normalizedValue != null && attributeType instanceof AtlasBuiltInTypes.AtlasDateType) {
                    ret = ((Date) normalizedValue).getTime();
                } else {
                    ret = normalizedValue;
                }
            }
        }

        return ret;
    }

    private boolean containsNumberAndLettersOnly(String rhs) {
        return Pattern.matches(REGEX_ALPHA_NUMERIC_PATTERN, IdentifierHelper.removeWildcards(rhs));
    }

    private String parseNumber(String rhs, Context context) {
        return rhs.replace("'", "").replace("\"", "") + context.getNumericTypeFormatter();
    }

    public void addAndClauses(List<GremlinQueryComposer> queryComposers) {
        List<String> clauses = addToSubClause(queryComposers);

        add(GremlinClause.AND, String.join(",", clauses));
    }

    public void addOrClauses(List<GremlinQueryComposer> queryComposers) {
        List<String> clauses = addToSubClause(queryComposers);

        add(GremlinClause.OR, String.join(",", clauses));
    }

    public Set<String> getAttributesProcessed() {
        return attributesProcessed;
    }

    public void addProcessedAttributes(Set<String> attributesProcessed) {
        this.attributesProcessed.addAll(attributesProcessed);
    }

    public void addSelect(SelectClauseComposer selectClauseComposer) {
        process(selectClauseComposer);

        // If the query contains orderBy and groupBy then the transformation determination is deferred to the method processing orderBy
        if (!(queryMetadata.hasOrderBy() && queryMetadata.hasGroupBy())) {
            addSelectTransformation(selectClauseComposer, null, false);
        }

        this.context.setSelectClauseComposer(selectClauseComposer);
    }

    public GremlinQueryComposer createNestedProcessor() {
        return new GremlinQueryComposer(lookup, this.context, queryMetadata, this.providedLimit, this.providedOffset, this);
    }

    public GremlinQueryComposer newInstance() {
        return new GremlinQueryComposer(lookup, new Context(lookup), queryMetadata, this.providedLimit, this.providedOffset, null);
    }

    public void addFromAlias(String typeName, String alias) {
        addFrom(typeName);
        addAsClause(alias);
        context.registerAlias(alias);
    }

    public void addAsClause(String alias) {
        add(GremlinClause.AS, alias);
    }

    public void addGroupBy(String item) {
        addGroupByClause(item);
    }

    public void addLimit(String limit, String offset) {
        SelectClauseComposer scc = context.getSelectClauseComposer();

        if (scc == null) {
            addLimitHelper(limit, offset);
        } else {
            if (!scc.hasAggregators()) {
                addLimitHelper(limit, offset);
            }
        }
    }

    public void addDefaultLimit() {
        addLimit(Integer.toString(providedLimit), Integer.toString(providedOffset));
    }

    public String get() {
        close();

        boolean  mustTransform = !isNestedQuery() && queryMetadata.needTransformation();
        String[] items         = getFormattedClauses(mustTransform);
        String   s             = mustTransform ? getTransformedClauses(items) : String.join(".", items);

        return s;
    }

    public List<String> getErrorList() {
        return context.getErrorList();
    }

    public void addOrderBy(String name, boolean isDesc) {
        IdentifierHelper.Info ia = createInfo(name);

        if (queryMetadata.hasSelect() && queryMetadata.hasGroupBy()) {
            addSelectTransformation(this.context.selectClauseComposer, getPropertyForClause(ia), isDesc);
        } else if (queryMetadata.hasGroupBy()) {
            addOrderByClause(ia, isDesc);
            moveToLast(GremlinClause.GROUP_BY);
        } else {
            addOrderByClause(ia, isDesc);
        }
    }

    public boolean hasFromClause() {
        return queryClauses.contains(GremlinClause.HAS_TYPE) != -1 || queryClauses.contains(GremlinClause.HAS_TYPE_WITHIN) != -1;
    }

    private void addWithNormalizedValue(GremlinClause clause, String propertyForClause, String symbol, Object normalizedRhs, String strValue) {
        queryClauses.add(new GremlinClauseValue(clause, propertyForClause, symbol, normalizedRhs, strValue));
    }

    private long getDateFormat(String s) {
        for (DateFormat dateFormat : DSL_DATE_FORMAT.get()) {
            try {
                return dateFormat.parse(s).getTime();
            } catch (ParseException ignored) {
                // ignore
            }
        }

        context.validator.check(false, AtlasErrorCode.INVALID_DSL_INVALID_DATE, s);

        return -1;
    }

    private List<String> addToSubClause(List<GremlinQueryComposer> clauses) {
        for (GremlinQueryComposer entry : clauses) {
            this.addSubClauses(this.queryClauses.size(), entry.getQueryClauses());
        }

        return clauses.stream().map(x -> x.get()).collect(Collectors.toList());
    }

    private String getPropertyForClause(IdentifierHelper.Info ia) {
        String vertexPropertyName = lookup.getVertexPropertyName(ia.getTypeName(), ia.getAttributeName());

        if (StringUtils.isNotEmpty(vertexPropertyName)) {
            return vertexPropertyName;
        }

        if (StringUtils.isNotEmpty(ia.getQualifiedName())) {
            return ia.getQualifiedName();
        }

        return ia.getRaw();
    }

    private void process(SelectClauseComposer scc) {
        if (scc.getItems() == null) {
            return;
        }

        for (int i = 0; i < scc.getItems().length; i++) {
            IdentifierHelper.Info ia = createInfo(scc.getItem(i));

            if(StringUtils.isEmpty(ia.getQualifiedName())) {
                context.getErrorList().add("Unable to find qualified name for " + ia.getAttributeName());

                continue;
            }

            if (scc.isAggregatorWithArgument(i) && !ia.isPrimitive()) {
                context.check(false, AtlasErrorCode.INVALID_DSL_SELECT_INVALID_AGG, ia.getQualifiedName());

                return;
            }

            if (!scc.getItem(i).equals(scc.getLabel(i))) {
                context.addAlias(scc.getLabel(i), ia.getQualifiedName());
            }

            if (scc.updateAsApplicable(i, getPropertyForClause(ia), ia.getQualifiedName())) {
                continue;
            }

            scc.setIsSelectNoop(hasNoopCondition(ia));

            if (scc.getIsSelectNoop()) {
                return;
            }

            if (introduceType(ia)) {
                scc.incrementTypesIntroduced();
                scc.setIsSelectNoop(!ia.hasParts());

                if (ia.hasParts()) {
                    scc.assign(i, getPropertyForClause(createInfo(ia.get())), GremlinClause.INLINE_GET_PROPERTY);
                }
            } else {
                scc.assign(i, getPropertyForClause(ia), GremlinClause.INLINE_GET_PROPERTY);
                scc.setIsPrimitiveAttr(i);
            }
        }

        context.validator.check(!scc.hasMultipleReferredTypes(),
                                AtlasErrorCode.INVALID_DSL_SELECT_REFERRED_ATTR, Integer.toString(scc.getIntroducedTypesCount()));
        context.validator.check(!scc.hasMixedAttributes(), AtlasErrorCode.INVALID_DSL_SELECT_ATTR_MIXING);
    }

    private boolean hasNoopCondition(IdentifierHelper.Info ia) {
        return !ia.isPrimitive() && !ia.isAttribute() && context.hasAlias(ia.getRaw());
    }

    private void addLimitHelper(final String limit, final String offset) {
        if (offset.equalsIgnoreCase("0")) {
            add(GremlinClause.LIMIT, limit, limit);
        } else {
            addRangeClause(offset, limit);
        }
    }

    private String getTransformedClauses(String[] items) {
        String ret;
        String body     = String.join(".", Stream.of(items).filter(Objects::nonNull).collect(Collectors.toList()));
        String inlineFn = queryClauses.getValue(queryClauses.size() - 1);
        String funCall  = String.format(inlineFn, body);

        if (isNestedQuery()) {
            ret = String.join(".", queryClauses.getValue(0), funCall);
        } else {
            ret = queryClauses.getValue(0) + funCall;
        }

        return ret;
    }

    private String[] getFormattedClauses(boolean needTransformation) {
        String[] items    = new String[queryClauses.size()];
        int      startIdx = needTransformation ? 1 : 0;
        int      endIdx   = needTransformation ? queryClauses.size() - 1 : queryClauses.size();

        for (int i = startIdx; i < endIdx; i++) {
            items[i] = queryClauses.getValue(i);
        }

        return items;
    }

    private void addSelectTransformation(final SelectClauseComposer selectClauseComposer,
                                         final String orderByQualifiedAttrName,
                                         final boolean isDesc) {
        GremlinClause gremlinClause;

        if (selectClauseComposer.getIsSelectNoop()) {
            gremlinClause = GremlinClause.SELECT_NOOP_FN;
        } else if (queryMetadata.hasGroupBy()) {
            gremlinClause = selectClauseComposer.onlyAggregators() ? GremlinClause.SELECT_ONLY_AGG_GRP_FN : GremlinClause.SELECT_MULTI_ATTR_GRP_FN;
        } else {
            gremlinClause = selectClauseComposer.onlyAggregators() ? GremlinClause.SELECT_ONLY_AGG_FN : GremlinClause.SELECT_FN;
        }

        if (StringUtils.isEmpty(orderByQualifiedAttrName)) {
            add(0, gremlinClause,
                selectClauseComposer.getLabelHeader(),
                selectClauseComposer.getAssignmentExprString(),
                selectClauseComposer.getItemsString(),
                EMPTY_STRING);
        } else {
            int           itemIdx    = selectClauseComposer.getAttrIndex(orderByQualifiedAttrName);
            GremlinClause sortClause = GremlinClause.INLINE_DEFAULT_TUPLE_SORT;

            if (itemIdx != -1) {
                sortClause = isDesc ? GremlinClause.INLINE_TUPLE_SORT_DESC : GremlinClause.INLINE_TUPLE_SORT_ASC;
            }

            String idxStr = String.valueOf(itemIdx);

            add(0, gremlinClause,
                selectClauseComposer.getLabelHeader(),
                selectClauseComposer.getAssignmentExprString(),
                selectClauseComposer.getItemsString(),
                sortClause.get(idxStr, idxStr)
            );
        }

        add(GremlinClause.INLINE_TRANSFORM_CALL);
    }

    private String addQuotesIfNecessary(IdentifierHelper.Info rhsI, String rhs) {
        if(rhsI.isNumeric()) {
            return rhs;
        }

        if (IdentifierHelper.isTrueOrFalse(rhs)) {
            return rhs;
        }

        if (IdentifierHelper.isQuoted(rhs)) {
            return rhs;
        }

        return IdentifierHelper.getQuoted(rhs);
    }

    private String parseDate(String rhs) {
        String s = IdentifierHelper.isQuoted(rhs) ? IdentifierHelper.removeQuotes(rhs) : rhs;

        return String.format("'%d'", getDateFormat(s));
    }

    private void close() {
        if (isNestedQuery()) {
            return;
        }

        // Need de-duping at the end so that correct results are fetched
        if (queryClauses.size() > 2) {
            // QueryClauses should've something more that just g.V() (hence 2)
            add(GremlinClause.DEDUP);
            // Range and limit must be present after the de-duping construct
            moveToLast(GremlinClause.RANGE);
            moveToLast(GremlinClause.LIMIT);
        }

        if (!queryMetadata.hasLimitOffset()) {
            addDefaultLimit();
        }

        if (queryClauses.isEmpty()) {
            queryClauses.clear();

            return;
        }

        moveToLast(GremlinClause.LIMIT);
        add(GremlinClause.TO_LIST);
        moveToLast(GremlinClause.INLINE_TRANSFORM_CALL);
    }

    private boolean isNestedQuery() {
        return this.parent != null;
    }

    private void addSubClauses(int index, GremlinClauseList queryClauses) {
        this.queryClauses.addSubClauses(index, queryClauses);
    }

    private void moveToLast(GremlinClause clause) {
        int index = queryClauses.contains(clause);

        if (-1 == index) {
            return;
        }

        GremlinClauseValue gcv = queryClauses.remove(index);

        queryClauses.add(gcv);
    }

    public void remove(GremlinClause clause) {
        int index = queryClauses.contains(clause);

        if (-1 == index) {
            return;
        }

        queryClauses.remove(index);
    }

    public GremlinClauseList getQueryClauses(){
        return queryClauses;
    }

    private void init() {
        if (!isNestedQuery()) {
            add(GremlinClause.G);
            add(GremlinClause.V);
        } else {
            add(GremlinClause.NESTED_START);
        }
    }

    private boolean introduceType(IdentifierHelper.Info ia) {
        if (ia.isReferredType()) {
            if (ia.getEdgeDirection() != null) {
                GremlinClause gremlinClauseForEdgeLabel = ia.getEdgeDirection().equals(OUT) ? GremlinClause.OUT : GremlinClause.IN;

                add(gremlinClauseForEdgeLabel, ia.getEdgeLabel());
            } else {
                add(GremlinClause.OUT, ia.getEdgeLabel());
            }

            context.registerActive(ia);
        }

        return ia.isReferredType();
    }

    private IdentifierHelper.Info createInfo(String actualTypeName) {
        return IdentifierHelper.create(context, lookup, actualTypeName);
    }

    private void addRangeClause(String startIndex, String endIndex) {
        if (queryMetadata.hasSelect()) {
            add(queryClauses.size() - 1, GremlinClause.RANGE, startIndex, startIndex, endIndex, startIndex, startIndex, endIndex);
        } else {
            add(GremlinClause.RANGE, startIndex, startIndex, endIndex, startIndex, startIndex, endIndex);
        }
    }

    private void addOrderByClause(IdentifierHelper.Info ia, boolean descr) {
        add((!descr) ? GremlinClause.ORDER_BY : GremlinClause.ORDER_BY_DESC, ia);
    }

    private void addGroupByClause(String name) {
        IdentifierHelper.Info ia = createInfo(name);

        add(GremlinClause.GROUP_BY, ia);
    }

    private void add(GremlinClause clause, IdentifierHelper.Info idInfo) {
        if (context != null && !context.validator.isValid(context, clause, idInfo)) {
            return;
        }

        add(clause, getPropertyForClause(idInfo));
    }

    private void add(GremlinClause clause, String... args) {
        queryClauses.add(new GremlinClauseValue(clause, args));
    }

    public void add(GremlinClauseValue gv) {
        queryClauses.add(gv);
    }

    public void addAll(GremlinClauseList gcList) {
        if (gcList != null) {
            List<GremlinQueryComposer.GremlinClauseValue> list = gcList.getList();

            if (CollectionUtils.isNotEmpty(list)) {
                queryClauses.clear();
                for (GremlinClauseValue value : list) {
                    queryClauses.add(value);
                }
            }
        }
    }

    private void add(int idx, GremlinClause clause, String... args) {
        queryClauses.add(idx, new GremlinClauseValue(clause, args));
    }

    private void addTrait(GremlinClause clause, IdentifierHelper.Info idInfo) {
        if (context != null && !context.validator.isValid(context, clause, idInfo)) {
            return;
        }

        add(clause, idInfo.get(), idInfo.get());
    }

    public GremlinClauseList clauses() {
        return queryClauses;
    }

    public SelectClauseComposer getSelectComposer() {
        return this.context.selectClauseComposer;
    }

    public boolean hasAnyTraitAttributeClause() {
        return this.hasTrait;
    }

    public static class GremlinClauseValue {
        private final GremlinClause clause;
        private final String        value;
        private final String[]      values;
        private final Object        rawValue;

        public GremlinClauseValue(GremlinClause clause, String property, String operator, Object rawValue, String str) {
            this.clause   = clause;
            this.value    = clause.get(property, operator, str);
            this.values   = new String[] {property, operator, str};
            this.rawValue = rawValue;
        }

        public GremlinClauseValue(GremlinClause clause, String... values) {
            this.clause   = clause;
            this.value    = clause.get(values);
            this.values   = values;
            this.rawValue = null;
        }

        public GremlinClause getClause() {
            return clause;
        }

        public String getClauseWithValue() {
            return value;
        }

        public String[] getValues() {
            return values;
        }

        public Object getRawValue() {
            return this.rawValue;
        }

        @Override
        public String toString() {
            return String.format("%s", clause);
        }
    }

    @VisibleForTesting
    static class Context {
        private static final AtlasStructType UNKNOWN_TYPE = new AtlasStructType(new AtlasStructDef());

        private final Lookup               lookup;
        private final ClauseValidator      validator;
        private final Map<String, String>  aliasMap = new HashMap<>();
        private       AtlasType            activeType;
        private       SelectClauseComposer selectClauseComposer;
        private       String               numericTypeFormatter = "";

        public Context(Lookup lookup) {
            this.lookup    = lookup;
            this.validator = new ClauseValidator();
        }

        public void registerActive(String typeName) {
            if (shouldRegister(typeName)) {
                try {
                    activeType = lookup.getType(typeName);

                    aliasMap.put(typeName, typeName);
                } catch (AtlasBaseException e) {
                    validator.check(e, AtlasErrorCode.INVALID_DSL_UNKNOWN_TYPE, typeName);

                    activeType = UNKNOWN_TYPE;
                }
            }
        }

        public void setActiveTypeToUnknown() {
            activeType = UNKNOWN_TYPE;
        }

        public void registerActive(IdentifierHelper.Info info) {
            if (validator.check(StringUtils.isNotEmpty(info.getTypeName()),
                                AtlasErrorCode.INVALID_DSL_UNKNOWN_TYPE, info.getRaw())) {
                registerActive(info.getTypeName());
            } else {
                activeType = UNKNOWN_TYPE;
            }
        }

        public AtlasEntityType getActiveEntityType() {
            return (activeType instanceof AtlasEntityType) ? (AtlasEntityType) activeType : null;
        }

        public String getActiveTypeName() {
            return activeType.getTypeName();
        }

        public AtlasType getActiveType() {
            return activeType;
        }

        public boolean shouldRegister(String typeName) {
            return activeType == null ||
                           (activeType != null && !StringUtils.equals(getActiveTypeName(), typeName)) &&
                                   (activeType != null && !lookup.hasAttribute(this, typeName));
        }

        public void registerAlias(String alias) {
            addAlias(alias, getActiveTypeName());
        }

        public boolean hasAlias(String alias) {
            return aliasMap.containsKey(alias);
        }

        public String getTypeNameFromAlias(String alias) {
            return aliasMap.get(alias);
        }

        public boolean isEmpty() {
            return activeType == null;
        }

        public SelectClauseComposer getSelectClauseComposer() {
            return selectClauseComposer;
        }

        public void setSelectClauseComposer(SelectClauseComposer selectClauseComposer) {
            this.selectClauseComposer = selectClauseComposer;
        }

        public void addAlias(String alias, String typeName) {
            if (aliasMap.containsKey(alias)) {
                check(false, AtlasErrorCode.INVALID_DSL_DUPLICATE_ALIAS, alias, getActiveTypeName());

                return;
            }

            aliasMap.put(alias, typeName);
        }

        public List<String> getErrorList() {
            return validator.getErrorList();
        }

        public boolean error(AtlasBaseException e, AtlasErrorCode ec, String t, String name) {
            return validator.check(e, ec, t, name);
        }

        public boolean check(boolean condition, AtlasErrorCode vm, String... args) {
            return validator.check(condition, vm, args);
        }

        public void setNumericTypeFormatter(String formatter) {
            this.numericTypeFormatter = formatter;
        }

        public String getNumericTypeFormatter() {
            return this.numericTypeFormatter;
        }
    }

    private static class ClauseValidator {
        final List<String> errorList = new ArrayList<>();

        public ClauseValidator() {
        }

        public boolean isValid(Context ctx, GremlinClause clause, IdentifierHelper.Info ia) {
            switch (clause) {
                case TRAIT:
                case ANY_TRAIT:
                case NO_TRAIT:
                    return check(ia.isTrait(), AtlasErrorCode.INVALID_DSL_UNKNOWN_CLASSIFICATION, ia.getRaw());

                case HAS_TYPE:
                    TypeCategory typeCategory = ctx.getActiveType().getTypeCategory();
                    return check(StringUtils.isNotEmpty(ia.getTypeName()) &&
                                         typeCategory == TypeCategory.CLASSIFICATION || typeCategory == TypeCategory.ENTITY,
                                 AtlasErrorCode.INVALID_DSL_UNKNOWN_TYPE, ia.getRaw());

                case HAS_PROPERTY:
                    return check(ia.isPrimitive(), AtlasErrorCode.INVALID_DSL_HAS_PROPERTY, ia.getRaw());

                case ORDER_BY:
                    return check(ia.isPrimitive(), AtlasErrorCode.INVALID_DSL_ORDERBY, ia.getRaw());

                case GROUP_BY:
                    return check(ia.isPrimitive(), AtlasErrorCode.INVALID_DSL_SELECT_INVALID_AGG, ia.getRaw());

                default:
                    return (getErrorList().size() == 0);
            }
        }

        public boolean check(Exception ex, AtlasErrorCode vm, String... args) {
            String[] extraArgs = getExtraSlotArgs(args, ex.getMessage());

            return check(false, vm, extraArgs);
        }

        public boolean check(boolean condition, AtlasErrorCode vm, String... args) {
            if (!condition) {
                addError(vm, args);
            }

            return condition;
        }

        public void addError(AtlasErrorCode ec, String... messages) {
            errorList.add(ec.getFormattedErrorMessage(messages));
        }

        public List<String> getErrorList() {
            return errorList;
        }

        public boolean isValidQualifiedName(String qualifiedName, String raw) {
            return check(StringUtils.isNotEmpty(qualifiedName), AtlasErrorCode.INVALID_DSL_QUALIFIED_NAME, raw);
        }

        private String[] getExtraSlotArgs(String[] args, String s) {
            String[] argsPlus1 = new String[args.length + 1];

            System.arraycopy(args, 0, argsPlus1, 0, args.length);

            argsPlus1[args.length] = s;

            return argsPlus1;
        }
    }
}
