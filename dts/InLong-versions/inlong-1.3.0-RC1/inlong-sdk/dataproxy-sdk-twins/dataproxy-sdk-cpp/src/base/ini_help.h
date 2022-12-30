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

#ifndef DATAPROXY_SDK_BASE_INI_HELP_H_
#define DATAPROXY_SDK_BASE_INI_HELP_H_

#include <algorithm>
#include <string.h>
#include <string>
#include <vector>

namespace dataproxy_sdk
{
const char delim[] = "\n";
struct Iterm
{
    std::string key;
    std::string value;
    std::string comment;
    std::string rightComment;
};

struct IniSection
{
    using ItermIterator = std::vector<Iterm>::iterator; 
    ItermIterator begin()
    {
        return items.begin();
    }

    ItermIterator end()
    {
        return items.end(); 
    }

    std::string name;
    std::string comment;
    std::string rightComment;
    std::vector<Iterm> items;
};

class IniFile
{
  public:
    IniFile(){}
    ~IniFile() { close(); }

    int load(const std::string& fileName);
    int getString(const std::string& section, const std::string& key, std::string* value);
    int getInt(const std::string& section, const std::string& key, int* value);

  private:
    IniSection* getSection(const std::string& section = "");
    static void trim(std::string& str);
    int updateSection(const std::string& cleanLine,
                      const std::string& comment,
                      const std::string& rightComment,
                      IniSection** section);
    int addKV(const std::string& cleanLine,
                        const std::string& comment,
                        const std::string& rightComment,
                        IniSection* section);
    void close();
    bool split(const std::string& str, const std::string& sep, std::string* left, std::string* right);
    bool parse(const std::string& content, std::string* key, std::string* value);
    int getValue(const std::string& section, const std::string& key, std::string* value);
    int getValue(const std::string& section, const std::string& key, std::string* value, std::string* comment);

  private:
    using SectionIterator = std::vector<IniSection*>::iterator;
    std::vector<IniSection*> sections_;
    std::string ini_file_name_;
    std::string err_msg_;  // save err msg
};

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_INI_HELP_H_