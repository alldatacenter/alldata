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

package project

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

func TestExportTemplatePackage(t *testing.T) {
	bdl := &bundle.Bundle{}
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetAppsByProject", func(b *bundle.Bundle, projectID, orgID uint64, userID string) (*apistructs.ApplicationListResponseData, error) {
		return &apistructs.ApplicationListResponseData{Total: 1, List: []apistructs.ApplicationDTO{
			{
				ID:          1,
				Name:        "erda",
				DisplayName: "erda",
			},
		}}, nil
	})
	defer pm1.Unpatch()

	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetOrg", func(b *bundle.Bundle, idOrName interface{}) (*apistructs.OrgDTO, error) {
		return &apistructs.OrgDTO{}, nil
	})
	defer pm2.Unpatch()

	proSvc := &Project{bdl: bdl}

	proSvc.UpdateFileRecord = func(req apistructs.TestFileRecordRequest) error {
		return nil
	}

	pm4 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "UploadFile", func(b *bundle.Bundle, req apistructs.FileUploadRequest, clientTimeout ...int64) (*apistructs.File, error) {
		return &apistructs.File{}, nil
	})
	defer pm4.Unpatch()

	req := &dao.TestFileRecord{
		ApiFileUUID: "12345",
		Extra: dao.TestFileExtra{
			ProjectTemplateFileExtraInfo: &apistructs.ProjectTemplateFileExtraInfo{
				ExportRequest: &apistructs.ExportProjectTemplateRequest{
					ProjectID:   1,
					ProjectName: "erda",
					OrgID:       1,
				},
			},
		},
	}
	t.Run("ExportTemplatePackage", func(t *testing.T) {
		proSvc.ExportTemplatePackage(req)
	})
}
