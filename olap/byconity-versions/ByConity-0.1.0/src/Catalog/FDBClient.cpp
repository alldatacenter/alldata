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

#include <Catalog/FDBClient.h>
#include <Catalog/FDBError.h>
#include <Common/Exception.h>

namespace DB
{


namespace ErrorCodes
{
    extern const int METASTORE_EXCEPTION;
}

namespace FDB
{

#define RETURN_ON_ERROR(code) \
    if (code) \
        return code

#define THROW_ON_ERROR(code) \
    if (code) \
        throw Exception("Error occurs while initializing fdb client. code: " + std::to_string(code) + ", msg: " \
                + std::string(fdb_get_error(code)), ErrorCodes::METASTORE_EXCEPTION)

static void AssertTrsansactionStatus(FDBTransactionPtr tr)
{
    if (!tr || !tr->transaction)
        throw Exception("Transaction is not initialized.", ErrorCodes::METASTORE_EXCEPTION);
}

FDBTransactionRAII::FDBTransactionRAII()
{
}

FDBTransactionRAII::FDBTransactionRAII(FDBTransaction * tr_)
    : transaction(std::move(tr_))
{
}

FDBTransactionRAII::~FDBTransactionRAII()
{
    if (transaction)
        fdb_transaction_destroy(transaction);
}

FDBFutureRAII::FDBFutureRAII(FDBFuture * future_)
    : future(std::move(future_))
{
}

FDBFutureRAII::~FDBFutureRAII()
{
    if (future)
        fdb_future_destroy(future);
}

FDBClient::FDBClient(const std::string & cluster_file)
{
    THROW_ON_ERROR(fdb_select_api_version(710));
    THROW_ON_ERROR(fdb_setup_network());
    pthread_create(&fdb_netThread, nullptr, [](void* )->void * {THROW_ON_ERROR(fdb_run_network()); return nullptr;}, nullptr);
    THROW_ON_ERROR(fdb_create_database(cluster_file.c_str(), &fdb));
}

FDBClient::~FDBClient()
{
    if (fdb)
        fdb_database_destroy(fdb);
    [[maybe_unused]]auto code = fdb_stop_network();
    pthread_join(fdb_netThread, nullptr);
}

fdb_error_t FDBClient::CreateTransaction(FDBTransactionPtr tr)
{
    RETURN_ON_ERROR(fdb_database_create_transaction(fdb, &(tr->transaction)));
    return 0;
}

fdb_error_t FDBClient::Get(FDBTransactionPtr tr, const std::string & key, GetResponse & res)
{
    AssertTrsansactionStatus(tr);
    const uint8_t* p_key = reinterpret_cast<const uint8_t*>(key.c_str());
    FDBFuturePtr f_read = std::make_shared<FDBFutureRAII>(fdb_transaction_get(tr->transaction, p_key, key.size(), 1));
    RETURN_ON_ERROR(fdb_future_block_until_ready(f_read->future));
    RETURN_ON_ERROR(fdb_future_get_error(f_read->future));
    fdb_bool_t present;
    uint8_t const *outValue;
    int outValueLength;
    RETURN_ON_ERROR(fdb_future_get_value(f_read->future, &present, &outValue, &outValueLength));
    if (present)
    {
        res.is_present = true;
        res.value = std::string(outValue, outValue+outValueLength);
    }

    return 0;
}

fdb_error_t FDBClient::Put(FDBTransactionPtr tr, const PutRequest & put)
{
    AssertTrsansactionStatus(tr);
    if (put.if_not_exists || put.expected_value)
    {
        GetResponse get_res;
        Get(tr, std::string(put.key.data, put.key.size), get_res);

        if ((put.if_not_exists && get_res.is_present)
            || (put.expected_value && std::string_view(put.expected_value.value()) != get_res.value))
            return FDBError::FDB_not_committed;
    }

    fdb_transaction_set(tr->transaction, reinterpret_cast<const uint8_t*>(put.key.data), put.key.size, reinterpret_cast<const uint8_t*>(put.value.data), put.value.size);
    FDBFuturePtr f = std::make_shared<FDBFutureRAII>(fdb_transaction_commit(tr->transaction));
    RETURN_ON_ERROR(fdb_future_block_until_ready(f->future));
    RETURN_ON_ERROR(fdb_future_get_error(f->future));

    return 0;
}

std::shared_ptr<Iterator> FDBClient::Scan(FDBTransactionPtr tr, const ScanRequest & scan_req)
{
    AssertTrsansactionStatus(tr);
    return std::make_shared<Iterator>(tr, scan_req);
}

fdb_error_t FDBClient::MultiGet(FDBTransactionPtr tr, const std::vector<std::string> & keys, std::vector<std::pair<std::string, UInt64>> & values)
{
    AssertTrsansactionStatus(tr);
    RETURN_ON_ERROR(fdb_database_create_transaction(fdb, &(tr->transaction)));
    std::vector<FDBFuturePtr> future_list;
    for (auto & key : keys)
    {
        const uint8_t* p_key = reinterpret_cast<const uint8_t*>(key.c_str());
        FDBFuturePtr f_read = std::make_shared<FDBFutureRAII>(fdb_transaction_get(tr->transaction, p_key, key.size(), 1));
        future_list.emplace_back(f_read);
    }

    fdb_bool_t present;
    uint8_t const *outValue;
    int outValueLength;
    for (FDBFuturePtr f_read : future_list)
    {
        RETURN_ON_ERROR(fdb_future_block_until_ready(f_read->future));
        RETURN_ON_ERROR(fdb_future_get_value(f_read->future, &present, &outValue, &outValueLength));
        if (present)
            values.emplace_back(std::make_pair(std::string(outValue, outValue+outValueLength), 1));
        else
            values.emplace_back(std::make_pair("", 0));
    }

    return 0;
}

fdb_error_t FDBClient::MultiWrite(FDBTransactionPtr tr, const Catalog::BatchCommitRequest & req, Catalog::BatchCommitResponse & resp)
{
    AssertTrsansactionStatus(tr);
    std::vector<int> index_of_cas_req;
    std::vector<FDBFuturePtr> future_list;
    for (size_t i=0; i<req.puts.size(); i++)
    {
        const Catalog::SinglePutRequest & single_put_req = req.puts[i];
        if (single_put_req.if_not_exists || single_put_req.expected_value)
        {
            const uint8_t* p_key = reinterpret_cast<const uint8_t*>(single_put_req.key.data());
            FDBFuturePtr f_read = std::make_shared<FDBFutureRAII>(fdb_transaction_get(tr->transaction, p_key, single_put_req.key.size(), 0));
            future_list.emplace_back(f_read);
            index_of_cas_req.emplace_back(i);
        }
    }

    fdb_bool_t present;
    uint8_t const *outValue;
    int outValueLength;

    for (size_t i=0; i<future_list.size(); i++)
    {
        RETURN_ON_ERROR(fdb_future_block_until_ready(future_list[i]->future));
        RETURN_ON_ERROR(fdb_future_get_error(future_list[i]->future));
        RETURN_ON_ERROR(fdb_future_get_value(future_list[i]->future, &present, &outValue, &outValueLength));

        const Catalog::SinglePutRequest & put_req = req.puts[index_of_cas_req[i]];

        if (put_req.if_not_exists)
        {
            if (present)
                resp.puts.emplace(index_of_cas_req[i], std::string(outValue, outValue+outValueLength));
        }
        else if (put_req.expected_value)
        {
            if (present)
            {
                if (put_req.expected_value->size()!=static_cast<size_t>(outValueLength) || memcmp(put_req.expected_value->data(), outValue, outValueLength))
                    resp.puts.emplace(index_of_cas_req[i], std::string(outValue, outValue+outValueLength));
            }
            else
            {
                resp.puts.emplace(index_of_cas_req[i], "");
            }
        }
    }

    /// return immediately if find any conflict in current commit;
    if (resp.puts.size())
        return FDBError::FDB_not_committed;

    for (size_t i=0; i<req.puts.size(); i++)
    {
         fdb_transaction_set(tr->transaction,
            reinterpret_cast<const uint8_t*>(req.puts[i].key.data()),
            req.puts[i].key.size(),
            reinterpret_cast<const uint8_t*>(req.puts[i].value.data()),
            req.puts[i].value.size()
        );
    }

    for (size_t i=0; i<req.deletes.size(); i++)
        fdb_transaction_clear(tr->transaction, reinterpret_cast<const uint8_t*>(req.deletes[i].c_str()), req.deletes[i].size());

    FDBFuturePtr f = std::make_shared<FDBFutureRAII>(fdb_transaction_commit(tr->transaction));
    RETURN_ON_ERROR(fdb_future_block_until_ready(f->future));
    fdb_error_t error_code = fdb_future_get_error(f->future);

    return error_code;
}

fdb_error_t FDBClient::Delete(FDBTransactionPtr tr, const std::string & key)
{
    AssertTrsansactionStatus(tr);
    fdb_transaction_clear(tr->transaction, reinterpret_cast<const uint8_t*>(key.c_str()), key.size());
    FDBFuturePtr f = std::make_shared<FDBFutureRAII>(fdb_transaction_commit(tr->transaction));
    RETURN_ON_ERROR(fdb_future_block_until_ready(f->future));
    RETURN_ON_ERROR(fdb_future_get_error(f->future));
    return 0;
}

fdb_error_t FDBClient::Clear(FDBTransactionPtr tr, const std::string & start_key, const std::string & end_key)
{
    AssertTrsansactionStatus(tr);
    fdb_transaction_clear_range(tr->transaction, reinterpret_cast<const uint8_t*>(start_key.c_str()), start_key.size(), reinterpret_cast<const uint8_t*>(end_key.c_str()), end_key.size());
    FDBFuturePtr f = std::make_shared<FDBFutureRAII>(fdb_transaction_commit(tr->transaction));
    RETURN_ON_ERROR(fdb_future_block_until_ready(f->future));
    RETURN_ON_ERROR(fdb_future_get_error(f->future));
    return 0;
}

void FDBClient::DestroyTransaction(FDBTransactionPtr tr)
{
    AssertTrsansactionStatus(tr);
    fdb_transaction_destroy(tr->transaction);
}

Iterator::Iterator(FDBTransactionPtr tr_, const ScanRequest & req_)
    : tr(tr_), req(req_)
{
    AssertTrsansactionStatus(tr);
    start_key_batch = req.start_key;
}

bool Iterator::Next(fdb_error_t & code)
{
    if (batch_count > batch_read_index)
    {
        batch_read_index++;
        return true;
    }
    else
    {
        if (iteration > 1 && !has_more)
            return false;

        if (iteration==1)
        {
            batch_future = std::make_shared<FDBFutureRAII>(fdb_transaction_get_range(tr->transaction,
                FDB_KEYSEL_FIRST_GREATER_OR_EQUAL(reinterpret_cast<const uint8_t*>(start_key_batch.c_str()), start_key_batch.size()),
                FDB_KEYSEL_FIRST_GREATER_OR_EQUAL(reinterpret_cast<const uint8_t*>(req.end_key.c_str()), req.end_key.size()),
                req.row_limit, 0, FDB_STREAMING_MODE_ITERATOR, iteration, 1, req.reverse_order));
        }
        else
        {
            batch_future = std::make_shared<FDBFutureRAII>(fdb_transaction_get_range(tr->transaction,
                FDB_KEYSEL_FIRST_GREATER_THAN(reinterpret_cast<const uint8_t*>(start_key_batch.c_str()), start_key_batch.size()),
                FDB_KEYSEL_FIRST_GREATER_OR_EQUAL(reinterpret_cast<const uint8_t*>(req.end_key.c_str()), req.end_key.size()),
                req.row_limit, 0, FDB_STREAMING_MODE_ITERATOR, iteration, 1, req.reverse_order));
        }

        code = fdb_future_block_until_ready(batch_future->future);
        if (code = fdb_future_block_until_ready(batch_future->future); code)
            return false;
        if (code = fdb_future_get_error(batch_future->future); code)
            return false;
        if (code = fdb_future_get_keyvalue_array(batch_future->future, &batch_kvs, &batch_count, &has_more); code)
            return false;

        if (batch_count > 0)
        {
            start_key_batch = std::string(reinterpret_cast<const char *>(batch_kvs[batch_count-1].key), batch_kvs[batch_count-1].key_length);
            iteration++;
            batch_read_index = 1;
            return true;
        }
        else
            return false;
    }
}

std::string Iterator::Key()
{
    return std::string(reinterpret_cast<const char *>(batch_kvs[batch_read_index-1].key), batch_kvs[batch_read_index-1].key_length);
}

std::string Iterator::Value()
{
    return std::string(reinterpret_cast<const char *>(batch_kvs[batch_read_index-1].value), batch_kvs[batch_read_index-1].value_length);
}

}
}
