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

#include "ini_help.h"

#include <ctype.h>
#include <fstream>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>

namespace dataproxy_sdk
{

/**
 * @description: parse line content
 * @return {*} true if success
 * @param {string&} content
 * @param {string*} key
 * @param {string*} value
 */
bool IniFile::parse(const std::string& content, std::string* key, std::string* value) { return split(content, "=", key, value); }

int IniFile::updateSection(const std::string& cleanLine,
                           const std::string& comment,
                           const std::string& rightComment,
                           IniSection** section)
{
    IniSection* newSection;
    // find ']'
    size_t index = cleanLine.find_first_of(']');
    if (index == std::string::npos)
    {
        err_msg_ = std::string("no matched ] found");
        return 1;
    }

    int len = index - 1;

    if (len <= 0)
    {
        err_msg_ = std::string("section name is empty");
        return 1;
    }

    // set section name
    std::string s(cleanLine, 1, len);

    trim(s);

    //check section 
    if (getSection(s) != NULL)
    {
        err_msg_ = std::string("section ") + s + std::string("already exist");
        return 1;
    }

    newSection = new IniSection();
    newSection->name = s;
    newSection->comment      = comment;
    newSection->rightComment = rightComment;

    sections_.push_back(newSection);

    *section = newSection;

    return 0;
}

int IniFile::addKV(const std::string& cleanLine,
                             const std::string& comment,
                             const std::string& rightComment,
                             IniSection* section)
{
    std::string key, value;

    if (!parse(cleanLine, &key, &value))
    {
        err_msg_ = std::string("parse line failed:") + cleanLine;
        return 1;
    }

    Iterm item;
    item.key          = key;
    item.value        = value;
    item.comment      = comment;
    item.rightComment = rightComment;

    section->items.push_back(item);

    return 0;
}

int IniFile::load(const std::string& fileName)
{
    int err;
    std::string line;
    std::string comment;
    std::string rightComment;
    IniSection* currSection = NULL;  //init a section

    close();

    ini_file_name_ = fileName;
    std::ifstream istream(ini_file_name_);
    if (!istream.is_open())
    {
        err_msg_ = std::string("open") + ini_file_name_ + std::string(" file failed");
        return 1;
    }

    // add new section
    currSection       = new IniSection();
    currSection->name = "";
    sections_.push_back(currSection);

    // read line
    while (std::getline(istream, line))
    {
        trim(line);

        // skip empty
        if (line.length() <= 0)
        {
            comment += delim;
            continue;
        }

        // whether contains section or key
        // find '['
        if (line[0] == '[') { err = updateSection(line, comment, rightComment, &currSection); }
        else
        {
            err = addKV(line, comment, rightComment, currSection);
        }

        if (err != 0)
        {
            istream.close();
            return err;
        }

        // clear
        comment      = "";
        rightComment = "";
    }

    istream.close();

    return 0;
}

IniSection* IniFile::getSection(const std::string& section)
{
    for (SectionIterator it = sections_.begin(); it != sections_.end(); ++it)
    {
        if ((*it)->name == section) { return *it; }
    }

    return NULL;
}

int IniFile::getString(const std::string& section, const std::string& key, std::string* value)
{
    return getValue(section, key, value);
}

int IniFile::getInt(const std::string& section, const std::string& key, int* intValue)
{
    int err;
    std::string strValue;

    err = getValue(section, key, &strValue);

    *intValue = atoi(strValue.c_str());

    return err;
}

int IniFile::getValue(const std::string& section, const std::string& key, std::string* value)
{
    std::string comment;
    return getValue(section, key, value, &comment);
}

int IniFile::getValue(const std::string& section, const std::string& key, std::string* value, std::string* comment)
{
    IniSection* sect = getSection(section);

    if (sect == NULL)
    {
        err_msg_ = std::string("not find the section ") + section;
        return 1;
    }

    for (IniSection::ItermIterator it = sect->begin(); it != sect->end(); ++it)
    {
        if (it->key == key)
        {
            *value   = it->value;
            *comment = it->comment;
            return 0;
        }
    }

    err_msg_ = std::string("not find the key ") + key;
    return 1;
}

void IniFile::close()
{
    ini_file_name_ = "";

    for (SectionIterator it = sections_.begin(); it != sections_.end(); ++it)
    {
        delete (*it);  // clear section
    }

    sections_.clear();
}

void IniFile::trim(std::string& str)
{
    int len = str.length();

    int i = 0;

    while ((i < len) && isspace(str[i]) && (str[i] != '\0'))
    {
        i++;
    }

    if (i != 0) { str = std::string(str, i, len - i); }

    len = str.length();

    for (i = len - 1; i >= 0; --i)
    {
        if (!isspace(str[i])) { break; }
    }

    str = std::string(str, 0, i + 1);
}

bool IniFile::split(const std::string& str, const std::string& sep, std::string* pleft, std::string* pright)
{
    size_t pos = str.find(sep);
    std::string left, right;

    if (pos != std::string::npos)
    {
        left  = std::string(str, 0, pos);
        right = std::string(str, pos + 1);

        trim(left);
        trim(right);

        *pleft  = left;
        *pright = right;
        return true;
    }
    else
    {
        left  = str;
        right = "";

        trim(left);

        *pleft  = left;
        *pright = right;
        return false;
    }
}


}  // namespace dataproxy_sdk
