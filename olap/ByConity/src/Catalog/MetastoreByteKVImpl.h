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

#if 0
#pragma once

#include <Catalog/IMetastore.h>
#include <bytekv4cpp/bytekv/client.h>
#include <Catalog/MetaStoreOperations.h>
#include <Common/Exception.h>
#include <iostream>

namespace DB
{

namespace ErrorCodes
{
    extern const int METASTORE_OPERATION_ERROR;
    extern const int METASTORE_EXCEPTION;
}

namespace Catalog
{

using namespace bytekv::sdk;

class MetastoreByteKVImpl : public IMetaStore
{

public:
    using ExpectedCodes = std::initializer_list<Errorcode>;

    struct ByteKVIterator: public IMetaStore::Iterator
    {
    public:
        ByteKVIterator(std::shared_ptr<bytekv::sdk::Iterator> & it) : inner_it(it) {}

        ~ByteKVIterator() override = default;

        inline bool next() override
        {
            bool has_next = inner_it->Next(&code);
            if (!has_next && code != Errorcode::ITERATOR_END)
                throw Exception("ByteKV iterator not end correctly, " + String(ErrorString(code)) + ". Reason : " + inner_it->ErrorText(), ErrorCodes::METASTORE_EXCEPTION);
            return has_next;
        }

        inline String key() override
        {
            return inner_it->Key();
        }

        inline String value() override
        {
            return inner_it->Value();
        }
    private:
        Errorcode code;
        std::shared_ptr<bytekv::sdk::Iterator> inner_it;
    };

    MetastoreByteKVImpl(const String & service_name_, const String & cluster_name_,
                        const String & name_space_, const String & table_name_);

    void init();

    void put(const String & key, const String & value, bool if_not_exists = false) override;

    void putTTL(const String & key, const String & value, UInt64 ttl);

    std::pair<bool, String> putCAS(const String & key, const String & value, const String & expected, bool with_old_value = false) override;

    uint64_t get(const String & key, String & value) override;

    std::vector<std::pair<String, UInt64>> multiGet(const std::vector<String> & keys) override;

    bool batchWrite(const BatchCommitRequest & req, BatchCommitResponse response) override;

    void drop(const String & key, const UInt64 & expected_version = 0) override;

    IteratorPtr getAll() override;

    IteratorPtr getByPrefix(const String & partition_id, const size_t & limit = 0, uint32_t scan_batch_size = DEFAULT_SCAN_BATCH_COUNT) override;

    IteratorPtr getByRange(const String & range_start, const String & range_end, const bool include_start, const bool include_end) override;

    void clean(const String & prefix) override;

    ///only for test;
    void cleanAll();

    void close() override {}

    static void assertStatus(const OperationType & op, const Errorcode & code, const ExpectedCodes & expected);

public:
    std::shared_ptr<ByteKVClient> client;


private:

    String service_name;
    String cluster_name;
    String name_space;
    String table_name;

    /// convert metastore specific error code to Clickhouse error code for processing convenience in upper layer.
    static int toCommonErrorCode(const Errorcode & code);
};
}


}
#endif
