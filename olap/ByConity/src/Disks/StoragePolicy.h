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

#include <Disks/IStoragePolicy.h>
#include <Disks/DiskSelector.h>
#include <Disks/IDisk.h>
#include <Disks/IVolume.h>
#include <Disks/VolumeJBOD.h>
#include <Disks/VolumeRAID1.h>
#include <Disks/SingleDiskVolume.h>
#include <IO/WriteHelpers.h>
#include <Common/CurrentMetrics.h>
#include <Common/Exception.h>
#include <Common/formatReadable.h>
#include <common/logger_useful.h>

#include <memory>
#include <mutex>
#include <unordered_map>
#include <unistd.h>
#include <boost/noncopyable.hpp>
#include <Poco/Util/AbstractConfiguration.h>


namespace DB
{

/**
 * Contains all information about volumes configuration for Storage.
 * Can determine appropriate Volume and Disk for each reservation.
 */
class StoragePolicy : public IStoragePolicy
{
public:
    StoragePolicy(String name_, const Poco::Util::AbstractConfiguration & config, const String & config_prefix, DiskSelectorPtr disks);

    StoragePolicy(String name_, Volumes volumes_, double move_factor_);

    StoragePolicy(
        StoragePolicyPtr storage_policy,
        const Poco::Util::AbstractConfiguration & config,
        const String & config_prefix,
        DiskSelectorPtr disks
    );

    bool isDefaultPolicy() const override;

    /// Returns disks ordered by volumes priority
    Disks getDisks() const override;

    /// Returns disks by type ordered by volumes priority
    Disks getDisksByType(DiskType::Type type) const override;

    /// Returns any disk
    /// Used when it's not important, for example for
    /// mutations files
    DiskPtr getAnyDisk() const override;

    DiskPtr getDiskByName(const String & disk_name) const override;

    DiskPtr getDiskByID(const UInt64 & disk_id) const override;

    /// Get free space from most free disk
    UInt64 getMaxUnreservedFreeSpace() const override;

    const String & getName() const override{ return name; }

    /// Returns valid reservation or nullptr
    ReservationPtr reserve(UInt64 bytes) const override;

    /// Reserves space on any volume or throws
    ReservationPtr reserveAndCheck(UInt64 bytes) const override;

    /// Reserves space on any volume with index > min_volume_index or returns nullptr
    ReservationPtr reserve(UInt64 bytes, size_t min_volume_index) const override;

    /// Find volume index, which contains disk
    size_t getVolumeIndexByDisk(const DiskPtr & disk_ptr) const override;

    /// Reserves 0 bytes on disk with max available space
    /// Do not use this function when it is possible to predict size.
    ReservationPtr makeEmptyReservationOnLargestDisk() const override;

    const Volumes & getVolumes() const  override{ return volumes; }

    /// Returns number [0., 1.] -- fraction of free space on disk
    /// which should be kept with help of background moves
    double getMoveFactor() const  override{ return move_factor; }

    /// Get volume by index.
    VolumePtr getVolume(size_t index) const override;

    VolumePtr getVolumeByName(const String & volume_name) const override;

    /// Checks if storage policy can be replaced by another one.
    void checkCompatibleWith(const StoragePolicyPtr & new_storage_policy) const override;

    /// Check if we have any volume with stopped merges
    bool hasAnyVolumeWithDisabledMerges() const override;

    bool containsVolume(const String & volume_name) const override;
private:
    Volumes volumes;
    const String name;
    std::unordered_map<String, size_t> volume_index_by_volume_name;
    std::unordered_map<String, size_t> volume_index_by_disk_name;

    /// move_factor from interval [0., 1.]
    /// We move something if disk from this policy
    /// filled more than total_size * move_factor
    double move_factor = 0.1; /// by default move factor is 10%

    void buildVolumeIndices();
};


class StoragePolicySelector;
using StoragePolicySelectorPtr = std::shared_ptr<const StoragePolicySelector>;
using StoragePoliciesMap = std::map<String, StoragePolicyPtr>;

/// Parse .xml configuration and store information about policies
/// Mostly used for introspection.
class StoragePolicySelector
{
public:
    StoragePolicySelector(const Poco::Util::AbstractConfiguration & config, const String & config_prefix,
        DiskSelectorPtr disks, const String& default_cnch_policy_name);

    StoragePolicySelectorPtr updateFromConfig(const Poco::Util::AbstractConfiguration & config, const String & config_prefix, DiskSelectorPtr disks, const String& default_cnch_policy_name) const;

    /// Policy by name
    StoragePolicyPtr get(const String & name) const;

    /// All policies
    const StoragePoliciesMap & getPoliciesMap() const { return policies; }

private:
    StoragePoliciesMap policies;
};

}
