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
#include <Catalog/MetastoreByteKVImpl.h>
#include <Catalog/CatalogUtils.h>
#include <iostream>
#include <common/defines.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int METASTORE_OPERATION_ERROR;
    extern const int METASTORE_COMMIT_CAS_FAILURE;
}

namespace Catalog
{

using namespace bytekv::sdk;

MetastoreByteKVImpl::MetastoreByteKVImpl(const String & service_name_, const String & cluster_name_,
                                         const String & name_space_, const String & table_name_)
    : IMetaStore(), service_name(service_name_), cluster_name(cluster_name_), name_space(name_space_), table_name(table_name_)
{
    init();
}

void MetastoreByteKVImpl::init()
{
    auto code = ByteKVClientBuilder()
        .setServiceName(service_name)
        .setClusterName(cluster_name)
        .setNameSpace(name_space)
        .setConnectionTimeoutMS(3000)
        .setReadTimeoutMS(30000)
        .setWriteTimeoutMS(30000)
        .build(client);
    assertStatus(OperationType::OPEN, code, {Errorcode::OK});
}

void MetastoreByteKVImpl::put(const String & key, const String & value, bool if_not_exists)
{
    PutRequest put_req;
    PutResponse put_resp;
    put_req.table = this->table_name;
    put_req.key =  key;
    put_req.value = value;
    put_req.if_not_exists = if_not_exists;
    auto code = client->Put(put_req, &put_resp);
    assertStatus(OperationType::PUT, code, {Errorcode::OK});
}

void MetastoreByteKVImpl::putTTL(const String & key, const String & value, UInt64 ttl)
{
    PutRequest put_req{this->table_name, key, value, ttl};
    PutResponse put_resp;
    auto code = client->Put(put_req, &put_resp);
    assertStatus(OperationType::PUT, code, {Errorcode::OK});
}

std::pair<bool, String> MetastoreByteKVImpl::putCAS(const String & key, const String & value, const String & expected, [[maybe_unused]]bool with_old_value)
{
    PutRequest put_req;
    PutResponse put_resp;
    Slice expected_value{expected};
    put_req.table = this->table_name;
    put_req.key =  key;
    put_req.value = value;
    put_req.expected_value = &expected_value;
    auto code = client->Put(put_req, &put_resp);

    assertStatus(OperationType::PUT, code, {Errorcode::OK, Errorcode::CAS_FAILED});

    if (code == Errorcode::OK)
        return std::make_pair(true, "");

    return std::make_pair(false, std::move(put_resp.current_value));
}

uint64_t MetastoreByteKVImpl::get(const String & key, String & value)
{
    GetRequest get_req;
    GetResponse get_resp;
    get_req.table = this->table_name;
    get_req.key = key;
    auto code = client->Get(get_req, &get_resp);
    assertStatus(OperationType::GET, code, {Errorcode::OK, Errorcode::KEY_NOT_FOUND});
    value = std::move(get_resp.value);
    return get_resp.version;
}

std::vector<std::pair<String, UInt64>> MetastoreByteKVImpl::multiGet(const std::vector<String> & keys)
{
    std::vector<std::pair<String, UInt64>> res;
    MultiGetRequest mg_req;
    mg_req.gets_.resize(keys.size());
    for (size_t i = 0; i < keys.size(); ++i)
    {
        mg_req.gets_[i].table = this->table_name;
        mg_req.gets_[i].key = keys[i];
    }

    MultiGetResponse mg_resp;
    auto code = client->MultiGet(mg_req, &mg_resp);
    assertStatus(OperationType::MULTIGET, code, {Errorcode::OK});
    for (auto ele : mg_resp.results)
    {
        if (ele.first == Errorcode::OK)
            res.emplace_back(std::move(ele.second.value), ele.second.version);
        else
            res.emplace_back("", 0);
    }

    return res;
}

bool MetastoreByteKVImpl::batchWrite(const BatchCommitRequest & req, BatchCommitResponse response)
{
    bytekv::sdk::WriteBatchRequest wb_req;
    bytekv::sdk::WriteBatchResponse wb_resp;
    std::vector<Slice> expected_values;
    expected_values.reserve(req.puts.size());

    for (auto & single_put : req.puts)
    {
        bytekv::sdk::PutRequest put_req;
        put_req.table = this->table_name;
        put_req.key = Slice(single_put.key);
        put_req.value = Slice(single_put.value);
        put_req.if_not_exists = single_put.if_not_exists;

        if (single_put.expected_value)
        {
            expected_values.push_back(Slice(single_put.expected_value.value()));
            put_req.expected_value = &expected_values.back();
        }

        wb_req.AddPut(put_req);
    }

    for (auto & delete_key : req.deletes)
    {
        DeleteRequest del_req;
        del_req.table = this->table_name;
        del_req.key = Slice(delete_key);
        // del_req.expected_version = expected_version;
        wb_req.AddDelete(del_req);
    }

    auto collect_conflict_info = [&]()
    {
        for (size_t i=0; i < wb_resp.puts_.size(); i++)
        {
            PutResponse & put_response = std::get<1>(wb_resp.puts_[i]);
            if (req.puts[i].if_not_exists)
            {
                if (put_response.current_version!=0)
                    response.puts.emplace(i, put_response.current_value);
            }
            else if (req.puts[i].expected_value && req.puts[i].expected_value.value() != put_response.current_value)
            {
                response.puts.emplace(i, put_response.current_value);
            }
        }
    };

    if (req.with_cas)
    {
        auto code = client->WriteBatch(wb_req, &wb_resp);

        if (code == Errorcode::CAS_FAILED)
            collect_conflict_info();

        if (code == Errorcode::CAS_FAILED && req.allow_cas_fail)
        {
            /// report conflict info if cas failure is allowed
            assertStatus(OperationType::WRITEBATCH, code, {Errorcode::OK, Errorcode::CAS_FAILED});
        }
        else
            assertStatus(OperationType::WRITEBATCH, code, {Errorcode::OK});
    }
    else
    {
        auto code = client->MultiWrite(wb_req, &wb_resp);
        assertStatus(OperationType::MULTIWRITE, code, {Errorcode::OK});
        /// return code is OK cannot ensure all KV records be inserted successfully in MultiWrite, still need check the response.
        collect_conflict_info();
    }

    return response.puts.size() == 0;
}


void MetastoreByteKVImpl::drop(const String & key, const UInt64 & expected_version)
{
    DeleteRequest del_req;
    DeleteResponse del_resp;
    del_req.table = this->table_name;
    del_req.key = key;
    del_req.expected_version = expected_version;
    auto code = client->Delete(del_req, &del_resp);
    assertStatus(OperationType::DELETE, code, {Errorcode::OK});
}

MetastoreByteKVImpl::IteratorPtr MetastoreByteKVImpl::getAll()
{
    return getByPrefix("");
}

MetastoreByteKVImpl::IteratorPtr MetastoreByteKVImpl::getByPrefix(const String & partition_id, const size_t & limit, uint32_t scan_batch_size)
{
    ScanRequest scan_req;
    scan_req.scan_batch_count = scan_batch_size;
    scan_req.limit = limit;
    scan_req.table = table_name;
    scan_req.start_key = partition_id;
    String end_key = getNextKey(partition_id);
    scan_req.end_key = end_key;
    ScanResponse scan_resp;

    auto code = client->Scan(scan_req, &scan_resp);
    assertStatus(OperationType::SCAN, code, {Errorcode::OK});

    return std::make_shared<ByteKVIterator>(scan_resp.iterator);
}

MetastoreByteKVImpl::IteratorPtr MetastoreByteKVImpl::getByRange(const String & range_start, const String & range_end, const bool include_start, const bool include_end)
{
    ScanRequest scan_req;
    ScanResponse scan_resp;

    String start_key = include_start ? range_start : getNextKey(range_start);
    String end_key = include_end ? getNextKey(range_end) : range_end;

    scan_req.scan_batch_count = DEFAULT_SCAN_BATCH_COUNT;
    scan_req.table = table_name;
    scan_req.start_key = start_key;
    scan_req.end_key = end_key;

    auto code = client->Scan(scan_req, &scan_resp);
    assertStatus(OperationType::SCAN, code, {Errorcode::OK});

    return std::make_shared<ByteKVIterator>(scan_resp.iterator);
}

void MetastoreByteKVImpl::clean(const String & prefix)
{
    auto batch_delete = [&](std::vector<String> & keys)
    {
        WriteBatchRequest wb_req;
        wb_req.deletes_.resize(keys.size());
        for (size_t i=0; i < keys.size(); i++)
        {
            auto & del_req = wb_req.deletes_[i];
            del_req.table = this->table_name;
            del_req.key = keys[i];
        }

        WriteBatchResponse wb_resp;
        auto code = client->WriteBatch(wb_req, &wb_resp);
        assertStatus(OperationType::CLEAN, code, {Errorcode::OK});
    };

    IteratorPtr it = getByPrefix(prefix);

    std::vector<String> to_be_deleted;

    while(it->next())
    {
        to_be_deleted.emplace_back(it->key());

        /// safe guard. avoid too big write.
        if (to_be_deleted.size() == MAX_BATCH_SIZE)
        {
            batch_delete(to_be_deleted);
            to_be_deleted.clear();
        }
    }

    if (!to_be_deleted.empty())
        batch_delete(to_be_deleted);
}

void MetastoreByteKVImpl::cleanAll()
{
    ScanRequest scan_req;
    scan_req.scan_batch_count = DEFAULT_SCAN_BATCH_COUNT;
    scan_req.table = table_name;
    scan_req.start_key = "";
    ScanResponse scan_resp;

    auto code = client->Scan(scan_req, &scan_resp);
    assertStatus(OperationType::SCAN, code, {Errorcode::OK});

    IteratorPtr  it= std::make_shared<ByteKVIterator>(scan_resp.iterator);

    auto batch_delete = [&](std::vector<String> & keys)
    {
        WriteBatchRequest wb_req;
        for (size_t i=0; i<keys.size(); i++)
        {
            DeleteRequest del_req;
            del_req.table = this->table_name;
            del_req.key = keys[i];
            wb_req.AddDelete(del_req);
        }

        WriteBatchResponse wb_resp;
        code = client->WriteBatch(wb_req, &wb_resp);
        assertStatus(OperationType::CLEAN, code, {Errorcode::OK});
    };

    std::vector<String> to_be_deleted;

    while(it->next())
    {
        String key = it->key();
        to_be_deleted.emplace_back(std::move(key));

        /// safe guard. avoid too big write.
        if (to_be_deleted.size() == MAX_BATCH_SIZE)
        {
            batch_delete(to_be_deleted);
            to_be_deleted.clear();
        }
    }

    if (!to_be_deleted.empty())
        batch_delete(to_be_deleted);

}

void MetastoreByteKVImpl::assertStatus(const OperationType & op, const Errorcode & code, const ExpectedCodes & expected)
{
    for (auto expected_code : expected)
    {
        if (expected_code == code)
            return;
    }
    throw Exception("Unexpected result from byteKV. Operation : " + Operation(op) + ", Errormsg : " + ErrorString(code) , toCommonErrorCode(code));
}

int MetastoreByteKVImpl::toCommonErrorCode(const Errorcode & code)
{
    switch (code)
    {
    case Errorcode::CAS_FAILED :
        return ErrorCodes::METASTORE_COMMIT_CAS_FAILURE;

    default:
        return code;
    }
}


}
}
#endif
