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

package dbclient

import (
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
)

const (
	orderTableName   = "erda_deployment_order"
	releaseTableName = "dice_release"
)

type DeploymentOrder struct {
	ID              string `gorm:"size:36"`
	Type            string
	Description     string
	ReleaseId       string
	Operator        user.ID
	ProjectId       uint64
	ProjectName     string
	ApplicationId   int64
	ApplicationName string
	Workspace       string
	StatusDetail    string // application status
	Status          string // deployment order status
	Params          string
	IsOutdated      uint16
	CurrentBatch    uint64
	BatchSize       uint64
	DeployList      string
	Modes           string // release deploy modes, used to make modes env, separated by ","
	CreatedAt       time.Time
	UpdatedAt       time.Time
	StartedAt       time.Time `gorm:"default:'1970-01-01 00:00:00'"`
}

func (DeploymentOrder) TableName() string {
	return orderTableName
}

type Release struct {
	ReleaseId        string    `json:"releaseId" gorm:"type:varchar(64);primary_key"`
	ReleaseName      string    `json:"releaseName" gorm:"index:idx_release_name;not null"`
	Desc             string    `json:"desc" gorm:"type:text"`
	DiceYaml         string    `json:"dice" gorm:"type:text"` // dice.yml
	Addon            string    `json:"addon" gorm:"type:text"`
	Changelog        string    `json:"changelog" gorm:"type:text"`
	IsStable         bool      `json:"isStable" gorm:"type:tinyint(1)"`
	IsFormal         bool      `json:"isFormal" gorm:"type:tinyint(1)"`
	IsProjectRelease bool      `json:"IsProjectRelease" gorm:"type:tinyint(1)"`
	Modes            string    `json:"modes" gorm:"type:text"`
	Labels           string    `json:"labels" gorm:"type:varchar(1000)"`
	GitBranch        string    `json:"gitBranch" gorm:"type:varchar(255)"`
	Tags             string    `json:"tags" gorm:"type:varchar(100)"`
	Version          string    `json:"version" gorm:"type:varchar(100)"`
	OrgID            int64     `json:"orgId" gorm:"index:idx_org_id"`
	ProjectID        int64     `json:"projectId"`
	ApplicationId    uint64    `json:"applicationId"`
	ProjectName      string    `json:"projectName" gorm:"type:varchar(80)"`
	ApplicationName  string    `json:"applicationName" gorm:"type:varchar(80)"`
	UserId           string    `json:"userId" gorm:"type:varchar(50)"`
	ClusterName      string    `json:"clusterName" gorm:"type:varchar(80)"` // 所属集群
	Resources        string    `json:"resources,omitempty" gorm:"type:text"`
	Reference        int64     `json:"reference"`
	CrossCluster     bool      `json:"crossCluster"`
	CreatedAt        time.Time `json:"createdAt"`
	UpdatedAt        time.Time `json:"updatedAt"`
	IsLatest         bool      `json:"isLatest"`
}

func (Release) TableName() string {
	return releaseTableName
}

func (db *DBClient) ListDeploymentOrder(conditions *apistructs.DeploymentOrderListConditions, pageInfo *apistructs.PageInfo) (int, []DeploymentOrder, error) {
	cursor := db.Where("project_id = ? and workspace = ?", conditions.ProjectId, conditions.Workspace)

	// parse user permission apps orders and project orders
	cursor = cursor.Where("type = ? or application_id in (?)", apistructs.TypeProjectRelease, conditions.MyApplicationIds)

	// parse query
	if conditions.Query != "" {
		qv := "%" + conditions.Query + "%"

		// parse user range
		type UserIndex struct {
			Id int
		}
		var UserRange []UserIndex
		if err := db.Table("uc_user").Where("username like ? or nickname like ?", qv, qv).
			Select("id").Scan(&UserRange).Error; err != nil {
			return 0, nil, fmt.Errorf("failed to query release info, err: %v", err)
		}

		// parse release range
		type ReleaseIndex struct {
			ReleaseId string
		}
		var ReleaseRange []ReleaseIndex
		if err := db.Table("dice_release").Where("project_id = ? and version like ?", conditions.ProjectId, qv).
			Select("release_id").Scan(&ReleaseRange).Error; err != nil {
			return 0, nil, fmt.Errorf("failed to query user info, err: %v", err)
		}

		// add query condition
		var (
			uRet = make([]string, 0)
			rRet = make([]string, 0)
		)
		for _, i := range UserRange {
			uRet = append(uRet, strconv.Itoa(i.Id))
		}
		for _, i := range ReleaseRange {
			rRet = append(rRet, i.ReleaseId)
		}

		cursor = cursor.Where("SUBSTRING(id,1,6) like ? or release_id in (?) or operator in (?)",
			qv, rRet, uRet)
	}

	var (
		total  int
		orders = make([]DeploymentOrder, 0)
	)

	if err := cursor.Order("created_at desc").Offset(pageInfo.GetOffset()).Limit(pageInfo.GetLimit()).Find(&orders).
		Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return 0, nil, fmt.Errorf("failed to list deployment order, projectId: %d, err: %v", pageInfo, err)
	}

	return total, orders, nil
}

func (db *DBClient) GetDeploymentOrder(id string) (*DeploymentOrder, error) {
	var deploymentOrder DeploymentOrder
	if err := db.
		Where("id = ?", id).
		Find(&deploymentOrder).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get deployment order %s", id)
	}
	return &deploymentOrder, nil
}

func (db *DBClient) UpdateDeploymentOrder(deploymentOrder *DeploymentOrder) error {
	if err := db.Save(deploymentOrder).Error; err != nil {
		return errors.Wrapf(err, "failed to update deployment order, id: %v.",
			deploymentOrder.ID)
	}
	return nil
}

