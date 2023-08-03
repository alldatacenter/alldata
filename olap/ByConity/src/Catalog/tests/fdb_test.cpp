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

#include <Catalog/MetastoreFDBImpl.h>
#include <iostream>
#include <sstream>
#include <iomanip>
#include <string>
#include <thread>
#include <atomic>

using String = std::string;
using Test = std::function<bool()>;

bool testPutAndGet(DB::Catalog::MetastoreFDBImpl & metastore)
{
    metastore.put("key1", "value1");
    String value;
    metastore.get("key1", value);
    if (value == "value1")
        return true;
    else
        return false;
}

// bool testMultiPut(DB::Catalog::MetastoreFDBImpl & metastore)
// {
//     auto multiwrite = metastore.createMultiWrite();
//     int batch_size = 1000;
//     String base_key_prefix = "batch_write_key_";
//     String base_value_prefix = "batch_write_value_";
//     for (int i=1; i<=batch_size; i++)
//         multiwrite->addPut(base_key_prefix+std::to_string(i), base_value_prefix+std::to_string(i));
//     multiwrite->commit();

//     DB::Catalog::MetastoreFDBImpl::IteratorPtr it = metastore.getByPrefix(base_key_prefix);
//     int counter = 0;
//     while (it->next())
//         counter++;

//     if (counter == batch_size)
//         return true;
//     std::cout << "counter is : " << std::to_string(counter) << std::endl;
//     return false;
// }

bool testMultiGet(DB::Catalog::MetastoreFDBImpl & metastore)
{
    /// get data which has been inserted in previous test.
    int total_size = 1000;
    std::vector<String> keys;
    String base_key_prefix = "batch_write_key_";
    String base_value_prefix = "batch_write_value_";
    for (int i=1; i<=total_size; i++)
        keys.emplace_back(base_key_prefix+std::to_string(i));
    auto res = metastore.multiGet(keys);
    bool all_values_expected = 1;
    int count = 0;
    for (size_t i=0; i<keys.size(); i++)
    {
        count++;
        all_values_expected = std::get<0>(res[i]) == base_value_prefix + std::to_string(i+1);
        if (!all_values_expected)
        {
            std::cout << "For key :" << keys[i] << " expected : " << base_value_prefix + std::to_string(i+1) << ", but get : " << std::get<0>(res[i]) << std::endl;
            break;
        }
    }

    if (count==total_size && all_values_expected)
        return true;
    else
        return false;
}

bool testDropRange(DB::Catalog::MetastoreFDBImpl & metastore)
{
    String prefix = "batch_write_key_";
    metastore.clean(prefix);
    DB::Catalog::MetastoreFDBImpl::IteratorPtr it = metastore.getByPrefix(prefix);
    int counter = 0;
    while (it->next())
        counter++;
    if (counter == 0)
        return true;
    else
        return false;
}

bool testPutCAS(DB::Catalog::MetastoreFDBImpl & metastore)
{
    std::atomic_bool test_success {true};
    String put_key = "put_cas_key";
    String origin_value = "put_cas_origin";
    metastore.put(put_key, origin_value);
    auto func = [&](String value_to_write)
    {
        auto res = metastore.putCAS(put_key, value_to_write, "put_cas_origin", true);
        if (res.first)
            std::cout << "write succeses , current value is : " << res.second << std::endl;
        else
        {
            test_success = false;
            std::cout << "write falied , current value is : " << res.second << std::endl;
        }
    };

    std::thread t1([&](){func("put_cas_value1");});
    std::thread t2([&](){func("put_cas_value2");});
    t1.join();
    t2.join();

    return test_success;
}

bool testBatchWriteWithoutConflict(DB::Catalog::MetastoreFDBImpl & metastore)
{
    int total_size = 10;
    std::vector<String> keys;
    std::vector<std::pair<uint32_t , String>> cas_failed;
    String base_key_prefix = "multi_write_cas_key_";
    metastore.clean(base_key_prefix);

    for (int i=1; i<=total_size; i++)
        keys.emplace_back(base_key_prefix+std::to_string(i));

    // metastore.multiPutCAS(keys, "multi_write_value", {}, true, cas_failed);

    if (!cas_failed.empty())
    {
        for (auto & pair : cas_failed)
            std::cout << keys[pair.first] << " alreay has velue " << pair.second << std::endl;
        return false;
    }
    return true;
}

bool testBatchWriteWithConflict(DB::Catalog::MetastoreFDBImpl & metastore)
{
    String base_key_prefix = "multi_write_cas_key_";
    // metastore.clean(base_key_prefix);
    auto func = [&](std::vector<String> keys, std::vector<String> expected_values, String value)
    {
        std::vector<std::pair<uint32_t , String>> cas_failed;
        // metastore.multiPutCAS(keys, value, expected_values, false, cas_failed);
        if (!cas_failed.empty())
        {
            std::cout << "MultiPut falied." << std::endl;
            for (auto & pair : cas_failed)
                std::cout << keys[pair.first] << " alreay has velue " << pair.second << std::endl;
            return false;
        }
        else
        {
            std::cout << "MultiPut success." << std::endl;
        }

        return true;
    };

    std::atomic_bool test_success = true;
    std::vector<String> keys, keys1, keys2, expected_values;

    for (int i=1; i<=7; i++)
        keys1.emplace_back(base_key_prefix + std::to_string(i));

    for (int i=4; i<=10; i++)
        keys2.emplace_back(base_key_prefix + std::to_string(i));

    for (int i=1; i<=7; i++)
        expected_values.emplace_back("multi_write_value");

    std::thread f1([&](){test_success = func(keys1, expected_values, "multi_write_value_new1");});
    std::thread f2([&](){test_success = func(keys2, expected_values, "multi_write_value_new2");});
    f1.join();
    f2.join();

    return test_success;
}

void doTest(const String & test_name, const Test & test)
{
    if (test())
        std::cout << "Test " << test_name << " passed." << std::endl;
    else
        std::cout << "Test " << test_name << " failed." << std::endl;
}

int main(int , char ** argv)
{
    String cluster_path = argv[1];
    DB::Catalog::MetastoreFDBImpl metastore(cluster_path);

    {
        doTest("Put & Get", [&]()->bool{return testPutAndGet(metastore);});
        // doTest("MultiPut", [&]()->bool{return testMultiPut(metastore);});
        doTest("MultiGet", [&]()->bool{return testMultiGet(metastore);});
        doTest("DropRange", [&]()->bool{return testDropRange(metastore);});
    }
    doTest("PutCAS", [&]()->bool{return testPutCAS(metastore);});
    std::cout << " preparing data for test" << std::endl;
    std::cout << "===== Finish all tests. ======" << std::endl;
}
