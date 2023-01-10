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

#pragma once

#include <Catalog/IMetastore.h>
#include <Catalog/FDBClient.h>
#include <Common/Exception.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int METASTORE_EXCEPTION;
}

namespace Catalog
{

class MetastoreFDBImpl : public IMetaStore
{
public:
    struct FDBIterator: public IMetaStore::Iterator
    {
    public:
        FDBIterator(std::shared_ptr<FDB::Iterator> iter)
            : inner_iter(iter)
        {
        }

        ~FDBIterator() override = default;

        inline bool next() override
        {
            bool has_next = inner_iter->Next(error_code);
            if (error_code)
                throw Exception("Error whiling scanning data through FBDItrator. message: " + String(fdb_get_error(error_code)), ErrorCodes::METASTORE_EXCEPTION);
            return has_next;
        }

        inline String key() override
        {
            return inner_iter->Key();
        }

        inline String value() override
        {
            return inner_iter->Value();
        }

    private:
        std::shared_ptr<FDB::Iterator> inner_iter;
        fdb_error_t error_code = 0;
    };

    MetastoreFDBImpl(const String & cluster_config_path);

    void put(const String & key, const String & value, bool if_not_exists = false) override;

    std::pair<bool, String> putCAS(const String & key, const String & value, const String & expected, bool with_old_value = false) override;

    uint64_t get(const String & key, String & value) override;

    std::vector<std::pair<String, UInt64>> multiGet(const std::vector<String> & keys) override;

    bool batchWrite(const BatchCommitRequest & req, BatchCommitResponse response) override;

    void drop(const String &, const UInt64 & expected = 0) override;

    IteratorPtr getAll() override;

    IteratorPtr getByPrefix(const String &, const size_t & limit = 0, uint32_t scan_batch_size = DEFAULT_SCAN_BATCH_COUNT) override;

    IteratorPtr getByRange(const String & range_start, const String & range_end, const bool include_start, const bool include_end) override;

    void clean(const String & prefix) override;

    void close() override {}

private:
    static void check_fdb_op(const fdb_error_t & error_t);
    /// convert metastore specific error code to Clickhouse error code for processing convenience in upper layer.
    static int toCommonErrorCode(const fdb_error_t & error_t);

    FDB::FDBClientPtr fdb_client;
};

}


}
