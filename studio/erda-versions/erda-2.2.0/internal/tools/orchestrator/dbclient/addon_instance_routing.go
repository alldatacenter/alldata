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
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

// AddonInstanceRouting addon 真实实例
type AddonInstanceRouting struct {
	ID                  string `gorm:"type:varchar(64);primary_key"` // 主键
	RealInstance        string `gorm:"type:varchar(64)"`             // AddonInstance 主键
	Name                string `gorm:"type:varchar(64)"`             // 用户 dice.yml 指定
	AddonID             string `gorm:"type:varchar(64)"`             // addonID
	AddonName           string `gorm:"type:varchar(64)"`             // 应用市场 addon 名称
	Category            string
	Plan                string
	Version             string
	Options             string `gorm:"type:text"`
	Status              string
	ShareScope          string // 共享级别 企业/项目/集群/平台
	OrgID               string
	Cluster             string `gorm:"column:az"` // 集群名称
	ProjectID           string
	ApplicationID       string
	AppID               string    `gorm:"column:app_id"`
	Workspace           string    `gorm:"column:env;type:varchar(20)"` // DEV/TEST/STAGING/PROD
	InsideAddon         string    `gorm:"type:varchar(1)"`             // N or Y
	Tag                 string    `gorm:"type:varchar(64)"`            // 实例标签
	Reference           int       `gorm:"column:attach_count"`         // addon 实例引用数
	Deleted             string    `gorm:"column:is_deleted"`           // Y: 已删除 N: 未删除
	IsPlatform          bool      // 是否为平台Addon实例
	PlatformServiceType int       `gorm:"type:int(1)"` // 服务类型，0：基础addon,1:微服务,2:通用能力
	CreatedAt           time.Time `gorm:"column:create_time"`
	UpdatedAt           time.Time `gorm:"column:update_time"`
}

// TableName 数据库表名
func (AddonInstanceRouting) TableName() string {
	return "tb_addon_instance_routing"
}

// CreateAddonInstanceRouting insert addon routing info
func (db *DBClient) CreateAddonInstanceRouting(addonRouting *AddonInstanceRouting) error {
	return db.Create(addonRouting).Error
}

// UpdateInstanceRouting 更新 instanceRouting 信息
func (db *DBClient) UpdateInstanceRouting(routing *AddonInstanceRouting) error {
	return db.Save(routing).Error
}

// DestroyByRoutingID 根据routingId删除信息
func (db *DBClient) DestroyByRoutingID(routingInstanceID string) error {
	if err := db.Model(&AddonInstanceRouting{}).
		Where("id = ?", routingInstanceID).
		Updates(map[string]interface{}{"is_deleted": apistructs.AddonDeleted, "status": apistructs.AddonDetached}).Error; err != nil {
		return errors.Wrapf(err, "failed to delete routing info, routingInstanceID: %v", routingInstanceID)
	}
	return nil
}

// GetInstanceRouting 获取 instanceRouting 实例
func (db *DBClient) GetInstanceRouting(id string) (*AddonInstanceRouting, error) {
	var instanceRouting AddonInstanceRouting
	if err := db.Where("id = ?", id).Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRouting).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &instanceRouting, nil
}

// GetInstanceRoutingByRealInstance 通过真实例Id查找routing信息
func (db *DBClient) GetInstanceRoutingByRealInstance(realIns string) (*[]AddonInstanceRouting, error) {
	var instanceRouting []AddonInstanceRouting
	if err := db.Where("real_instance = ?", realIns).Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRouting).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &instanceRouting, nil
}

// GetByRoutingIDs 根据Id列表获取routingInstance信息
func (db *DBClient) GetByRoutingIDs(routingInstanceIDs []string) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	if err := db.
		Where("id in (?)", routingInstanceIDs).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get routing info, routingInstanceIDs : %s",
			routingInstanceIDs)
	}
	return &instanceRoutings, nil
}

// GetAliveByAddonIDs 根据addonId列表获取信息
func (db *DBClient) GetAliveByAddonIDs(addonIDs []string) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	if err := db.
		Where("addon_id in (?)", addonIDs).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttached, apistructs.AddonAttaching}).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get routing info, addonIDs : %s",
			addonIDs)
	}
	return &instanceRoutings, nil
}

