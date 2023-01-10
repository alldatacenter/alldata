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

#include <bytekv4cpp/bytekv/client.h>
#include <TSO/TSOOperations.h>
#include <Common/Exception.h>
#include <Core/Types.h>
#include <TSO/TSOMetastore.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int TSO_OPERATION_ERROR;
}

namespace TSO
{

using namespace bytekv::sdk;

class TSOMetaByteKVImpl : public TSOMetastore
{

public:
    using ExpectedCodes = std::initializer_list<Errorcode>;

    TSOMetaByteKVImpl(
        const String & service_name_,
        const String & cluster_name_,
        const String & name_space_,
        const String & table_name_,
        const String & key_name_)
        : TSOMetastore(key_name_), service_name(service_name_), cluster_name(cluster_name_), name_space(name_space_), table_name(table_name_)
    {
        init();
    }

    ~TSOMetaByteKVImpl() override {}

    void init()
    {
        auto code = ByteKVClientBuilder()
                .setServiceName(service_name)
                .setClusterName(cluster_name)
                .setNameSpace(name_space)
                .build(client);
        assertStatus(OperationType::OPEN, code, {Errorcode::OK});
    }

    void put(const String & value) override
    {
        PutRequest put_req;
        PutResponse put_resp;
        put_req.table = this->table_name;
        put_req.key =  this->key_name;
        put_req.value = value;
        auto code = client->Put(put_req, &put_resp);
        assertStatus(OperationType::PUT, code, {Errorcode::OK});
    }

    void get(String & value) override
    {
        GetRequest get_req;
        GetResponse get_resp;
        get_req.table = this->table_name;
        get_req.key = this->key_name;
        auto code = client->Get(get_req, &get_resp);
        assertStatus(OperationType::GET, code, {Errorcode::OK, Errorcode::KEY_NOT_FOUND});
        value = std::move(get_resp.value);
    }

    void clean() override
    {
        DeleteRequest del_req;
        DeleteResponse del_resp;
        del_req.table = this->table_name;
        del_req.key = this->key_name;
        auto code = client->Delete(del_req, &del_resp);
        assertStatus(OperationType::CLEAN, code, {Errorcode::OK});
    }

public:
    std::shared_ptr<ByteKVClient> client;

private:
    String service_name;
    String cluster_name;
    String name_space;
    String table_name;

private:
    void assertStatus(const OperationType & op, const Errorcode & code, const ExpectedCodes & expected)
    {
        for (auto expected_code : expected)
        {
            if (expected_code == code)
                return;
        }
        throw Exception("Unexpected result from byteKV. Operation : " + Operation(op) + ", Errormsg : " + ErrorString(code) , ErrorCodes::TSO_OPERATION_ERROR);
    }
};

}

}
#endif
