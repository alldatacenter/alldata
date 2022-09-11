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

#include <errno.h>
#include <fcntl.h>
#include <libgen.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <time.h>
#include <string>
#include <chrono>
#include <set>
#include <thread>
#include "tubemq/tubemq_atomic.h"
#include "tubemq/tubemq_client.h"
#include "tubemq/tubemq_config.h"
#include "tubemq/tubemq_errcode.h"
#include "tubemq/tubemq_message.h"
#include "tubemq/tubemq_return.h"


using namespace std;
using namespace tubemq;

TubeMQConsumer consumer_1;



AtomicLong last_print_time(0);
AtomicLong last_msg_count(0);
AtomicLong last_print_count(0);



void calc_message_count(int64_t msg_count) {
  int64_t last_time = last_print_time.Get();
  int64_t cur_count = last_msg_count.AddAndGet(msg_count);
  int64_t cur_time = time(NULL);
  if (cur_count - last_print_count.Get() >= 50000
    || cur_time - last_time > 90) {
    if (last_print_time.CompareAndSet(last_time, cur_time)) {
      printf("\n %ld Current message count=%ld", cur_time, last_msg_count.Get());
      last_print_count.Set(cur_count);
    }
  }
}

void thread_task_pull(int32_t thread_no) {
  bool result;
  int64_t msg_count = 0;
  ConsumerResult gentRet;
  ConsumerResult confirm_result;
  printf("\n thread_task_pull start: %d", thread_no);
  do {
    msg_count = 0;
    // 1. get Message;
    result = consumer_1.GetMessage(gentRet);
    if (result) {
      // 2.1.1  if success, process message
      list<Message> msgs = gentRet.GetMessageList();
      msg_count = msgs.size();
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
        if (gentRet.GetErrCode() == err_code::kErrMQServiceStop
          || gentRet.GetErrCode() == err_code::kErrClientStop) {
          break;
        }
        printf("\n GetMessage failure, err_code=%d, err_msg is: %s ",
          gentRet.GetErrCode(), gentRet.GetErrMessage().c_str());
      }
    }
    calc_message_count(msg_count);
  } while (true);
  printf("\n thread_task_pull finished: %d", thread_no);
}


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

  int32_t thread_num = 15;
  set<string> topic_list;
  topic_list.insert(topic_name);
  ConsumerConfig consumer_config;
  TubeMQServiceConfig serviceConfig;

  consumer_config.SetRpcReadTimeoutMs(20000);
  result = consumer_config.SetMasterAddrInfo(err_info, master_addr);
  if (!result) {
    printf("\n Set Master AddrInfo failure: %s ", err_info.c_str());
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

  std::thread pull_threads[thread_num];
  for (int32_t i = 0; i < thread_num; i++) {
    pull_threads[i] = std::thread(thread_task_pull, i);
  }

  getchar();  // for test hold the test thread
  consumer_1.ShutDown();
  //
  for (int32_t i = 0; i < thread_num; i++) {
    if (pull_threads[i].joinable()) {
      pull_threads[i].join();
    }
  }

  getchar();  // for test hold the test thread
  result = StopTubeMQService(err_info);
  if (!result) {
    printf("\n *** StopTubeMQService failure, reason is %s", err_info.c_str());
  }

  printf("\n finishe test exist");
  return 0;
}

