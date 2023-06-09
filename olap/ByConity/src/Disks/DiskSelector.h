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

#pragma once

#include <Disks/DiskFactory.h>
#include <Disks/IDisk.h>

#include <Poco/Util/AbstractConfiguration.h>

#include <map>

namespace DB
{

class DiskSelector;
using DiskSelectorPtr = std::shared_ptr<const DiskSelector>;
using DisksMap = std::map<String, DiskPtr>;
using DiskIDMap = std::map<UInt64, DiskPtr>;
using DisksInfo = std::map<UInt64, std::pair<String, String>>;

/// Parse .xml configuration and store information about disks
/// Mostly used for introspection.
class DiskSelector
{
public:
    DiskSelector(const Poco::Util::AbstractConfiguration & config, const String & config_prefix, ContextPtr context);
    DiskSelector(const DiskSelector & from) : disks(from.disks), id_to_disks(from.id_to_disks) { }

    DiskSelectorPtr updateFromConfig(
        const Poco::Util::AbstractConfiguration & config,
        const String & config_prefix,
        ContextPtr context
    ) const;

    /// Get disk by name
    DiskPtr get(const String & name) const;

    DiskPtr getByID(const UInt64 & disk_id) const;

    /// Get all disks with names
    const DisksMap & getDisksMap() const { return disks; }
    void addToDiskMap(String name, DiskPtr disk);

    /// save information of current disks into file.
    void flushDiskInfo() const;

    /// load disk info from file. help to check uniqueness of disk id
    DisksInfo loadDiskInfo() const;


private:
    fs::path disks_path;
    DisksMap disks;
    DiskIDMap id_to_disks;
};

}
