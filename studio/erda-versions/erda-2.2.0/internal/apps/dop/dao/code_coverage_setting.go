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

	"github.com/erda-project/erda/pkg/database/dbengine"
)

type CodeCoverageSetting struct {
	dbengine.BaseModel

	ProjectID    uint64 `json:"project_id"`
	MavenSetting string `json:"maven_setting"`
	Includes     string `json:"includes"`
	Excludes     string `json:"excludes"`
	Workspace    string `json:"workspace"`
}

func (CodeCoverageSetting) TableName() string {
	return "erda_code_coverage_setting"
}

func (client *DBClient) GetCodeCoverageSettingByProjectID(projectID uint64, workSpace string) (*CodeCoverageSetting, error) {
	var record CodeCoverageSetting
	err := client.Model(&CodeCoverageExecRecord{}).Where("project_id = ? and workspace = ?", projectID, workSpace).First(&record).Error
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	return &record, nil
}

func (client *DBClient) SaveCodeCoverageSettingByProjectID(record *CodeCoverageSetting) (*CodeCoverageSetting, error) {
	err := client.Model(&CodeCoverageExecRecord{}).Save(record).Error
	return record, err
}