// GetProjectAddon 获取project级别addon信息
func (db *DBClient) GetProjectAddon(addonName, orgID, env string, projectIds []string) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	conn := db.DB.
		Where("addon_name = ?", addonName).
		Where("org_id = ?", orgID).
		Where("status = ?", apistructs.AddonAttached).
		Where("is_deleted = ?", apistructs.AddonNotDeleted)

	if env != "" {
		conn = conn.Where("env = ?", env)
	}

	if len(projectIds) > 0 {
		conn = conn.Where("project_id in (?)", projectIds)
	}

	if err := conn.
		Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get project level addon info, addonName : %s",
			addonName)
	}
	return &instanceRoutings, nil
}

// GetInstanceRoutingsByProjectIDs 获取project级别的实例路由
func (db *DBClient) GetInstanceRoutingsByProjectIDs(platformServiceType int, projectIDs []string, az, env string) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	conn := db.DB.
		Where("platform_service_type = ?", platformServiceType).
		Where("project_id in (?)", projectIDs).
		Where("status = ?", apistructs.AddonAttached).
		Where("is_deleted = ?", apistructs.AddonNotDeleted)
	if env != "" {
		conn = conn.Where("env = ?", env)
	}
	if az != "" {
		conn = conn.Where("az = ?", az)
	}

	if err := conn.Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get project level instance routing info, platform service type: %d", platformServiceType)
	}
	return &instanceRoutings, nil
}

// GetInstanceRoutingsByIDs 根据 ID 查询实例路由
func (db *DBClient) GetInstanceRoutingsByIDs(ids []string) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	if len(ids) == 0 {
		return &instanceRoutings, nil
	}
	if err := db.Where("id in (?)", ids).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRoutings).Error; err != nil {
		return nil, err
	}
	return &instanceRoutings, nil
}

// GetClusterAddon 获取cluster级别addon信息
func (db *DBClient) GetClusterAddon(addonName string, clusterName []string) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	if err := db.
		Where("addon_name = ?", addonName).
		Where("az in (?)", clusterName).
		Where("share_scope in (?)", "CLUSTER").
		Where("status = ?", apistructs.AddonAttached).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get project level addon info, addonName : %s",
			addonName)
	}
	return &instanceRoutings, nil
}

// ExistRoost 给定 projectID+workspace下是否有roost
func (db *DBClient) ExistRoost(projectID uint64, clusterName, workspace string) (bool, error) {
	// 判断是否有 roost
	addons, err := db.GetAddonInstanceRoutingByProjectAndAddonName(strconv.FormatUint(projectID, 10), clusterName, "terminus-roost", workspace)
	if err != nil {
		return false, err
	}
	if len(*addons) > 0 {
		return true, nil
	}

	return false, nil
}

// ExistZK 给定 projectID+workspace下是否有zk
func (db *DBClient) ExistZK(projectID uint64, clusterName, workspace string) (bool, error) {
	// 判断是否有 zookeeper
	addons, err := db.GetAddonInstanceRoutingByProjectAndAddonName(strconv.FormatUint(projectID, 10), clusterName, "terminus-zookeeper", workspace)
	if err != nil {
		return false, err
	}
	if len(*addons) > 0 {
		return true, nil
	}

	return false, nil
}

// GetAddonInstanceRoutingByProjectAndAddonName 根据 projectID & addonName & clusterName & workspace 获取
func (db *DBClient) GetAddonInstanceRoutingByProjectAndAddonName(projectID, clusterName, addonName, workspace string) (
	*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	if err := db.Where("az = ?", clusterName).
		Where("project_id = ?", projectID).
		Where("addon_name = ?", addonName).
		Where("env = ?", workspace).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Where("inside_addon = ?", apistructs.NOT_INSIDE).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addons).Error; err != nil {
		return nil, err
	}

	return &addons, nil
}

// GetAddonInstanceRoutingByOrgAndAddonName 根据 OrgID & addonName & clusterName & workspace 获取
func (db *DBClient) GetAddonInstanceRoutingByOrgAndAddonName(OrgID, clusterName, addonName, workspace, shareScope string) (
	*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	status := []string{string(apistructs.AddonAttaching), string(apistructs.AddonAttached)}
	if err := db.Where("az = ?", clusterName).
		Where("org_id = ?", OrgID).
		Where("addon_name = ?", addonName).
		Where("share_scope = ?", shareScope).
		Where("env = ?", workspace).
		Where("status in (?)", status).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addons).Error; err != nil {
		return nil, err
	}

	return &addons, nil
}

// GetAliveProjectAddon 获取project级别微服务addon信息
func (db *DBClient) GetAliveProjectAddons(projectID, clusterName, workspace string) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	if err := db.Where("az = ?", clusterName).
		Where("project_id = ?", projectID).
		Where("env = ?", workspace).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttached}).
		Where("inside_addon = ?", apistructs.NOT_INSIDE).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addons).Error; err != nil {
		return nil, err
	}

	return &addons, nil
}

