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

#include <Disks/createVolume.h>
#include <Disks/VolumeJBOD.h>


namespace DB
{

class VolumeRAID1;

using VolumeRAID1Ptr = std::shared_ptr<VolumeRAID1>;

/// Volume which reserves space on each underlying disk.
///
/// NOTE: Just interface implementation, doesn't used in codebase,
/// also not available for user.
class VolumeRAID1 : public VolumeJBOD
{
public:
    VolumeRAID1(String name_, Disks disks_, const String& default_disk_name_, UInt64 max_data_part_size_, bool are_merges_avoided_in_config_)
        : VolumeJBOD(name_, disks_, default_disk_name_, max_data_part_size_, are_merges_avoided_in_config_)
    {
    }

    VolumeRAID1(
        String name_,
        const Poco::Util::AbstractConfiguration & config,
        const String & config_prefix,
        DiskSelectorPtr disk_selector)
        : VolumeJBOD(name_, config, config_prefix, disk_selector)
    {
    }

    VolumeRAID1(
        VolumeRAID1 & volume_raid1,
        const Poco::Util::AbstractConfiguration & config,
        const String & config_prefix,
        DiskSelectorPtr disk_selector)
        : VolumeJBOD(volume_raid1, config, config_prefix, disk_selector)
    {
    }

    VolumeType getType() const override { return VolumeType::RAID1; }

    ReservationPtr reserve(UInt64 bytes) override;
};

}
