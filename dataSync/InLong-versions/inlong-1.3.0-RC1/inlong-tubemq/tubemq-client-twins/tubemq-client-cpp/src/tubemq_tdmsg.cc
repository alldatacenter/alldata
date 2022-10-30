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

#include "tubemq/tubemq_tdmsg.h"

#include <arpa/inet.h>
#include <snappy-c.h>
#include <stdlib.h>
#include <string.h>
#include <sstream>
#include "const_config.h"
#include "utils.h"


namespace tubemq {

using std::stringstream;


#define TUBEMQ_TDMSG_V4_MSG_FORMAT_SIZE 29
#define TUBEMQ_TDMSG_V4_MSG_COUNT_OFFSET 15
#define TUBEMQ_TDMSG_V4_MSG_EXTFIELD_OFFSET 9

static bool getDataChar(const char* data, int32_t& pos, uint32_t& remain, char& chrVal,
                        string& err_info);
static bool getDataMagic(const char* data, int32_t& pos, uint32_t& remain, int32_t& ver,
                         string& err_info);
static bool getDataCreateTime(const char* data, int32_t& pos, uint32_t& remain, int64_t& createTime,
                              string& err_info);
static bool getDatantohlInt(const char* data, int32_t& pos, uint32_t& remain, uint32_t& intVal,
                            string& err_info);
static bool getDatantohsInt(const char* data, int32_t& pos, uint32_t& remain, uint32_t& intVal,
                            string& err_info);

DataItem::DataItem() {
  length_ = 0;
  data_ = NULL;
}

DataItem::DataItem(const DataItem& target) {
  length_ = target.length_;
  copyData(target.data_, target.length_);
}

DataItem::DataItem(const uint32_t length, const char* data) {
  length_ = length;
  copyData(data, length);
}

DataItem::~DataItem() { clearData(); }

DataItem& DataItem::operator=(const DataItem& target) {
  if (this != &target) {
    length_ = target.length_;
    clearData();
    copyData(target.data_, target.length_);
  }
  return *this;
}

void DataItem::clearData() {
  if (data_ != NULL) {
    delete[] data_;
    data_ = NULL;
    length_ = 0;
  }
}

void DataItem::copyData(const char* data, uint32_t length) {
  if (data == NULL) {
    data_ = NULL;
    length_ = 0;
  } else {
    length_ = length;
    data_ = new char[length + 1];
    memset(data_, 0, length + 1);
    memcpy(data_, data, length);
  }
}

TubeMQTDMsg::TubeMQTDMsg() {
  is_parsed_ = false;
  is_numbid_ = false;
  version_ = -1;
  create_time_ = -1;
  msg_count_ = 0;
  attr_count_ = 0;
}

TubeMQTDMsg::~TubeMQTDMsg() {
  is_parsed_ = false;
  is_numbid_ = false;
  version_ = -1;
  create_time_ = -1;
  msg_count_ = 0;
  attr_count_ = 0;
  attr2data_map_.clear();
}

bool TubeMQTDMsg::ParseTDMsg(const char* data, uint32_t data_length, string& err_info) {
  int32_t pos1 = 0;
  uint32_t remain = 0;
  bool result = false;
  if (is_parsed_) {
    err_info = "TDMsg is parsed, if need re-parse, please clear first!";
    return result;
  }
  if ((data == NULL) || (data_length == 0)) {
    err_info = "Check parameter data is NULL or data_length is zero!";
    return result;
  }
  remain = data_length;
  if (!getDataMagic(data, pos1, remain, version_, err_info)) {
    return result;
  }
  switch (version_) {
    case 3: {
      if (!getDataCreateTime(data, pos1, remain, create_time_, err_info)) {
        return result;
      }
      if (!getDatantohlInt(data, pos1, remain, msg_count_, err_info)) {
        err_info += " for msgCount parameter";
        return result;
      }
      if (!getDatantohlInt(data, pos1, remain, attr_count_, err_info)) {
        err_info += " for attrCount parameter";
        return result;
      }
      result = parseMixAttrMsg(data, remain, pos1, err_info);
      break;
    }

    case 4: {
      uint32_t dataRemain = remain;
      if (dataRemain < TUBEMQ_TDMSG_V4_MSG_FORMAT_SIZE) {
        err_info = "Parse message error: no enough data length for v4 msg fixed data";
        return false;
      }
      int32_t msgCountPos = pos1 + TUBEMQ_TDMSG_V4_MSG_COUNT_OFFSET;
      if (!getDatantohsInt(data, msgCountPos, dataRemain, msg_count_, err_info)) {
        err_info += " for v4 msgCount parameter";
        return result;
      }
      uint32_t isNumBidVal = 0;
      int32_t isNumBidPos = pos1 + TUBEMQ_TDMSG_V4_MSG_EXTFIELD_OFFSET;
      if (!getDatantohsInt(data, isNumBidPos, dataRemain, isNumBidVal, err_info)) {
        err_info += " for v4 extendField parameter";
        return result;
      }
      isNumBidVal &= 0x4;
      if (isNumBidVal == 0) {
        is_numbid_ = false;
      } else {
        is_numbid_ = true;
      }
      result = parseBinMsg(data, remain, pos1, err_info);
      break;
    }

    default: {
      if (version_ >= 1) {
        if (!getDataCreateTime(data, pos1, remain, create_time_, err_info)) {
          return result;
        }
      }
      if (version_ >= 2) {
        if (!getDatantohlInt(data, pos1, remain, msg_count_, err_info)) {
          err_info += " for msgCount parameter";
          return result;
        }
      }
      if (!getDatantohlInt(data, pos1, remain, attr_count_, err_info)) {
        err_info += " for attrCount parameter";
        return result;
      }
      result = parseDefaultMsg(data, remain, pos1, err_info);
      break;
    }
  }
  return result;
}

bool TubeMQTDMsg::ParseTDMsg(const vector<char>& data_vec, string& err_info) {
  bool result = false;

  if (is_parsed_) {
    err_info = "TDMsg is parsed, if need re-parse, please clear first!";
    return result;
  }
  if (data_vec.empty()) {
      err_info = "Check parameter data is NULL or data_length is zero!";
      return result;
  }
  result = ParseTDMsg(data_vec.data(), data_vec.size(), err_info);
  return result;
}

bool TubeMQTDMsg::parseDefaultMsg(const char* data, uint32_t data_length, int32_t start_pos,
                                  string& err_info) {
  // #lizard forgives
  int32_t pos1 = start_pos;
  uint32_t remain = data_length;
  for (uint32_t i = 0; i < attr_count_; i++) {
    uint32_t origAttrLen = 0;
    char* origAttrData = NULL;
    string commAttr;
    uint32_t dataCnt = 1;
    uint32_t dataLen = 0;
    char compress = 0;
    if (remain <= 2) {
      if (i == 0) {
        err_info = "Parse message error: invalid databody length length";
        return false;
      } else {
        break;
      }
    }
    if (!getDatantohsInt(data, pos1, remain, origAttrLen, err_info)) {
      err_info += " for attr length parameter";
      return false;
    }
    if ((origAttrLen <= 0) || (origAttrLen > remain)) {
      err_info = "Parse message error: invalid attr length";
      return false;
    }
    origAttrData = static_cast<char*>(malloc(origAttrLen + 1));
    if (origAttrData == NULL) {
      err_info = "Parse message error: malloc buffer for default attr value failure!";
      return false;
    }
    memset(origAttrData, 0, origAttrLen + 1);
    memcpy(origAttrData, data + pos1, origAttrLen);
    pos1 += origAttrLen;
    remain -= origAttrLen;
    commAttr = origAttrData;
    free(origAttrData);
    origAttrData = NULL;
    if (version_ == 2) {
      if (!getDatantohlInt(data, pos1, remain, dataCnt, err_info)) {
        err_info += " for data count parameter";
        return false;
      }
    }
    if (!getDatantohlInt(data, pos1, remain, dataLen, err_info)) {
      err_info += " for data len parameter";
      return false;
    }
    if ((dataLen <= 0) || (dataLen > remain)) {
      err_info = "Parse message error: invalid data length";
      return false;
    }
    if (!getDataChar(data, pos1, remain, compress, err_info)) {
      return false;
    }
    size_t uncompressDataLen = 0;
    char* uncompressData = NULL;
    if (compress != 0) {
      if (snappy_uncompressed_length(data + pos1, dataLen - 1, &uncompressDataLen) != SNAPPY_OK) {
        err_info = "Parse message error:  snappy uncompressed default compress's length failure!";
        return false;
      }
      uncompressData = static_cast<char*>(malloc(uncompressDataLen));
      if (uncompressData == NULL) {
        err_info = "Parse message error: malloc buffer for default compress's data failure!";
        return false;
      }
      if (snappy_uncompress(data + pos1, dataLen - 1, uncompressData, &uncompressDataLen) !=
          SNAPPY_OK) {
        free(uncompressData);
        uncompressData = NULL;
        err_info = "Parse message error:  snappy uncompressed default compress's data failure!";
        return false;
      }
    } else {
      uncompressDataLen = dataLen - 1;
      uncompressData = static_cast<char*>(malloc(uncompressDataLen));
      if (uncompressData == NULL) {
        err_info = "Parse message error: malloc buffer for default's data failure!";
        return false;
      }
      memcpy(uncompressData, data + pos1, dataLen - 1);
    }
    pos1 += dataLen - 1;
    remain -= dataLen - 1;
    int32_t itemPos = 0;
    // unsigned int totalItemDataLen = 0;
    uint32_t itemRemain = uncompressDataLen;
    while (itemRemain > 0) {
      uint32_t singleMsgLen = 0;
      // unsigned int dataMsgLen = 0;
      // char *singleData = NULL;
      if (!getDatantohlInt(uncompressData, itemPos, itemRemain, singleMsgLen, err_info)) {
        free(uncompressData);
        uncompressData = NULL;
        err_info += " for default item's msgLength parameter";
        return false;
      }
      if (singleMsgLen <= 0) {
        continue;
      }
      if (singleMsgLen > itemRemain) {
        free(uncompressData);
        uncompressData = NULL;
        err_info = "Parse message error: invalid default attr's msg Length";
        return false;
      }
      DataItem tmpDataItem(singleMsgLen, uncompressData + itemPos);
      addDataItem2Map(commAttr, tmpDataItem);
      itemPos += singleMsgLen;
      itemRemain -= singleMsgLen;
    }
    free(uncompressData);
    uncompressData = NULL;
  }
  is_parsed_ = true;
  return true;
}

bool TubeMQTDMsg::parseMixAttrMsg(const char* data, uint32_t data_length, int32_t start_pos,
                                  string& err_info) {
  // #lizard forgives
  int32_t pos1 = start_pos;
  uint32_t remain = data_length;
  for (uint32_t i = 0; i < attr_count_; i++) {
    uint32_t origAttrLen = 0;
    char* origAttrData = NULL;
    string commAttr;
    uint32_t bodyDataLen = 0;
    char compress = 0;
    if (remain <= 2) {
      if (i == 0) {
        err_info = "Parse message error: invalid databody length length";
        return false;
      } else {
        break;
      }
    }
    if (!getDatantohsInt(data, pos1, remain, origAttrLen, err_info)) {
      err_info += " for attr length parameter";
      return false;
    }
    if ((origAttrLen <= 0) || (origAttrLen > remain)) {
      err_info = "Parse message error: invalid attr length";
      return false;
    }
    origAttrData = static_cast<char*>(malloc(origAttrLen + 1));
    if (origAttrData == NULL) {
      err_info = "Parse message error: malloc buffer for v3 attr value failure!";
      return false;
    }
    memset(origAttrData, 0, origAttrLen + 1);
    memcpy(origAttrData, data + pos1, origAttrLen);
    pos1 += origAttrLen;
    remain -= origAttrLen;
    commAttr = origAttrData;
    free(origAttrData);
    origAttrData = NULL;
    if (!getDatantohlInt(data, pos1, remain, bodyDataLen, err_info)) {
      err_info += " for body data len parameter";
      return false;
    }
    if ((bodyDataLen <= 0) || (bodyDataLen > remain)) {
      err_info = "Parse message error: invalid data length";
      return false;
    }
    if (!getDataChar(data, pos1, remain, compress, err_info)) {
      err_info += " for attr compress parameter";
      return false;
    }
    size_t uncompressDataLen = 0;
    char* uncompressData = NULL;
    if (compress != 0) {
      if (snappy_uncompressed_length(data + pos1, bodyDataLen - 1, &uncompressDataLen) !=
          SNAPPY_OK) {
        err_info = "Parse message error:  snappy uncompressed v3 compress's length failure!";
        return false;
      }
      uncompressData = static_cast<char*>(malloc(uncompressDataLen));
      if (uncompressData == NULL) {
        err_info = "Parse message error: malloc buffer for v3 compress's data failure!";
        return false;
      }
      if (snappy_uncompress(data + pos1, bodyDataLen - 1, uncompressData, &uncompressDataLen) !=
          SNAPPY_OK) {
        free(uncompressData);
        uncompressData = NULL;
        err_info = "Parse message error:  snappy uncompressed v3 compress's data failure!";
        return false;
      }
    } else {
      uncompressDataLen = bodyDataLen - 1;
      uncompressData = static_cast<char*>(malloc(uncompressDataLen));
      if (uncompressData == NULL) {
        err_info = "Parse message error: malloc buffer for v3 compress's data failure!";
        return false;
      }
      memcpy(uncompressData, data + pos1, bodyDataLen - 1);
    }
    pos1 += bodyDataLen - 1;
    remain -= bodyDataLen - 1;
    int32_t itemPos = 0;
    uint32_t totalItemDataLen = 0;
    uint32_t itemRemain = uncompressDataLen;
    if (!getDatantohlInt(uncompressData, itemPos, itemRemain, totalItemDataLen, err_info)) {
      free(uncompressData);
      uncompressData = NULL;
      err_info += " for v3 item's msgLength parameter";
      return false;
    }
    if ((totalItemDataLen <= 0) || (totalItemDataLen > itemRemain)) {
      free(uncompressData);
      uncompressData = NULL;
      err_info = "Parse message error: invalid v3 attr's msg Length";
      return false;
    }
    while (itemRemain > 0) {
      uint32_t singleMsgLen = 0;
      char* singleData = NULL;
      uint32_t singleAttrLen = 0;
      char* singleAttr = NULL;
      string finalAttr;
      if (!getDatantohlInt(uncompressData, itemPos, itemRemain, singleMsgLen, err_info)) {
        free(uncompressData);
        uncompressData = NULL;
        err_info += " for v3 item's msgLength parameter";
        return false;
      }
      if ((singleMsgLen <= 0) || (singleMsgLen > itemRemain)) {
        free(uncompressData);
        uncompressData = NULL;
        err_info = "Parse message error: invalid v3 attr's msg Length";
        return false;
      }
      singleData = static_cast<char*>(malloc(singleMsgLen));
      if (singleData == NULL) {
        free(uncompressData);
        uncompressData = NULL;
        err_info = "Parse message error: malloc buffer for v3 single data failure!";
        return false;
      }
      memcpy(singleData, uncompressData + itemPos, singleMsgLen);
      itemPos += singleMsgLen;
      itemRemain -= singleMsgLen;
      if (itemRemain > 0) {
        if (!getDatantohlInt(uncompressData, itemPos, itemRemain, singleAttrLen, err_info)) {
          free(uncompressData);
          free(singleData);
          uncompressData = NULL;
          singleData = NULL;
          err_info += " for v3 attr's single length parameter";
          return false;
        }
        if ((singleAttrLen <= 0) || (singleAttrLen > itemRemain)) {
          free(uncompressData);
          free(singleData);
          uncompressData = NULL;
          singleData = NULL;
          err_info = "Parse message error: invalid v3 attr's attr Length";
          return false;
        }
        singleAttr = static_cast<char*>(malloc(singleAttrLen + 1));
        if (singleAttr == NULL) {
          free(uncompressData);
          free(singleData);
          uncompressData = NULL;
          singleData = NULL;
          err_info = "Parse message error: malloc buffer for v3 single attr failure!";
          return false;
        }
        memset(singleAttr, 0, singleAttrLen + 1);
        memcpy(singleAttr, uncompressData + itemPos, singleAttrLen);
        itemPos += singleAttrLen;
        itemRemain -= singleAttrLen;
        string strSingleAttr = singleAttr;
        finalAttr = commAttr + "&" + strSingleAttr;
        free(singleAttr);
        singleAttr = NULL;
      } else {
        finalAttr = commAttr;
      }
      DataItem tmpDataItem(singleMsgLen, singleData);
      addDataItem2Map(finalAttr, tmpDataItem);
      free(singleData);
      singleData = NULL;
    }
    free(uncompressData);
    uncompressData = NULL;
  }
  is_parsed_ = true;
  return true;
}

bool TubeMQTDMsg::parseBinMsg(const char* data, uint32_t data_length, int32_t start_pos,
                              string& err_info) {
  // #lizard forgives
  uint32_t totalLen = 0;
  char msgType = 0;
  uint32_t bidNum = 0;
  uint32_t tidNum = 0;
  uint32_t extField = 0;
  uint32_t dataTime = 0;
  uint32_t msgCnt = 0;
  uint32_t uniqueId = 0;
  uint32_t bodyLen = 0;
  uint32_t attrLen = 0;
  uint32_t msgMagic = 0;
  size_t realBodyLen = 0;
  char* bodyData = NULL;

  int32_t pos1 = start_pos;
  uint32_t remain = data_length;
  if (!getDatantohlInt(data, pos1, remain, totalLen, err_info)) {
    err_info += " for data v4 totalLen parameter";
    return false;
  }
  if (!getDataChar(data, pos1, remain, msgType, err_info)) {
    err_info += " for data v4 msgType parameter";
    return false;
  }
  if (!getDatantohsInt(data, pos1, remain, bidNum, err_info)) {
    err_info += " for v4 bidNum parameter";
    return false;
  }
  if (!getDatantohsInt(data, pos1, remain, tidNum, err_info)) {
    err_info += " for v4 tidNum parameter";
    return false;
  }
  if (!getDatantohsInt(data, pos1, remain, extField, err_info)) {
    err_info += " for v4 extField parameter";
    return false;
  }
  if (!getDatantohlInt(data, pos1, remain, dataTime, err_info)) {
    err_info += " for data v4 dataTime parameter";
    return false;
  }
  create_time_ = dataTime;
  create_time_ *= 1000;
  if (!getDatantohsInt(data, pos1, remain, msgCnt, err_info)) {
    err_info += " for v4 cnt parameter";
    return false;
  }
  if (!getDatantohlInt(data, pos1, remain, uniqueId, err_info)) {
    err_info += " for data v4 uniq parameter";
    return false;
  }
  if (!getDatantohlInt(data, pos1, remain, bodyLen, err_info)) {
    err_info += " for data v4 bodyLen parameter";
    return false;
  }
  if (remain < bodyLen + 2) {
    err_info += "Parse message error: no enough data length for v4 attr_len data";
    return false;
  }
  int32_t attrLenPos = pos1 + bodyLen;
  uint32_t attrLenRemain = remain - bodyLen;
  if (!getDatantohsInt(data, attrLenPos, attrLenRemain, attrLen, err_info)) {
    err_info += " for data v4 attrLen parameter";
    return false;
  }
  if (remain < attrLen + 2) {
    err_info += "Parse message error: no enough data length for v4 msgMagic data";
    return false;
  }
  int32_t msgMagicPos = attrLenPos + attrLen;
  uint32_t msgMagicRemain = remain - attrLen;
  if (!getDatantohsInt(data, msgMagicPos, msgMagicRemain, msgMagic, err_info)) {
    err_info += " for v4 msgMagic parameter";
    return false;
  }
  msgMagic &= 0xFFFF;
  // get attr data
  bool result = false;
  map<string, string> commonAttrMap;
  if (attrLen != 0) {
    char* commonAttr = static_cast<char*>(malloc(attrLen + 1));
    if (commonAttr == NULL) {
      err_info = "Parse message error: malloc buffer for v3 common attr failure!";
      return false;
    }
    memset(commonAttr, 0, attrLen + 1);
    memcpy(commonAttr, data + attrLenPos, attrLen);
    string strAttr = commonAttr;
    Utils::Split(strAttr, commonAttrMap, delimiter::kDelimiterAnd, delimiter::kDelimiterEqual);
    if (commonAttrMap.empty()) {
      free(commonAttr);
      commonAttr = NULL;
      err_info += " for v4 common attribute parameter";
      return result;
    }
    free(commonAttr);
    commonAttr = NULL;
  }
  // get body data
  switch ((msgType & 0xE0) >> 5) {
    case 1: {
      if (snappy_uncompressed_length(data + pos1, bodyLen, &realBodyLen) != SNAPPY_OK) {
        err_info = "Parse message error:  snappy uncompressed v4 body's length failure!";
        return false;
      }
      bodyData = static_cast<char*>(malloc(realBodyLen));
      if (bodyData == NULL) {
        err_info = "Parse message error: malloc buffer for v4 body's data failure!";
        return false;
      }
      if (snappy_uncompress(data + pos1, bodyLen, bodyData, &realBodyLen) != SNAPPY_OK) {
        free(bodyData);
        bodyData = NULL;
        err_info = "Parse message error:  snappy uncompressed v4 body's data failure!";
        return false;
      }
      break;
    }

    case 0:
    default: {
      realBodyLen = bodyLen;
      bodyData = static_cast<char*>(malloc(realBodyLen));
      if (bodyData == NULL) {
        err_info = "Parse message error: malloc buffer for v4 body's data failure!";
        return false;
      }
      memcpy(bodyData, data + pos1, realBodyLen);
      break;
    }
  }
  //  build attr
  commonAttrMap["dt"] = Utils::Long2str(create_time_);
  if ((extField & 0x4) == 0x0) {
    commonAttrMap["bid"] = Utils::Int2str(bidNum);
    commonAttrMap["tid"] = Utils::Int2str(tidNum);
  }
  commonAttrMap["cnt"] = "1";
  int msgCount = msgCnt;
  //  build data
  if ((extField & 0x1) == 0x0) {
    int32_t bodyPos = 0;
    uint32_t bodyRemain = realBodyLen;
    string outKeyValStr;
    Utils::Join(commonAttrMap, outKeyValStr, delimiter::kDelimiterAnd, delimiter::kDelimiterEqual);
    while ((bodyRemain > 0) && (msgCount-- > 0)) {
      uint32_t singleMsgLen = 0;
      if (!getDatantohlInt(bodyData, bodyPos, bodyRemain, singleMsgLen, err_info)) {
        free(bodyData);
        bodyData = NULL;
        err_info += " for v4 attr's msgLength parameter";
        return false;
      }
      if (singleMsgLen <= 0) {
        continue;
      }
      if (singleMsgLen > bodyRemain) {
        free(bodyData);
        bodyData = NULL;
        err_info = "Parse message error: invalid v4 attr's msg Length 1";
        return false;
      }
      char* singleData = static_cast<char*>(malloc(singleMsgLen));
      if (singleData == NULL) {
        free(bodyData);
        bodyData = NULL;
        err_info = "Parse message error: malloc buffer for v4 single data failure!";
        return false;
      }
      memcpy(singleData, bodyData + bodyPos, singleMsgLen);
      bodyPos += singleMsgLen;
      bodyRemain -= singleMsgLen;
      DataItem tmpDataItem(singleMsgLen, singleData);
      addDataItem2Map(outKeyValStr, tmpDataItem);
      free(singleData);
      singleData = NULL;
    }
    free(bodyData);
    bodyData = NULL;
  } else {
    int32_t bodyPos = 0;
    uint32_t bodyRemain = realBodyLen;
    while ((bodyRemain > 0) && (msgCount-- > 0)) {
      uint32_t singleMsgLen = 0;
      if (!getDatantohlInt(bodyData, bodyPos, bodyRemain, singleMsgLen, err_info)) {
        free(bodyData);
        bodyData = NULL;
        err_info += " for v4 attr's msgLength parameter";
        return false;
      }
      if (singleMsgLen <= 0) {
        continue;
      }
      if (singleMsgLen > bodyRemain) {
        free(bodyData);
        bodyData = NULL;
        err_info = "Parse message error: invalid v4 attr's msg Length 2";
        return false;
      }
      char* singleData = static_cast<char*>(malloc(singleMsgLen));
      if (singleData == NULL) {
        free(bodyData);
        bodyData = NULL;
        err_info = "Parse message error: malloc buffer for v4 single data failure!";
        return false;
      }
      memcpy(singleData, bodyData + bodyPos, singleMsgLen);
      bodyPos += singleMsgLen;
      bodyRemain -= singleMsgLen;
      uint32_t singleAttrLen = 0;
      if (!getDatantohlInt(bodyData, bodyPos, bodyRemain, singleAttrLen, err_info)) {
        free(bodyData);
        free(singleData);
        bodyData = NULL;
        singleData = NULL;
        err_info += " for v4 attr's single length parameter";
        return false;
      }
      if ((singleAttrLen <= 0) || (singleAttrLen > bodyRemain)) {
        free(bodyData);
        free(singleData);
        bodyData = NULL;
        singleData = NULL;
        err_info = "Parse message error: invalid v4 attr's attr Length";
        return false;
      }
      map<string, string> privAttrMap;
      map<string, string>::iterator tempIt;
      for (tempIt = commonAttrMap.begin(); tempIt != commonAttrMap.end(); ++tempIt) {
        privAttrMap[tempIt->first] = tempIt->second;
      }
      string strSingleAttr;
      if (singleAttrLen > 0) {
        char* singleAttr = static_cast<char*>(malloc(singleAttrLen + 1));
        if (singleAttr == NULL) {
          free(bodyData);
          free(singleData);
          bodyData = NULL;
          singleData = NULL;
          err_info = "Parse message error: malloc buffer for v4 single attr failure!";
          return false;
        }
        memset(singleAttr, 0, singleAttrLen + 1);
        memcpy(singleAttr, bodyData + bodyPos, singleAttrLen);
        bodyPos += singleAttrLen;
        attrLenRemain -= singleAttrLen;
        bodyRemain -= singleAttrLen;
        strSingleAttr = singleAttr;
        Utils::Split(strSingleAttr, privAttrMap, delimiter::kDelimiterAnd,
                     delimiter::kDelimiterEqual);
        if (privAttrMap.empty()) {
          free(bodyData);
          free(singleAttr);
          free(singleData);
          bodyData = NULL;
          singleData = NULL;
          singleAttr = NULL;
          err_info += " for v4 private attribute parameter";
          return result;
        }
        free(singleAttr);
        singleAttr = NULL;
      }
      string outKeyValStr;
      Utils::Join(privAttrMap, outKeyValStr, delimiter::kDelimiterAnd, delimiter::kDelimiterEqual);
      DataItem tmpDataItem(singleMsgLen, singleData);
      addDataItem2Map(outKeyValStr, tmpDataItem);
      free(singleData);
      singleData = NULL;
    }
    free(bodyData);
    bodyData = NULL;
  }
  is_parsed_ = true;
  return true;
}

void TubeMQTDMsg::Clear() {
  is_parsed_ = false;
  is_numbid_ = false;
  version_ = -1;
  create_time_ = -1;
  msg_count_ = 0;
  attr_count_ = 0;
  attr2data_map_.clear();
}

bool TubeMQTDMsg::ParseAttrValue(string attr_value, map<string, string>& result, string& err_info) {
  if (attr_value.empty()) {
    err_info = "parmeter attr_value is empty";
    return false;
  }
  if (string::npos == attr_value.find(delimiter::kDelimiterAnd)) {
    err_info = "Unregular attr_value error: not found token '&'!";
    return false;
  }
  Utils::Split(attr_value, result, delimiter::kDelimiterAnd, delimiter::kDelimiterEqual);
  err_info = "Ok";
  return true;
}

bool TubeMQTDMsg::addDataItem2Map(const string& datakey, const DataItem& data_item) {
  map<string, list<DataItem> >::iterator itDataList = attr2data_map_.find(datakey);
  if (itDataList == attr2data_map_.end()) {
    list<DataItem> tmpDataList;
    tmpDataList.push_back(data_item);
    attr2data_map_[datakey] = tmpDataList;
  } else {
    itDataList->second.push_back(data_item);
  }
  return true;
}

static bool getDataChar(const char* data, int32_t& pos, uint32_t& remain, char& chrVal,
                        string& err_info) {
  const char* p = data;
  if (remain < 1) {
    err_info = "Parse message error: no enough char data length";
    return false;
  }
  chrVal = (p[pos] & 0xFF);
  pos += 1;
  remain -= 1;
  return true;
}

static bool getDatantohlInt(const char* data, int32_t& pos, uint32_t& remain, uint32_t& intVal,
                            string& err_info) {
  const char* p = data;
  if (remain < 4) {
    err_info = "Parse error: no enough data length";
    return false;
  }
  intVal = ntohl(*(unsigned int*)(&p[pos]));
  pos += 4;
  remain -= 4;
  return true;
}

static bool getDatantohsInt(const char* data, int32_t& pos, uint32_t& remain, uint32_t& intVal,
                            string& err_info) {
  const char* p = data;
  if (remain < 2) {
    err_info = "Parse message error: no enough data length";
    return false;
  }
  intVal = ntohs(*(unsigned int*)(&p[pos]));
  pos += 2;
  remain -= 2;
  return true;
}

static bool getDataCreateTime(const char* data, int32_t& pos, uint32_t& remain, int64_t& createTime,
                              string& err_info) {
  const char* p = data;
  if (remain < 8) {
    err_info = "Parse message error: no enough data length for createtime data";
    return false;
  }
  createTime = (((int64_t)p[pos] << 56)
    + ((int64_t)(p[pos + 1] & 255) << 48)
    + ((int64_t)(p[pos + 2] & 255) << 40)
    + ((int64_t)(p[pos + 3] & 255) << 32)
    + ((int64_t)(p[pos + 4] & 255) << 24)
    + ((p[pos + 5] & 255) << 16)
    + ((p[pos + 6] & 255) << 8)
    + ((p[pos + 7] & 255) << 0));
  pos += 8;
  remain -= 8;
  return true;
}

static bool getDataMagic(const char* data, int32_t& pos, uint32_t& remain, int32_t& ver,
                         string& err_info) {
  // #lizard forgives
  ver = -1;
  const char* p = data;
  if (remain < 4) {
    err_info = "Parse message error: no enough data length for magic data";
    return false;
  }
  if (((p[pos] == 0xf) && (p[pos + 1] == 0x2)) &&
      ((p[pos + remain - 2] == 0xf) && (p[pos + remain - 1] == 0x2))) {
    ver = 2;
    pos += 2;
    remain -= 2;
    return true;
  }
  if (((p[pos] == 0xf) && (p[pos + 1] == 0x1)) &&
      ((p[pos + remain - 2] == 0xf) && (p[pos + remain - 1] == 0x1))) {
    ver = 1;
    pos += 2;
    remain -= 2;
    return true;
  }
  if (((p[pos] == 0xf) && (p[pos + 1] == 0x4)) &&
      ((p[pos + remain - 2] == 0xf) && (p[pos + remain - 1] == 0x4))) {
    ver = 4;
    pos += 2;
    remain -= 2;
    return true;
  }
  if (((p[pos] == 0xf) && (p[pos + 1] == 0x3)) &&
      ((p[pos + remain - 2] == 0xf) && (p[pos + remain - 1] == 0x3))) {
    ver = 3;
    pos += 2;
    remain -= 2;
    return true;
  }
  if (((p[pos] == 0xf) && (p[pos + 1] == 0x0)) &&
      ((p[pos + remain - 2] == 0xf) && (p[pos + remain - 1] == 0x0))) {
    ver = 0;
    pos += 2;
    remain -= 2;
    return true;
  }
  err_info = "Parse message error: Unsupported message format";
  return false;
}

}  // namespace tubemq