// GetAliveClusterAddon 获取cluster级别微服务addon信息
func (db *DBClient) GetAliveClusterAddon(addonName, clusterName string, status []apistructs.AddonStatus) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	if err := db.
		Where("addon_name = ?", addonName).
		Where("az in (?)", clusterName).
		Where("status in (?)", status).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get cluster level addon info, addonName : %s, clusterName : %s",
			addonName, clusterName)
	}
	return &instanceRoutings, nil
}

// GetAliveDiceAddon 获取dice级别addon信息
func (db *DBClient) GetAliveDiceAddon(addonName string, status []apistructs.AddonStatus) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	if err := db.
		Where("addon_name = ?", addonName).
		Where("share_scope = ?", apistructs.DiceShareScope).
		Where("status in (?)", status).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get dice addon info, addonName : %s",
			addonName)
	}
	return &instanceRoutings, nil
}

// GetByRealInstance 获取 realInstanceID 的数据信息
func (db *DBClient) GetByRealInstance(realInstanceID string) (*[]AddonInstanceRouting, error) {
	var instanceRoutings []AddonInstanceRouting
	if err := db.
		Where("real_instance = ?", realInstanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instanceRoutings).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon instance routing info, realInstanceId : %s",
			realInstanceID)
	}
	return &instanceRoutings, nil
}

// UpdateAddonInstanceResource 根据 Id 更新 resource 信息
func (db *DBClient) UpdateAddonInstanceResource(ID string, cpurequest, cpulimit float64, memrequest, memlimit int) error {
	if err := db.Model(&AddonInstance{}).
		Where("id = ?", ID).
		Update(map[string]interface{}{
			"cpu_request": cpurequest,
			"cpu_limit":   cpulimit,
			"mem_request": memrequest,
			"mem_limit":   memlimit,
		}).Error; err != nil {
		return errors.Wrapf(err, "failed to update addoninstance resource, instanceID: %v", ID)
	}
	return nil
}

// UpdateAddonInstanceRoutingStatus 根据Id更新信息
func (db *DBClient) UpdateAddonInstanceRoutingStatus(ID, status string) error {
	if err := db.Model(&AddonInstanceRouting{}).
		Where("id = ?", ID).
		Updates(map[string]interface{}{"status": status}).Error; err != nil {
		return errors.Wrapf(err, "failed to update routing status, instanceID: %v", ID)
	}
	return nil
}

// GetRoutingInstancesByAddonName 根据 addonName 获取指定企业下的 addon 实例列表
func (db *DBClient) GetRoutingInstancesByAddonName(orgID uint64, addonName string) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	if err := db.Where("org_id = ?", orgID).
		Where("addon_name = ?", addonName).
		Where("category != ?", "discovery").
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetRoutingInstancesByCategory 根据 category 获取指定企业下的 addon 实例列表
func (db *DBClient) GetRoutingInstancesByCategory(orgID uint64, category string) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	if err := db.Where("org_id = ?", orgID).
		Where("category = ?", category).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetRoutingInstancesByOrg 根据 orgID 获取 addon 实例列表
func (db *DBClient) GetRoutingInstancesByOrg(orgID uint64) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	if err := db.Where("org_id = ?", orgID).
		Where("category != ?", "discovery").
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetOrgRoutingInstances 获取企业下可用的企业级共享 addon 实例列表
func (db *DBClient) GetOrgRoutingInstances(orgID, workspace, cluster string) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	if err := db.Where("org_id = ?", orgID).
		Where("env = ?", workspace).
		Where("share_scope = ?", apistructs.OrgShareScope).
		Where("az = ?", cluster).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetRoutingInstancesByWorkbench 获取用户有权限访问的 addon 实例列表
func (db *DBClient) GetRoutingInstancesByWorkbench(orgID uint64, projectIDs []string, category string) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	client := db.Where("org_id = ?", orgID).
		Where("project_id in (?)", projectIDs).
		Where("category != ?", "discovery").
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttached})
	if category != "" {
		client = db.Where("category = ?", category)
	}

	if err := client.Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetRoutingInstancesByProject 根据 projectID 获取 addon 实例列表
func (db *DBClient) GetRoutingInstancesByProject(orgID, projectID uint64, category string) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	client := db.Where("org_id = ?", orgID).
		Where("project_id = ?", projectID).
		Where("category != ?", "discovery").
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttached, apistructs.AddonAttaching, apistructs.AddonAttachFail, apistructs.AddonOffline})
	if category != "" {
		client = client.Where("category = ?", category)
	}

	if err := client.Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetRoutingInstanceByProjectAndName 根据 projectID 等信息获取 addon
