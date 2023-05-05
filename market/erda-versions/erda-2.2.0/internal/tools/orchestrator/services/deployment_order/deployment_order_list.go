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
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
)

func (d *DeploymentOrder) List(userId string, orgId uint64, conditions *apistructs.DeploymentOrderListConditions, pageInfo *apistructs.PageInfo) (*apistructs.DeploymentOrderListData, error) {
	apps, err := d.bdl.GetMyApps(userId, orgId)
	if err != nil {
		logrus.Errorf("failed to get my apps, err: %v", err)
		return nil, err
	}

	conditions.MyApplicationIds = make([]uint64, 0)

	for _, dto := range apps.List {
		conditions.MyApplicationIds = append(conditions.MyApplicationIds, dto.ID)
	}

	total, data, err := d.db.ListDeploymentOrder(conditions, pageInfo)
	if err != nil {
		logrus.Errorf("failed to list deployment order, err: %v", err)
		return nil, err
	}

	ret, err := d.convertDeploymentOrderToResponseItem(data)
	if err != nil {
		logrus.Errorf("failed to convert deployment order to response, err: %v", err)
		return nil, err
	}

	return &apistructs.DeploymentOrderListData{
		Total: total,
		List:  ret,
	}, nil
}

func (d *DeploymentOrder) convertDeploymentOrderToResponseItem(orders []dbclient.DeploymentOrder) ([]*apistructs.DeploymentOrderItem, error) {
	ret := make([]*apistructs.DeploymentOrderItem, 0)

	setIds := make(map[string]byte)
	for _, order := range orders {
		setIds[order.ReleaseId] = 0
	}
	releasesId := make([]string, 0)
	for id := range setIds {
		releasesId = append(releasesId, id)
	}

	releases, err := d.db.ListReleases(releasesId)
	if err != nil {
		return nil, fmt.Errorf("failed to list release, ids: %+v", releasesId)
	}

	releasesMap := make(map[string]*dbclient.Release)
	for _, r := range releases {
		releasesMap[r.ReleaseId] = r
	}

	for _, order := range orders {
		appsStatus := make(apistructs.DeploymentOrderStatusMap, 0)
		if order.StatusDetail != "" {
			// parse status
			if err := json.Unmarshal([]byte(order.StatusDetail), &appsStatus); err != nil {
				return nil, fmt.Errorf("failed to unmarshal applications status, err: %v", err)
			}
		}

		applicationCount := 1

		r, ok := releasesMap[order.ReleaseId]
		if !ok {
			logrus.Errorf("failed to get release %s, not found", order.ReleaseId)
			continue
		}

		if r.IsProjectRelease {
			deployList, err := unmarshalDeployList(order.DeployList)
			if err != nil {
				logrus.Errorf("failed to unmarshal deploy list for order %s", order.ID)
				continue
			}
			applicationCount = 0
			for _, l := range deployList {
				applicationCount += len(l)
			}
		}

		applicationStatus := strings.Join([]string{
			strconv.Itoa(parseApplicationStatus(appsStatus)),
			strconv.Itoa(applicationCount)}, "/")

		status := apistructs.DeploymentOrderStatus(order.Status)
		if status == "" {
			status = utils.ParseDeploymentOrderStatus(appsStatus)
		}

		ret = append(ret, &apistructs.DeploymentOrderItem{
			ID:   order.ID,
			Name: utils.ParseOrderName(order.ID),
			ReleaseInfo: &apistructs.ReleaseInfo{
				Id:        order.ReleaseId,
				Version:   r.Version,
				Type:      convertReleaseType(r.IsProjectRelease),
				CreatedAt: r.CreatedAt,
				UpdatedAt: r.UpdatedAt,
			},
			Type:              order.Type,
			ApplicationStatus: applicationStatus,
			Workspace:         order.Workspace,
			BatchSize:         order.BatchSize,
			CurrentBatch:      order.CurrentBatch,
			Status:            status,
			Operator:          string(order.Operator),
			CreatedAt:         order.CreatedAt,
			UpdatedAt:         order.UpdatedAt,
			StartedAt:         parseStartedTime(order.StartedAt),
		})
	}

	return ret, nil
}

func parseApplicationStatus(status apistructs.DeploymentOrderStatusMap) int {
	var okCount int

	for _, s := range status {
		if s.DeploymentStatus == apistructs.DeploymentStatusOK {
			okCount++
		}
	}

	return okCount
}
