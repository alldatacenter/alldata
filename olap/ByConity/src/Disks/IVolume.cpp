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

#include "IVolume.h"

#include <Common/StringUtils/StringUtils.h>
#include <Common/quoteString.h>

#include <memory>

namespace DB
{
namespace ErrorCodes
{
    extern const int NO_ELEMENTS_IN_CONFIG;
    extern const int INCONSISTENT_RESERVATIONS;
    extern const int NO_RESERVATIONS_PROVIDED;
    extern const int UNKNOWN_VOLUME_TYPE;
}

String volumeTypeToString(VolumeType type)
{
    switch (type)
    {
        case VolumeType::JBOD:
            return "JBOD";
        case VolumeType::RAID1:
            return "RAID1";
        case VolumeType::SINGLE_DISK:
            return "SINGLE_DISK";
        case VolumeType::UNKNOWN:
            return "UNKNOWN";
    }
    throw Exception("Unknown volume type, please add it to DB::volumeTypeToString", ErrorCodes::UNKNOWN_VOLUME_TYPE);
}

IVolume::IVolume(
    String name_,
    const Poco::Util::AbstractConfiguration & config,
    const String & config_prefix,
    DiskSelectorPtr disk_selector)
    : name(std::move(name_))
{
    Poco::Util::AbstractConfiguration::Keys keys;
    config.keys(config_prefix, keys);

    for (const auto & disk : keys)
    {
        if (startsWith(disk, "disk"))
        {
            auto disk_name = config.getString(config_prefix + "." + disk);
            disks.push_back(disk_selector->get(disk_name));
        }
    }

    if (config.has(config_prefix + ".default"))
    {
        default_disk_name = config.getString(config_prefix + ".default");
    }

    if (disks.empty())
        throw Exception("Volume must contain at least one disk", ErrorCodes::NO_ELEMENTS_IN_CONFIG);
}

DiskPtr IVolume::getDefaultDisk() const
{
    for (const DiskPtr& disk : disks)
    {
        if (disk->getName() == default_disk_name)
        {
            return disk;
        }
    }
    throw Exception("Default disk " + default_disk_name + " not exists",
        ErrorCodes::LOGICAL_ERROR);
}

UInt64 IVolume::getMaxUnreservedFreeSpace() const
{
    UInt64 res = 0;
    for (const auto & disk : disks)
        res = std::max(res, disk->getUnreservedSpace());
    return res;
}

MultiDiskReservation::MultiDiskReservation(Reservations & reservations_, UInt64 size_)
    : reservations(std::move(reservations_))
    , size(size_)
{
    if (reservations.empty())
    {
        throw Exception("At least one reservation must be provided to MultiDiskReservation", ErrorCodes::NO_RESERVATIONS_PROVIDED);
    }

    for (auto & reservation : reservations)
    {
        if (reservation->getSize() != size_)
        {
            throw Exception("Reservations must have same size", ErrorCodes::INCONSISTENT_RESERVATIONS);
        }
    }
}

Disks MultiDiskReservation::getDisks() const
{
    Disks res;
    res.reserve(reservations.size());
    for (const auto & reservation : reservations)
    {
        res.push_back(reservation->getDisk());
    }
    return res;
}

void MultiDiskReservation::update(UInt64 new_size)
{
    for (auto & reservation : reservations)
    {
        reservation->update(new_size);
    }
    size = new_size;
}


}
