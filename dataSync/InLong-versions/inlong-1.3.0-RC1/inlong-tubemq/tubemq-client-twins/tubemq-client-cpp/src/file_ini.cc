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

#include "file_ini.h"

#include <stdlib.h>

#include <fstream>
#include <sstream>

#include "const_config.h"
#include "utils.h"

namespace tubemq {

using std::ifstream;



Fileini::Fileini() {
  init_flag_ = false;
  ini_map_.clear();
}

Fileini::~Fileini() {
  init_flag_ = false;
  ini_map_.clear();
}

bool Fileini::Loadini(string& err_info, const string& file_name) {
  // check paramter
  if (file_name.empty()) {
    err_info = "Ini configure file is null!";
    return false;
  }
  // open configure file and check
  ifstream conf_file(file_name.c_str());
  if (!conf_file.is_open()) {
    err_info = "Open file " + file_name + " failure!";
    return false;
  }
  string line_str = "";
  string sector = "";
  string key = "";
  string value = "";
  string lftsb = delimiter::kDelimiterLftSB;
  string rgtsb = delimiter::kDelimiterRgtSB;
  string equal = delimiter::kDelimiterEqual;
  string::size_type lftsb_pos = 0;
  string::size_type rgtsb_pos = 0;
  string::size_type equal_pos = 0;
  // read ini file and parse content
  while (getline(conf_file, line_str)) {
    // check if a comment
    line_str = Utils::Trim(line_str);
    if (line_str.empty() || line_str.find(delimiter::kDelimiterDbSlash) == 0 ||
        line_str.find(delimiter::kDelimiterSemicolon) == 0) {
      continue;
    }
    // check if a sector head
    lftsb_pos = line_str.find(lftsb);
    rgtsb_pos = line_str.find(rgtsb);
    if (lftsb_pos != string::npos && rgtsb_pos != string::npos) {
      sector = line_str.substr(lftsb_pos + lftsb.size(), rgtsb_pos - rgtsb.size());
      sector = Utils::Trim(sector);
      continue;
    }
    // check if a key=value string
    equal_pos = line_str.find(equal);
    if (equal_pos == string::npos) {
      continue;
    }
    key = line_str.substr(0, equal_pos);
    value = line_str.substr(equal_pos + equal.size(), line_str.size());
    key = Utils::Trim(key);
    value = Utils::Trim(value);
    // get data from file to memory
    if (sector.empty() && key.empty() && value.empty()) {
      continue;
    }
    map<string, map<string, string> >::iterator it_sec;
    it_sec = ini_map_.find(sector);
    if (it_sec == ini_map_.end()) {
      map<string, string> tmp_key_val_map;
      tmp_key_val_map[key] = value;
      ini_map_[sector] = tmp_key_val_map;
    } else {
      it_sec->second[key] = value;
    }
  }
  // close configure file and clear status
  conf_file.close();
  conf_file.clear();
  // set parser status
  init_flag_ = true;
  // end
  err_info = "Ok";
  return true;
}

bool Fileini::GetValue(string& err_info, const string& sector, const string& key, string& value,
                       const string& def) const {
  if (!init_flag_) {
    err_info = "Please load configure file first!";
    return false;
  }
  err_info = "Ok";
  value.clear();
  // search key's value in sector
  map<string, map<string, string> >::const_iterator it_sec;
  map<string, string>::const_iterator it_keyval;
  it_sec = ini_map_.find(sector);
  if (it_sec == ini_map_.end()) {
    value = def;
    return true;
  }
  it_keyval = it_sec->second.find(key);
  if (it_keyval == it_sec->second.end()) {
    value = def;
    return true;
  }
  value = it_keyval->second;
  return true;
}

bool Fileini::GetValue(string& err_info, const string& sector, const string& key,
                          int32_t& value, int32_t def) const {
  string val_str;
  string def_str = Utils::Int2str(def);
  bool result = GetValue(err_info, sector, key, val_str, def_str);
  if (!result) {
    return result;
  }
  value = atoi(val_str.c_str());
  return true;
}

}  // namespace tubemq
