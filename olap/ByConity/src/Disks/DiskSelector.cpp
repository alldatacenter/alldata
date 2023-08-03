/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include "DiskLocal.h"
#include "DiskSelector.h"

#include <IO/WriteHelpers.h>
#include <IO/ReadBufferFromFile.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromFile.h>
#include <Common/escapeForFileName.h>
#include <Common/quoteString.h>
#include <common/logger_useful.h>
#include <Interpreters/Context.h>

#include <set>

#define ALL_DISK_INFO_FILE  "DISK_INFOS"
namespace DB
{

namespace ErrorCodes
{
    extern const int EXCESSIVE_ELEMENT_IN_CONFIG;
    extern const int UNKNOWN_DISK;
    extern const int DISK_ID_NOT_UNIQUE;
}

DiskSelector::DiskSelector(const Poco::Util::AbstractConfiguration & config, const String & config_prefix, ContextPtr context)
{
    disks_path = fs::path(context->getPath()) / "disks/";

    auto disks_info = loadDiskInfo();

    Poco::Util::AbstractConfiguration::Keys keys;
    config.keys(config_prefix, keys);

    auto & factory = DiskFactory::instance();

    constexpr auto default_disk_name = "default";
    bool has_default_disk = false;
    for (const auto & disk_name : keys)
    {
        if (!std::all_of(disk_name.begin(), disk_name.end(), isWordCharASCII))
            throw Exception("Disk name can contain only alphanumeric and '_' (" + disk_name + ")", ErrorCodes::EXCESSIVE_ELEMENT_IN_CONFIG);

        if (disk_name == default_disk_name)
            has_default_disk = true;

        auto disk_config_prefix = config_prefix + "." + disk_name;

        disks.emplace(disk_name, factory.create(disk_name, config, disk_config_prefix, context));
    }
    if (!has_default_disk)
        disks.emplace(default_disk_name, std::make_shared<DiskLocal>(default_disk_name, context->getPath(), 0));

    /// fill id_to_disks.
    for (auto & [ _, disk_ptr] : disks)
    {
        UInt64 disk_id = disk_ptr->getID();
        auto found = disks_info.find(disk_id);
        if (found!=disks_info.end())
        {
            /// check uniqueness of disk id.
            if (found->second.first != DiskType::toString(disk_ptr->getType()) || found->second.second != disk_ptr->getPath())
                throw Exception(ErrorCodes::DISK_ID_NOT_UNIQUE, "Disk '{}' has conflict id({}) with another disk which has path : {}",
                            disk_ptr->getName(), found->first, found->second.second);
        }

        if (id_to_disks.count(disk_id))
            throw Exception(ErrorCodes::DISK_ID_NOT_UNIQUE, "Cannot add disk '{}' because disk with the same id({}) already exists.", disk_ptr->getName(), disk_id);
        id_to_disks.emplace(disk_id, disk_ptr);
    }

    /// write disk infos into file
    flushDiskInfo();
}


DiskSelectorPtr DiskSelector::updateFromConfig(
    const Poco::Util::AbstractConfiguration & config, const String & config_prefix, ContextPtr context) const
{
    Poco::Util::AbstractConfiguration::Keys keys;
    config.keys(config_prefix, keys);

    auto & factory = DiskFactory::instance();

    std::shared_ptr<DiskSelector> result = std::make_shared<DiskSelector>(*this);

    constexpr auto default_disk_name = "default";
    DisksMap old_disks_minus_new_disks (result->getDisksMap());

    for (const auto & disk_name : keys)
    {
        if (!std::all_of(disk_name.begin(), disk_name.end(), isWordCharASCII))
            throw Exception("Disk name can contain only alphanumeric and '_' (" + disk_name + ")", ErrorCodes::EXCESSIVE_ELEMENT_IN_CONFIG);

        if (result->getDisksMap().count(disk_name) == 0)
        {
            auto disk_config_prefix = config_prefix + "." + disk_name;
            result->addToDiskMap(disk_name, factory.create(disk_name, config, disk_config_prefix, context));
        }
        else
        {
            auto disk = old_disks_minus_new_disks[disk_name];

            disk->applyNewSettings(config, context);

            old_disks_minus_new_disks.erase(disk_name);
        }
    }

    old_disks_minus_new_disks.erase(default_disk_name);

    if (!old_disks_minus_new_disks.empty())
    {
        WriteBufferFromOwnString warning;
        if (old_disks_minus_new_disks.size() == 1)
            writeString("Disk ", warning);
        else
            writeString("Disks ", warning);

        int index = 0;
        for (const auto & [name, _] : old_disks_minus_new_disks)
        {
            if (index++ > 0)
                writeString(", ", warning);
            writeBackQuotedString(name, warning);
        }

        writeString(" disappeared from configuration, this change will be applied after restart of ClickHouse", warning);
        LOG_WARNING(&Poco::Logger::get("DiskSelector"), warning.str());
    }

    result->flushDiskInfo();

    return result;
}

void DiskSelector::addToDiskMap(String name, DiskPtr disk)
{
    UInt64 disk_id = disk->getID();
    auto found = id_to_disks.find(disk_id);
    if (found != id_to_disks.end())
    {
        if (found->second->getPath() != disk->getPath() || found->second->getType() != disk->getType())
            throw Exception(ErrorCodes::DISK_ID_NOT_UNIQUE, "Disk '{}' has conflict id({}) with existing disk '{}'.", disk->getName(), disk_id, found->second->getName());
    }
    disks.emplace(name, disk);
    id_to_disks.emplace(disk_id, disk);
}


DiskPtr DiskSelector::get(const String & name) const
{
    auto it = disks.find(name);
    if (it == disks.end())
        throw Exception("Unknown disk " + name, ErrorCodes::UNKNOWN_DISK);
    return it->second;
}

DiskPtr DiskSelector::getByID(const UInt64 & disk_id) const
{
    auto it = id_to_disks.find(disk_id);
    if (it == id_to_disks.end())
        throw Exception("Unknown disk with id " + std::to_string(disk_id), ErrorCodes::UNKNOWN_DISK);
    return it->second;
}

void DiskSelector::flushDiskInfo() const
{
    auto tmp_path = disks_path / (String(ALL_DISK_INFO_FILE) + ".tmp");
    fs::remove(tmp_path);
    {
        WriteBufferFromFile buf(tmp_path, 1024);

        writeIntText(id_to_disks.size(), buf);
        writeChar('\n', buf);

        for (auto it=id_to_disks.begin(); it!=id_to_disks.end(); it++)
        {
            writeIntText(it->first, buf);
            writeChar('\t', buf);
            writeEscapedString(DiskType::toString(it->second->getType()), buf);
            writeChar('\t', buf);
            writeEscapedString(it->second->getPath(), buf);
            writeChar('\n', buf);
        }

        buf.finalize();
        buf.sync();
    }

    auto disk_info_path = disks_path / ALL_DISK_INFO_FILE;
    if (fs::exists(disk_info_path))
        fs::remove(disk_info_path);

    fs::rename(tmp_path, disk_info_path);
}

DisksInfo DiskSelector::loadDiskInfo() const
{
    DisksInfo disk_info;

    auto disk_info_path = disks_path / ALL_DISK_INFO_FILE;
    if (fs::exists(disk_info_path))
    {
        ReadBufferFromFile buf(disk_info_path, 1024);
        size_t disk_count;
        readIntText(disk_count, buf);
        assertChar('\n', buf);

        for (size_t i=0; i<disk_count; i++)
        {
            UInt64 id;
            String type_str, path;
            readIntText(id, buf);
            assertChar('\t', buf);
            readEscapedString(type_str, buf);
            assertChar('\t', buf);
            readEscapedString(path, buf);
            assertChar('\n', buf);

            disk_info.emplace(id, std::make_pair(type_str, path));
        }
    }
    return disk_info;
}

}
