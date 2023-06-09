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

#include "createVolume.h"

#include <Disks/SingleDiskVolume.h>
#include <Disks/VolumeJBOD.h>
#include <Disks/VolumeRAID1.h>

#include <boost/algorithm/string.hpp>

namespace DB
{

namespace ErrorCodes
{
    extern const int UNKNOWN_RAID_TYPE;
    extern const int INVALID_RAID_TYPE;
}

VolumePtr createVolumeFromReservation(const ReservationPtr & reservation, VolumePtr other_volume)
{
    if (other_volume->getType() == VolumeType::JBOD || other_volume->getType() == VolumeType::SINGLE_DISK)
    {
        /// Since reservation on JBOD chooses one of disks and makes reservation there, volume
        /// for such type of reservation will be with one disk.
        return std::make_shared<SingleDiskVolume>(other_volume->getName(), reservation->getDisk(), other_volume->max_data_part_size);
    }
    if (other_volume->getType() == VolumeType::RAID1)
    {
        auto volume = std::dynamic_pointer_cast<VolumeRAID1>(other_volume);
        Disks disks = reservation->getDisks();
        String default_disk_name = disks.empty() ? "" : disks[0]->getName();
        return std::make_shared<VolumeRAID1>(volume->getName(), disks, default_disk_name, volume->max_data_part_size, volume->are_merges_avoided);
    }
    return nullptr;
}

VolumePtr createVolumeFromConfig(
    String name,
    const Poco::Util::AbstractConfiguration & config,
    const String & config_prefix,
    DiskSelectorPtr disk_selector
)
{
    String raid_type = config.getString(config_prefix + ".raid_type", "JBOD");
    if (raid_type == "JBOD")
    {
        return std::make_shared<VolumeJBOD>(name, config, config_prefix, disk_selector);
    }
    throw Exception("Unknown RAID type '" + raid_type + "'", ErrorCodes::UNKNOWN_RAID_TYPE);
}

VolumePtr updateVolumeFromConfig(
    VolumePtr volume,
    const Poco::Util::AbstractConfiguration & config,
    const String & config_prefix,
    DiskSelectorPtr & disk_selector
)
{
    String raid_type = config.getString(config_prefix + ".raid_type", "JBOD");
    if (raid_type == "JBOD")
    {
        VolumeJBODPtr volume_jbod = std::dynamic_pointer_cast<VolumeJBOD>(volume);
        if (!volume_jbod)
            throw Exception("Invalid RAID type '" + raid_type + "', shall be JBOD", ErrorCodes::INVALID_RAID_TYPE);

        return std::make_shared<VolumeJBOD>(*volume_jbod, config, config_prefix, disk_selector);
    }
    throw Exception("Unknown RAID type '" + raid_type + "'", ErrorCodes::UNKNOWN_RAID_TYPE);
}

}
