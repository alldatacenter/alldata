#pragma once
#include <Common/config.h>

#if USE_RDKAFKA

#include <Storages/IStorage.h>

#include <common/shared_ptr_helper.h>

namespace DB
{

class StorageSystemCnchKafkaTables : public shared_ptr_helper<StorageSystemCnchKafkaTables>, public IStorage
{
    friend struct shared_ptr_helper<StorageSystemCnchKafkaTables>;
public:
    std::string getName() const override { return "SystemCnchKafkaTables"; }

    Pipe read(
        const Names & /*column_names*/,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum /*processed_stage*/,
        size_t /*max_block_size*/,
        unsigned /*num_streams*/) override;

protected:
    explicit StorageSystemCnchKafkaTables(const StorageID & table_id_);
};

} /// namespace DB

#endif
