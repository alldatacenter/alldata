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
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

// Addon prebuild信息
type AddonPrebuild struct {
	ID                uint64    `gorm:"primary_key"`
	ApplicationID     string    `gorm:"type:varchar(32)"`
	GitBranch         string    `gorm:"type:varchar(128)"`
	Env               string    `gorm:"type:varchar(10)"`
	RuntimeID         string    `gorm:"type:varchar(32)"`
	RoutingInstanceID string    `gorm:"type:varchar(64)"`
	InstanceID        string    `gorm:"type:varchar(64)"`
	InstanceName      string    `gorm:"type:varchar(128)"`
	AddonName         string    `gorm:"type:varchar(128)"`
	Plan              string    `gorm:"column:addon_class;type:varchar(64)"`
	Options           string    `gorm:"type:varchar(1024)"`
	Config            string    `gorm:"type:varchar(1024)"`
	BuildFrom         int       `gorm:"type:int(1);default:0"`            // 0: dice.yml 来源 1: 重新分析
	DeleteStatus      int       `gorm:"type:int(1),column:delete_status"` // 0: 未删除，1: diceyml删除，2: 重新分析删除
	Deleted           string    `gorm:"column:is_deleted"`
	CreatedAt         time.Time `gorm:"column:create_time"`
	UpdatedAt         time.Time `gorm:"column:update_time"`
}

func (AddonPrebuild) TableName() string {
	return "tb_addon_prebuild"
}

// CreatePrebuild insert addon prebuild info
func (db *DBClient) CreatePrebuild(addonPrebuild *AddonPrebuild) error {
	return db.Create(addonPrebuild).Error
}

// UpdatePrebuild 更新prebuild信息
func (db *DBClient) UpdatePrebuild(addonPrebuild *AddonPrebuild) error {
	if err := db.Save(addonPrebuild).Error; err != nil {
		return errors.Wrapf(err, "failed to update prebuild info, id: %v", addonPrebuild.ID)
	}
	return nil
}

// UpdateRuntimeId 更新prebuild中runtime信息
func (db *DBClient) UpdateRuntimeId(applicationID, gitBranch, env, runtimeId string) error {
	if err := db.Model(&AddonPrebuild{}).
		Where("application_id = ?", applicationID).
		Where("git_branch = ?", gitBranch).
		Where("env = ?", env).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("runtime_id != ?", nil).
		Update("runtime_id", runtimeId).Error; err != nil {
		return errors.Wrapf(err, "failed to update prebuild info, applicationID : %s, gitBranch : %s, env : %s",
			applicationID, gitBranch, env)
	}
	return nil
}

// UpdateInstanceId 更新prebuild中addon实例Id信息
func (db *DBClient) UpdateInstanceId(id int64, instanceId, routingInstanceId string) error {
	if err := db.Model(&AddonPrebuild{}).
		Where("id = ?", id).
		Updates(map[string]interface{}{"routing_instance_id": routingInstanceId, "instance_id": instanceId}).Error; err != nil {
		return errors.Wrapf(err, "failed to update prebuild addon instance info , id : %d",
			id)
	}
	return nil
}

// UpdateDeleteSTatus 更新prebuild中删除状态
func (db *DBClient) UpdateDeleteStatus(id int64, deleteStatus int8) error {
	if err := db.Model(&AddonPrebuild{}).
		Where("id = ?", id).
		Update("delete_status", deleteStatus).Error; err != nil {
		return errors.Wrapf(err, "failed to update prebuild delete status , id : %d, deleteStatus : %d",
			id, deleteStatus)
	}
	return nil
}

// DestroyPrebuildByRuntimeID 根据runtimeId删除信息
func (db *DBClient) DestroyPrebuildByRuntimeID(runtimeID string) error {
	if err := db.Model(&AddonPrebuild{}).
		Where("runtime_id = ?", runtimeID).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return errors.Wrapf(err, "failed to delete prebuild infos, runtimeID: %v", runtimeID)
	}
	return nil
}

// GetByAppIdAndBranchAndEnv 通过applicationID、branch、env获取prebuild信息
func (db *DBClient) GetByAppIdAndBranchAndEnv(applicationID, gitBranch, env string) (*[]AddonPrebuild, error) {
	var addonPrebuilds []AddonPrebuild
	if err := db.
		Where("application_id = ?", applicationID).
		Where("git_branch = ?", gitBranch).
		Where("env = ?", env).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonPrebuilds).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon prebuilds, applicationID : %s, gitBranch : %s, env : %s",
			applicationID, gitBranch, env)
	}
	return &addonPrebuilds, nil
}

// GetById 通过id获取prebuild信息
func (db *DBClient) GetById(id int64) (*AddonPrebuild, error) {
	var addonPrebuild AddonPrebuild
	if err := db.
		Where("id = ?", id).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		First(&addonPrebuild).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon prebuild, id : %d",
			id)
	}
	return &addonPrebuild, nil
}

// GetPreBuildsByRuntimeID 通过 runtimeID 获取 prebuild 信息
func (db *DBClient) GetPreBuildsByRuntimeID(runtimeID uint64) (*[]AddonPrebuild, error) {
	var addonPrebuilds []AddonPrebuild
	if err := db.
		Where("runtime_id = ?", runtimeID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonPrebuilds).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon prebuild, runtimeID : %d",
			runtimeID)
	}
	return &addonPrebuilds, nil
}

// GetByAppIdAndBranchAndEnvAndInstanceName 获取prebuild信息
func (db *DBClient) GetByAppIdAndBranchAndEnvAndInstanceName(applicationID, gitBranch, env, instanceName string) (*[]AddonPrebuild, error) {
	var addonPrebuilds []AddonPrebuild
	if err := db.
		Where("runtime_id = ?", applicationID).
		Where("git_branch = ?", gitBranch).
		Where("env = ?", env).
		Where("instance_name = ?", instanceName).
		Find(&addonPrebuilds).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon prebuild, applicationID : %s, gitBranch : %s, env : %s, instanceName : %s",
			applicationID, gitBranch, env, instanceName)
	}
	return &addonPrebuilds, nil
}
