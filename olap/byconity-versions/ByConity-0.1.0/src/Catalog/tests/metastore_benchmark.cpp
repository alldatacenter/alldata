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

#pragma clang diagnostic ignored "-Wzero-as-null-pointer-constant"

#include <benchmark/benchmark.h>
#include <Catalog/MetastoreFDBImpl.h>
#include <Catalog/MetastoreByteKVImpl.h>
#include <Poco/Util/XMLConfiguration.h>
#include <boost/program_options.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <random>
#include <algorithm>
#include <Common/Stopwatch.h>

/// some helper functions
[[maybe_unused]] std::string random_string(size_t length)
{
    auto randchar = []() -> char
    {
        const char charset[] =
        "0123456789"
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        "abcdefghijklmnopqrstuvwxyz";
        const size_t max_index = (sizeof(charset) - 1);
        return charset[ rand() % max_index ];
    };
    std::string str(length,0);
    std::generate_n( str.begin(), length, randchar );
    return str;
}

/// main tests
static std::shared_ptr<DB::Catalog::IMetaStore> metastore_ptr = nullptr;

[[maybe_unused]] static void BM_metastore_put(benchmark::State& state)
{
    std::string common_key_prefix = "metastore_performance_put_test_";
    std::string common_value_prefix = random_string(120);
    size_t index = 1;
    for (auto _ : state)
    {
        metastore_ptr->put(common_key_prefix + std::to_string(index), common_value_prefix + "_" + std::to_string(index));
        index++;
    }

    metastore_ptr->clean(common_key_prefix);
};

[[maybe_unused]] static void BM_metastore_get(benchmark::State& state)
{
    std::string prepared_key = "metastore_performance_get_test";
    std::string prepared_value = random_string(120);
    metastore_ptr->put(prepared_key, prepared_value);

    std::string value;
    for (auto _ : state)
    {
        metastore_ptr->get(prepared_key, value);
        if (value != prepared_value)
        {
            state.SkipWithError("Get error!");
            break;
        }
    }

    metastore_ptr->drop(prepared_key);
}

[[maybe_unused]] static void BM_metastore_multiput(benchmark::State& state)
{
    size_t batch_size = state.range(0);
    std::string common_key_prefix = "metastore_performance_multiput_test_";
    std::string common_value_prefix = random_string(120); //mock part metadata size
    std::vector<std::string> keys;
    keys.reserve(batch_size);
    //Setup, generate BatchCommitRequest with 100 records to put.
    DB::Catalog::BatchCommitRequest put_request;
    for (size_t i=0; i<batch_size; i++)
    {
        keys.emplace_back(common_key_prefix + std::to_string(i));
        put_request.AddPut(DB::Catalog::SinglePutRequest(keys.back(), common_value_prefix + "_" + std::to_string(i)));
    }

    // test loop
    for (auto _ : state)
    {
        DB::Catalog::BatchCommitResponse put_response;
        bool success = metastore_ptr->batchWrite(put_request, put_response);
        if (!success)
        {
            state.SkipWithError("Fail to commit!");
            break;
        }
    }

    //Teardown, clear all written records;
    DB::Catalog::BatchCommitRequest delete_request;
    DB::Catalog::BatchCommitResponse delete_response;
    for (auto & delete_key : keys)
    {
        delete_request.AddDelete(delete_key);
    }
    metastore_ptr->batchWrite(delete_request, delete_response);
}

[[maybe_unused]] static void BM_metastore_multiget(benchmark::State& state)
{
    size_t batch_size = state.range(0);
    std::string common_key_prefix = "metastore_performance_multiput_test_";
    std::string common_value_prefix = random_string(120); //mock part metadata size
    std::vector<std::string> keys;
    /// prepare data to test.
    keys.reserve(batch_size);
    //Setup, generate BatchCommitRequest with 100 records to put.
    DB::Catalog::BatchCommitRequest put_request;
    DB::Catalog::BatchCommitResponse put_response;
    for (size_t i=0; i<batch_size; i++)
    {
        keys.emplace_back(common_key_prefix + std::to_string(i));
        put_request.AddPut(DB::Catalog::SinglePutRequest(keys.back(), common_value_prefix + "_" + std::to_string(i)));
    }
    metastore_ptr->batchWrite(put_request, put_response);

    // test loop
    for (auto _ : state)
    {
        auto res = metastore_ptr->multiGet(keys);
        if (res.size() != batch_size)
        {
            std::string msg = "Fail to get all data! Expect " + std::to_string(batch_size) + ", but get " + std::to_string(res.size());
            state.SkipWithError(msg.c_str());
            break;
        }
    }

    /// clear all test data from meatastore
    metastore_ptr->clean(common_key_prefix);
}

