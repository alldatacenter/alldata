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

#include <Storages/MergeTree/IMetastore.h>
#include <Common/Exception.h>
#include <atomic>
#include <shared_mutex>
#include <rocksdb/db.h>
#include <rocksdb/options.h>
#include <rocksdb/write_batch.h>

namespace DB
{

class MetastoreRocksDBImpl: public IMetaStore
{

public:
    struct RocksDBIterator : public IMetaStore::Iterator
    {
        RocksDBIterator(rocksdb::Iterator* it): inner_it(it){}

        ~RocksDBIterator() override
        {
            if(inner_it->status().ok())
                delete inner_it;
        }

        bool hasNext() override
        {
            return inner_it->Valid();
        }

        void next() override
        {
            inner_it->Next();
        }

        String key() override
        {
            return inner_it->key().ToString();
        }

        String value() override
        {
            return inner_it->value().ToString();
        }

    private:
        rocksdb::Iterator* inner_it;
    };

    class MultiWrite
    {
    public:
        MultiWrite(const std::shared_ptr<IMetaStore> & metastore_)
            : metastore(metastore_) {}
        void addPut(const String & key, const String & value);
        void addDelete(const String & key);
        void commit();
    private:
        std::shared_ptr<IMetaStore> metastore;
        BatchWriteRequest batch_req;
    };

private:
    void init();

    void assert_status(bool status, const rocksdb::Status & s, const String & msg);

    void assert_db_open();

    std::atomic_bool db_closed = true;
    rocksdb::DB* db = nullptr;
    Poco::Logger * log;

public:
    MetastoreRocksDBImpl(const String & db_path_);

    ~MetastoreRocksDBImpl() override;

    void put(const String & key, const String & value) override;

    void multiWrite(const BatchWriteRequest & batch) override;

    void get(const String & key, String & value) override;

    std::vector<String> multiGet(std::vector<String> & keys) override;

    void update(const String & key, const String & new_value) override;

    void drop(const String & key) override;

    IteratorPtr scan(const String & prefix = "") override;

    void clean() override;

    void close() override;

    void startUp() override;
};

}
