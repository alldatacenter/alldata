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

#include <algorithm>
#include <memory>
#include <Optimizer/Property/PropertyDeriver.h>

#include <Core/Names.h>
#include <Optimizer/ExpressionRewriter.h>
#include <Optimizer/Property/Property.h>
#include <Optimizer/SymbolsExtractor.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/UnionStep.h>
#include <Storages/StorageDistributed.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/IAST_fwd.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int OPTIMIZER_NONSUPPORT;
}

Property PropertyDeriver::deriveProperty(ConstQueryPlanStepPtr step, Context & context)
{
    PropertySet property_set;
    return deriveProperty(step, property_set, context);
}

Property PropertyDeriver::deriveProperty(ConstQueryPlanStepPtr step, Property & input_property, Context & context)
{
    PropertySet input_properties = std::vector<Property>();
    input_properties.emplace_back(input_property);
    return deriveProperty(step, input_properties, context);
}

Property PropertyDeriver::deriveProperty(ConstQueryPlanStepPtr step, PropertySet & input_properties, Context & context)
{
    DeriverContext deriver_context{input_properties, context};
    static DeriverVisitor visitor{};
    auto result = VisitorUtil::accept(step, visitor, deriver_context);

    CTEDescriptions cte_descriptions;
    for (auto & property : input_properties)
        for (auto & item : property.getCTEDescriptions())
            cte_descriptions.emplace(item);
    if (!cte_descriptions.empty())
        result.setCTEDescriptions(cte_descriptions);

    return result;
}

Property PropertyDeriver::deriveStorageProperty(const StoragePtr & storage, Context & context)
{
    if (storage->getStorageID().getDatabaseName() == "system")
    {
        return Property{Partitioning(Partitioning::Handle::SINGLE), Partitioning(Partitioning::Handle::ARBITRARY)};
    }

    if (context.getSettingsRef().enable_sharding_optimize)
    {
        if (const auto * distribute_table = dynamic_cast<const StorageDistributed *>(storage.get()))
        {
            auto sharding_key = distribute_table->getShardingKey();
            auto symbols = SymbolsExtractor::extract(sharding_key);

            ConstASTMap expression_map;
            size_t index = 0;
            for (auto symbol : symbols)
            {
                ASTPtr name = std::make_shared<ASTIdentifier>(symbol);
                ASTPtr id = std::make_shared<ASTLiteral>(Field(index));
                expression_map[name] = id;
                index++;
            }

            ASTPtr rewritten = ExpressionRewriter::rewrite(sharding_key, expression_map);

            if (!symbols.empty())
            {
                Names partition_keys{symbols.begin(), symbols.end()};
                std::sort(partition_keys.begin(), partition_keys.end());
                return Property{
                    Partitioning{
                        Partitioning::Handle::BUCKET_TABLE,
                        partition_keys,
                        true,
                        distribute_table->getShardCount(),
                        rewritten},
                    Partitioning{}};
            }
        }
    }

    return Property{Partitioning(Partitioning::Handle::UNKNOWN), Partitioning(Partitioning::Handle::UNKNOWN)};
}