func (db *DBClient) GetReleases(releaseId string) (*Release, error) {
	var r Release
	if err := db.Where("release_id = ?", releaseId).Find(&r).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get release %s", releaseId)
	}
	return &r, nil
}

func (db *DBClient) ListReleases(releasesId []string) ([]*Release, error) {
	releases := make([]*Release, 0)
	if err := db.Where("release_id in (?)", releasesId).Find(&releases).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to list release %+v", releasesId)
	}
	return releases, nil
}

func (db *DBClient) ListReleasesMap(releasesId []string) (map[string]*Release, error) {
	releases := make([]*Release, 0)
	releaseMap := make(map[string]*Release, 0)
	if err := db.Where("release_id in (?)", releasesId).Find(&releases).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to list release %+v", releasesId)
	}
	for _, r := range releases {
		releaseMap[r.ReleaseId] = r
	}
	return releaseMap, nil
}

func (db *DBClient) UpdateDeploymentOrderStatusDetail(orderId string, newOrderStatusMap apistructs.DeploymentOrderStatusMap) error {
	var (
		deploymentOrder   DeploymentOrder
		curOrderStatusMap apistructs.DeploymentOrderStatusMap
	)

	if err := db.Where("id = ?", orderId).Find(&deploymentOrder).Error; err != nil {
		return errors.Wrapf(err, "failed to get deployment order %s", orderId)
	}

	if deploymentOrder.StatusDetail != "" {
		if err := json.Unmarshal([]byte(deploymentOrder.StatusDetail), &curOrderStatusMap); err != nil {
			return errors.Wrapf(err, "failed to unmarshal to deployment order status (%s)",
				deploymentOrder.ID)
		}
	}

	for appName, status := range newOrderStatusMap {
		if status.DeploymentID == 0 || status.DeploymentStatus == "" {
			continue
		}
		curOrderStatusMap[appName] = status
	}

	orderStatusMapJson, err := json.Marshal(curOrderStatusMap)
	if err != nil {
		return errors.Wrapf(err, "failed to marshal to deployment order status (%s)",
			deploymentOrder.ID)
	}

	deploymentOrder.StatusDetail = string(orderStatusMapJson)
	if err = db.Save(&deploymentOrder).Error; err != nil {
		return errors.Wrapf(err, "failed to update deployment order, id: %v.",
			deploymentOrder.ID)
	}

	return nil
}

func (db *DBClient) UpdateDeploymentOrderAppStatus(id string, appName string,
	appStatus apistructs.DeploymentOrderStatusItem) error {
	return db.Transaction(func(tx *gorm.DB) error {
		var (
			deploymentOrder    DeploymentOrder
			orderStatusMapJson []byte
			err                error
		)
		deploymentOrderStatusMap := make(apistructs.DeploymentOrderStatusMap)
		if err = tx.
			Where("id = ?", id).
			Find(&deploymentOrder).Error; err != nil {
			return errors.Wrapf(err, "failed to get deployment order %s", id)
		}
		if deploymentOrder.StatusDetail != "" {
			if err := json.Unmarshal([]byte(deploymentOrder.StatusDetail), &deploymentOrderStatusMap); err != nil {
				return errors.Wrapf(err, "failed to unmarshal to deployment order status (%s)",
					deploymentOrder.ID)
			}
		}
		deploymentOrderStatusMap[appName] = appStatus
		if orderStatusMapJson, err = json.Marshal(deploymentOrderStatusMap); err != nil {
			return errors.Wrapf(err, "failed to marshal to deployment order status (%s)",
				deploymentOrder.ID)
		}
		deploymentOrder.StatusDetail = string(orderStatusMapJson)
		if deploymentOrder.Type == apistructs.TypeApplicationRelease {
			deploymentOrder.Status = string(utils.ParseDeploymentOrderStatus(deploymentOrderStatusMap))
		}
		if err = tx.Save(&deploymentOrder).Error; err != nil {
			return errors.Wrapf(err, "failed to update deployment order, id: %v.",
				deploymentOrder.ID)
		}
		return nil
	})
}

func (db *DBClient) GetProjectReleaseByVersion(version string, projectId uint64) (*Release, error) {
	var r Release
	if err := db.Model(&Release{}).Where("project_id = ? and version = ? and is_project_release = ?",
		projectId, version, true).Find(&r).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, errors.Wrapf(err, "version: %s, projectId: %d", version, projectId)
		}
		return nil, errors.Wrapf(err, "failed to get project release, version: %s, projectId: %d", version, projectId)
	}
	return &r, nil
}

func (db *DBClient) GetApplicationReleaseByVersion(version, appName string) (*Release, error) {
	var r Release
	if err := db.Model(&Release{}).Where("application_name = ? and version = ? and is_project_release = ?",
		appName, version, false).Find(&r).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, errors.Wrapf(err, "version: %s, application_name: %s", version, appName)
		}
		return nil, errors.Wrapf(err, "failed to get project release, version: %s, application_name: %s",
			version, appName)
	}
	return &r, nil
}

func (db *DBClient) FindUnfinishedDeploymentOrders() ([]DeploymentOrder, error) {
	var deploymentOrders []DeploymentOrder
	if err := db.Where("type = ?", apistructs.TypeProjectRelease).
		Where("status in (?)", []apistructs.DeploymentStatus{
			apistructs.DeploymentStatusDeploying,
		}).Find(&deploymentOrders).Error; err != nil {
		return nil, errors.Wrap(err, "failed to find unfinished deployment orders")
	}
	return deploymentOrders, nil
}
