// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package deployment_order

import (
	"encoding/json"
	"fmt"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/queue"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
)

func (d *DeploymentOrder) PushOnDeploymentOrderPolling() (abort bool, err0 error) {
	// polling deployment order which is deploying and project release
	// application release status update with deployment fsm
	deploymentOrders, err := d.db.FindUnfinishedDeploymentOrders()
	if err != nil {
		logrus.Errorf("failed to find unfinished deployment order to continue, (%v)", err)
		return
	}
	if len(deploymentOrders) == 0 {
		logrus.Debugf("find empty unfinished deployment orders to continue")
		return
	}

	for _, order := range deploymentOrders {
		// current batch == 0 -> deployment order not stared
		if order.CurrentBatch == 0 {
			logrus.Debugf("deployment order %s is not stared, current batch: %d, batch size: %d", order.ID,
				order.CurrentBatch, order.BatchSize)
			continue
		}

		// compose current order status map
		statusMap, err := d.composeDeploymentOrderStatusMap(order)
		if err != nil {
			logrus.Errorf("failed to compose deployment order status map, (%v)", err)
			continue
		}

		// status update, only update status of current batch
		if err := inspectDeploymentStatusDetail(&order, statusMap); err != nil {
			logrus.Errorf("failed to inspect deployment status detail, (%v)", err)
			continue
		}

		// if current batch is success and is not last batch, deploy next batch
		if utils.ParseDeploymentOrderStatus(statusMap) == apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusOK) {
			if order.CurrentBatch == order.BatchSize {
				logrus.Debugf("deployment order %s is finished", order.ID)
				order.Status = string(apistructs.DeploymentStatusOK)
				if err := d.db.UpdateDeploymentOrder(&order); err != nil {
					logrus.Errorf("failed to update deployment order %s, (%v)", order.ID, err)
				}
				// finished or try update status again.
				continue
			}

			order.CurrentBatch++
			order.Status = string(apistructs.DeploymentStatusDeploying)
			if err := d.db.UpdateDeploymentOrder(&order); err != nil {
				logrus.Errorf("failed to update deployment order %s, (%v)", order.ID, err)
				// try advance batch again
				continue
			}

			// if event lost, reprocess will be triggered
			logrus.Infof("start to push deployment order %s to queue, batch size: %d, current batch: %d", order.ID,
				order.BatchSize, order.CurrentBatch)
			if err := d.queue.Push(queue.DEPLOYMENT_ORDER_BATCHES, order.ID); err != nil {
				logrus.Errorf("failed to push on DEPLOYMENT_ORDER_BATCHES, deploymentOrderID: %s, (%v)", order.ID, err)
				continue
			}
		} else {
			order.Status = string(utils.ParseDeploymentOrderStatus(statusMap))
			if err := d.db.UpdateDeploymentOrder(&order); err != nil {
				logrus.Errorf("failed to update deployment order %s, (%v)", order.ID, err)
				continue
			}
		}
	}

	return
}

func (d *DeploymentOrder) composeDeploymentOrderStatusMap(order dbclient.DeploymentOrder) (apistructs.DeploymentOrderStatusMap, error) {
	var (
		statusMap  = apistructs.DeploymentOrderStatusMap{}
		releaseIds = make([]string, 0)
		id2Release = make(map[string]*dbclient.Release)
	)

	// unmarshal deployment list
	deployList, err := unmarshalDeployList(order.DeployList)
	if err != nil {
		return statusMap, errors.Errorf("failed to unmarshal deploy list, (%v)", err)
	}

	if len(deployList) == 0 {
		return statusMap, errors.Errorf("deployment order %s has invalid deploy list", order.ID)
	}

	for _, l := range deployList {
		releaseIds = append(releaseIds, l...)
	}

	// list releases
	releases, err := d.db.ListReleases(releaseIds)
	if err != nil {
		return statusMap, fmt.Errorf("failed to list releases for order %s, %v", order.ID, err)
	}

	for _, r := range releases {
		id2Release[r.ReleaseId] = r
	}

	// get runtime status, and update
	for _, id := range deployList[order.CurrentBatch-1] {
		release, ok := id2Release[id]
		if !ok || releases == nil {
			// reset deployment order status is failed
			order.Status = string(apistructs.DeploymentStatusFailed)
			if err := d.db.UpdateDeploymentOrder(&order); err != nil {
				logrus.Errorf("failed to update deployment order %s, (%v)", order.ID, err)
				return statusMap, err
			}
			return statusMap, fmt.Errorf("failed to find release %s", id)
		}

		rt, errGetRuntime := d.db.GetRuntimeByAppName(order.Workspace, order.ProjectId, release.ApplicationName)
		if errGetRuntime != nil {
			if !errors.Is(errGetRuntime, gorm.ErrRecordNotFound) {
				return statusMap, errors.Errorf("failed to get runtime by app name %s, (%v)", release.ApplicationName, errGetRuntime)
			}

			statusMap[release.ApplicationName] = apistructs.DeploymentOrderStatusItem{
				AppID:            release.ApplicationId,
				DeploymentStatus: apistructs.DeploymentStatusFailed,
			}
			continue
		}

		lastDeployment, err := d.db.FindLastDeployment(rt.ID)
		if err != nil {
			return statusMap, errors.Errorf("failed to find last deployment for runtime %d, (%v)", rt.ID, err)
		}
		if lastDeployment == nil {
			return statusMap, errors.Errorf("failed to find last deployment for runtime, last deployment is nil, runtime id: %d", rt.ID)
		}

		statusMap[release.ApplicationName] = apistructs.DeploymentOrderStatusItem{
			RuntimeID:        rt.ID,
			AppID:            rt.ApplicationID,
			DeploymentID:     lastDeployment.ID,
			DeploymentStatus: lastDeployment.Status,
		}
	}
	return statusMap, nil
}

func inspectDeploymentStatusDetail(order *dbclient.DeploymentOrder, newOrderStatusMap apistructs.DeploymentOrderStatusMap) error {
	curOrderStatusMap := make(apistructs.DeploymentOrderStatusMap, 0)

	if order.StatusDetail != "" {
		if err := json.Unmarshal([]byte(order.StatusDetail), &curOrderStatusMap); err != nil {
			return errors.Wrapf(err, "failed to unmarshal to deployment order status (%s)",
				order.ID)
		}
	}

	for appName, newStatus := range newOrderStatusMap {
		curStatus, ok := curOrderStatusMap[appName]
		if !ok {
			curStatus = apistructs.DeploymentOrderStatusItem{}
		}
		// doesn't need to update the status (ok) even if the Runtime is deleted
		if newStatus.DeploymentStatus == "" || curStatus.DeploymentStatus == apistructs.DeploymentStatusOK {
			continue
		}
		// if status deployment id is nil, append status
		if newStatus.DeploymentID == 0 {
			curStatus.DeploymentStatus = newStatus.DeploymentStatus
		} else {
			curStatus = newStatus
		}
		curOrderStatusMap[appName] = curStatus
	}

	orderStatusMapJson, err := json.Marshal(curOrderStatusMap)
	if err != nil {
		return errors.Wrapf(err, "failed to marshal to deployment order status (%s)",
			order.ID)
	}

	order.StatusDetail = string(orderStatusMapJson)
	return nil
}
