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

#include "utils.h"

#include <arpa/inet.h>
#include <ctype.h>
#include <linux/if.h>
#include <netdb.h>
#include <netinet/in.h>
#include <regex.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>
#include <sstream>
#include <vector>
#include "const_config.h"
#include "const_rpc.h"



namespace tubemq {

using std::stringstream;


static const char kWhitespaceCharSet[] = " \n\r\t\f\v";

/*
 *  copy from https://web.mit.edu/freebsd/head/sys/libkern/crc32.c
*/
static const uint32_t crc32_tab[256] = {
  0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f,
  0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988,
  0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2,
  0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,
  0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,
  0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4, 0xa2677172,
  0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b, 0x35b5a8fa, 0x42b2986c,
  0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59,
  0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423,
  0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,
  0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190, 0x01db7106,
  0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433,
  0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d,
  0x91646c97, 0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e,
  0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,
  0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65,
  0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7,
  0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0,
  0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa,
  0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
  0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81,
  0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a,
  0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84,
  0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,
  0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,
  0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a, 0x67dd4acc,
  0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e,
  0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,
  0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55,
  0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,
  0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe, 0xb2bd0b28,
  0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d,
  0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f,
  0x72076785, 0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38,
  0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
  0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777,
  0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69,
  0x616bffd3, 0x166ccf45, 0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2,
  0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc,
  0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
  0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693,
  0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94,
  0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d
};

int32_t Utils::Crc32(const string &buf) {
  uint32_t crc = ~0U;
  for (uint32_t i = 0; i < buf.size(); ++i) {
    unsigned char c_data = buf[i];
    crc = crc32_tab[(crc ^ c_data) & 0xFF] ^ (crc >> 8);
  }
  return ((~crc)& 0x7FFFFFFF);
}

string Utils::Trim(const string& source) {
  string target = source;
  if (!target.empty()) {
    size_t foud_pos = target.find_first_not_of(kWhitespaceCharSet);
    if (foud_pos != string::npos) {
      target = target.substr(foud_pos);
    }
    foud_pos = target.find_last_not_of(kWhitespaceCharSet);
    if (foud_pos != string::npos) {
      target = target.substr(0, foud_pos + 1);
    }
  }
  return target;
}

void Utils::Split(const string& source, vector<string>& result, const string& delimiter) {
  string item_str;
  string::size_type pos1 = 0;
  string::size_type pos2 = 0;
  result.clear();
  if (!source.empty()) {
    pos1 = 0;
    pos2 = source.find(delimiter);
    while (string::npos != pos2) {
      item_str = Utils::Trim(source.substr(pos1, pos2 - pos1));
      pos1 = pos2 + delimiter.size();
      pos2 = source.find(delimiter, pos1);
      if (!item_str.empty()) {
        result.push_back(item_str);
      }
    }
    if (pos1 != source.length()) {
      item_str = Utils::Trim(source.substr(pos1));
      if (!item_str.empty()) {
        result.push_back(item_str);
      }
    }
  }
}

void Utils::Split(const string& source, map<string, int32_t>& result,
  const string& delimiter_step1, const string& delimiter_step2) {
  map<string, string> tmp_result;
  map<string, string>::iterator it;
  result.clear();
  Split(source, tmp_result, delimiter_step1, delimiter_step2);
  if (!tmp_result.empty()) {
    for (it = tmp_result.begin(); it != tmp_result.end(); ++it) {
      result[it->first] = atoi(it->second.c_str());
    }
  }
}

void Utils::Split(const string& source, map<string, string>& result,
  const string& delimiter_step1, const string& delimiter_step2) {
  string item_str;
  string key_str;
  string val_str;
  string::size_type pos1 = 0;
  string::size_type pos2 = 0;
  string::size_type pos3 = 0;
  if (!source.empty()) {
    pos1 = 0;
    pos2 = source.find(delimiter_step1);
    while (string::npos != pos2) {
      item_str = source.substr(pos1, pos2 - pos1);
      item_str = Utils::Trim(item_str);
      pos1 = pos2 + delimiter_step1.length();
      pos2 = source.find(delimiter_step1, pos1);
      if (item_str.empty()) {
        continue;
      }
      pos3 = item_str.find(delimiter_step2);
      if (string::npos == pos3) {
        continue;
      }
      key_str = item_str.substr(0, pos3);
      val_str = item_str.substr(pos3 + delimiter_step2.length());
      key_str = Utils::Trim(key_str);
      val_str = Utils::Trim(val_str);
      if (key_str.empty()) {
        continue;
      }
      result[key_str] = val_str;
    }
    if (pos1 != source.length()) {
      item_str = source.substr(pos1);
      item_str = Utils::Trim(item_str);
      pos3 = item_str.find(delimiter_step2);
      if (string::npos != pos3) {
        key_str = item_str.substr(0, pos3);
        val_str = item_str.substr(pos3 + delimiter_step2.length());
        key_str = Utils::Trim(key_str);
        val_str = Utils::Trim(val_str);
        if (!key_str.empty()) {
          result[key_str] = val_str;
        }
      }
    }
  }
}

void Utils::Join(const vector<string>& vec, const string& delimiter, string& target) {
  vector<string>::const_iterator it;
  target.clear();
  for (it = vec.begin(); it != vec.end(); ++it) {
    target += *it;
    if (it != vec.end() - 1) {
      target += delimiter;
    }
  }
}

void Utils::Join(const map<string, string>& source, string& target,
  const string& delimiter_step1, const string& delimiter_step2) {
  map<string, string>::const_iterator it;
  target.clear();
  int count = 0;
  for (it = source.begin(); it != source.end(); ++it) {
    if (count++ > 0) {
      target += delimiter_step1;
    }
    target += it->first + delimiter_step2 + it->second;
  }
}

bool Utils::ValidString(string& err_info, const string& source, bool allow_empty, bool pat_match,
                        bool check_max_length, unsigned int maxlen) {
  if (source.empty()) {
    if (allow_empty) {
      err_info = "Ok";
      return true;
    }
    err_info = "value is empty";
    return false;
  }
  if (check_max_length) {
    if (source.length() > maxlen) {
      stringstream ss;
      ss << source;
      ss << " over max length, the max allowed length is ";
      ss << maxlen;
      err_info = ss.str();
      return false;
    }
  }

  if (pat_match) {
    int cflags = REG_EXTENDED;
    regex_t reg;
    regmatch_t pmatch[1];
    const char* patRule = "^[a-zA-Z]\\w+$";
    regcomp(&reg, patRule, cflags);
    int status = regexec(&reg, source.c_str(), 1, pmatch, 0);
    regfree(&reg);
    if (status == REG_NOMATCH) {
      stringstream ss;
      ss << source;
      ss << " must begin with a letter,can only contain characters,numbers,and underscores";
      err_info = ss.str();
      return false;
    }
  }
  err_info = "Ok";
  return true;
}

bool Utils::ValidGroupName(string& err_info, const string& group_name, string& tgt_group_name) {
  tgt_group_name = Utils::Trim(group_name);
  if (tgt_group_name.empty()) {
    err_info = "Illegal parameter: group_name is blank!";
    return false;
  }
  if (tgt_group_name.length() > tb_config::kGroupNameMaxLength) {
    stringstream ss;
    ss << "Illegal parameter: ";
    ss << group_name;
    ss << " over max length, the max allowed length is ";
    ss << tb_config::kGroupNameMaxLength;
    err_info = ss.str();
    return false;
  }
  int cflags = REG_EXTENDED;
  regex_t reg;
  regmatch_t pmatch[1];
  //  const char* patRule = "^[a-zA-Z][\\w-]+$";
  const char* patRule = "^[a-zA-Z]\\w+$";
  regcomp(&reg, patRule, cflags);
  int status = regexec(&reg, tgt_group_name.c_str(), 1, pmatch, 0);
  regfree(&reg);
  if (status == REG_NOMATCH) {
    stringstream ss;
    ss << "Illegal parameter: ";
    ss << group_name;
    ss << " must begin with a letter,can only contain characters,numbers,and underscores";
    //  ss << " must begin with a letter,can only contain ";
    //  ss << "characters,numbers,hyphen,and underscores";
    err_info = ss.str();
    return false;
  }
  err_info = "Ok";
  return true;
}

bool Utils::ValidFilterItem(string& err_info, const string& src_filteritem,
                            string& tgt_filteritem) {
  tgt_filteritem = Utils::Trim(src_filteritem);
  if (tgt_filteritem.empty()) {
    err_info = "value is blank!";
    return false;
  }

  if (tgt_filteritem.length() > tb_config::kFilterItemMaxLength) {
    stringstream ss;
    ss << "value over max length ";
    ss << tb_config::kFilterItemMaxLength;
    err_info = ss.str();
    return false;
  }
  int cflags = REG_EXTENDED;
  regex_t reg;
  regmatch_t pmatch[1];
  const char* patRule = "^[_A-Za-z0-9]+$";
  regcomp(&reg, patRule, cflags);
  int status = regexec(&reg, tgt_filteritem.c_str(), 1, pmatch, 0);
  regfree(&reg);
  if (status == REG_NOMATCH) {
    err_info = "value only contain characters,numbers,and underscores";
    return false;
  }
  err_info = "Ok";
  return true;
}

string Utils::Int2str(int32_t data) {
  stringstream ss;
  ss << data;
  return ss.str();
}

string Utils::Long2str(int64_t data) {
  stringstream ss;
  ss << data;
  return ss.str();
}

uint32_t Utils::IpToInt(const string& ipv4_addr) {
  uint32_t result = 0;
  vector<string> result_vec;

  Utils::Split(ipv4_addr, result_vec, delimiter::kDelimiterDot);
  result = ((char)atoi(result_vec[3].c_str())) & 0xFF;
  result |= ((char)atoi(result_vec[2].c_str()) << 8) & 0xFF00;
  result |= ((char)atoi(result_vec[1].c_str()) << 16) & 0xFF0000;
  result |= ((char)atoi(result_vec[0].c_str()) << 24) & 0xFF000000;
  return result;
}

int64_t Utils::GetCurrentTimeMillis() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

bool Utils::ValidConfigFile(string& err_info, const string& conf_file) {
  FILE *fp = NULL;

  if (conf_file.length() == 0) {
    err_info = "Configure file is blank";
    return false;
  }
  fp = fopen(conf_file.c_str(), "r");
  if (fp == NULL) {
    err_info = "Open configure file Failed!";
    return false;
  }
  fclose(fp);
  err_info = "Ok";
  return true;
}

bool Utils::GetLocalIPV4Address(string& err_info, string& localhost) {
  int32_t sockfd;
  int32_t ip_num = 0;
  char  buf[1024] = {0};
  struct ifreq *ifreq;
  struct ifreq if_flag;
  struct ifconf ifconf;

  ifconf.ifc_len = sizeof(buf);
  ifconf.ifc_buf = buf;
  if ((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
    err_info = "Open the local socket(AF_INET, SOCK_DGRAM) failure!";
    return false;
  }

  ioctl(sockfd, SIOCGIFCONF, &ifconf);
  ifreq  = (struct ifreq *)buf;
  ip_num = ifconf.ifc_len / sizeof(struct ifreq);
  for (int32_t i = 0; i < ip_num; i++, ifreq++) {
    if (ifreq->ifr_flags != AF_INET) {
      continue;
    }
    if (0 == strncmp(&ifreq->ifr_name[0], "lo", sizeof("lo"))) {
      continue;
    }
    memcpy(&if_flag.ifr_name[0], &ifreq->ifr_name[0], sizeof(ifreq->ifr_name));
    if ((ioctl(sockfd, SIOCGIFFLAGS, (char *) &if_flag)) < 0) {
      continue;
    }
    if ((if_flag.ifr_flags & IFF_LOOPBACK)
      || !(if_flag.ifr_flags & IFF_UP)) {
      continue;
    }

    if (!strncmp(inet_ntoa(((struct sockaddr_in*)&(ifreq->ifr_addr))->sin_addr),
      "127.0.0.1", 7)) {
      continue;
    }
    localhost = inet_ntoa(((struct sockaddr_in*)&(ifreq->ifr_addr))->sin_addr);
    close(sockfd);
    err_info = "Ok";
    return true;
  }
  close(sockfd);
  err_info = "Not found the localHost in local OS";
  return false;
}

int32_t Utils::GetServiceTypeByMethodId(int32_t method_id) {
  switch (method_id) {
    // broker write service
    case rpc_config::kBrokerMethoddProducerRegister:
    case rpc_config::kBrokerMethoddProducerHeatbeat:
    case rpc_config::kBrokerMethoddProducerSendMsg:
    case rpc_config::kBrokerMethoddProducerClose: {
      return rpc_config::kBrokerWriteService;
    }
    // broker read service
    case rpc_config::kBrokerMethoddConsumerRegister:
    case rpc_config::kBrokerMethoddConsumerHeatbeat:
    case rpc_config::kBrokerMethoddConsumerGetMsg:
    case rpc_config::kBrokerMethoddConsumerCommit:
    case rpc_config::kBrokerMethoddConsumerClose: {
      return rpc_config::kBrokerReadService;
    }
    // master service
    case rpc_config::kMasterMethoddProducerRegister:
    case rpc_config::kMasterMethoddProducerHeatbeat:
    case rpc_config::kMasterMethoddProducerClose:
    case rpc_config::kMasterMethoddConsumerRegister:
    case rpc_config::kMasterMethoddConsumerHeatbeat:
    case rpc_config::kMasterMethoddConsumerClose:
    default: {
      return rpc_config::kMasterService;
    }
  }
}

void Utils::XfsAddrByDns(const map<string, int32_t>& orig_addr_map,
  map<string, string>& target_addr_map) {
  hostent* host = NULL;
  map<string, int32_t>::const_iterator it;
  for (it = orig_addr_map.begin(); it != orig_addr_map.end(); it++) {
    char first_char =  it->first.c_str()[0];
    if (isalpha(first_char)) {
      host = gethostbyname(it->first.c_str());
      if (host != NULL) {
        switch (host->h_addrtype) {
          case AF_INET:
          case AF_INET6: {
            char **pptr = NULL;
            unsigned int addr = 0;
            char temp_str[32];
            memset(temp_str, 0, 32);
            pptr = host->h_addr_list;
            addr = ((unsigned int *) host->h_addr_list[0])[0];
            if ((addr & 0xffff) == 0x0a0a) {
              pptr++;
              addr = ((unsigned int *) host->h_addr_list[0])[1];
            }
            inet_ntop(host->h_addrtype, *pptr, temp_str, sizeof(temp_str));
            string tempIpaddr = temp_str;
            if (tempIpaddr.length() > 0) {
              target_addr_map[it->first] = tempIpaddr;
            }
          }
          break;

          default:
            break;
        }
      }
    } else {
      target_addr_map[it->first] = it->first;
    }
  }
}

bool Utils::NeedDnsXfs(const string& masteraddr) {
  if (masteraddr.length() > 0) {
    char first_char = masteraddr.c_str()[0];
    if (isalpha(first_char)) {
      return true;
    }
  }
  return false;
}

string Utils::GenBrokerAuthenticateToken(const string& username,
  const string& usrpassword) {
  return "";
}


}  // namespace tubemq

