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

#include <FormaterTool/PartToolkitBase.h>
#include <FormaterTool/ZipHelper.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTPartToolKit.h>
#include <Interpreters/InterpreterCreateQuery.h>
#include <Interpreters/Context.h>
#include <Storages/StorageFactory.h>
#include <Storages/StorageMergeTree.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INCORRECT_QUERY;
}

PartToolkitBase::PartToolkitBase(const ASTPtr & query_ptr_, ContextMutablePtr context_)
    : WithMutableContext(context_), query_ptr(query_ptr_)
{

}

PartToolkitBase::~PartToolkitBase()
{
}

void PartToolkitBase::applySettings()
{
    auto & mergetree_settings = const_cast<MergeTreeSettings &>(getContext()->getMergeTreeSettings());
    auto & settings = const_cast<Settings &>(getContext()->getSettingsRef());

    /*** apply some default settings ***/
    mergetree_settings.set("enable_metastore", false);
    // pw specific settings
    mergetree_settings.set("min_rows_for_compact_part", 0);
    mergetree_settings.set("min_bytes_for_compact_part", 0);
    mergetree_settings.set("enable_local_disk_cache",0);
    settings.set("input_format_skip_unknown_fields", true);
    settings.set("skip_nullinput_notnull_col", true);


    /// apply user defined settings.
    const ASTPartToolKit & pw_query = query_ptr->as<ASTPartToolKit &>();
    if (pw_query.settings)
    {
        const ASTSetQuery & set_ast = pw_query.settings->as<ASTSetQuery &>();
        for (auto & change : set_ast.changes)
        {
            if (settings.has(change.name))
                settings.set(change.name, change.value);
            else if (mergetree_settings.has(change.name))
                mergetree_settings.set(change.name, change.value);
            else if (change.name == "hdfs_user")
                getContext()->setHdfsUser(change.value.safeGet<String>());
            else if (change.name == "hdfs_nnproxy")
                getContext()->setHdfsNNProxy(change.value.safeGet<String>());
            else
                user_settings.emplace(change.name, change.value);
        }
    }
}

StoragePtr PartToolkitBase::getTable()
{
    if (storage)
        return storage;
    else
    {
        const ASTPartToolKit & pw_query = query_ptr->as<ASTPartToolKit &>();
        ASTPtr create_query = pw_query.create_query;
        auto & create = create_query->as<ASTCreateQuery &>();

        if (!create.storage || !create.columns_list)
            throw Exception("Wrong create query.", ErrorCodes::INCORRECT_QUERY);

        ColumnsDescription columns = InterpreterCreateQuery::getColumnsDescription(*create.columns_list->columns, getContext(), create.attach);
        ConstraintsDescription constraints = InterpreterCreateQuery::getConstraintsDescription(create.columns_list->constraints);

        StoragePtr res = StorageFactory::instance().get(create,
            PT_RELATIVE_LOCAL_PATH,
            getContext(),
            getContext()->getGlobalContext(),
            columns,
            constraints,
            false);

        storage = res;
        return res;
    }
}


PartNamesWithDisks PartToolkitBase::collectPartsFromSource(const String & source_dirs_str, const String & dest_dir)
{
    std::vector<String> source_dirs;
    /// parse all source directories from input path string;
    size_t begin_pos = 0;
    size_t len = 0;
    auto pos = source_dirs_str.find(",", begin_pos);
    while (pos != source_dirs_str.npos)
    {
        len = pos - begin_pos;
        if (len > 0)
        {
            source_dirs.emplace_back(source_dirs_str.substr(begin_pos, len));
        }
        begin_pos = pos + 1;
        pos = source_dirs_str.find(",", begin_pos);
    }
    if (begin_pos < source_dirs_str.size())
    {
        source_dirs.emplace_back(source_dirs_str.substr(begin_pos));
    }

    if (!fs::exists(dest_dir))
        fs::create_directories(dest_dir);

    PartNamesWithDisks res;
    auto disk_ptr = getContext()->getDisk("default");
    ZipHelper ziphelper;

    Poco::DirectoryIterator dir_end;
    for (auto & dir : source_dirs)
    {
        for (Poco::DirectoryIterator it(dir); it != dir_end; ++it)
        {
            const String & file_name = it.name();
            if (it->isFile() && endsWith(file_name, ".zip"))
            {
                String part_name = file_name.substr(0, file_name.find(".zip"));
                fs::rename(dir + file_name, dest_dir + file_name);
                ziphelper.unzipFile(dest_dir + file_name, dest_dir + part_name);
                res.emplace_back(part_name, disk_ptr);
            }
        }
    }

    return res;
}

}
