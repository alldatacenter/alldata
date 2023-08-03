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

#include <Storages/PartLinker.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Disks/IDisk.h>
#include <Common/StringUtils/StringUtils.h>
#include <iostream>


namespace DB
{

/**
 * collect files to skip when hard link files to ingested new part file.
 * It is same with MergeTreeDataMergerMutator::collectFilesToSkip.
 */
NameSet PartLinker::collectFilesToSkip(
    const MergeTreeDataPartPtr & source_part,
    const Block & updated_header,
    const std::set<MergeTreeIndexPtr> & indices_to_recalc,
    const String & mrk_extension,
    const std::set<MergeTreeProjectionPtr> & projections_to_recalc,
    bool update_delete_bitmap)
{
    NameSet files_to_skip = source_part->getFileNamesWithoutChecksums();

    /// Skip updated files
    for (const auto & entry : updated_header)
    {
        ISerialization::StreamCallback callback = [&](const ISerialization::SubstreamPath & substream_path)
        {
            String stream_name = ISerialization::getFileNameForStream({entry.name, entry.type}, substream_path);
            files_to_skip.insert(stream_name + ".bin");
            files_to_skip.insert(stream_name + mrk_extension);
        };

        if (auto column = source_part->getColumns().tryGetByName(entry.name))
        {
            if (column->type->isMap() && !column->type->isMapKVStore())
            {
                Strings files = source_part->getChecksums()->collectFilesForMapColumnNotKV(column->name);
                files_to_skip.insert(files.begin(), files.end());
            }
            else
            {
                auto serialization = source_part->getSerializationForColumn({entry.name, entry.type});
                serialization->enumerateStreams(callback);
            }
        }
    }
    for (const auto & index : indices_to_recalc)
    {
        files_to_skip.insert(index->getFileName() + ".idx");
        files_to_skip.insert(index->getFileName() + mrk_extension);
    }
    for (const auto & projection : projections_to_recalc)
    {
        files_to_skip.insert(projection->getDirectoryName());
    }
    if (update_delete_bitmap)
    {
        files_to_skip.insert(DELETE_BITMAP_FILE_NAME);
    }

    return files_to_skip;
}

void PartLinker::execute()
{
    disk->createDirectories(new_part_path);
    /// Create hardlinks for unchanged files
    for (auto it = disk->iterateDirectory(source_part_path); it->isValid(); it->next())
    {
        if (files_to_skip.count(it->name()))
            continue;

        String destination = new_part_path;
        String file_name = it->name();
        auto rename_it = std::find_if(files_to_rename.begin(), files_to_rename.end(),
                        [&file_name](const auto & rename_pair) { return rename_pair.first == file_name; });

        if (rename_it != files_to_rename.end())
        {
            if (rename_it->second.empty())
                continue;
            destination += rename_it->second;
        }
        else
        {
            destination += it->name();
        }

        if (!disk->isDirectory(it->path()))
            disk->createHardLink(it->path(), destination);
        else if (!startsWith("tmp_", it->name())) // ignore projection tmp merge dir
        {
            // it's a projection part directory
            disk->createDirectories(destination);
            for (auto p_it = disk->iterateDirectory(it->path()); p_it->isValid(); p_it->next())
            {
                String p_destination = destination + "/";
                String p_file_name = p_it->name();
                p_destination += p_it->name();
                disk->createHardLink(p_it->path(), p_destination);
            }
        }
    }
}

}
