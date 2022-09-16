/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
#define private public

#include "atomic.h"
#include "logger.h"
#include "tc_api.h"
#include "utils.h"
#include <algorithm>
#include <gtest/gtest.h>
#include <iostream>
#include <map>
#include <stdint.h>
#include <string>
#include <thread>
using namespace std;
using namespace dataproxy_sdk;

AtomicInt init_flag;

TEST(sort, test)
{
    map<string, int> result;
    result["wegre"]   = 3;
    result["aaaaa"]   = 1;
    result["bbbbbbb"] = 0;
    result["test"]    = 1;
    result["ggggg"]   = 9;
    result["ddddd"]   = -1;
    vector<PAIR> vlist(result.begin(), result.end());
    sort(vlist.begin(), vlist.end(), &Utils::upValueSort);
    cout << "up sort" << endl;
    for (size_t i = 0; i < vlist.size(); i++)
    {
        cout << vlist[i].first << ":" << vlist[i].second << endl;
    }

    cout << "down sort" << endl;
    sort(vlist.begin(), vlist.end(), &Utils::downValueSort);
    for (size_t i = 0; i < vlist.size(); i++)
    {
        cout << vlist[i].first << ":" << vlist[i].second << endl;
    }
}

TEST(timetest, test)
{
    uint64_t t1 = Utils::getCurrentMsTime();
    Utils::taskWaitTime(1);
    uint64_t t2 = Utils::getCurrentMsTime();
    ASSERT_EQ(t1 + 1000, t2);
}

TEST(bindCPU, test) { ASSERT_EQ(Utils::bindCPU(10), true); }

TEST(getIpAddress, test)
{
    string ip;
    ASSERT_EQ(Utils::getFirstIpAddr(ip), true);
    // ASSERT_NE(ip, "11.45.17.175");
    ASSERT_NE(ip, "127.0.0.1");
}

TEST(getFormatCurTime, test)
{
    string curtime = Utils::getFormatTime(Utils::getCurrentMsTime());
    cout << curtime << endl;
}

TEST(waitNextMills, test)
{
    uint64_t t1 = Utils::getCurrentMsTime();
    Utils::waitNextMills(t1);
    uint64_t t2 = Utils::getCurrentMsTime();
    ASSERT_EQ(t1 + 1, t2);
}

TEST(isLegalTime, test)
{
    ASSERT_NE(Utils::isLegalTime(0), true);
    ASSERT_EQ(Utils::isLegalTime(2435101567000), true);
}

TEST(snowflake, test)
{
    string id1 = Utils::getSnowflakeId();
    Utils::taskWaitTime(3);
    string id2 = Utils::getSnowflakeId();
    ASSERT_NE(id1, id2);
}

TEST(requestUrl, test)
{
    string str;
    string url = "http://127.0.0.1:8099/api/inlong/manager/openapi/dataproxy/getIpList?inlong_group_id=test_group_id";
    HttpRequest request = {url, 1000, false, "", "", str};
    int32_t res = Utils::requestUrl(url, &request);
    cout << str << endl;
    ASSERT_EQ(res, 0);
}

TEST(zip, test)
{
    string input = "Running transaction check---> Package glibc-debuginfo.i686 0:2.12-1.132.el6 will be installed--> Processing"
                   "Dependency: glibc-debuginfo-common = 2.12-1.132.el6 for package: glibc-debuginfo-2.12-1.132.el6.i686---> Package "
                   "nss-softokn-debuginfo.i686 0:3.12.9-11.el6 will be installe---> Package yum-plugin-auto-update-debug-info.noarch "
                   "0:1.1.30-17.el6_5 will be installe--> Running transaction chec---> Package glibc-debuginfo-common.i686 "
                   "0:2.12-1.132.el6buginfo-2.12-1.132.el6.i686---> Packagebuginfo-2.12-1.132.el6.i686---> "
                   "Packagebuginfo-2.12-1.132.el6.i686---> Package "
                   "will be installed-- > Finished Dependency Resolution ";
    // string input = "827206790347609547869045780928509482-2058409869054869052";

    string res;
    Utils::zipData(input.c_str(), input.size(), res);
    cout << "zip len: " << input.size() << ", after zip: " << res.size() << endl;
    cout << "zip res: " << res << endl;
    ASSERT_LE(res.size(), input.size());
}

TEST(zip, test2)
{
    string input(1024, 'a');
    input += "end\n";

    string output;

    Utils::zipData(input.data(), input.size(), output);

    cout << "zip,len:" << output.size() << endl;
    cout << "output:" << output << endl;

    string res;
    snappy::Uncompress(output.data(), output.size(), &res);
    cout << "uncompress,len:" << res.size() << endl;
    cout << "uncompress content:" << res << endl;

    EXPECT_EQ(res, input);
}

TEST(atomic, test)
{
    int res = 0;
    if (!init_flag.compareAndSwap(0, 1)) { res = SDKInvalidResult::kMultiInit; }
    ASSERT_EQ(res, 0);
    if (!init_flag.compareAndSwap(0, 1)) { res = SDKInvalidResult::kMultiInit; }
    ASSERT_EQ(res, 4);
}

TEST(atomic, test2)
{
    int res = 0;
    if (!init_flag.compareAndSwap(0, 1)) { res = SDKInvalidResult::kMultiInit; }
    ASSERT_EQ(res, 4);
}

TEST(readfile, test1)
{
    string file_path  = "config.json";
    string wrong_path = "ddddd.txt";
    string res;
    EXPECT_EQ(Utils::readFile(file_path, res), true);
    cout << "res: " << res << endl;
    EXPECT_EQ(Utils::readFile(wrong_path, res), false);
}

TEST(splitStr, test)
{
    string a = "ewigjreiher,oeirhgoer,oeaihrn__gee,jj";
    string b = "big,bibbb,";
    string c = " ";
    string d = "b_bidlone";
    string e = " , ";
    string f = "big,bibbb, ";
    vector<string> av, bv, cv, dv, ev, fv;
    EXPECT_EQ(Utils::splitOperate(a, av, ","), 4);
    EXPECT_EQ(Utils::splitOperate(b, bv, ","), 2);
    EXPECT_EQ(Utils::splitOperate(c, cv, ","), 0);
    EXPECT_EQ(Utils::splitOperate(d, dv, ","), 1);
    EXPECT_EQ(Utils::splitOperate(e, ev, ","), 0);
    EXPECT_EQ(Utils::splitOperate(f, fv, ","), 2);
    cout << Utils::getVectorStr(av) << endl;
}

int main(int argc, char* argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}