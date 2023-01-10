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
#include <string>
#include <optional>
#include <vector>
#include <unordered_map>

namespace DB
{

namespace Catalog
{

#define MAX_BATCH_SIZE 1024
#define DEFAULT_SCAN_BATCH_COUNT 10000

enum CatalogCode : int
{
    OK = 0,
    KEY_NOT_FOUND = -10002,
    CAS_FAILED = -10003,
};

struct SinglePutRequest
{
    SinglePutRequest(const std::string & key_, const std::string & value_, bool if_not_exists_ = false)
        : key(key_), value(value_), if_not_exists(if_not_exists_)
    {
    }
    SinglePutRequest(const std::string & key_, const std::string & value_, const std::string & expected_)
        : key(key_), value(value_)
    {
        if (!expected_.empty())
            expected_value = expected_;
    }
    std::string key;
    std::string value;
    bool if_not_exists = false;
    std::optional<std::string> expected_value;
    /// custom callback when conflict happens. Pass the error code and error message as parameter
    std::function<void(int, const std::string &)> callback;
};


struct BatchCommitRequest
{
    BatchCommitRequest(const bool with_cas_ = true, const bool allow_cas_fail_ = false)
        : with_cas(with_cas_), allow_cas_fail(allow_cas_fail_)
    {
    }
    void AddPut(const SinglePutRequest & put) { puts.emplace_back(put); }
    void AddDelete(const std::string & del) { deletes.emplace_back(del); }
    void SetTimeout(uint32_t time_out) { commit_timeout_ms = time_out; }
    bool isEmpty() { return puts.empty() && deletes.empty(); }

    std::vector<SinglePutRequest> puts;
    std::vector<std::string> deletes;
    bool with_cas = true;
    bool allow_cas_fail = true;
    uint32_t commit_timeout_ms = 0;
};

/// Response for batch commit request.It contains the conflict info for both put requests and delete requests.
/// The map represents the conflicting requests index in BatchCommit and its corresponding current value in metastore.
struct BatchCommitResponse
{
    std::unordered_map<int, std::string> puts;
    std::unordered_map<int, std::string> deletes;
};

}

}
