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

#ifndef TUBEMQ_CLIENT_FILE_INI_H_
#define TUBEMQ_CLIENT_FILE_INI_H_

#include <stdint.h>

#include <map>
#include <string>

namespace tubemq {

using std::map;
using std::string;

class Fileini {
 public:
  Fileini();
  ~Fileini();
  bool Loadini(string& err_info, const string& file_name);
  bool GetValue(string& err_info, const string& sector, const string& key,
                    string& value, const string& def) const;
  bool GetValue(string& err_info, const string& sector, const string& key,
                   int32_t& value, int32_t def) const;

 private:
  bool init_flag_;
  // sector        key    value
  map<string, map<string, string> > ini_map_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_FILE_INI_H_
