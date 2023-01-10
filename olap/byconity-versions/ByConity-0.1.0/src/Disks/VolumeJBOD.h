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

#include <memory>
#include <optional>

#include <Disks/IVolume.h>


namespace DB
{

class VolumeJBOD;

using VolumeJBODPtr = std::shared_ptr<VolumeJBOD>;
using VolumesJBOD = std::vector<VolumeJBODPtr>;

/**
 * Implements something similar to JBOD (https://en.wikipedia.org/wiki/Non-RAID_drive_architectures#JBOD).
 * When MergeTree engine wants to write part — it requests VolumeJBOD to reserve space on the next available
 * disk and then writes new part to that disk.
 */
class VolumeJBOD : public IVolume
{
public:
    VolumeJBOD(String name_, Disks disks_, const String& default_disk_name_, UInt64 max_data_part_size_, bool are_merges_avoided_)
        : IVolume(name_, disks_, default_disk_name_, max_data_part_size_)
        , are_merges_avoided(are_merges_avoided_)
    {
    }

    VolumeJBOD(
        String name_,
        const Poco::Util::AbstractConfiguration & config,
        const String & config_prefix,
        DiskSelectorPtr disk_selector
    );

    VolumeJBOD(
        const VolumeJBOD & volume_jbod,
        const Poco::Util::AbstractConfiguration & config,
        const String & config_prefix,
        DiskSelectorPtr disk_selector
    );

    VolumeType getType() const override { return VolumeType::JBOD; }

    /// Always returns next disk (round-robin), ignores argument.
    ///
    /// - Used with policy for temporary data
    /// - Ignores all limitations
    /// - Shares last access with reserve()
    DiskPtr getDisk(size_t index) const override;

    /// Uses Round-robin to choose disk for reservation.
    /// Returns valid reservation or nullptr if there is no space left on any disk.
    ReservationPtr reserve(UInt64 bytes) override;

    bool areMergesAvoided() const override;

    void setAvoidMergesUserOverride(bool avoid) override;

    /// True if parts on this volume participate in merges according to configuration.
    bool are_merges_avoided = true;

private:
    /// Index of last used disk.
    mutable std::atomic<size_t> last_used = 0;

    /// True if parts on this volume participate in merges according to START/STOP MERGES ON VOLUME.
    std::atomic<std::optional<bool>> are_merges_avoided_user_override{std::nullopt};
};

}
