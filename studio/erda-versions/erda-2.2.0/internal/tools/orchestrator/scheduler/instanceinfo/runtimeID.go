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

package instanceinfo

import "time"

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

func (c *Client) GetRuntimeID(instanceID string) (string, error) {
	var addonPrebuild AddonPrebuild
	if err := c.db.Find(&addonPrebuild, map[string]interface{}{
		"instance_id": instanceID,
	}).Error; err != nil {
		return "", err
	}
	return addonPrebuild.RuntimeID, nil
}