Property DeriverVisitor::visitStep(const IQueryPlanStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitProjectionStep(const ProjectionStep & step, DeriverContext & context)
{
    auto assignments = step.getAssignments();
    std::unordered_map<String, String> identities = Utils::computeIdentityTranslations(assignments);
    std::unordered_map<String, String> revert_identifies;

    for (auto & item : identities)
    {
        revert_identifies[item.second] = item.first;
    }
    Property translated;
    if (!context.getInput().empty())
    {
        translated = context.getInput()[0].translate(revert_identifies);
    }

    // if partition columns are pruned, the output data has no property.
    if (translated.getNodePartitioning().getPartitioningColumns().size()
        != context.getInput()[0].getNodePartitioning().getPartitioningColumns().size())
    {
        return Property{};
    }

    if (translated.getStreamPartitioning().getPartitioningColumns().size()
        != context.getInput()[0].getStreamPartitioning().getPartitioningColumns().size())
    {
        // TODO stream partition
    }
    return translated;
}

// TODO derive constants from predicate, e.g (where a = 1, ...)
Property DeriverVisitor::visitFilterStep(const FilterStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitJoinStep(const JoinStep & step, DeriverContext & context)
{
    std::unordered_map<String, String> identities;
    for (auto & item : step.getOutputStream().header)
    {
        identities[item.name] = item.name;
    }
    Property translated;
    if (!context.getInput().empty())
    {
        translated = context.getInput()[0].translate(identities);
    }

    // if partition columns are pruned, the output data has no property.
    if (translated.getNodePartitioning().getPartitioningColumns().size()
        != context.getInput()[0].getNodePartitioning().getPartitioningColumns().size())
    {
        return Property{};
    }
    if (translated.getStreamPartitioning().getPartitioningColumns().size()
        != context.getInput()[0].getStreamPartitioning().getPartitioningColumns().size())
    {
        // TODO stream partition
    }
    return translated;
}

// TODO partition key inference, translate properties according to group by keys
Property DeriverVisitor::visitAggregatingStep(const AggregatingStep & step, DeriverContext & context)
{
    if (step.getKeys().empty())
    {
        return Property{Partitioning{Partitioning::Handle::SINGLE}, Partitioning{Partitioning::Handle::SINGLE}};
    }
    return context.getInput()[0];
}

Property DeriverVisitor::visitMergingAggregatedStep(const MergingAggregatedStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitUnionStep(const UnionStep & step, DeriverContext & context)
{
    Property first_child_property = context.getInput()[0];
    if (first_child_property.getNodePartitioning().getPartitioningHandle() == Partitioning::Handle::SINGLE)
    {
        if (step.isLocal())
        {
            return Property{Partitioning{Partitioning::Handle::SINGLE}, Partitioning{Partitioning::Handle::SINGLE}};
        }
        else
        {
            return Property{Partitioning{Partitioning::Handle::SINGLE}};
        }
    }

    std::vector<Property> transformed_children_prop;
    const auto & output_to_inputs = step.getOutToInputs();
    size_t index = 0;
    for (const auto & child_prop : context.getInput())
    {
        NameToNameMap mapping;
        for (const auto & output_to_input : output_to_inputs)
        {
            mapping[output_to_input.second[index]] = output_to_input.first;
        }
        index++;
        transformed_children_prop.emplace_back(child_prop.translate(mapping));
    }

    if (first_child_property.getNodePartitioning().getPartitioningHandle() == Partitioning::Handle::FIXED_HASH)
    {
        const Names & keys = first_child_property.getNodePartitioning().getPartitioningColumns();
        Names output_keys;
        bool match = true;
        for (const auto & transformed : transformed_children_prop)
        {
            if (transformed != transformed_children_prop[0])
            {
                match = false;
                break;
            }
        }

        if (match && keys.size() == transformed_children_prop[0].getNodePartitioning().getPartitioningColumns().size())
        {
            output_keys = transformed_children_prop[0].getNodePartitioning().getPartitioningColumns();
        }
        if (step.isLocal())
        {
            return Property{
                Partitioning{Partitioning::Handle::FIXED_HASH, output_keys, true, first_child_property.getNodePartitioning().getBuckets()},
                Partitioning{Partitioning::Handle::SINGLE}};
        }
        else
        {
            return Property{
                Partitioning{Partitioning::Handle::FIXED_HASH, output_keys, true, first_child_property.getNodePartitioning().getBuckets()}};
        }
    }
    return Property{};
}

Property DeriverVisitor::visitExceptStep(const ExceptStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitIntersectStep(const IntersectStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitExchangeStep(const ExchangeStep & step, DeriverContext & context)
{
    const ExchangeMode & mode = step.getExchangeMode();
    if (mode == ExchangeMode::GATHER)
    {
        return Property{Partitioning{Partitioning::Handle::SINGLE}};
    }

    if (mode == ExchangeMode::REPARTITION)
    {
        return Property{step.getSchema()};
    }

    if (mode == ExchangeMode::BROADCAST)
    {
        return Property{Partitioning{Partitioning::Handle::FIXED_BROADCAST}};
    }

    if (mode == ExchangeMode::LOCAL_NO_NEED_REPARTITION)
    {
        Property output = context.getInput()[0];
        output.setStreamPartitioning(Partitioning{});
        return output;
    }

    return context.getInput()[0];
}

Property DeriverVisitor::visitRemoteExchangeSourceStep(const RemoteExchangeSourceStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitTableScanStep(const TableScanStep & step, DeriverContext & context)
{
    NameToNameMap translation;
    for (const auto & item : step.getColumnAlias())
        translation.emplace(item.first, item.second);

    return PropertyDeriver::deriveStorageProperty(step.getStorage(), context.getContext()).translate(translation);
}

Property DeriverVisitor::visitReadNothingStep(const ReadNothingStep &, DeriverContext &)
{
    return Property{Partitioning(Partitioning::Handle::SINGLE), Partitioning(Partitioning::Handle::ARBITRARY)};
}

Property DeriverVisitor::visitValuesStep(const ValuesStep &, DeriverContext &)
{
    return Property{Partitioning(Partitioning::Handle::SINGLE), Partitioning(Partitioning::Handle::ARBITRARY)};
}

Property DeriverVisitor::visitLimitStep(const LimitStep &, DeriverContext & context)
{
    return Property{context.getInput()[0].getNodePartitioning(), Partitioning(Partitioning::Handle::SINGLE)};
}

Property DeriverVisitor::visitLimitByStep(const LimitByStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitSortingStep(const SortingStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitMergeSortingStep(const MergeSortingStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitPartialSortingStep(const PartialSortingStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitMergingSortedStep(const MergingSortedStep &, DeriverContext & context)
{
    return Property{context.getInput()[0].getNodePartitioning(), Partitioning(Partitioning::Handle::SINGLE)};
}
//Property DeriverVisitor::visitMaterializingStep(const MaterializingStep &, DeriverContext & context)
//{
//    return context.getInput()[0];
//}
//
//Property DeriverVisitor::visitDecompressionStep(const DecompressionStep &, DeriverContext & context)
//{
//    return context.getInput()[0];
//}

Property DeriverVisitor::visitDistinctStep(const DistinctStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitExtremesStep(const ExtremesStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

//Property DeriverVisitor::visitFinalSamplingStep(const FinalSamplingStep &, DeriverContext & context)
//{
//    return context.getInput()[0];
//}

Property DeriverVisitor::visitWindowStep(const WindowStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitApplyStep(const ApplyStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitEnforceSingleRowStep(const EnforceSingleRowStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitAssignUniqueIdStep(const AssignUniqueIdStep &, DeriverContext & context)
{
    return context.getInput()[0];
}

Property DeriverVisitor::visitCTERefStep(const CTERefStep &, DeriverContext &)
{
    //    // CTERefStep output property can not be determined locally, it has been determined globally,
    //    //  Described in CTEDescription of property. If They don't match, we just ignore required property.
    //    // eg, input required property: <Repartition[B], CTE(0)=Repartition[A]> don't match,
    //    //    we ignore local required property Repartition[B] and prefer global property Repartition[A].
    //    CTEId cte_id = step.getId();
    //    Property cte_required_property = context.getRequiredProperty().getCTEDescriptions().at(cte_id).toProperty();
    //
    //    auto output_prop = cte_required_property.translate(step.getReverseOutputColumns());
    //    output_prop.getCTEDescriptions().emplace(cte_id, cte_required_property);
    //    return output_prop;
    throw Exception("CTERefStep is not supported", ErrorCodes::OPTIMIZER_NONSUPPORT);
}
}