#define SCAN_TEST_KEY_PREFIX "metastore_performance_scan_test_"
[[maybe_unused]] static void ScanSetup(const benchmark::State& state)
{
    /// prepare records to test scan
    size_t scan_size = state.range(0);
    size_t max_commit_size_one_batch = 2000;
    size_t index = 0;
    std::string common_value_prefix = random_string(120); //mock part metadata size
    while (index < scan_size)
    {
        DB::Catalog::BatchCommitRequest request;
        for (size_t i=0; i<max_commit_size_one_batch; i++)
        {
            request.AddPut(DB::Catalog::SinglePutRequest(SCAN_TEST_KEY_PREFIX + std::to_string(index), common_value_prefix + "_" + std::to_string(index)));
            if (++index >= scan_size)
                break;
        }
        DB::Catalog::BatchCommitResponse response;
        metastore_ptr->batchWrite(request, response);
    }
}

[[maybe_unused]] static void ScanTearDown(const benchmark::State& )
{
    metastore_ptr->clean(SCAN_TEST_KEY_PREFIX);
}

[[maybe_unused]] static void BM_metastore_scan(benchmark::State& state)
{
    size_t scan_size = state.range(0);
    for (auto _ : state)
    {
        size_t count = 0;
        auto it = metastore_ptr->getByPrefix(SCAN_TEST_KEY_PREFIX);
        while(it->next())
            count++;
        if (count != scan_size)
        {
            std::string msg = "Fail to scan all data! Expect " + std::to_string(scan_size) + ", but get " + std::to_string(count);
            state.SkipWithError(msg.c_str());
            break;
        }
    }
}


int main(int argc, char** argv) {
    boost::program_options::options_description desc("Options");
    desc.add_options()
        ("help,h", "help list")
        ("type,t", boost::program_options::value<std::string>(), "metastore type")
        ("config,c", boost::program_options::value<std::string>(), "metastore config")
    ;

    boost::program_options::variables_map options;
    boost::program_options::store(boost::program_options::parse_command_line(argc, argv, desc), options);

    if (options.count("help"))
    {
        std::cout << "Usage: " << argv[0] << " [options]" << std::endl;
        std::cout << desc << std::endl;
        return 1;
    }

    std::string type = options["type"].as<std::string>();
    std::string config_file = options["config"].as<std::string>();

    if (type == "fdb")
    {
        metastore_ptr = std::make_shared<DB::Catalog::MetastoreFDBImpl>(config_file);
    }
    else
    {
        std::cerr << "unsupported metastore type : " << type << std::endl;
        return -1;
    }

    BENCHMARK(BM_metastore_put)->Unit(benchmark::kMicrosecond);
    BENCHMARK(BM_metastore_get)->Unit(benchmark::kMicrosecond);
    BENCHMARK(BM_metastore_multiput)->Unit(benchmark::kMicrosecond)->Arg(100)->Arg(1000);
    BENCHMARK(BM_metastore_multiget)->Unit(benchmark::kMicrosecond)->Arg(100)->Arg(1000);
    BENCHMARK(BM_metastore_scan)->Unit(benchmark::kMicrosecond)->Arg(10000)->Arg(100000)->Setup(ScanSetup)->Teardown(ScanTearDown);
    ::benchmark::Initialize(&(argc), argv);
    // if (::benchmark::ReportUnrecognizedArguments(argc, argv))
    //     return 1;
    ::benchmark::RunSpecifiedBenchmarks();
}
