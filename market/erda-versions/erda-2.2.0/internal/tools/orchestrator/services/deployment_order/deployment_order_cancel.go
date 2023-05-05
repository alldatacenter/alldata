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
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
)

func (d *DeploymentOrder) Cancel(ctx context.Context, req *apistructs.DeploymentOrderCancelRequest) (*dbclient.DeploymentOrder, error) {
	order, err := d.db.GetDeploymentOrder(req.DeploymentOrderId)
	if err != nil {
		logrus.Errorf("failed to get order, id: %s, err: %v", req.DeploymentOrderId, err)
		return nil, err
	}

	appsInfo, err := d.parseAppsInfoWithOrder(order)
	if err != nil {
		logrus.Errorf("failed to get applications info, err: %v", err)
		return nil, err
	}

	if err := d.batchCheckExecutePermission(req.Operator, order.Workspace, appsInfo); err != nil {
		return nil, apierrors.ErrCancelDeploymentOrder.InternalError(err)
	}

	runtimes, err := d.db.GetRuntimeByDeployOrderId(order.ProjectId, order.ID)
	if err != nil {
		logrus.Errorf("failed to get runtime by deployment order id: %s, project: %s, err: %v", order.ID,
			order.ProjectName, err)
		return nil, err
	}

	if len(*runtimes) == 0 {
		logrus.Warnf("none runtimes need cancel deploying")
		// project release order deal with sync
		if order.Status == string(apistructs.DeploymentStatusCanceled) || order.Type == apistructs.TypeProjectRelease {
			return nil, nil
		}
		defaultStatusItem := apistructs.DeploymentOrderStatusItem{
			AppID:            uint64(order.ApplicationId),
			DeploymentStatus: apistructs.DeploymentStatusCanceled,
		}

		// deal with application release order
		order.Status = string(apistructs.DeploymentStatusCanceled)
		statusMap := make(apistructs.DeploymentOrderStatusMap)
		if err := json.Unmarshal([]byte(order.StatusDetail), &statusMap); err != nil {
			logrus.Errorf("failed to unmarshal status detail, err: %v", err)
			statusMap[order.ApplicationName] = defaultStatusItem
		} else {
			if app, ok := statusMap[order.ApplicationName]; ok {
				app.DeploymentStatus = apistructs.DeploymentStatusFailed
				statusMap[order.ApplicationName] = app
			} else {
				statusMap[order.ApplicationName] = defaultStatusItem
			}
		}

		newStatusDetail, err := json.Marshal(statusMap)
		if err == nil {
			order.StatusDetail = string(newStatusDetail)
		} else {
			logrus.Errorf("failed to marshal status detail, err: %v", err)
		}

		if err := d.db.UpdateDeploymentOrder(order); err != nil {
			logrus.Errorf("failed to update deployment order, id: %s, err: %v", order.ID, err)
			return nil, err
		}
		return nil, nil
	}

	for _, runtime := range *runtimes {
		if err := d.deploy.CancelLastDeploy(runtime.ID, req.Operator, req.Force); err != nil {
			logrus.Errorf("failed to cancel deploy, runtime: %d, err: %v", runtime.ID, err)
			return nil, err
		}
	}

	return order, nil
}
