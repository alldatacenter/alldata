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
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
)

func (d *DeploymentOrder) Get(userId string, orderId string) (*apistructs.DeploymentOrderDetail, error) {
	order, err := d.db.GetDeploymentOrder(orderId)
	if err != nil {
		return nil, fmt.Errorf("failed to get deployment order, err: %v", err)
	}

	if access, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userId,
		Scope:    apistructs.ProjectScope,
		ScopeID:  order.ProjectId,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	}); err != nil || !access.Access {
		return nil, apierrors.ErrListDeploymentOrder.AccessDenied()
	}

	// parse params
	var params map[string]apistructs.DeploymentOrderParam

	if err := json.Unmarshal([]byte(order.Params), &params); err != nil {
		return nil, fmt.Errorf("failed to unmarshal params, err: %v", err)
	}

	// parse status
	appsStatus := make(apistructs.DeploymentOrderStatusMap)
	if order.StatusDetail != "" {
		if err := json.Unmarshal([]byte(order.StatusDetail), &appsStatus); err != nil {
			return nil, fmt.Errorf("failed to unmarshal applications status, err: %v", err)
		}
	}

	curRelease, err := d.db.GetReleases(order.ReleaseId)
	if err != nil {
		return nil, fmt.Errorf("failed to get release, err: %v", err)
	}

	releases := make([][]*dbclient.Release, 0)

	if order.Type == apistructs.TypeProjectRelease {
		deployList, err := unmarshalDeployList(order.DeployList)
		if err != nil {
			return nil, errors.Errorf("failed to unmarshal deploy list for deploy order %s, %v", order.ID, err)
		}

		var releaseIds []string
		for _, l := range deployList {
			releaseIds = append(releaseIds, l...)
		}
		appReleases, err := d.db.ListReleases(releaseIds)
		if err != nil {
			return nil, errors.Errorf("failed to list releases, %v", err)
		}
		id2Release := make(map[string]*dbclient.Release)
		for _, release := range appReleases {
			id2Release[release.ReleaseId] = release
		}

		for _, l := range deployList {
			var rs []*dbclient.Release
			for _, id := range l {
				r, ok := id2Release[id]
				if !ok {
					return nil, errors.Errorf("release %s not found", id)
				}
				rs = append(rs, r)
			}
			releases = append(releases, rs)
		}
	} else {
		releases = append(releases, []*dbclient.Release{curRelease})
	}

	// compose applications info
	asi, err := composeApplicationsInfo(releases, params, appsStatus)
	if err != nil {
		return nil, err
	}

	return &apistructs.DeploymentOrderDetail{
		DeploymentOrderItem: apistructs.DeploymentOrderItem{
			ID:   order.ID,
			Name: utils.ParseOrderName(order.ID),
			ReleaseInfo: &apistructs.ReleaseInfo{
				Id:        order.ReleaseId,
				Version:   curRelease.Version,
				Type:      convertReleaseType(curRelease.IsProjectRelease),
				Creator:   curRelease.UserId,
				CreatedAt: curRelease.CreatedAt,
				UpdatedAt: curRelease.UpdatedAt,
			},
			Type:         order.Type,
			Workspace:    order.Workspace,
			BatchSize:    order.BatchSize,
			CurrentBatch: order.CurrentBatch,
			Status:       apistructs.DeploymentOrderStatus(order.Status),
			Operator:     order.Operator.String(),
			DeployList:   order.DeployList,
			CreatedAt:    order.CreatedAt,
			UpdatedAt:    order.UpdatedAt,
			StartedAt:    parseStartedTime(order.StartedAt),
		},
		ApplicationsInfo: asi,
	}, nil
}

func composeApplicationsInfo(releases [][]*dbclient.Release, params map[string]apistructs.DeploymentOrderParam,
	appsStatus apistructs.DeploymentOrderStatusMap) ([][]*apistructs.ApplicationInfo, error) {

	asi := make([][]*apistructs.ApplicationInfo, 0)

	for _, sr := range releases {
		ai := make([]*apistructs.ApplicationInfo, 0)
		for _, r := range sr {
			applicationName := r.ApplicationName

			// parse deployment order
			orderParamsData := make(apistructs.DeploymentOrderParam, 0)

			param, ok := params[applicationName]
			if ok {
				for _, data := range param {
					if data.Encrypt {
						data.Value = ""
					}
					orderParamsData = append(orderParamsData, &apistructs.DeploymentOrderParamData{
						Key:     data.Key,
						Value:   data.Value,
						Encrypt: data.Encrypt,
						Type:    convertConfigType(data.Type),
						Comment: data.Comment,
					})
				}
			}

			var status apistructs.DeploymentStatus = apistructs.DeployStatusWaitDeploy
			app, ok := appsStatus[r.ApplicationName]
			if ok {
				status = app.DeploymentStatus
			}

			labels := make(map[string]string)
			if err := json.Unmarshal([]byte(r.Labels), &labels); err != nil {
				return nil, fmt.Errorf("failed to unmarshal release labels, err: %v", err)
			}

			ai = append(ai, &apistructs.ApplicationInfo{
				Id:             r.ApplicationId,
				Name:           applicationName,
				DeploymentId:   app.DeploymentID,
				Params:         &orderParamsData,
				ReleaseId:      r.ReleaseId,
				ReleaseVersion: r.Version,
				Branch:         labels["gitBranch"],
				DiceYaml:       r.DiceYaml,
				CommitId:       labels["gitCommitId"],
				Status:         utils.ParseDeploymentStatus(status),
			})
		}
		asi = append(asi, ai)
	}

	return asi, nil
}

func parseStartedTime(t time.Time) *time.Time {
	// TODO: equal started default time with unix zero
	if t.Year() < 2000 {
		return nil
	}
	return &t
}

func convertConfigType(configType string) string {
	if configType == "dice-file" || configType == "kv" {
		return configType
	}
	if configType == "FILE" {
		return "dice-file"
	}
	return "kv"
}

func convertReleaseType(isProjectRelease bool) string {
	if isProjectRelease {
		return apistructs.ReleaseTypeProject
	}
	return apistructs.ReleaseTypeApplication
}
