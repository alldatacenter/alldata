#include <Storages/System/StorageSystemCnchKafkaTables.h>
#if USE_RDKAFKA

#include <Catalog/Catalog.h>
#include <DataStreams/materializeBlock.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeString.h>
#include <Interpreters/ClusterProxy/executeQuery.h>
#include <Interpreters/ClusterProxy/SelectStreamFactory.h>
#include <Interpreters/Context.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/TreeRewriter.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <Storages/ColumnsDescription.h>

namespace DB
{
StorageSystemCnchKafkaTables::StorageSystemCnchKafkaTables(const StorageID & table_id_)
    : IStorage(table_id_)
{
    StorageInMemoryMetadata storage_metadata;
    storage_metadata.setColumns(ColumnsDescription({
        { "database",                   std::make_shared<DataTypeString>()  },
        { "name",                       std::make_shared<DataTypeString>()  },
        { "uuid",                       std::make_shared<DataTypeString>()  },
        { "kafka_cluster",              std::make_shared<DataTypeString>()  },
        { "topics",                     std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>()) },
        { "consumer_group",             std::make_shared<DataTypeString>()  },
        { "num_consumers",              std::make_shared<DataTypeUInt32>()  },
        { "consumer_tables",            std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "consumer_hosts",             std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "consumer_partitions",        std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "consumer_offsets",           std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())  },
        { "last_exception",             std::make_shared<DataTypeString>()  },

    }));
    setInMemoryMetadata(storage_metadata);
}

/// Create local query for `system.kafka_tables`
static ASTPtr getSelectQuery(const SelectQueryInfo & query_info)
{
    if (!query_info.syntax_analyzer_result)
        throw Exception("No syntax_analyzer_result found in SelectQueryInfo", ErrorCodes::LOGICAL_ERROR);
    if (!query_info.query)
        throw Exception("No query found in SelectQueryInfo", ErrorCodes::LOGICAL_ERROR);

    auto required_columns = query_info.syntax_analyzer_result->requiredSourceColumns();

    auto select_query = std::make_shared<ASTSelectQuery>();
    auto select_expression_list = std::make_shared<ASTExpressionList>();
    for (const auto & column_name: required_columns)
        select_expression_list->children.push_back(std::make_shared<ASTIdentifier>(column_name));

    auto tables_list = std::make_shared<ASTTablesInSelectQuery>();
    auto element = std::make_shared<ASTTablesInSelectQueryElement>();
    auto table_expr = std::make_shared<ASTTableExpression>();
    auto table_ast = std::make_shared<ASTTableIdentifier>("system", "kafka_tables");
    table_expr->database_and_table_name = table_ast;
    table_expr->children.push_back(table_ast);
    element->table_expression = table_expr;
    element->children.push_back(table_expr);
    tables_list->children.push_back(element);

    select_query->setExpression(ASTSelectQuery::Expression::SELECT, std::move(select_expression_list));
    select_query->setExpression(ASTSelectQuery::Expression::TABLES, std::move(tables_list));

    if (auto where_expression = query_info.query->as<ASTSelectQuery &>().where())
        select_query->setExpression(ASTSelectQuery::Expression::WHERE, std::move(where_expression));

    return select_query;
}

Pipe StorageSystemCnchKafkaTables::read(
    const Names & /*column_names*/,
    const StorageMetadataPtr & /*metadata_snapshot*/,
    SelectQueryInfo & query_info,
    ContextPtr context,
    QueryProcessingStage::Enum /*processed_stage*/,
    const size_t /*max_block_size*/,
    const unsigned /*num_streams*/)
{
    Catalog::CatalogPtr cnch_catalog = context->getCnchCatalog();
    if (context->getServerType() != ServerType::cnch_server || !cnch_catalog)
        throw Exception("Table system.kafka_tables is only supported on cnch_server side", ErrorCodes::NOT_IMPLEMENTED);

    auto select_query = getSelectQuery(query_info);
    Block header = materializeBlock(InterpreterSelectQuery(select_query, context, QueryProcessingStage::Complete).getSampleBlock());
    ClusterProxy::SelectStreamFactory select_stream_factory = ClusterProxy::SelectStreamFactory(
                                            header, QueryProcessingStage::Complete, StorageID("system", "kafka_tables"), {}, false, {});

    /// Set `query_info.cluster` to forward query to all instances of `server cluster`
    query_info.cluster = context->mockCnchServersCluster();

    QueryPlan query_plan;
    Poco::Logger * log = &Poco::Logger::get("SystemCnchKafkaTables");
    ClusterProxy::executeQuery(query_plan, select_stream_factory, log, select_query, context, query_info, nullptr, {}, nullptr);

    return query_plan.convertToPipe(QueryPlanOptimizationSettings::fromContext(context), BuildQueryPipelineSettings::fromContext(context));
}

} /// namespace DB

#endif
