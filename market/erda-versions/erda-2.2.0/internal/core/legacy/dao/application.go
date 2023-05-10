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

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateApplication 创建应用
func (client *DBClient) CreateApplication(application *model.Application) error {
	return client.Create(application).Error
}

// UpdateApplication 更新应用
func (client *DBClient) UpdateApplication(application *model.Application) error {
	return client.Save(application).Error
}

// DeleteApplication 删除应用
func (client *DBClient) DeleteApplication(applicationID int64) error {
	return client.Where("id = ?", applicationID).Delete(&model.Application{}).Error
}

// GetApplicationByID 根据applicationID获取应用信息
func (client *DBClient) GetApplicationByID(applicationID int64) (model.Application, error) {
	var application model.Application
	if err := client.Where("id = ?", applicationID).Find(&application).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return application, ErrNotFoundApplication
		}
		return application, err
	}
	return application, nil
}

// GetApplicationByName 根据projectID & name获取应用
func (client *DBClient) GetApplicationByName(projectID int64, name string) (model.Application, error) {
	var application model.Application
	if err := client.Where("project_id = ?", projectID).Where("name = ?", name).
		Find(&application).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return application, nil
		}
		return application, err
	}
	return application, nil
}

// GetApplicationsByProjectID 根据projectID获取应用列表
func (client *DBClient) GetApplicationsByProjectID(projectID, pageNum, pageSize int64) ([]model.Application, error) {
	var applications []model.Application
	// TODO 权限控制
	if err := client.Where("project_id = ?", projectID).Order("updated_at DESC").
		Offset((pageNum - 1) * pageSize).Limit(pageSize).Find(&applications).Error; err != nil {
		return nil, err
	}
	return applications, nil
}

// GetApplicationsByProjectIDs 根据项目ID列表批量查询应用
func (client *DBClient) GetApplicationsByProjectIDs(projectIDs []uint64) ([]model.Application, error) {
	if len(projectIDs) == 0 {
		return nil, nil
	}
	var applications []model.Application
	if err := client.Where("project_id in (?)", projectIDs).
		Order("updated_at DESC").Find(&applications).Error; err != nil {
		return nil, err
	}
	return applications, nil
}

// GetApplicationCountByProjectID 根据projectID获取应用总数
func (client *DBClient) GetApplicationCountByProjectID(projectID int64) (int64, error) {
	var total int64
	if err := client.Model(&model.Application{}).Where("project_id = ?", projectID).
		Count(&total).Error; err != nil {
		return 0, err
	}
	return total, nil
}

type AppCount struct {
	ProjectID int64
	Count     int
}

func (client *DBClient) GetApplicationCountByProjectIDs(projectIDs []int64) ([]AppCount, error) {
	var counts []AppCount
	if err := client.Table(model.Application{}.TableName()).Select("project_id, COUNT(id) as count").Where("project_id IN (?)", projectIDs).
		Group("project_id").Find(&counts).Error; err != nil {
		return nil, err
	}
	return counts, nil
}

// GetApplicationsByIDs 根据applicationIDs & 名称模糊匹配获取应用列表
func (client *DBClient) GetApplicationsByIDs(orgID *int64, projectID *int64, applicationIDs []uint64, request *apistructs.ApplicationListRequest) (
	int, []model.Application, error) {
	var (
		total        int
		applications []model.Application
	)

	db := client.Model(&model.Application{})
	if request.Mode != "" {
		db = db.Where("mode = ?", request.Mode)
	}
	if request.Name != "" {
		db = db.Where("name = ?", request.Name)
	}
	if request.OrderBy != "" {
		db = db.Order(fmt.Sprintf("%s", request.OrderBy))
	}
	if request.Query != "" {
		db = db.Where("name LIKE ? OR display_name LIKE ?", strutil.Concat("%", request.Query, "%"), strutil.Concat("%", request.Query, "%"))
	}
	if request.ProjectID != 0 {
		db = db.Where("project_id = ?", request.ProjectID)
	}
	if orgID != nil {
		db = db.Where("org_id = ?", *orgID)
	}
	if len(applicationIDs) > 0 {
		db = db.Where("id in (?)", applicationIDs)
	}
	if request.Public != "" {
		db = db.Where("is_public = ?", request.Public == "public")
	}

	// 获取分页列表
	dbPart := db
	dbPart = dbPart.Order("updated_at DESC")
	if err := dbPart.Offset((request.PageNo - 1) * request.PageSize).Limit(request.PageSize).
		Find(&applications).Error; err != nil {
		return 0, nil, err
	}

	// 获取总量
	dbTotal := db
	if err := dbTotal.Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, applications, nil
}

// GetApplicationByOrgAndName 根据orgID & 应用名称获取应用
func (client *DBClient) GetApplicationByOrgAndName(orgID int64, name string) (*model.Application, error) {
	var app model.Application
	if err := client.Where("org_id = ?", orgID).
		Where("name = ?", name).Find(&app).Error; err != nil {
		return nil, err
	}
	return &app, nil
}

// GetProjectApplications 根据projectID获取所有应用列表
func (client *DBClient) GetProjectApplications(projectID int64) ([]model.Application, error) {
	var applications []model.Application
	if err := client.Where("project_id = ?", projectID).Find(&applications).Error; err != nil {
		return nil, err
	}
	return applications, nil
}

// GetAllApps 获取所有app列表
func (client *DBClient) GetAllApps() ([]model.Application, error) {
	var applications []model.Application
	if err := client.Find(&applications).Error; err != nil {
		return nil, err
	}
	return applications, nil
}

// GetJoinedAppNumByUserId get joined apps num by user and org
func (client *DBClient) GetJoinedAppNumByUserId(userID, orgID string) (int, error) {
	var total int
	if err := client.Model(&model.Member{}).Where("user_id = ? and org_id = ? and scope_type=\"?\"", userID, orgID, apistructs.AppScope).Count(&total).Error; err != nil {
		return total, err
	}
	return total, nil
}

// GetRuntimeCountByAppIDS get every app runtime counts by id list
func (client *DBClient) GetRuntimeCountByAppIDS(appIDS []int64) ([]model.ApplicationRuntimeCount, error) {
	counters := make([]model.ApplicationRuntimeCount, 0)
	if err := client.Table("ps_v2_project_runtimes").
		Where("application_id in (?)", appIDS).
		Group("application_id").
		Select("count(*) as runtime_count, application_id").
		Scan(&counters).Error; err != nil {
		return nil, err
	}
	return counters, nil
}

// ListUnblockAppCountsByProjectIDS count project's unblock app nums by unblock time
func (client *DBClient) ListUnblockAppCountsByProjectIDS(projectIDS []uint64) ([]model.ProjectUnblockAppCount, error) {
	counters := make([]model.ProjectUnblockAppCount, 0)
	if err := client.Table("dice_app").
		Where("project_id in (?) and unblock_start < now() and unblock_end > now()", projectIDS).
		Group("project_id").
		Select("count(*) as unblock_app_count, project_id").
		Scan(&counters).Error; err != nil {
		return nil, err
	}
	return counters, nil
}

func (client *DBClient) GetApplicationsByNames(projectID uint64, names []string) ([]model.Application, error) {
	var applications []model.Application
	err := client.Where("project_id = ?", projectID).Where("name in (?)", names).Find(&applications).Error
	return applications, err
}
