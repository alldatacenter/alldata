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

package model

import (
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

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
			// 老数据兼容
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
		Label:     notifyGroup.Label,
	}
	return data
}
