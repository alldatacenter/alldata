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
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
)

// GetProjectWorkspaceAbilities get ProjectWorkSpaceAbility in target workspace and project
func (client *DBClient) GetProjectWorkspaceAbilities(projectID uint64, workspace string) (apistructs.ProjectWorkSpaceAbility, error) {
	var ability apistructs.ProjectWorkSpaceAbility
	if err := client.Debug().Where("project_id = ? AND workspace = ? ", projectID, workspace).Order("updated_at DESC").Limit(1).Find(&ability).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return ability, ErrNotFoundProjectWorkSpace
		}
		return ability, err
	}
	return ability, nil
}

// CreateAbilitiesByWorkspace create ProjectWorkSpaceAbility for target workspace and project
func (client *DBClient) CreateProjectWorkspaceAbilities(ability apistructs.ProjectWorkSpaceAbility) error {
	return client.Debug().Create(&ability).Error
}

// UpdateAbilitiesByWorkspace update ProjectWorkSpaceAbility for target workspace and project
func (client *DBClient) UpdateProjectWorkspaceAbilities(ability apistructs.ProjectWorkSpaceAbility) error {
	return client.Debug().Save(&ability).Error
}

// DeleteAbilitiesByWorkspace delete ProjectWorkSpaceAbility for target project and/or workspace
func (client *DBClient) DeleteProjectWorkspaceAbilities(projectID uint64, workspace string) error {
	if workspace != "" {
		return client.Debug().Delete(new(apistructs.ProjectWorkSpaceAbility), map[string]interface{}{"project_id": projectID, "workspace": workspace}).Error
	}

	return client.Debug().Delete(new(apistructs.ProjectWorkSpaceAbility), map[string]interface{}{"project_id": projectID}).Error
}
