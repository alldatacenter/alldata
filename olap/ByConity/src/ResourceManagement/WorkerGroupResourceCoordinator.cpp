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

#include <ResourceManagement/WorkerGroupResourceCoordinator.h>
#include <Common/Configurations.h>
#include <ResourceManagement/ResourceManagerController.h>
#include <ResourceManagement/VirtualWarehouse.h>
#include <ResourceManagement/VirtualWarehouseManager.h>
#include <ResourceManagement/IWorkerGroup.h>
#include <ResourceManagement/WorkerGroupManager.h>

#include <common/logger_useful.h>

namespace DB
{
namespace ErrorCodes
{
    const extern int LOGICAL_ERROR;
    const extern int VIRTUAL_WAREHOUSE_NOT_FOUND;
}
}

namespace DB::ResourceManagement
{

WorkerGroupResourceCoordinator::WorkerGroupResourceCoordinator(ResourceManagerController & rm_controller_)
    : rm_controller(rm_controller_)
    , log(&Poco::Logger::get("WorkerGroupResourceCoordinator"))
    , background_task(rm_controller.getContext()->getSchedulePool().createTask("WorkerGroupResourceCoordinator", [&]() { run(); }))
    , task_interval_ms(rm_controller.getContext()->getRootConfig().resource_manager.auto_resource_sharing_task_interval_ms)
    {
    }

void WorkerGroupResourceCoordinator::start()
{
    LOG_DEBUG(log, "Activating auto-sharing background task");
    background_task->activateAndSchedule();
}

void WorkerGroupResourceCoordinator::stop()
{
    LOG_DEBUG(log, "Dectivating auto-sharing background task");
    background_task->deactivate();
}

WorkerGroupResourceCoordinator::~WorkerGroupResourceCoordinator()
{
    try
    {
        LOG_TRACE(log, "Stopping WorkerGroupResourceCoordinator");
        stop();
    }
    catch (...)
    {
        tryLogCurrentException(log);
    }
}

void WorkerGroupResourceCoordinator::unlinkBusyAndOverlentGroups(const VirtualWarehousePtr & vw, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    auto lent_groups = vw->getLentGroups();
    auto num_lent_groups = vw->getNumLentGroups();

    for (const auto & lent_group : lent_groups)
    {
        auto physical_group = dynamic_pointer_cast<PhysicalWorkerGroup>(lent_group);
        if (!physical_group)
        {
            LOG_ERROR(log, "Lent group is not of Physical type");
            continue;
        }
        auto group_metrics = lent_group->getAggregatedMetrics();
        auto vw_settings = vw->getSettings();
        auto drop_worker_group = [this, &vw, &vw_settings, &group_metrics, num_lent_groups, &vw_lock, &wg_lock] (const String & lent_group_id)
            {
                LOG_DEBUG(
                    log,
                    "Unlending shared group " + lent_group_id + " from vw " + vw->getName() + " due to resource limitations \
                        \nCurrent/max number of lent groups: "
                        + std::to_string(num_lent_groups) + "/" + std::to_string(vw_settings.max_auto_lend_links)
                        + "\nCurrent/threshold CPU usage: " + std::to_string(group_metrics.max_cpu_usage) + "/"
                        + std::to_string(vw_settings.cpu_threshold_for_recall) + "\nCurrent/threshold mem usage: "
                        + std::to_string(group_metrics.max_mem_usage) + "/" + std::to_string(vw_settings.mem_threshold_for_recall));
                rm_controller.dropWorkerGroup(lent_group_id, true, vw_lock, wg_lock);
        };

        if (num_lent_groups  > vw_settings.max_auto_lend_links)
        {
            //Return overlent groups
            auto lent_groups_ids = physical_group->getLentGroupsDestIDs();

            auto lent_group_id_it = lent_groups_ids.begin();

            // Only return excess groups
            for (size_t i = 0; i < num_lent_groups - vw_settings.max_auto_lend_links; ++i)
            {
                auto lent_group_id = *lent_group_id_it;
                drop_worker_group(lent_group_id);
                ++lent_group_id_it;
            }
        }
        else if (group_metrics.max_cpu_usage >= vw_settings.cpu_threshold_for_recall
            || group_metrics.max_mem_usage >= vw_settings.mem_threshold_for_recall)
        {
            // Return busy groups
            auto lent_groups_ids = physical_group->getLentGroupsDestIDs();
            for (const auto & lent_group_id : lent_groups_ids)
            {
                drop_worker_group(lent_group_id);
            }
        }
    }
}

void WorkerGroupResourceCoordinator::unlinkOverborrowedGroups(const VirtualWarehousePtr & vw, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    const auto & borrowed_groups = vw->getBorrowedGroups();

    auto num_borrowed_groups = borrowed_groups.size();
    auto max_auto_borrow_links = vw->getSettings().max_auto_borrow_links;
    // Unborrow groups if over-borrowed
    if (num_borrowed_groups > max_auto_borrow_links)
    {
        auto borrowed_it = borrowed_groups.begin();
        for (size_t i = 0; i < num_borrowed_groups - max_auto_borrow_links; i++)
        {
            auto borrowed_group = *borrowed_it;

            LOG_DEBUG(log, "Unborrowing group " + borrowed_group->getID() + " from vw " + vw->getName() + " due to overborrowing. \
                    \nCurrent/max number of borrowed groups: " + std::to_string(num_borrowed_groups) + "/" + std::to_string(max_auto_borrow_links));

            rm_controller.dropWorkerGroup(borrowed_group->getID(), true, vw_lock, wg_lock);

            ++borrowed_it;
        }
    }

}

void WorkerGroupResourceCoordinator::unlinkIneligibleGroups(const std::unordered_map<String, VirtualWarehousePtr> & vws, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    for (const auto & [name, vw] : vws)
    {
        unlinkBusyAndOverlentGroups(vw, vw_lock, wg_lock);
        unlinkOverborrowedGroups(vw, vw_lock, wg_lock);
    }
}

void WorkerGroupResourceCoordinator::getEligibleGroups(std::unordered_map<String, VirtualWarehousePtr> & vws, std::vector<VirtualWarehousePtr> & eligible_vw_borrowers, std::vector<WorkerGroupPtr> & eligible_wg_lenders, size_t & borrow_slots, size_t & lend_slots)
{
    for (const auto & vw_it : vws)
    {
        auto & vw = vw_it.second;
        auto vw_settings = vw->getSettings();
        const auto & borrowed_groups = vw->getBorrowedGroups();

        size_t time_now = time(nullptr);
        bool require_borrowing = borrowed_groups.size() < vw_settings.max_auto_borrow_links
                && (vw->getLastBorrowTimestamp() == 0
                || vw->getLastBorrowTimestamp() + vw_settings.cooldown_seconds_after_auto_link <= time_now);

        if (require_borrowing)
        {
            for (const auto & wg : vw->getNonborrowedGroups())
            {
                auto wg_data = wg->getAggregatedMetrics();
                // TODO: Update with more configurable strategy
                if (wg_data.max_cpu_usage < vw_settings.cpu_threshold_for_borrow
                    && wg_data.max_mem_usage < vw_settings.mem_threshold_for_borrow)
                {
                    // If there is at least one free worker group, then we do not require borrowing
                    require_borrowing = false;
                    break;
                }
            }
        }

        if (!require_borrowing)
        {
            if (borrowed_groups.empty() && vw->getNumLentGroups() < vw_settings.max_auto_lend_links
                    && (vw->getLastLendTimestamp() == 0
                        || (vw->getLastLendTimestamp() + vw_settings.cooldown_seconds_after_auto_unlink <= time_now)))
            {
                for (const auto & wg : vw->getNonborrowedGroups())
                {
                    auto wg_data = wg->getAggregatedMetrics();
                    // Offer worker group for lending if eligible
                    if (wg_data.max_cpu_usage < vw_settings.cpu_threshold_for_lend
                        && wg_data.max_mem_usage < vw_settings.mem_threshold_for_lend)
                    {
                        eligible_wg_lenders.push_back(wg);
                        ++lend_slots;
                    }
                }
            }
        }
        else
        {
            // Add to borrower list if eligible
            eligible_vw_borrowers.push_back(vw);
            borrow_slots += vw_settings.max_auto_borrow_links - borrowed_groups.size();
        }
    }
}

void WorkerGroupResourceCoordinator::linkEligibleGroups(std::unordered_map<String, VirtualWarehousePtr> & vws, std::vector<VirtualWarehousePtr> & eligible_vw_borrowers, std::vector<WorkerGroupPtr> & eligible_wg_lenders, size_t & borrow_slots, size_t & lend_slots, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    if (borrow_slots == 0 || lend_slots == 0)
        return;

    size_t lender_limit_per_vw = (lend_slots >= borrow_slots) ? std::numeric_limits<size_t>::max() : eligible_wg_lenders.size() / eligible_vw_borrowers.size();
    size_t lender_remainder = eligible_wg_lenders.size() % eligible_vw_borrowers.size();

    auto borrow_it = eligible_vw_borrowers.begin();
    auto lend_it = eligible_wg_lenders.begin();

    // Use borrower index for issuing of remainder lenders
    size_t vw_idx = 0;
    auto suffix = UUIDHelpers::UUIDToString(UUIDHelpers::generateV4()).substr(0, 8);

    // Iterate through all eligible VW borrowers and WG lenders
    while (lend_it != eligible_wg_lenders.end() && borrow_it != eligible_vw_borrowers.end())
    {
        auto & vw = *borrow_it;
        auto vw_settings = vw->getSettings();
        const auto & borrowed_groups = vw->getBorrowedGroups();
        size_t borrow_count = vw_settings.max_auto_borrow_links - borrowed_groups.size();
        // Distribute WG lenders (including remainder) among borrowers
        if (vw_idx < lender_remainder)
            borrow_count = std::min(borrow_count, lender_limit_per_vw + 1);
        else
            borrow_count = std::min(borrow_count, lender_limit_per_vw);

        bool borrowed = false;

        // Borrow up to borrow_count WG lenders for each VW
        while (borrow_count > 0 && lend_it != eligible_wg_lenders.end())
        {
            WorkerGroupPtr wg;
            String wg_vw_name;
            VirtualWarehousePtr lender_vw;

            // Retrieve next eligible WG lender
            do
            {
                if (lend_it == eligible_wg_lenders.end())
                    return;
                wg = *lend_it;
                wg_vw_name = wg->getVWName();
                auto wg_vw_it = vws.find(wg_vw_name);
                if (wg_vw_it == vws.end())
                    throw Exception("Virtual warehouse `" + wg_vw_name + "` not found.", ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND);
                lender_vw = wg_vw_it->second;
                if (lender_vw->getNumLentGroups() >= lender_vw->getSettings().max_auto_lend_links)
                    ++lend_it;
            } while (lender_vw->getNumLentGroups() >= lender_vw->getSettings().max_auto_lend_links);


            // Get eligible linked group name
            try
            {
                String orig_linked_group_id = vw->getName() + linked_group_infix + wg->getID();
                String linked_group_id = orig_linked_group_id + "_" + suffix;
                auto existing_grp = rm_controller.getWorkerGroupManager().tryGetWorkerGroupImpl(linked_group_id, vw_lock, wg_lock);
                if (existing_grp)
                {
                    while (existing_grp)
                    {
                        auto prev_linked_group_id = linked_group_id;
                        linked_group_id = orig_linked_group_id + "_" + suffix;
                        LOG_DEBUG(log, "Linked worker group " + prev_linked_group_id + " already exists. Retrying with " + linked_group_id);
                        suffix = UUIDHelpers::UUIDToString(UUIDHelpers::generateV4()).substr(0, 8);
                        existing_grp = rm_controller.getWorkerGroupManager().tryGetWorkerGroupImpl(linked_group_id, vw_lock, wg_lock);
                    }
                }

                // Perform creation of auto-linked WG and link to VW
                WorkerGroupData wg_data;
                wg_data.id = linked_group_id;
                wg_data.type = WorkerGroupType::Shared;
                wg_data.vw_uuid = vw->getUUID();
                wg_data.vw_name = vw->getName();
                wg_data.linked_id = wg->getID();
                wg_data.is_auto_linked = true;
                auto linked_group = rm_controller.createWorkerGroup(linked_group_id, false, vw->getName(), wg_data, vw_lock, wg_lock);
                borrowed = true;
                auto time_now = time(nullptr);
                lender_vw->setLastLendTimestamp(time_now);
                --borrow_count;
            }
            catch (const Exception & e)
            {
                LOG_DEBUG(log, "Unable to link group due to " + e.displayText() + ", skipping " + wg->getID());
            }
            ++lend_it;
        }

        // Update VW's last borrowed timestamp
        if (borrowed)
        {
            auto time_now = time(nullptr);
            vw->setLastBorrowTimestamp(time_now);
        }
        ++borrow_it;
        ++vw_idx;
    }
}

void WorkerGroupResourceCoordinator::run()
{
    try
    {
        std::vector<VirtualWarehousePtr> eligible_vw_borrowers;
        std::vector<WorkerGroupPtr> eligible_wg_lenders;

        size_t borrow_slots = 0;
        size_t lend_slots = 0;

        // Prevent changes to VWs and WGs
        auto vw_lock = rm_controller.getVirtualWarehouseManager().getLock();
        auto wg_lock = rm_controller.getWorkerGroupManager().getLock();

        auto vws = rm_controller.getVirtualWarehouseManager().getAllVirtualWarehousesImpl(&vw_lock);
        getEligibleGroups(vws, eligible_vw_borrowers, eligible_wg_lenders, borrow_slots, lend_slots);

        // Unlend currently loaned WG that are ineligible
        unlinkIneligibleGroups(vws, &vw_lock, &wg_lock);
        linkEligibleGroups(vws, eligible_vw_borrowers, eligible_wg_lenders, borrow_slots, lend_slots, &vw_lock, &wg_lock);

    }
    catch (...)
    {
        tryLogCurrentException(log);
    }

    background_task->scheduleAfter(task_interval_ms);

}

}
