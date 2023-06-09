/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <Parsers/parseQuery.h>
#include <Parsers/ParserPartToolkitQuery.h>
#include <iostream>
#include <string>

int main(int argc, char ** argv)
{
    if (argc < 2)
    {
        std::cerr << "Error! No input part writer query." << std::endl;
        return -1;
    }

    std::string query = argv[1];

    const char * begin = query.data();
    const char * end =  query.data() + query.size();

    DB::ParserPartToolkitQuery parser(end);

    auto ast = DB::parseQuery(parser, begin, end, "", 10000, 100);

    return 0;
}
