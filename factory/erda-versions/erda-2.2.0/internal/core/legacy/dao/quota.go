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
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

// GetWorkspaceQuota get project workspace quota
// Valid workspace: prod, staging, test, dev
func (client *DBClient) GetWorkspaceQuota(projectID, workspace string) (uint64, uint64, error) {
	var projectQuota apistructs.ProjectQuota
	if err := client.Find(&projectQuota, map[string]interface{}{
		"project_id": projectID,
	}).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return 0, 0, nil
		}
		return 0, 0, err
	}

	switch workspace {
	case "prod":
		return projectQuota.ProdCPUQuota, projectQuota.ProdMemQuota, nil
	case "staging":
		return projectQuota.StagingCPUQuota, projectQuota.StagingMemQuota, nil
	case "test":
		return projectQuota.TestCPUQuota, projectQuota.TestMemQuota, nil
	case "dev":
		return projectQuota.DevCPUQuota, projectQuota.DevMemQuota, nil
	default:
		return 0, 0, errors.Errorf("invalid workspace: %s", workspace)
	}
}
