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

package workList

import (
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
)

type NopTranslator struct{}

func (t NopTranslator) Get(lang i18n.LanguageCodes, key, def string) string { return key }

func (t NopTranslator) Text(lang i18n.LanguageCodes, key string) string { return key }

func (t NopTranslator) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return fmt.Sprintf(key, args...)
}

var defaultSDK = &cptype.SDK{

	GlobalState: &cptype.GlobalStateData{},
	Tran:        &NopTranslator{},
}

func TestDoFilterProj(t *testing.T) {
	sdk := &cptype.SDK{}
	bdl := &bundle.Bundle{}
	wbSvc := &workbench.Workbench{}

	w := WorkList{
		sdk:   sdk,
		bdl:   bdl,
		wbSvc: wbSvc,
	}

	// monkey patch sdk
	monkey.PatchInstanceMethod(reflect.TypeOf(sdk), "I18n", func(_ *cptype.SDK, key string, args ...interface{}) string {
		return key
	})

	// monkey patch bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListSubscribes", func(_ *bundle.Bundle, userID, orgID string, req apistructs.GetSubscribeReq) (data *apistructs.SubscribeDTO, err error) {
		return &apistructs.SubscribeDTO{
			Total: 1,
			List: []apistructs.Subscribe{{
				ID:     "111",
				Type:   "project",
				TypeID: 666,
				Name:   "fake-project",
				UserID: "2",
				OrgID:  1,
			},
			},
		}, nil
	})

	// monkey patch Workbench
	monkey.PatchInstanceMethod(reflect.TypeOf(wbSvc), "ListQueryProjWbData", func(_ *workbench.Workbench, identity apistructs.Identity, page apistructs.PageRequest, query string) (data *apistructs.WorkbenchProjOverviewRespData, err error) {
		return &apistructs.WorkbenchProjOverviewRespData{
			Total: 1,
			List: []apistructs.WorkbenchProjOverviewItem{{
				ProjectDTO: apistructs.ProjectDTO{
					ID:          666,
					Name:        "fake-project",
					DisplayName: "fake-project",
					Type:        "DevOps",
				},
				IssueInfo: &apistructs.ProjectIssueInfo{},
			}},
		}, nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(wbSvc), "GetMspUrlParamsMap", func(_ *workbench.Workbench, identity apistructs.Identity, projectIDs []uint64, limit int) (urlParams map[string]workbench.UrlParams, err error) {
		return map[string]workbench.UrlParams{}, nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(wbSvc), "GetProjIssueQueries", func(_ *workbench.Workbench, userID string, projIDs []uint64, limit int) (data map[uint64]workbench.IssueUrlQueries, err error) {
		return map[uint64]workbench.IssueUrlQueries{}, nil
	})

	w.doFilterProj()
}
