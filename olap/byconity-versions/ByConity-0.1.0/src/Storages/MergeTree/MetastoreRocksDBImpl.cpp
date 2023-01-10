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

#include <Common/Exception.h>
#include <Storages/MergeTree/MetastoreRocksDBImpl.h>
#include <Poco/Logger.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int METASTORE_OPERATION_ERROR;
}

/// MultiWrite subclass definitions

void MetastoreRocksDBImpl::MultiWrite::addPut(const String & key, const String & value)
{
    batch_req.put_req.push_back(std::make_pair(key, value));
}

void MetastoreRocksDBImpl::MultiWrite::addDelete(const String & key)
{
    batch_req.delete_req.push_back(key);
}

void MetastoreRocksDBImpl::MultiWrite::commit()
{
    metastore->multiWrite(batch_req);
}

///MetastoreRocksDBImpl definitions
MetastoreRocksDBImpl::MetastoreRocksDBImpl(const String & db_path_)
        : IMetaStore(db_path_)
        , log(&Poco::Logger::get("MetastoreRocksDBImpl"))
{
    init();
}

MetastoreRocksDBImpl::~MetastoreRocksDBImpl()
{
    close();
}

void MetastoreRocksDBImpl::put(const String & key, const String & value)
{
    assert_db_open();
    rocksdb::Status s = db->Put(rocksdb::WriteOptions(), key, value);
    assert_status(s.ok(), s, "Failed to put metadata.");
}

void MetastoreRocksDBImpl::multiWrite(const BatchWriteRequest & batch_req)
{
    rocksdb::WriteBatch batch;
    for (auto & del_key : batch_req.delete_req)
        batch.Delete(del_key);
    for (auto & [k, v] : batch_req.put_req)
        batch.Put(k, v);
    rocksdb::Status s = db->Write(rocksdb::WriteOptions(), &batch);
    assert_status(s.ok(), s, "Failed to write batch into metadata.");
}

void MetastoreRocksDBImpl::get(const String & key, String & value)
{
    assert_db_open();
    rocksdb::Status s = db->Get(rocksdb::ReadOptions(), key, &value);
    assert_status(s.ok() || s.IsNotFound() , s, "Failed to get metadata.");
}

std::vector<String> MetastoreRocksDBImpl::multiGet(std::vector<String> & keys)
{
    assert_db_open();
    size_t batch_size = keys.size();
    std::vector<rocksdb::Slice> tmp_keys;
    std::vector<String> tmp_values, res;

    for (size_t i=0; i<batch_size; i++)
        tmp_keys.emplace_back(keys[i]);

    std::vector<rocksdb::Status> statuses = db->MultiGet(rocksdb::ReadOptions(), tmp_keys, &tmp_values);

    for (size_t i=0; i<batch_size; i++)
        if (statuses[i].ok())
            res.emplace_back(tmp_values[i]);
        else
            res.emplace_back("");

    return res;
}

void MetastoreRocksDBImpl::update(const String & key, const String & new_value)
{
    assert_db_open();
    String old_value;
    rocksdb::Status s = db->Get(rocksdb::ReadOptions(), key, &old_value);
    if (s.ok())
    {
        rocksdb::WriteBatch batch;
        batch.Delete(key);
        batch.Put(key, new_value);
        s = db->Write(rocksdb::WriteOptions(), &batch);
        assert_status(s.ok(), s, "Failed to update metadata.");
    }
}

void MetastoreRocksDBImpl::drop(const String & key)
{
    assert_db_open();
    rocksdb::Status s = db->SingleDelete(rocksdb::WriteOptions(), key);
    assert_status(s.ok(), s, "Failed to drop metadata.");
}

IMetaStore::IteratorPtr MetastoreRocksDBImpl::scan(const String & prefix)
{
    assert_db_open();
    rocksdb::Iterator * it = db->NewIterator(rocksdb::ReadOptions());
    if (prefix.empty())
        it->SeekToFirst();
    else
        it->Seek(prefix);
    return std::make_shared<RocksDBIterator>(it);
}

void MetastoreRocksDBImpl::clean()
{
    close();
    rocksdb::Options options;
    rocksdb::Status s = rocksdb::DestroyDB(db_path, options);
    assert_status(s.ok(), s, "Failed to drop metastore.");
    init();
}

void MetastoreRocksDBImpl::close()
{
    if (!db_closed)
    {
        db_closed = true;
        if (db)
        {
            db->Close();
            delete db;
            db = nullptr;
        }
    }
}

void MetastoreRocksDBImpl::startUp()
{
    if (db_closed)
    {
        init();
    }
}

void MetastoreRocksDBImpl::assert_status(bool status, const rocksdb::Status & s, const String & msg)
{
    if (!status)
        throw Exception("MetaStore Error! " +  msg + " Return status : " + s.ToString(), ErrorCodes::METASTORE_OPERATION_ERROR);
}

void MetastoreRocksDBImpl::assert_db_open()
{
    if (db_closed)
        throw Exception("MetaStore is not initialized or has been closed.", ErrorCodes::METASTORE_OPERATION_ERROR);
}


void MetastoreRocksDBImpl::init()
{
    rocksdb::Options options;
    options.create_if_missing = true;
    rocksdb::Status s = rocksdb::DB::Open(options, db_path, &db);
    assert_status(s.ok(), s, "Cannot open metastore.");
    db_closed = false;
}


}
