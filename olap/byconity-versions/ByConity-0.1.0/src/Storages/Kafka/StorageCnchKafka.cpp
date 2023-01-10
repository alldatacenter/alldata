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

#include <Common/config.h>
#if USE_RDKAFKA

#include <Storages/Kafka/StorageCnchKafka.h>

#include <DaemonManager/DaemonManagerClient.h>
#include <Databases/DatabaseOnDisk.h>
#include <Interpreters/InterpreterSystemQuery.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/ASTSystemQuery.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/parseQuery.h>
#include <Parsers/queryToString.h>
#include <Storages/StorageMaterializedView.h>
#include <Transaction/Actions/DDLAlterAction.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Transaction/CnchWorkerTransaction.h>

namespace DB
{

[[maybe_unused]] String & removeKafkaPrefix(String & name)
{
    if (startsWith(name, "kafka_"))
        name = name.substr(strlen("kafka_"));
    return name;
}

[[maybe_unused]] String & addKafkaPrefix(String & name)
{
    if (!startsWith(name, "kafka_"))
        name = "kafka_" + name;
    return name;
}

StorageCnchKafka::StorageCnchKafka(
    const StorageID & table_id_,
    ContextMutablePtr context_,
    const ColumnsDescription & columns_,
    const ConstraintsDescription & constraints_,
    const ASTPtr setting_changes_,
    const KafkaSettings & settings_)
    : IStorageCnchKafka(table_id_, context_, setting_changes_, settings_, columns_, constraints_),
      log(&Poco::Logger::get(table_id_.getNameForLogs()  + " (StorageCnchKafka)"))
{
}

void StorageCnchKafka::startup()
{
}

void StorageCnchKafka::shutdown()
{
}

void StorageCnchKafka::drop()
{
    auto cnch_catalog = getContext()->getCnchCatalog();
    const String & group = getGroupForBytekv();

    LOG_INFO(log, "Clear offsets for group '{}' before drop table: {}", group, getStorageID().getFullTableName());
    for (const auto & topic : getTopics())
        cnch_catalog->clearOffsetsForWholeTopic(topic, group);
}

void StorageCnchKafka::checkAlterIsPossible(const AlterCommands & commands, ContextPtr) const
{
    static std::set<AlterCommand::Type> cnchkafka_support_alter_types = {
        AlterCommand::ADD_COLUMN,
        AlterCommand::DROP_COLUMN,
        AlterCommand::MODIFY_COLUMN,
        AlterCommand::COMMENT_COLUMN,
        AlterCommand::MODIFY_SETTING,
        AlterCommand::RENAME_COLUMN,
        AlterCommand::ADD_CONSTRAINT,
        AlterCommand::DROP_CONSTRAINT,
    };

    for (const auto & command : commands)
    {
        if (!cnchkafka_support_alter_types.count(command.type))
            throw Exception("Alter of type '" + alterTypeToString(command.type) + "' is not supported by CnchKafka", ErrorCodes::NOT_IMPLEMENTED);

        LOG_INFO(log, "Executing CnchKafka ALTER command: {}", alterTypeToString(command.type));
    }

}

void StorageCnchKafka::alter(const AlterCommands & commands, ContextPtr local_context, TableLockHolder & /* alter_lock_holder */)
{
    auto daemon_manager = getGlobalContext()->getDaemonManagerClient();
    bool kafka_table_is_active = tableIsActive();

    const String full_name = getStorageID().getNameForLogs();
    if (kafka_table_is_active)
    {
        LOG_TRACE(log, "Stop consumption before altering table {}", full_name);
        daemon_manager->controlDaemonJob(getStorageID(), CnchBGThreadType::Consumer, CnchBGThreadAction::Stop);
    }

    SCOPE_EXIT({
        if (kafka_table_is_active)
        {
            LOG_TRACE(log, "Restart consumption no matter if ALTER succ for table {}", full_name);
            try
            {
                daemon_manager->controlDaemonJob(getStorageID(), CnchBGThreadType::Consumer, CnchBGThreadAction::Start);
            }
            catch (...)
            {
                tryLogCurrentException(log, "Failed to restart consume for table " + full_name + " after ALTER");
            }
        }
    });

    /// start alter
    LOG_TRACE(log, "Start altering table {}", full_name);

    StorageInMemoryMetadata new_metadata = getInMemoryMetadata();
    StorageInMemoryMetadata old_metadata = getInMemoryMetadata();

    TransactionCnchPtr txn = local_context->getCurrentTransaction();
    auto action = txn->createAction<DDLAlterAction>(shared_from_this());
    auto & alter_act = action->as<DDLAlterAction &>();
    alter_act.setMutationCommands(commands.getMutationCommands(
        old_metadata, false, local_context));

    bool alter_setting = commands.isSettingsAlter();
    /// Check setting changes if has
    auto new_settings = this->settings;
    for (const auto & c : commands)
    {
        if (c.type != AlterCommand::MODIFY_SETTING)
            continue;
        new_settings.applyKafkaSettingChanges(c.settings_changes);
    }

    checkAndLoadingSettings(new_settings);

    /// Add 'kafka_' prefix for changed settings
    AlterCommands new_commands = commands;
    if (alter_setting)
    {
        for (auto & command : new_commands)
        {
            if (command.type != AlterCommand::MODIFY_SETTING)
                continue;
            for (auto & change : command.settings_changes)
                addKafkaPrefix(change.name);
        }
    }

    /// Apply alter commands to metadata
    new_commands.apply(new_metadata, local_context);

    /// Apply alter commands to create-sql
    {
        String create_table_query = getCreateTableSql();
        ParserCreateQuery parser;
        ASTPtr ast = parseQuery(parser, create_table_query, local_context->getSettingsRef().max_query_size
            , local_context->getSettingsRef().max_parser_depth);

        applyMetadataChangesToCreateQuery(ast, new_metadata);
        alter_act.setNewSchema(queryToString(ast));
        txn->appendAction(std::move(action));
    }

    auto & txn_coordinator = local_context->getCnchTransactionCoordinator();
    txn_coordinator.commitV1(txn);

    if (alter_setting)
        this->settings = new_settings;
    setInMemoryMetadata(new_metadata);
}

bool StorageCnchKafka::tableIsActive() const
{
    auto catalog = getGlobalContext()->getCnchCatalog();
    std::optional<CnchBGThreadStatus> thread_status = catalog->getBGJobStatus(getStorageUUID(), CnchBGThreadType::Consumer);
    if ((!thread_status) ||
        (*thread_status == CnchBGThreadStatus::Running))
        return true;
    return false;
}

/// TODO: merge logic of `checkDependencies` in KafkaConsumeManager
StoragePtr StorageCnchKafka::tryGetTargetTable()
{
    auto catalog = getGlobalContext()->getCnchCatalog();
    auto views = catalog->getAllViewsOn(*getGlobalContext(), shared_from_this(), getContext()->getTimestamp());
    if (views.size() > 1)
        throw Exception("CnchKafka should only support ONE MaterializedView table now, but got "
                        + toString(views.size()), ErrorCodes::LOGICAL_ERROR);

    for (const auto & view : views)
    {
        auto *mv = dynamic_cast<StorageMaterializedView *>(view.get());
        if (!mv)
            throw Exception("Dependence for CnchKafka should be MaterializedView, but got "
                            + view->getName(), ErrorCodes::LOGICAL_ERROR);

        /// target_table should be CnchMergeTree now, but it may be some other new types
        auto target_table = mv->getTargetTable();
        if (target_table)
            return target_table;
    }

    return nullptr;
}

}

#endif
