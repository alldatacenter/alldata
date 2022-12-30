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
#include <map>
#include <list>
#include <set>
#include <string>
#include <thread>
#include "tubemq/tubemq_atomic.h"
#include "tubemq/tubemq_client.h"
#include "tubemq/tubemq_config.h"
#include "tubemq/tubemq_errcode.h"
#include "tubemq/tubemq_message.h"
#include "tubemq/tubemq_return.h"
#include "tubemq/tubemq_tdmsg.h"




using namespace std;
using namespace tubemq;

TubeMQConsumer consumer_1;



AtomicLong last_print_time(0);
AtomicLong last_msg_count(0);
AtomicLong last_print_count(0);



bool parse_TDMsg1_type_msg(const list<Message>& messageSet) {
  string err_info;
  list<Message>::const_iterator it_list;
  map<string, string>::const_iterator it_attr;
  list<DataItem>::const_iterator it_item;
  map<string, list<DataItem> >::const_iterator it_map;
  for (it_list = messageSet.begin(); it_list != messageSet.end(); ++it_list) {
    printf("\nMessage id is %ld, topic is %s",
      it_list->GetMessageId(), it_list->GetTopic().c_str());
    TubeMQTDMsg tubemq_tdmsg;
    if (tubemq_tdmsg.ParseTDMsg(it_list->GetData(), it_list->GetDataLength(), err_info)) {
      printf("\n parse data success, version is %d, createTime is %ld",
        tubemq_tdmsg.GetVersion(), tubemq_tdmsg.GetCreateTime());
      map<string, list<DataItem> > data_map = tubemq_tdmsg.GetAttr2DataMap();
      for (it_map = data_map.begin(); it_map != data_map.end(); ++it_map) {
        map<string, string> key_vals;
        if (!tubemq_tdmsg.ParseAttrValue(it_map->first, key_vals, err_info)) {
          printf("\n parse attribute error, attr is %s, reason is %s",
            (it_map->first).c_str(), err_info.c_str());
          continue;
        }
        printf("\n parsed attribute:");
        for (it_attr = key_vals.begin(); it_attr != key_vals.end(); ++it_attr) {
          printf("\nkey is %s, value is %s",
            (it_attr->first).c_str(), (it_attr->second).c_str());
        }
        list<DataItem> data_items = it_map->second;
        printf("\n parsed msg count is %ld", data_items.size());
        for (it_item = data_items.begin(); it_item != data_items.end(); ++it_item) {
          printf("\n parsed msg data' length is %d, value is: %s",
            it_item->GetLength(), it_item->GetData());
        }
      }
    } else {
      printf("\n \n parse data error, reason is %s\n", err_info.c_str());
      break;
    }
  }
  return true;
}


bool parse_raw_type_msg(const list<Message>& messageSet) {
  int64_t last_time = last_print_time.Get();
  int64_t cur_count = last_msg_count.AddAndGet(messageSet.size());
  int64_t cur_time = time(NULL);
  if (cur_count - last_print_count.Get() >= 50000
    || cur_time - last_time > 90) {
    if (last_print_time.CompareAndSet(last_time, cur_time)) {
      printf("\n %ld Current message count=%ld", cur_time, last_msg_count.Get());
      last_print_count.Set(cur_count);
    }
  }
  return true;
}

void thread_task_pull(int32_t thread_no) {
  bool result;
  ConsumerResult gentRet;
  ConsumerResult confirm_result;
  printf("\n thread_task_pull start: %d", thread_no);
  do {
    // 1. get Message;
    result = consumer_1.GetMessage(gentRet);
    if (result) {
      // 2.1.1  if success, process message
      list<Message> msgs = gentRet.GetMessageList();
      // 2.1.2 confirm message result
      consumer_1.Confirm(gentRet.GetConfirmContext(), true, confirm_result);
      parse_TDMsg1_type_msg(msgs);
      // parse_raw_type_msg(msgs);
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
  ConsumerConfig consumer_config;
  TubeMQServiceConfig serviceConfig;
  consumer_config.SetRpcReadTimeoutMs(20000);
  result = consumer_config.SetMasterAddrInfo(err_info, master_addr);
  if (!result) {
    printf("\n Set Master AddrInfo failure: %s ", err_info.c_str());
    return -1;
  }

  // non-filter consume begin
  set<string> topic_list;
  topic_list.insert(topic_name);
  result = consumer_config.SetGroupConsumeTarget(err_info, group_name, topic_list);
  if (!result) {
    printf("\n Set GroupConsume Target failure: %s", err_info.c_str());
    return -1;
  }
  //  non-filter consume end


/*
  // filter consume begin
  set<string> filters;
  filters.insert("aaa");
  filters.insert("bbb");
  filters.insert("xxb");
  map<string, set<string> > subscribed_topic_and_filter_map;
  subscribed_topic_and_filter_map["test_1"] = filters; 
  result = consumer_config.SetGroupConsumeTarget(err_info,
    group_name, subscribed_topic_and_filter_map);
  if (!result) {
    printf("\n Set GroupConsume Target failure: %s", err_info.c_str());
    return -1;
  }
  // filter consume end

  // bound consume begin
  set<string> filters;
  filters.insert("aaa");
  filters.insert("bbb");
  filters.insert("xxb");
  map<string, set<string> > subscribed_topic_and_filter_map;
  subscribed_topic_and_filter_map[topic_name] = filters;
  string session_key = "test";
  uint32_t source_count = 2;
  bool is_select_big = true;
  map<string, int64_t> part_offset_map;
  part_offset_map["181895251:test_1:0"] = 0;
  part_offset_map["181895251:test_1:10"] = 0;
  part_offset_map["181895251:test_1:20"] = 0;
  part_offset_map["181895251:test_1:30"] = 0;
  result =  consumer_config.SetGroupConsumeTarget(err_info, group_name,
    subscribed_topic_and_filter_map, session_key,
    source_count, is_select_big, part_offset_map);
  if (!result) {
    printf("\n Set GroupConsume Target failure: %s", err_info.c_str());
    return -1;
  }
// bound consume end
*/

  result = StartTubeMQService(err_info, serviceConfig);
  if (!result) {
    printf("\n StartTubeMQService failure: %s", err_info.c_str());
    return -1;
  }

  result = consumer_1.Start(err_info, consumer_config);
  if (!result) {
    consumer_1.ShutDown();
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

