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

package apistructs

type TestSceneSetFileType string

var (
	TestSceneSetFileTypeExcel TestSceneSetFileType = "excel"
)

func (t TestSceneSetFileType) Valid() bool {
	switch t {
	case TestSceneSetFileTypeExcel:
		return true
	default:
		return false
	}
}

// AutoTestSceneSetExportRequest export autotest scene set
type AutoTestSceneSetExportRequest struct {
	ID           uint64               `json:"id"`
	Locale       string               `schema:"-"`
	IsCopy       bool                 `json:"-"`
	FileType     TestSceneSetFileType `schema:"fileType"`
	SceneSetName string               `json:"sceneSetName"`
	SpaceID      uint64               `json:"spaceID"`
	ProjectID    uint64               `json:"projectID"`

	IdentityInfo
}

type AutoTestSceneSetImportRequest struct {
	ProjectID uint64               `schema:"projectID"`
	SpaceID   uint64               `schema:"spaceID"`
	FileType  TestSceneSetFileType `schema:"fileType"`

	IdentityInfo
}

type AutoTestSceneSetImportResponse struct {
	Header
	Data uint64 `json:"data"`
}
