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

package db

import (
	"encoding/json"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/model"
)

type BaseModel struct {
	ID        int64     `json:"id" gorm:"primary_key"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
}

type NotifyGroup struct {
	BaseModel
	Name        string `gorm:"size:150"`
	ScopeType   string `gorm:"size:150;index:idx_scope_type"`
	ScopeID     string `gorm:"size:150;index:idx_scope_id"`
	OrgID       int64  `gorm:"index:idx_org_id"`
	TargetData  string `gorm:"type:text"`
	Label       string `gorm:"size:200"`
	ClusterName string
	AutoCreate  bool
	Creator     string `gorm:"size:150"`
}

func (NotifyGroup) TableName() string {
	return "dice_notify_groups"
}

func (notifyGroup *NotifyGroup) ToApiData() *apistructs.NotifyGroup {
	var targets []apistructs.NotifyTarget
	if notifyGroup.TargetData != "" {
		err := json.Unmarshal([]byte(notifyGroup.TargetData), &targets)
		if err != nil {
			// compatible with old data
			targets = targets[:0]
			var oldTarget []apistructs.OldNotifyTarget
			err = json.Unmarshal([]byte(notifyGroup.TargetData), &oldTarget)
			if err != nil {
				logrus.Errorf("compatible old notify target error: %v", err)
			}
			for _, ot := range oldTarget {
				targets = append(targets, ot.CovertToNewNotifyTarget())
			}
		}
	}
	data := &apistructs.NotifyGroup{
		ID:        notifyGroup.ID,
		Name:      notifyGroup.Name,
		ScopeType: notifyGroup.ScopeType,
		ScopeID:   notifyGroup.ScopeID,
		Targets:   targets,
		CreatedAt: notifyGroup.CreatedAt,
		Creator:   notifyGroup.Creator,
	}
	return data
}

func (n *NotifyDB) GetNotifyGroup(id int64) (*NotifyGroup, error) {
	var notifyGroup NotifyGroup
	err := n.DB.Model(&NotifyGroup{}).Where("id = ?", id).First(&notifyGroup).Error
	return &notifyGroup, err
}

func (n *NotifyDB) CreateNotifyGroup(notifyGroup *NotifyGroup) (id int64, err error) {
	err = n.DB.Create(notifyGroup).Error
	if err != nil {
		return 0, err
	}
	return notifyGroup.ID, nil
}

func (n *NotifyDB) GetAllNotifyGroup(scope, scopeId string, orgId int64) ([]model.GetAllGroupData, error) {
	var notifyGroup []NotifyGroup
	err := n.DB.Model(&NotifyGroup{}).Where("scope_type = ?", scope).
		Where("scope_id = ?", scopeId).Where("org_id = ?", orgId).Find(&notifyGroup).Error
	if err != nil {
		return nil, err
	}
	groupDatas := make([]model.GetAllGroupData, 0)
	for _, v := range notifyGroup {
		data := model.GetAllGroupData{
			Value: v.ID,
			Name:  v.Name,
		}
		var targetData []model.NotifyTarget
		err = json.Unmarshal([]byte(v.TargetData), &targetData)
		if err != nil {
			return nil, err
		}
		data.Type = targetData[0].Type
		groupDatas = append(groupDatas, data)
	}
	return groupDatas, nil
}