func (db *DBClient) GetRoutingInstanceByProjectAndName(projectID uint64, workspace, addonName, name, clusterName string) (*AddonInstanceRouting, error) {
	var addon AddonInstanceRouting
	if err := db.Where("project_id = ?", projectID).
		Where("env = ?", workspace).
		Where("addon_name = ?", addonName).
		Where("az = ?", clusterName).
		Where("name = ?", name).
		Where("share_scope = ?", apistructs.ProjectShareScope).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Find(&addon).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &addon, nil
}

// GetProjectRoutingInstances 获取项目下可用的项目级共享 addon 实例列表
func (db *DBClient) GetProjectRoutingInstances(orgID, projectID, workspace, cluster string) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting
	if err := db.Where("org_id = ?", orgID).
		Where("project_id = ?", projectID).
		Where("env = ?", workspace).
		Where("az = ?", cluster).
		Where("share_scope = ?", apistructs.ProjectShareScope).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetRoutingInstancesBySimilar 根据similar查询对应的addon信息
func (db *DBClient) GetRoutingInstancesBySimilar(addonNames []string, params *apistructs.AddonHandlerCreateItem) (*[]AddonInstanceRouting, error) {
	var addons []AddonInstanceRouting

	client := db.Where("az = ?", params.ClusterName).
		Where("addon_name in (?)", addonNames).
		Where("env = ?", params.Workspace).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Where("inside_addon = ?", apistructs.NOT_INSIDE).
		Where("is_deleted = ?", apistructs.AddonNotDeleted)

	switch params.ShareScope {
	case apistructs.ProjectShareScope:
		client = client.Where("project_id = ?", params.ProjectID).Where("share_scope = ?", apistructs.ProjectShareScope)
	case apistructs.OrgShareScope:
		client = client.Where("org_id = ?", params.OrgID).Where("share_scope = ?", apistructs.OrgShareScope)
	case apistructs.ApplicationShareScope:
		client = client.Where("application_id = ?", params.ApplicationID).Where("share_scope = ?", apistructs.ApplicationShareScope)
	}

	if err := client.Find(&addons).Error; err != nil {
		return nil, err
	}
	return &addons, nil
}

// GetDistinctProjectInfo 获取所有project信息
func (db *DBClient) GetDistinctProjectInfo() (*[]string, error) {
	var addons []AddonInstanceRouting
	client := db.Select("project_id").Table("tb_addon_instance_routing").
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttaching, apistructs.AddonAttached}).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("project_id != \"\"").
		Where("project_id is not null").
		Group("project_id")

	if err := client.Scan(&addons).Error; err != nil {
		return nil, err
	}
	if len(addons) > 0 {
		projects := make([]string, 0, len(addons))
		for _, ins := range addons {
			projects = append(projects, ins.ProjectID)
		}
		return &projects, nil
	}

	return nil, nil
}

// ListRoutingInstanceByCluster 根据 clusterName 查找 addon 列表
func (db *DBClient) ListRoutingInstanceByCluster(clusterName string) ([]AddonInstanceRouting, error) {
	var routingInstances []AddonInstanceRouting
	if err := db.Where("az = ?", clusterName).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("platform_service_type = ?", apistructs.PlatformServiceTypeBasic).
		Find(&routingInstances).Error; err != nil {
		return nil, err
	}
	return routingInstances, nil
}

// ListRoutingInstanceByOrgCluster 根据 clusterName 查找 addon 列表
func (db *DBClient) ListRoutingInstanceByOrgCluster(clusterName string, orgID uint64) ([]AddonInstanceRouting, error) {
	var routingInstances []AddonInstanceRouting
	if err := db.Where("az = ?", clusterName).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("platform_service_type = ?", apistructs.PlatformServiceTypeBasic).
		Where("org_id = ?", orgID).
		Find(&routingInstances).Error; err != nil {
		return nil, err
	}
	return routingInstances, nil
}

func (db *DBClient) ListAttachedRoutingInstance() ([]AddonInstanceRouting, error) {
	var instances []AddonInstanceRouting
	if err := db.Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("platform_service_type = ?", 0).
		Where("status in (?)", []apistructs.AddonStatus{apistructs.AddonAttached}).
		Find(&instances).Error; err != nil {
		return nil, err
	}
	return instances, nil
}
