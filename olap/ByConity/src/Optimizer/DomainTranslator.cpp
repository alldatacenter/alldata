/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <Optimizer/DomainTranslator.h>

namespace DB::Predicate
{

ASTPtr DomainTranslator::toPredicate(const TupleDomain & tuple_domain)
{
    if (tuple_domain.isNone())
        return PredicateConst::FALSE_VALUE;

    ConstASTs combine_predicates;

    for (const auto & domain : tuple_domain.getDomains())
        combine_predicates.emplace_back(toPredicate(std::make_shared<ASTIdentifier>(domain.first), domain.second));

    return PredicateUtils::combineConjuncts(combine_predicates);
}

ASTPtr DomainTranslator::toPredicate(const ASTPtr & symbol, const Domain & domain)
{
    if (domain.valueSetIsNone())
        return domain.isNullAllowed() ? makeASTFunction("isNull", symbol) : PredicateConst::FALSE_VALUE;

    if (domain.valueSetIsAll())
        return domain.isNullAllowed() ? PredicateConst::TRUE_VALUE : makeASTFunction("isNotNull", symbol);

    ConstASTs disjuncts;
    const ValueSet & value_set = domain.getValueSet();

    if (auto v = std::get_if<SortedRangeSet>(&value_set))
        disjuncts = extractDisjuncts(domain.getType(), v->getRanges(), symbol); //TODO:use std::move
    else if (auto d = std::get_if<DiscreteValueSet>(&value_set))
        disjuncts = extractDisjuncts(domain.getType(), *d, symbol);
    else
        throw Exception("Case should not be reachable", DB::ErrorCodes::LOGICAL_ERROR);

    // Add nullability disjuncts
    if (domain.isNullAllowed())
        disjuncts.emplace_back(makeASTFunction("isNull", symbol));

    return PredicateUtils::combineDisjunctsWithDefault(disjuncts, PredicateConst::TRUE_VALUE);
}

/**Convert an Expression predicate into an ExtractionResult consisting of:
 * 1) A successfully extracted TupleDomain
 * 2) An Expression fragment which represents the part of the original Expression that will need to be re-evaluated
 * after filtering with the TupleDomain.
 */
ExtractionResult DomainTranslator::getExtractionResult(ASTPtr predicate, NamesAndTypes types)
{
    TypeAnalyzer type_analyzer = TypeAnalyzer::create(context, types);
    NameToType column_types;
    for (const auto & type : types)
        column_types.emplace(type.name, type.type);

    return DomainVisitor(context, type_analyzer, std::move(column_types), is_ignored).process(predicate, false);
}

//Note: ranges should be sorted before!
ConstASTs DomainTranslator::extractDisjuncts(const DataTypePtr & type, const Ranges & ordered_ranges, ASTPtr symbol)
{
    ConstASTs disjuncts;
    ASTs single_values;
    SortedRangeSet sorted_range_set = SortedRangeSet(type, ordered_ranges);
    SortedRangeSet complement = sorted_range_set.complement();

    // collect "single value" ranges from the complement of "sorted_range_set"; e.g. get a = 3 from (a<3 and a > 3)
    Ranges single_exclusive_values = pickOutSingleValueRanges(complement);

    // union the "single value" ranges with the original SortedRangeSet
    Ranges original_union_single_values = SortedRangeSet(type, single_exclusive_values).unionn(sorted_range_set).getRanges();
    TypeIndex idx = type->getTypeId();
    auto iter = single_exclusive_values.begin();

    /**For types including NaN, it is incorrect to introduce range "all" while processing a set of ranges,
      *even if the component ranges cover the entire value set.
      *This is because partial ranges don't include NaN, while range "all" does.
      *Example: ranges (unbounded , 1.0) and (1.0, unbounded) should not be coalesced to (unbounded, unbounded) with excluded point 1.0.
      *That result would be further translated to expression "xxx <> 1.0", which is satisfied by NaN.
      *To avoid error, in such case the ranges are not optimised.
      */
    if (idx == TypeIndex::Float32 || idx == TypeIndex::Float64)
    {
        bool original_range_is_all = anyRangeIsAll(ordered_ranges);
        bool coalesced_range_is_all = anyRangeIsAll(original_union_single_values);
        if (!original_range_is_all && coalesced_range_is_all)
        {
            for (const auto & range : ordered_ranges)
                disjuncts.emplace_back(processRange(type, range, symbol));

            return disjuncts;
        }
    }

    for (const auto & range : original_union_single_values)
    {
        if (range.isSingleValue())
        {
            single_values.emplace_back(literalEncodeWithType(type, range.getSingleValue()));
            continue;
        }

        // attempt to optimize ranges that can be coalesced as long as single value points are excluded
        ASTs single_value_in_range;

        while (iter != single_exclusive_values.end() && range.contains(*iter))
        {
            single_value_in_range.emplace_back(literalEncodeWithType(type, iter->getSingleValue()));
            iter++;
        }

        if (!single_value_in_range.empty())
        {
            disjuncts.emplace_back(combineRangeWithExcludedPoints(type, symbol, range, single_value_in_range));
            continue;
        }

        disjuncts.emplace_back(processRange(type, range, symbol));
    }

    // Add back all of the possible single values either as an equality or an IN predicate
    if (single_values.size() == 1)
        disjuncts.emplace_back(makeASTFunction("equals", symbol, single_values[0]));
    else if (single_values.size() > 1)
        disjuncts.emplace_back(makeASTFunction("in", symbol, makeASTFunction("tuple", std::move(single_values))));

    return disjuncts;
}

ConstASTs DomainTranslator::extractDisjuncts(const DataTypePtr & type, const DiscreteValueSet & discrete_value_set, ASTPtr symbol)
{
    ASTs values;
    for (const auto & temp : discrete_value_set.getValues())
        values.push_back(literalEncodeWithType(type, temp));

    // If values is empty, then the equatableValues was either ALL or NONE, both of which should already have been checked for
    if (values.empty())
        throw Exception("which have been checked before", DB::ErrorCodes::LOGICAL_ERROR);

    ASTPtr predicate;

    if (values.size() == 1)
        predicate = makeASTFunction("equals", symbol, values[0]);
    else
        predicate = makeASTFunction("in", symbol, makeASTFunction("tuple", std::move(values)));

    if (!discrete_value_set.isInclusive())
        predicate = makeASTFunction("not", predicate);

    return ConstASTs{predicate};
}

ASTPtr DomainTranslator::processRange(const DataTypePtr & type, const Range & range, ASTPtr & symbol)
{
    if (range.isAll())
        return PredicateConst::TRUE_VALUE;

    ConstASTs range_conjuncts;
    if (!range.isLowUnbounded())
    {
        range_conjuncts.emplace_back(makeASTFunction(
            range.isLowInclusive() ? "greaterOrEquals" : "greater", symbol, literalEncodeWithType(type, range.getLowValue())));
    }

    if (!range.isHighUnbounded())
    {
        range_conjuncts.emplace_back(
            makeASTFunction(range.isHighInclusive() ? "lessOrEquals" : "less", symbol, literalEncodeWithType(type, range.getHighValue())));
    }

    // If range_conjuncts is empty, then the range was ALL, which should already have been checked for
    if (range_conjuncts.empty())
        throw Exception("'all' range have been checked before", DB::ErrorCodes::LOGICAL_ERROR);

    return PredicateUtils::combineConjuncts(range_conjuncts);
}

ASTPtr
DomainTranslator::combineRangeWithExcludedPoints(const DataTypePtr & type, ASTPtr & symbol, const Range & range, ASTs & excluded_points)
{
    if (excluded_points.empty())
        return processRange(type, range, symbol);

    ASTPtr excluded_points_ast;

    if (excluded_points.size() == 1)
        excluded_points_ast = makeASTFunction("notEquals", symbol, excluded_points[0]);
    else
        excluded_points_ast = makeASTFunction("notIn", symbol, makeASTFunction("tuple", std::move(excluded_points)));

    return PredicateUtils::combineConjuncts({processRange(type, range, symbol), excluded_points_ast});
}

bool DomainTranslator::anyRangeIsAll(const Ranges & ranges)
{
    for (const auto & range : ranges)
    {
        if (range.isAll())
            return true;
    }
    return false;
}
//todo update name
ASTPtr DomainTranslator::literalEncodeWithType(const DataTypePtr & type, const Field & field)
{
    if (isNumber(removeNullable(type)) || isStringOrFixedString(removeNullable(type)))
        return std::make_shared<ASTLiteral>(field);

    return LiteralEncoder::encode(field, removeNullable(type), context);
}

Ranges DomainTranslator::pickOutSingleValueRanges(const SortedRangeSet & sorted_range_set)
{
    Ranges single_exclusive_values;

    for (const auto & range : sorted_range_set.getRanges())
    {
        if (range.isSingleValue())
            single_exclusive_values.emplace_back(range);
    }

    return single_exclusive_values;
}

DataTypePtr DomainVisitor::checkedTypeLookup(const String & symbol) const
{
    DataTypePtr type = column_types.at(symbol); //TODO:need to be confirm
    if (!type)
        throw Exception("Types is missing info for symbol", DB::ErrorCodes::LOGICAL_ERROR);
    return type;
}

ASTPtr DomainVisitor::complementIfNecessary(const ASTPtr & ast, bool complement) const
{
    return complement ? makeASTFunction("not", ast) : ast;
}

ExtractionResult DomainVisitor::process(ASTPtr & node, const bool & complement)
{
    return ASTVisitorUtil::accept(node, *this, complement);
}

ExtractionResult DomainVisitor::visitASTFunction(ASTPtr & node, const bool & complement)
{
    auto & ast_fun = node->as<ASTFunction &>();
    const String & fun_name = ast_fun.name;

    if (fun_name == "and" || fun_name == "or")
        return visitLogicalFunction(node, complement, fun_name);
    if (fun_name == "not")
        return visitNotFunction(node, complement);
    //    if (fun_name == "in")
    //        return visitInFunction(node, complement);
    if (fun_name == "isNotNull")
        return visitIsNotNullFunction(node, complement);
    if (fun_name == "isNull")
        return visitIsNullFunction(node, complement);
    if (fun_name == "equals" || fun_name == "notEquals" || fun_name == "less" || fun_name == "greater" || fun_name == "lessOrEquals"
        || fun_name == "greaterOrEquals")
        return visitComparisonFunction(node, complement);

    //TODO:@cdy
    //  if (fun_name == "startsWith")
    //      return visitStartsWithFunction(node, complement);
    //  if (fun_name == "like")
    //      return visitLikeFunction(node, complement);

    //TODO:when fun_name == "notin", complement == true;

    return visitNode(node, complement);
}

ExtractionResult DomainVisitor::visitASTLiteral(ASTPtr & node, const bool & complement)
{
    auto literal = node->as<const ASTLiteral>();

    if (literal->value == 1u || literal->value == 1)
        return ExtractionResult(complement ? TupleDomain::none() : TupleDomain::all(), PredicateConst::TRUE_VALUE);
    else if (literal->value == 0u || literal->value == 0)
        return ExtractionResult(complement ? TupleDomain::all() : TupleDomain::none(), PredicateConst::TRUE_VALUE);
    else if (literal->value.isNull())
        return ExtractionResult(TupleDomain::none(), PredicateConst::TRUE_VALUE);

    return visitNode(node, complement);
}

ExtractionResult DomainVisitor::visitNode(ASTPtr & node, const bool & complement)
{
    // If we don't know how to process this node, the default response is to say that the TupleDomain is "all"
    return ExtractionResult(TupleDomain::all(), complementIfNecessary(node, complement));
}

ExtractionResult DomainVisitor::visitLogicalFunction(ASTPtr & node, const bool & complement, const String & fun_name)
{
    std::vector<ExtractionResult> results;
    auto * ast_fun = node->as<ASTFunction>();

    for (auto & child : ast_fun->arguments->children)
        results.emplace_back(process(child, complement));

    TupleDomains tuple_domains = extractTupleDomains(results);
    ConstASTs residuals = extractRemainingExpressions(results);

    //when the function_name is "and"
    if ((complement && fun_name == "or") || (!complement && fun_name == "and"))
    {
        TupleDomain temp = TupleDomain::intersect(tuple_domains);
        return ExtractionResult(temp, PredicateUtils::combineConjuncts(residuals));
    }
    else
    {
        bool use_origin_for_expression = true;

        //case:or
        TupleDomain column_unioned_tuple_domain = TupleDomain::columnWiseUnion(tuple_domains);

        // In most cases, the column_unioned_tuple_domain is only a superset of the actual strict union
        // and so we can return the current node as the remainingExpression so that all bounds will be double-checked again at execution time.
        ASTPtr remaining_expression = complementIfNecessary(node, complement);

        // However, there are a few cases where the column-wise union is actually equivalent to the strict union, so if we can detect
        // some of these cases, we won't have to double-check the bounds unnecessarily at execution time.

        // We can only make inferences if the remaining expressions on all terms are equal and deterministic
        if (ASTSet<ConstASTPtr>(residuals.begin(), residuals.end()).size() == 1
            && ExpressionDeterminism::isDeterministic(residuals[0], context))
        {
            // NONE are no-op for the purpose of OR
            std::vector<TupleDomain>::iterator it = tuple_domains.begin();
            while (it != tuple_domains.end())
            {
                if (it->isNone())
                    tuple_domains.erase(it);
                else
                    it++;
            }

            // The column-wise union is equivalent to the strict union if
            // 1) If all TupleDomains consist of the same exact single column (e.g. one TupleDomain => (a > 0), another TupleDomain => (a < 10))
            // 2) If one TupleDomain is a superset of the others (e.g. TupleDomain => (a > 0, b > 0 && b < 10) vs TupleDomain => (a > 5, b = 5))
            bool matching_single_symbol_domains = allTupleDomainsAreSameSingleColumn(tuple_domains);
            bool one_term_is_super_set = TupleDomain::maximal(tuple_domains).has_value();

            if (one_term_is_super_set)
            {
                remaining_expression = results[0].remaining_expression;
                use_origin_for_expression = false;
            }
            else if (matching_single_symbol_domains)
            {
                // Types REAL and DOUBLE require special handling because they include NaN value. In this case, we cannot rely on the union of domains.
                // That is because domains covering the value set partially might union up to a domain covering the whole value set.
                // While the component domains didn't include NaN, the resulting domain could be further translated to predicate "TRUE" or "a IS NOT NULL",
                // which is satisfied by NaN. So during domain union, NaN might be implicitly added.
                // Example: Let 'a' be a column of type DOUBLE.
                //          Let left TupleDomain => (a > 0) /false for NaN/, right TupleDomain => (a < 10) /false for NaN/.
                //          Unioned TupleDomain => "is not null" /true for NaN/
                // To guard against wrong results, the current node is returned as the remainingExpression.
                DataTypePtr type = tuple_domains[0].getDomains().begin()->second.getType();
                TypeIndex idx = type->getTypeId();
                // A Domain of a floating point type contains NaN in the following cases:
                // 1. When it contains all the values of the type and null.
                //    In such case the domain is 'all', and if it is the only domain
                //    in the TupleDomain, the TupleDomain gets normalized to TupleDomain 'all'.
                // 2. When it contains all the values of the type and doesn't contain null.
                //    In such case no normalization on the level of TupleDomain takes place,
                //    and the check for NaN is done by inspecting the Domain's valueSet.
                //    NaN is included when the valueSet is 'all'.
                bool unioned_domain_contains_nan = column_unioned_tuple_domain.isAll()
                    || (!column_unioned_tuple_domain.domainsIsEmpty()
                        && isAllValueSet(column_unioned_tuple_domain.getOnlyElement().getValueSet()));

                bool implicitly_added_nan = (idx == TypeIndex::Float32 || idx == TypeIndex::Float64)
                    && allTupleDomainsAreNotAll(tuple_domains) && unioned_domain_contains_nan;

                if (!implicitly_added_nan)
                {
                    remaining_expression = results[0].remaining_expression;
                    use_origin_for_expression = false;
                }
            }
        }

        if (use_origin_for_expression)
            is_ignored = true;

        return ExtractionResult(column_unioned_tuple_domain, remaining_expression);
    }
}

ExtractionResult DomainVisitor::visitNotFunction(ASTPtr & node, const bool & complement)
{
    return process(node->as<ASTFunction>()->arguments->children[0], !complement);
}

ExtractionResult DomainVisitor::visitComparisonFunction(ASTPtr & node, const bool & complement)
{
    std::optional<NormalizedSimpleComparison> optional_normalized = toNormalizedSimpleComparison(node);

    if (!optional_normalized.has_value())
        return visitNode(node, complement);

    NormalizedSimpleComparison normalized = optional_normalized.value();

    auto * identifier_if_exit = normalized.symbol_expression->as<ASTIdentifier>();
    if (identifier_if_exit)
    {
        String symbol = identifier_if_exit->name();
        const DataTypePtr & type = normalized.value_with_type.type;
        const Field & value = normalized.value_with_type.value;
        auto extraction_result = createComparisonExtractionResult(node, normalized.operator_name, symbol, type, value, complement);
        if (extraction_result.has_value())
            return extraction_result.value();
        return visitNode(node, complement);
    }
    //TODO: have not implement, Reference presto
    return visitNode(node, complement);
}

//Extract a normalized simple comparison between a QualifiedNameReference and a native value if possible.
std::optional<NormalizedSimpleComparison> DomainVisitor::toNormalizedSimpleComparison(ASTPtr & comparison) const
{
    auto * ast_fun = comparison->as<ASTFunction>();

    auto left = ExpressionInterpreter::evaluateConstantExpression(ast_fun->arguments->children[0], column_types, context);
    auto right = ExpressionInterpreter::evaluateConstantExpression(ast_fun->arguments->children[1], column_types, context);

    // TODO: re-enable this check once we fix the type coercions in the optimizers
    // checkArgument(leftType.equals(rightType), "left and right type do not match in comparison expression (%s)", comparison);
    // we expect one side to be expression and other to be value.
    if (left.has_value() == right.has_value())
        return std::nullopt;

    DataTypePtr left_type = left.has_value() ? left->first : type_analyzer.getType(ast_fun->arguments->children[0]);
    DataTypePtr right_type = right.has_value() ? right->first : type_analyzer.getType(ast_fun->arguments->children[1]);
    //TODO: not support float32(symbol) comparator float32(value)
    /** get the superType for leftType and right Type,
     * if the identifier's type is narrow type, then return std::nullopt;
     * e.g. we can not support the predicate expression such as col_a > 256, col_a's type is UInt8
     */
    ASTPtr symbol_ast;
    FieldWithType value_with_type;
    String operator_name;

    if (right.has_value())
    {
        auto & right_value = right->second;

        auto coerce_field = canImplicitCoerceValue(right_value, right_type, left_type);
        if (!right_value.isNull() && !coerce_field.has_value())
            return std::nullopt;

        symbol_ast = ast_fun->arguments->children[0];
        operator_name = ast_fun->name;
        value_with_type = {left_type, coerce_field.value()};
    }
    else if (left.has_value())
    {
        auto & left_value = left->second;

        auto coerce_field = canImplicitCoerceValue(left_value, left_type, right_type);
        if (!left_value.isNull() && !coerce_field.has_value())
            return std::nullopt;

        symbol_ast = ast_fun->arguments->children[1];
        operator_name = Utils::flipOperator(ast_fun->name);
        value_with_type = {right_type, coerce_field.value()};
    }
    return NormalizedSimpleComparison(symbol_ast, operator_name, value_with_type);
}

std::vector<TupleDomain> DomainVisitor::extractTupleDomains(const std::vector<ExtractionResult> & results) const
{
    std::vector<TupleDomain> res;

    for (const auto & temp : results)
        res.emplace_back(temp.tuple_domain);

    return res;
}

ConstASTs DomainVisitor::extractRemainingExpressions(const std::vector<ExtractionResult> & results) const
{
    ConstASTs res;

    for (const auto & temp : results)
        res.emplace_back(temp.remaining_expression);

    return res;
}

bool DomainVisitor::allTupleDomainsAreSameSingleColumn(const std::vector<TupleDomain> & tuple_domains) const
{
    if (tuple_domains.size() == 0)
        return false;

    //none tuple domain have been checked before;
    if (tuple_domains[0].getDomains().size() != 1)
        return false;

    String str = tuple_domains[0].getDomains().begin()->first;

    for (size_t i = 1; i < tuple_domains.size(); i++)
    {
        if (tuple_domains[i].getDomainCount() == 1 && str.compare(tuple_domains[i].getDomains().begin()->first) == 0)
            continue;
        else
            return false;
    }
    return true;
}

std::optional<Field> DomainVisitor::canImplicitCoerceValue(Field & value, DataTypePtr & from_type, DataTypePtr & to_type) const
{
    if (from_type->equals(*to_type))
        return value;

    if (value.isNull())
        return convertFieldToType(value, *to_type, from_type.get());

    //Based on whether there is a super type between them
    DataTypePtr super_type = nullptr;
    try
    {
        super_type = getLeastSupertype({from_type, to_type}, context->getSettingsRef().allow_extended_type_conversion);
        if (!super_type || !super_type->equals(*to_type))
        {
            return std::nullopt;
        }

        //have super_type and super_type equals to_type, which means to_type is wider type;
        return getConvertFieldToType(value, from_type, to_type);
    }
    catch (...)
    {
    }

    //There is no super type for from_type and to_type, now based on hand-coded rules
    TypeIndex from_id = from_type->getTypeId();
    TypeIndex to_id = to_type->getTypeId();
    if (from_id == TypeIndex::Int64 || from_id == TypeIndex::UInt64)
    {
        if (to_id == TypeIndex::Float64 || to_id == TypeIndex::Int128 || to_id == TypeIndex::Int64 || to_id == TypeIndex::UInt64)
        {
            return getConvertFieldToType(value, from_type, to_type);
        }
    }
    else if (from_id == TypeIndex::String)
    {
        if (to_id == TypeIndex::Date || to_id == TypeIndex::DateTime /*|| to_id == TypeIndex::Date32*/ || to_id == TypeIndex::DateTime64
            || to_id == TypeIndex::Time)
        {
            return getConvertFieldToType(value, from_type, to_type);
        }
    }
    //TODO: cover more case
    return std::nullopt;
}

std::optional<Field> DomainVisitor::getConvertFieldToType(Field & value, DataTypePtr & from_type, DataTypePtr & to_type) const
{
    Field to_value = convertFieldToType(value, *to_type, from_type.get());
    if (to_value.isNull())
        return std::nullopt;
    return to_value;
}

//Returns true if all tupleDomains are not "all" , false otherwise
bool DomainVisitor::allTupleDomainsAreNotAll(const std::vector<TupleDomain> & tuple_domains) const
{
    if (tuple_domains.size() == 0)
        return true;

    for (const auto & temp : tuple_domains)
    {
        if (temp.isAll())
            return false;
    }

    return true;
}

std::optional<ExtractionResult> DomainVisitor::createComparisonExtractionResult(
    ASTPtr & node, const String & operator_name, String symbol, const DataTypePtr & type, const Field & value, const bool & complement)
{
    if (value.isNull())
    {
        if (isValidOperatorForComparison(operator_name))
            return ExtractionResult(TupleDomain::none(), PredicateConst::TRUE_VALUE);
        else
            throw Exception("Unhandled operator" + operator_name, DB::ErrorCodes::LOGICAL_ERROR);
    }

    if (isTypeOrderable(type))
    {
        auto temp = extractOrderableDomain(operator_name, type, value, complement);

        if (!temp.has_value())
            return std::nullopt;

        return ExtractionResult(TupleDomain(std::unordered_map<String, Domain>{{symbol, temp.value()}}), PredicateConst::TRUE_VALUE);
    }

    if (isTypeComparable(type))
    {
        Domain domain = extractDiscreteDomain(operator_name, type, value, complement);
        return ExtractionResult(TupleDomain(std::unordered_map<String, Domain>{{symbol, domain}}), PredicateConst::TRUE_VALUE);
    }

    return visitNode(node, complement);
    //throw Exception("Type cannot be used in a comparison expression (should have been caught in analysis)", DB::ErrorCodes::LOGICAL_ERROR);
}

std::optional<Domain>
DomainVisitor::extractOrderableDomain(const String & operator_name, const DataTypePtr & type, const Field & value, const bool & complement)
{
    if (value.isNull())
        throw Exception("Value is not null!", DB::ErrorCodes::LOGICAL_ERROR);

    // Handle orderable types which do not have NaN.
    TypeIndex type_id = type->getTypeId();
    if (type_id != TypeIndex::Float32 && type_id != TypeIndex::Float64)
    {
        if (operator_name == "less")
        {
            return Domain(
                complement ? SortedRangeSet(type, Ranges{Range::lessThanRange(type, value)}).complement()
                           : SortedRangeSet(type, Ranges{Range::lessThanRange(type, value)}),
                false);
        }
        if (operator_name == "lessOrEquals")
        {
            return Domain(
                complement ? SortedRangeSet(type, Ranges{Range::lessThanOrEqualRange(type, value)}).complement()
                           : SortedRangeSet(type, Ranges{Range::lessThanOrEqualRange(type, value)}),
                false);
        }
        if (operator_name == "greater")
        {
            return Domain(
                complement ? SortedRangeSet(type, Ranges{Range::greaterThanRange(type, value)}).complement()
                           : SortedRangeSet(type, Ranges{Range::greaterThanRange(type, value)}),
                false);
        }
        if (operator_name == "greaterOrEquals")
        {
            return Domain(
                complement ? SortedRangeSet(type, Ranges{Range::greaterThanOrEqualRange(type, value)}).complement()
                           : SortedRangeSet(type, Ranges{Range::greaterThanOrEqualRange(type, value)}),
                false);
        }
        if (operator_name == "equals") //TODO: use define or enum
        {
            return Domain(
                complement ? SortedRangeSet(type, Ranges{Range::equalRange(type, value)}).complement()
                           : SortedRangeSet(type, Ranges{Range::equalRange(type, value)}),
                false);
        }
        if (operator_name == "notEquals")
        {
            return Domain(
                complement
                    ? SortedRangeSet(type, Ranges{Range::lessThanRange(type, value), Range::greaterThanRange(type, value)}).complement()
                    : SortedRangeSet(type, Ranges{Range::lessThanRange(type, value), Range::greaterThanRange(type, value)}),
                false);
        }
        throw Exception("Unhandled operator" + operator_name, DB::ErrorCodes::LOGICAL_ERROR);
    }

    // Handle comparisons against NaN
    if (DB::Utils::isFloatingPointNaN(type, value))
    {
        if (operator_name == "equals" || operator_name == "greater" || operator_name == "greaterOrEquals" || operator_name == "less"
            || operator_name == "lessOrEquals")
            return Domain(complement ? complementValueSet(createNone(type)) : createNone(type), false);

        if (operator_name == "notEquals")
            return Domain(complement ? complementValueSet(createAll(type)) : createAll(type), false);

        throw Exception("Unhandled operator" + operator_name, DB::ErrorCodes::LOGICAL_ERROR);
    }

    /**Handle comparisons against a non-NaN value when the compared value might be NaN
     * For comparison operators: EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL,
     * the Domain should not contain NaN, but complemented Domain should contain NaN. It is currently not supported.
     * Currently, NaN is only included when ValueSet.isAll().
     *
     * For comparison operators: NOT_EQUAL
     * the Domain should consist of ranges (which do not sum to the whole ValueSet), and NaN.
     * Currently, NaN is only included when ValueSet.isAll().
     */
    if (operator_name == "less")
        return complement ? std::nullopt
                          : std::make_optional<Domain>(SortedRangeSet(type, Ranges{Range::lessThanRange(type, value)}), false);
    else if (operator_name == "lessOrEquals")
        return complement ? std::nullopt
                          : std::make_optional<Domain>(SortedRangeSet(type, Ranges{Range::lessThanOrEqualRange(type, value)}), false);
    else if (operator_name == "greater")
        return complement ? std::nullopt
                          : std::make_optional<Domain>(SortedRangeSet(type, Ranges{Range::greaterThanRange(type, value)}), false);
    else if (operator_name == "greaterOrEquals")
        return complement ? std::nullopt
                          : std::make_optional<Domain>(SortedRangeSet(type, Ranges{Range::greaterThanOrEqualRange(type, value)}), false);
    else if (operator_name == "equals")
        return complement ? std::nullopt : std::make_optional<Domain>(SortedRangeSet(type, Ranges{Range::equalRange(type, value)}), false);
    else if (operator_name == "notEquals")
        return complement ? std::make_optional<Domain>(SortedRangeSet(type, Ranges{Range::equalRange(type, value)}), false) : std::nullopt;

    throw Exception("Unhandled operator" + operator_name, DB::ErrorCodes::LOGICAL_ERROR);
}

Domain
DomainVisitor::extractDiscreteDomain(const String & operator_name, const DataTypePtr & type, const Field & value, const bool & complement)
{
    if (value.isNull())
        throw Exception("Value is not null!", DB::ErrorCodes::LOGICAL_ERROR);

    if (operator_name == "equals")
        return Domain(complement ? complementValueSet(createValueSet(type, Array{value})) : createValueSet(type, Array{value}), false);

    if (operator_name == "notEquals")
        return Domain(complement ? createValueSet(type, Array{value}) : complementValueSet(createValueSet(type, Array{value})), false);

    throw Exception("Unhandled operator" + operator_name, DB::ErrorCodes::LOGICAL_ERROR);
}

bool DomainVisitor::isValidOperatorForComparison(const String & operator_name)
{
    if (operator_name == "equals" || operator_name == "notEquals" || operator_name == "less" || operator_name == "lessOrEquals"
        || operator_name == "greater" || operator_name == "greaterOrEquals")
        return true;

    return false;
}

ExtractionResult DomainVisitor::visitInFunction(ASTPtr & node, const bool & complement)
{
    auto * in_fun = node->as<ASTFunction>();
    auto * fun_right = in_fun->arguments->children[1]->as<ASTFunction>();
    ConstASTs disjuncts;

    if (!fun_right || fun_right->name != "tuple")
        return visitNode(node, complement);

    ASTs & value_list = fun_right->arguments->children;
    if (value_list.empty())
        throw Exception("InListExpression should never be empty", DB::ErrorCodes::LOGICAL_ERROR);

    auto dir_extraction_result = processSimpleInPredicate(node, complement);

    if (dir_extraction_result.has_value())
        return dir_extraction_result.value();

    for (ASTPtr ast : value_list)
        disjuncts.emplace_back(makeASTFunction("equals", in_fun->arguments->children[0], ast));

    ASTPtr ast_disjuncts = PredicateUtils::combineDisjuncts(disjuncts);
    ExtractionResult extraction_result = process(ast_disjuncts, complement);

    // preserve original IN predicate as remaining predicate
    if (extraction_result.tuple_domain.isAll())
    {
        ASTPtr origin_predicate = node;
        if (complement)
            origin_predicate = makeASTFunction("not", origin_predicate);
        return ExtractionResult(extraction_result.tuple_domain, origin_predicate);
    }
    return extraction_result;
}

std::optional<ExtractionResult> DomainVisitor::processSimpleInPredicate(ASTPtr & node, const bool & complement)
{
    auto * in_fun = node->as<ASTFunction>();
    auto * fun_left = in_fun->arguments->children[0]->as<ASTIdentifier>();
    auto * fun_right = in_fun->arguments->children[1]->as<ASTFunction>();

    if (!fun_left)
        return std::nullopt;

    String symbol = fun_left->name();
    DataTypePtr sym_type = type_analyzer.getType(in_fun->arguments->children[0]);
    const ASTs & value_list = fun_right->arguments->children;
    Array in_values;
    ASTs excluded_expressions;
    DataTypePtr super_type;
    in_values.reserve(value_list.size());

    for (auto & value : value_list)
    {
        auto field_temp = ExpressionInterpreter::evaluateConstantExpression(value, column_types, context);
        if (!field_temp.has_value())
            return visitNode(node, complement);

        auto & field_type = field_temp->first;
        auto & field_value = field_temp->second;
        auto coerce_field_opt = canImplicitCoerceValue(field_value, field_type, sym_type);

        if (!coerce_field_opt.has_value())
            return visitNode(node, complement);

        Field coerce_field = coerce_field_opt.value();

        if (coerce_field.isNull()) //TODO: Inconsistent with Presto code, errors
        {
            if (!complement)
            {
                // in case of IN, NULL on the right results with NULL comparison result (effectively false in predicate context), so can be ignored, as the
                // comparison results are OR-ed
                continue;
            }
            // NOT IN is equivalent to NOT(s eq v1) AND NOT(s eq v2). When any right value is NULL, the comparison result is NULL, so AND's result can be at most
            // NULL (effectively false in predicate context)
            return ExtractionResult(TupleDomain::none(), PredicateConst::TRUE_VALUE);
        }

        // NaN can be ignored: it always compares to false, as if it was not among IN's values
        if (DB::Utils::isFloatingPointNaN(sym_type, coerce_field))
            continue;

        // in case of NOT IN with floating point, the NaN on the left passes the test (unless a NULL is found, and we exited earlier)
        // but this cannot currently be described with a Domain other than Domain.all
        TypeIndex idx = sym_type->getTypeId();
        if (complement && (idx == TypeIndex::Float32 || idx == TypeIndex::Float64))
            excluded_expressions.emplace_back(value);
        else
            in_values.push_back(coerce_field);
    }

    ValueSet value_set = createValueSet(sym_type, in_values);
    if (complement)
        value_set = complementValueSet(value_set);

    TupleDomain tuple_domain = TupleDomain(DomainMap{{symbol, Domain(value_set, false)}});
    ASTPtr remaining_expression;

    if (excluded_expressions.empty())
        remaining_expression = PredicateConst::TRUE_VALUE;
    else if (excluded_expressions.size() == 1)
        remaining_expression = makeASTFunction("notEquals", in_fun->arguments->children[0], excluded_expressions[0]);
    else
        remaining_expression = makeASTFunction("notIn", in_fun->arguments->children[0], makeASTFunction("tuple", excluded_expressions));

    return ExtractionResult(tuple_domain, remaining_expression);
}

ExtractionResult DomainVisitor::visitLikeFunction(ASTPtr & node, const bool & complement)
{
    auto * ast_fun = node->as<ASTFunction>();
    auto left = ast_fun->arguments->children[0];
    auto right = ast_fun->arguments->children[1];

    // LIKE not on a symbol
    if (!(left->as<ASTIdentifier>()) || !(right->as<ASTLiteral>()))
        return visitNode(node, complement);

    //TODO: support FixedString, why?
    DataTypePtr type = type_analyzer.getType(left);
    if (type->getTypeId() != TypeIndex::String)
        return visitNode(node, complement);

    String symbol = left->as<ASTIdentifier>()->name();
    String pattern = right->as<ASTLiteral>()->value.safeGet<String>();
    size_t pattern_constant_prefix_bytes = patternConstantPrefixBytes(pattern);

    if (pattern_constant_prefix_bytes == pattern.length())
    {
        // This should not actually happen, constant LIKE pattern should be converted to equality predicate before DomainTranslator is invoked.
        String fixed_pattern = extractFixedStringFromLikePattern(pattern);
        ValueSet value_set = createSingleValueSet(type, convertFieldToType(Field(fixed_pattern.c_str(), fixed_pattern.size()), *type));
        Domain domain = Domain(complement ? complementValueSet(value_set) : value_set, false);
        return ExtractionResult(TupleDomain(DomainMap{{symbol, domain}}), PredicateConst::TRUE_VALUE);
    }

    if (complement || pattern_constant_prefix_bytes == 0)
        return visitNode(node, complement);

    String constant_prefix = extractFixedStringFromLikePattern(pattern.substr(0, pattern_constant_prefix_bytes));
    auto domain = createRangeDomain(type, constant_prefix);

    if (domain.has_value())
        return ExtractionResult(TupleDomain(DomainMap{{symbol, domain.value()}}), node);

    return visitNode(node, complement);
}

ExtractionResult DomainVisitor::visitStartsWithFunction(ASTPtr & node, const bool & complement)
{
    auto * ast_fun = node->as<ASTFunction>();
    ASTPtr & target = ast_fun->arguments->children[0];
    ASTPtr & prefix = ast_fun->arguments->children[1];
    DataTypePtr type = type_analyzer.getType(target);

    if (!target->as<ASTIdentifier>() || !prefix->as<ASTLiteral>())
        return visitNode(node, complement);

    //TODO: support FixedString, why?
    if (type->getTypeId() != TypeIndex::String || complement)
        return visitNode(node, complement);

    String symbol = target->as<ASTIdentifier>()->name();
    String constant_prefix = prefix->as<ASTLiteral>()->value.get<String>();
    auto domain = createRangeDomain(type, constant_prefix);

    if (domain.has_value())
        return ExtractionResult(TupleDomain(DomainMap{{symbol, domain.value()}}), node);

    return visitNode(node, complement);
}

ExtractionResult DomainVisitor::visitIsNullFunction(ASTPtr & node, const bool & complement)
{
    auto * ast_fun = node->as<ASTFunction>();
    auto left = ast_fun->arguments->children[0];

    if (!(left->as<ASTIdentifier>()))
        return visitNode(node, complement);

    String symbol = left->as<ASTIdentifier>()->name();
    DataTypePtr column_type = checkedTypeLookup(symbol);
    Domain domain = complement ? Domain::onlyNull(column_type).complement() : Domain::onlyNull(column_type);
    return ExtractionResult(TupleDomain(DomainMap{{symbol, domain}}), PredicateConst::TRUE_VALUE);
}

ExtractionResult DomainVisitor::visitIsNotNullFunction(ASTPtr & node, const bool & complement)
{
    auto * ast_fun = node->as<ASTFunction>();
    auto left = ast_fun->arguments->children[0];
    if (!(left->as<ASTIdentifier>()))
        return visitNode(node, complement);

    String symbol = left->as<ASTIdentifier>()->name();
    DataTypePtr column_type = checkedTypeLookup(symbol);
    Domain domain = complement ? Domain::notNull(column_type).complement() : Domain::notNull(column_type);
    return ExtractionResult(TupleDomain(DomainMap{{symbol, domain}}), PredicateConst::TRUE_VALUE);
}


std::optional<Domain> DomainVisitor::createRangeDomain(const DataTypePtr & type, const String & constant_prefix)
{
    int last_asc2_index = -1;
    size_t code_point_length = 0;
    for (size_t position = 0; position < constant_prefix.size(); position += code_point_length)
    {
        // Get last ASCII character to increment, so that character length in bytes does not change.
        // Also prefer not to produce non-ASCII if input is all-ASCII, to be on the safe side with connectors.
        UInt8 begin = static_cast<UInt8>(constant_prefix[position]);
        code_point_length = DB::UTF8::seqLength(begin);

        if (code_point_length == 1 && begin < 127)
            last_asc2_index = position;
    }

    if (last_asc2_index == -1)
        return std::nullopt;

    String upper_bound = constant_prefix.substr(0, last_asc2_index + 1);
    upper_bound[last_asc2_index] = (upper_bound[last_asc2_index] + 1);

    Field low_bound_field = convertFieldToType(Field(constant_prefix), *type);
    Field upper_bound_field = convertFieldToType(Field(upper_bound), *type);
    return Domain(SortedRangeSet(type, Ranges{Range(type, true, low_bound_field, false, upper_bound_field)}), false);
}

size_t patternConstantPrefixBytes(const String & pattern)
{
    bool escaped = false;
    size_t pos = 0;
    for (; pos < pattern.size();)
    {
        if (escaped && (pattern[pos] == '%' || pattern[pos] == '_' || pattern[pos] == '\\')) //TODO:need to be confirm
        {
            escaped = false;
            pos++;
        }
        else if (!escaped && (pattern[pos] == '%' || pattern[pos] == '_'))
        {
            return pos;
        }
        else if (!escaped && pattern[pos] == '\\')
        {
            escaped = true;
            ++pos;
        }
        else
        {
            pos += UTF8::seqLength(static_cast<UInt8>(pattern[pos]));
            escaped = false;
        }
    }
    return pos;
}

String extractFixedStringFromLikePattern(const String & like_pattern)
{
    String fixed_prefix;

    const char * pos = like_pattern.data();
    const char * end = pos + like_pattern.size();
    while (pos < end)
    {
        switch (*pos)
        {
            case '%':
                [[fallthrough]];
            case '_':
                return fixed_prefix;

            case '\\':
                ++pos;
                if (pos == end)
                    break;
                [[fallthrough]];
            default:
                fixed_prefix += *pos;
                break;
        }

        ++pos;
    }

    return fixed_prefix;
}

}
