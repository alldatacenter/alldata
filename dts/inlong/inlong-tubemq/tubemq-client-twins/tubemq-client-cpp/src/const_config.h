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

#ifndef TUBEMQ_CLIENT_CONST_CONFIG_H_
#define TUBEMQ_CLIENT_CONST_CONFIG_H_

#include <stdint.h>

#include <algorithm>
#include <map>
#include <string>

namespace tubemq {

using std::string;

#define TUBEMQ_MAX(a, b) std::max((a), (b))
#define TUBEMQ_MIN(a, b) std::min((a), (b))
#define TUBEMQ_MID(data, max, min) TUBEMQ_MAX(min, TUBEMQ_MIN((max), (data)))

// configuration value setting
namespace tb_config {
// log setting default define
static const int32_t kLogNumDef = 10;
static const int32_t kLogSizeDefMB = 100;
static const int32_t kLogLevelDef = 4;
static const char kLogPathDef[] = "../log/tubemq";

// dns tranlate period in ms
static const int32_t kDnsXfsPeriodInMsDef = 30000;

// frame threads define
static const int32_t kTimerThreadNumDef = 2;
static const int32_t kNetworkThreadNumDef = 4;
static const int32_t kSignalThreadNumDef = 8;

// rpc timeout define
static const int32_t kRpcTimoutDefMs = 15000;
static const int32_t kRpcTimoutMaxMs = 300000;
static const int32_t kRpcTimoutMinMs = 8000;

// heartbeat period define
static const int32_t kHeartBeatPeriodDefMs = 10000;
static const int32_t kHeartBeatFailRetryTimesDef = 5;
static const int32_t kHeartBeatSleepPeriodDefMs = 60000;
// default max master hb retry count
static const int32_t kMaxMasterHBRetryCount = 8;
// max masterAddrInfo length
static const int32_t kMasterAddrInfoMaxLength = 1024;

// max TopicName length
static const int32_t kTopicNameMaxLength = 64;
// max Consume GroupName length
static const int32_t kGroupNameMaxLength = 1024;
// max filter item length
static const int32_t kFilterItemMaxLength = 256;
// max allowed filter item count
static const int32_t kFilterItemMaxCount = 500;
// max session key length
static const int32_t kSessionKeyMaxLength = 1024;

// max subscribe info report times
static const int32_t kSubInfoReportMaxIntervalTimes = 6;
// default message not found response wait period
static const int32_t kMsgNotfoundWaitPeriodMsDef = 400;
// default confirm wait period if rebalance meeting
static const int32_t kRebConfirmWaitPeriodMsDef = 3000;
// max confirm wait period anyway
static const int32_t kConfirmWaitPeriodMsMax = 60000;
// default rebalance wait if shutdown meeting
static const int32_t kRebWaitPeriodWhenShutdownMs = 10000;
// default partition status check period
static const int32_t kMaxPartCheckPeriodMsDef = 60 * 1000;
// default partition status check slice
static const int32_t kPartCheckSliceMsDef = 300;

// max int value
static const int32_t kMaxIntValue = 0x7fffffff;
// max long value
static const int64_t kMaxLongValue = 0x7fffffffffffffffL;

// default broker port
static const uint32_t kBrokerPortDef = 8123;
// default broker TLS port
static const uint32_t kBrokerTlsPortDef = 8124;

// invalid value
static const int32_t kInvalidValue = -2;

static const uint32_t kMetaStoreInsBase = 10000;

// message flag's properties settings
static const int32_t kMsgFlagIncProperties = 0x01;

// reserved property key Filter Item
static const char kRsvPropKeyFilterItem[] = "$msgType$";
// reserved property key message send time
static const char kRsvPropKeyMsgTime[] = "$msgTime$";

// enum RegisterMasterStatus
enum RegisterMasterStatus {
  kMasterUnRegistered = 0,
  kMasterRegistering = 1,
  kMasterRegistered = 2
};

// enum MasterHBStatus
enum MasterHBStatus {
  kMasterHBWaiting = 0,
  kMasterHBRunning = 1
};

}  // namespace tb_config

namespace delimiter {
static const char kDelimiterDot[] = ".";
static const char kDelimiterEqual[] = "=";
static const char kDelimiterAnd[] = "&";
static const char kDelimiterComma[] = ",";
static const char kDelimiterColon[] = ":";
static const char kDelimiterAt[] = "@";
static const char kDelimiterPound[] = "#";
static const char kDelimiterSemicolon[] = ";";
// Double slash
static const char kDelimiterDbSlash[] = "//";
// left square bracket
static const char kDelimiterLftSB[] = "[";
// right square bracket
static const char kDelimiterRgtSB[] = "]";

}  // namespace delimiter

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_CONST_CONFIG_H_
