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

package dao

import (
	"fmt"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/strutil"
)

func NotDeleted(db *gorm.DB) *gorm.DB {
	return db.Where("soft_deleted_at = 0")
}

// CreateProject 创建项目
func (client *DBClient) CreateProject(project *model.Project) error {
	return client.Create(project).Error
}

// UpdateProject 更新项目
func (client *DBClient) UpdateProject(project *model.Project) error {
	return client.Scopes(NotDeleted).Save(project).Error
}

// DeleteProject 删除项目
func (client *DBClient) DeleteProject(projectID int64) error {
	return client.Debug().Model(&model.Project{}).Scopes(NotDeleted).Where("id = ?", projectID).Update("soft_deleted_at", time.Now().UnixNano()/1e6).Error
}

func (client *DBClient) DeleteProjectQutoa(projectID int64) error {
	return client.Delete(new(apistructs.ProjectQuota), map[string]interface{}{"project_id": projectID}).Error
}

// GetProjectByID 根据projectID获取项目信息
func (client *DBClient) GetProjectByID(projectID int64) (model.Project, error) {
	var project model.Project
	if err := client.Scopes(NotDeleted).Where("id = ?", projectID).Find(&project).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return project, ErrNotFoundProject
		}
		return project, err
	}
	return project, nil
}

func (client *DBClient) GetQuotaByProjectID(projectID int64) (*apistructs.ProjectQuota, error) {
	var quota = new(apistructs.ProjectQuota)
	err := client.First(quota, map[string]interface{}{"project_id": projectID}).Error
	if err == nil {
		return quota, nil
	}
	if gorm.IsRecordNotFoundError(err) {
		return nil, nil
	}
	return nil, err
}

// GetProjectsByOrgIDAndName 根据orgID与名称获取项目列表
func (client *DBClient) GetProjectsByOrgIDAndName(orgID int64, params *apistructs.ProjectListRequest) (
	int, []model.Project, error) {
	var (
		projects []model.Project
		total    int
	)
	db := client.Scopes(NotDeleted).Where("org_id = ?", orgID)
	if params.IsPublic {
		db = db.Where("is_public = ?", params.IsPublic)
	}
	if params.Name != "" {
		db = db.Where("name = ?", params.Name)
	}
	if params.Query != "" {
		db = db.Where("(name LIKE ? OR display_name LIKE ?)", strutil.Concat("%", params.Query, "%"), strutil.Concat("%", params.Query, "%"))
	}
	if params.OrderBy != "" {
		if params.Asc {
			db = db.Order(fmt.Sprintf("%s", params.OrderBy))
		} else {
			db = db.Order(fmt.Sprintf("%s DESC", params.OrderBy))
		}
	} else {
		db = db.Order("active_time DESC")
	}

	if err := db.Offset((params.PageNo - 1) * params.PageSize).Limit(params.PageSize).
		Find(&projects).Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, projects, nil
}

// GetProjectsByIDs 根据projectIDs获取项目列表
func (client *DBClient) GetProjectsByIDs(projectIDs []uint64, params *apistructs.ProjectListRequest) (
	int, []model.Project, error) {
	var (
		total    int
		projects []model.Project
	)
	db := client.Scopes(NotDeleted).Where("id in (?)", projectIDs)
	if params.Name != "" {
		db = db.Where("name = ?", params.Name)
	}
	if params.Query != "" {
		db = db.Where("(name LIKE ? OR display_name LIKE ?)", strutil.Concat("%", params.Query, "%"), strutil.Concat("%", params.Query, "%"))
	}
	if !params.KeepMsp {
		db = db.Where("`type` != ?", pb.Type_MSP.String())
	}
	if params.OrderBy != "" {
		if params.Asc {
			db = db.Order(fmt.Sprintf("%s", params.OrderBy))
		} else {
			db = db.Order(fmt.Sprintf("%s DESC", params.OrderBy))
		}
	} else {
		db = db.Order("active_time DESC")
	}
	if err := db.Offset((params.PageNo - 1) * params.PageSize).Limit(params.PageSize).
		Find(&projects).Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, projects, nil
}

// GetProjectByOrgAndName 根据orgID & 项目名称 获取项目
func (client *DBClient) GetProjectByOrgAndName(orgID int64, name string) (*model.Project, error) {
	var project model.Project
	if err := client.Scopes(NotDeleted).Where("org_id = ?", orgID).
		Where("name = ?", name).Find(&project).Error; err != nil {
		return nil, err
	}
	return &project, nil
}

// GetAllProjects get all projects
func (client *DBClient) GetAllProjects() ([]model.Project, error) {
	var projects []model.Project
	if err := client.Model(model.Project{}).Scopes(NotDeleted).Find(&projects).Error; err != nil {
		return nil, err
	}
	return projects, nil
}

