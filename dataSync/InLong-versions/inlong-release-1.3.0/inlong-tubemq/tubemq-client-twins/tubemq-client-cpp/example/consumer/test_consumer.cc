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

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include <fcntl.h>
#include <errno.h>
#include <libgen.h>
#include <sys/time.h>
#include <chrono>
#include <set>
#include <string>
#include <thread>
#include "tubemq/tubemq_client.h"
#include "tubemq/tubemq_config.h"
#include "tubemq/tubemq_errcode.h"
#include "tubemq/tubemq_message.h"
#include "tubemq/tubemq_return.h"


using namespace std;
using namespace tubemq;

using std::set;
using std::string;
using tubemq::ConsumerConfig;
using tubemq::ConsumerResult;
using tubemq::TubeMQConsumer;






int main(int argc, char* argv[]) {
  bool result;
  string err_info;
  if (argc < 4) {
    printf("\n must ./comd master_addr group_name topic_name [config_file_path]");
    return -1;
  }
  // set parameters
  string master_addr = argv[1];
  string group_name = argv[2];
  string topic_name = argv[3];
  string conf_file = "../conf/client.conf";
  if (argc > 4) {
    conf_file = argv[4];
  }

  TubeMQConsumer consumer_1;
  set<string> topic_list;
  topic_list.insert(topic_name);
  ConsumerConfig consumer_config;
  TubeMQServiceConfig serviceConfig;

  consumer_config.SetRpcReadTimeoutMs(20000);
  result = consumer_config.SetMasterAddrInfo(err_info, master_addr);
  if (!result) {
    printf("\n Set Master AddrInfo failure: %s", err_info.c_str());
    return -1;
  }
  result = consumer_config.SetGroupConsumeTarget(err_info, group_name, topic_list);
  if (!result) {
    printf("\n Set GroupConsume Target failure: %s", err_info.c_str());
    return -1;
  }
  result = StartTubeMQService(err_info, serviceConfig);
  if (!result) {
    printf("\n StartTubeMQService failure: %s", err_info.c_str());
    return -1;
  }

  result = consumer_1.Start(err_info, consumer_config);
  if (!result) {
    printf("\n Initial consumer failure, error is: %s ", err_info.c_str());
    return -2;
  }

  ConsumerResult gentRet;
  ConsumerResult confirm_result;
  int64_t start_time = time(NULL);
  do {
    // 1. get Message;
    result = consumer_1.GetMessage(gentRet);
    if (result) {
      // 2.1.1  if success, process message
      list<Message> msgs = gentRet.GetMessageList();
      printf("\n GetMessage success, msssage count =%ld ", msgs.size());
      // 2.1.2 confirm message result
      consumer_1.Confirm(gentRet.GetConfirmContext(), true, confirm_result);
    } else {
      // 2.2.1 if failure, check error code
      // print error message if errcode not in
      // [no partitions assigned, all partitions in use,
      //    or all partitons idle, reach max position]
      if (!(gentRet.GetErrCode() == err_code::kErrNotFound
        || gentRet.GetErrCode() == err_code::kErrNoPartAssigned
        || gentRet.GetErrCode() == err_code::kErrAllPartInUse
        || gentRet.GetErrCode() == err_code::kErrAllPartWaiting)) {
        printf("\n GetMessage failure, err_code=%d, err_msg is: %s ",
          gentRet.GetErrCode(), gentRet.GetErrMessage().c_str());
      }
    }
    // used for test, consume 10 minutes only
    if (time(NULL) - start_time > 10 * 60) {
      break;
    }
  } while (true);

  getchar();  // for test hold the test thread
  consumer_1.ShutDown();

  getchar();  // for test hold the test thread
  result = StopTubeMQService(err_info);
  if (!result) {
    printf("\n *** StopTubeMQService failure, reason is %s ", err_info.c_str());
  }

  printf("\n finishe test exist ");
  return 0;
}




