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

package endpoints

import (
	"net/http"
	"net/url"
	"testing"

	"github.com/gorilla/schema"
	"github.com/stretchr/testify/assert"
)

//func Test_transferAppsToApplicationDTOS(t *testing.T) {
//	var orgSvc = &org.Org{}
//	patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(orgSvc), "ListOrgs", func(app *org.Org, orgIDs []int64, req *apistructs.OrgSearchRequest, all bool) (int, []model.Org, error) {
//		return 1, []model.Org{{BaseModel: model.BaseModel{ID: 1}}}, nil
//	})
//	defer patch1.Unpatch()
//
//	var pj = &project.Project{}
//	patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(pj), "GetModelProjectsMap", func(project *project.Project, projectIDs []uint64) (map[int64]*model.Project, error) {
//		return map[int64]*model.Project{
//			1: {BaseModel: model.BaseModel{
//				ID: 1,
//			}},
//		}, nil
//	})
//	defer patch2.Unpatch()
//
//	var db = &dao.DBClient{}
//
//	ep := Endpoints{
//		org:     orgSvc,
//		project: pj,
//		db:      db,
//	}
//
//	apps := []model.Application{{BaseModel: model.BaseModel{ID: 1}, OrgID: 1, ProjectID: 1}}
//	_, err := ep.transferAppsToApplicationDTOS(true, apps, map[uint64]string{}, map[int64][]string{})
//	assert.NoError(t, err)
//}

func TestGetAppParams(t *testing.T) {
	// init Endpoints with queryStringDecoder
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
	ep := &Endpoints{
		queryStringDecoder: queryStringDecoder,
	}

	req, err := http.NewRequest("GET", "https://baidu.com", nil)
	if err != nil {
		panic(err)
	}

	params := make(url.Values)
	params.Add("applicationID", "1")
	params.Add("applicationID", "2")
	req.URL.RawQuery = params.Encode()

	parsedReq, err := getListApplicationsParam(ep, req)
	assert.NoError(t, err)
	assert.Equal(t, parsedReq.ApplicationID, []uint64{1, 2})
}