// ListProjectByOrgID 根据 orgID 获取项目列表
func (client *DBClient) ListProjectByOrgID(orgID uint64) ([]model.Project, error) {
	var projects []model.Project
	if err := client.Scopes(NotDeleted).Where("org_id = ?", orgID).Find(&projects).Error; err != nil {
		return nil, err
	}
	return projects, nil
}

// ListProjectByCluster 根据clusterName 获取项目列表
func (client *DBClient) ListProjectByCluster(clusterName string) ([]model.Project, error) {
	var projects []model.Project
	if err := client.Scopes(NotDeleted).Where("cluster_config LIKE ?", "%"+clusterName+"%").Find(&projects).Error; err != nil {
		return nil, err
	}
	return projects, nil
}

// ListProjectByOrgCluster 根据 orgID 和 clusterName 获取项目列表
func (client *DBClient) ListProjectByOrgCluster(clusterName string, orgID uint64) ([]model.Project, error) {
	var projects []model.Project
	if err := client.Scopes(NotDeleted).Where("cluster_config LIKE ? AND org_id = ?", "%"+clusterName+"%", orgID).Find(&projects).Error; err != nil {
		return nil, err
	}
	return projects, nil
}

// UpdateProjectQuota 更新项目配额
func (client *DBClient) UpdateProjectQuota(clusterName string, cpuOverSellChangeRatio float64) error {
	return client.Scopes(NotDeleted).Model(model.Project{}).
		Where("cluster_config LIKE ?", "%"+clusterName+"%").
		Update("cpu_quota", gorm.Expr("cpu_quota * ?", cpuOverSellChangeRatio)).Error
}

type ProjectID struct {
	ProjectID string `json:"project_id"`
}

// GetJoinedProjectNumByUserID get projects by userID and orgID
func (client *DBClient) GetJoinedProjectNumByUserID(userID string, orgID string) (int, []string, error) {
	var total int
	var proIDS []ProjectID
	res := make([]string, 0)
	if err := client.Model(&model.Member{}).
		Where("user_id = ? and org_id = ? and scope_type = \"?\"", userID, orgID, apistructs.ProjectScopeType).
		Select("project_id").Find(&proIDS).Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return total, res, err
	}
	for _, v := range proIDS {
		res = append(res, v.ProjectID)
	}
	return total, res, nil
}

// GetProjectIDListByStates get states by projectID list
func (client *DBClient) GetProjectIDListByStates(req apistructs.IssuePagingRequest, projectIDList []uint64) (int, []model.Project, error) {
	var (
		total int
		res   []model.Project
	)
	sql := client.Model(&model.Project{}).Scopes(NotDeleted).Where("id in (select distinct project_id from dice_issues where deleted = 0 and project_id in (?) and assignee IN (?) and state IN (?) and type IN(?) )", projectIDList, req.Assignees, req.State, req.Type).
		Order("name")
	offset := (req.PageNo - 1) * req.PageSize
	if err := sql.Offset(offset).Limit(req.PageSize).Find(&res).Error; err != nil {
		return total, res, err
	}
	if err := sql.Count(&total).Error; err != nil {
		return total, res, err
	}
	return total, res, nil
}

func (client *DBClient) GetProjectClustersNamespacesByProjectID(result map[string][]string, projectID uint64) error {
	if result == nil {
		return errors.New("the result map can not be nil")
	}

	var (
		podInfos          []*apistructs.PodInfo
		projectNamespaces []*apistructs.ProjectNamespaceModel
	)

	// 1) query from s_pod_info
	if err := client.Where(map[string]interface{}{"project_id": projectID, "phase": "running"}).
		Find(&podInfos).Error; err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			err = errors.Wrap(err, "failed to Find podInfos")
			return err
		}
	}

	// 2) query from project_namespaces
	if err := client.Where(map[string]interface{}{"project_id": projectID}).
		Find(&projectNamespaces).Error; err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			err = errors.Wrap(err, "failed to Find projectNamespace")
			return err
		}
	}

	// 3) patch to result
	for _, podInfo := range podInfos {
		clusters := result[podInfo.Cluster]
		clusters = append(clusters, podInfo.K8sNamespace)
		result[podInfo.Cluster] = clusters
	}
	for _, projectNamespace := range projectNamespaces {
		clusters := result[projectNamespace.ClusterName]
		clusters = append(clusters, projectNamespace.K8sNamespace)
		result[projectNamespace.ClusterName] = clusters
	}

	return nil
}

func (client *DBClient) ProjectIsExists(projectID uint64) bool {
	return client.Scopes(NotDeleted).First(new(model.Project), map[string]interface{}{"id": projectID}).Error == nil
}
