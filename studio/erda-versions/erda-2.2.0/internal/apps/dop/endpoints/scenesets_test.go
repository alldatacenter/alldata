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
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/gorilla/schema"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	atv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
	"github.com/erda-project/erda/internal/pkg/user"
)

func TestExportAutoSceneSet(t *testing.T) {
	pm1 := monkey.Patch(user.GetIdentityInfo, func(r *http.Request) (apistructs.IdentityInfo, error) {
		return apistructs.IdentityInfo{UserID: "1"}, nil
	})
	defer pm1.Unpatch()

	body := apistructs.AutoTestSceneSetExportRequest{
		ID:       1,
		FileType: apistructs.TestSceneSetFileTypeExcel,
		SpaceID:  1,
	}
	bodyDat, _ := json.Marshal(body)
	r := &http.Request{Body: ioutil.NopCloser(bytes.NewReader(bodyDat)), ContentLength: 100}
	autotestSvc := atv2.New()
	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(autotestSvc), "GetSpace", func(svc *atv2.Service, id uint64) (*apistructs.AutoTestSpace, error) {
		return nil, fmt.Errorf("not found")
	})
	defer pm2.Unpatch()

	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(autotestSvc), "GetSceneSet", func(svc *atv2.Service, setID uint64) (*apistructs.SceneSet, error) {
		return &apistructs.SceneSet{ID: 1}, nil
	})
	defer pm3.Unpatch()

	ep := Endpoints{
		bdl:        bdl.Bdl,
		autotestV2: autotestSvc,
	}

	_, err := ep.ExportAutotestSceneSet(context.Background(), r, map[string]string{})
	assert.NoError(t, err)
}

func TestImportAutoSceneSet(t *testing.T) {
	pm1 := monkey.Patch(user.GetIdentityInfo, func(r *http.Request) (apistructs.IdentityInfo, error) {
		return apistructs.IdentityInfo{UserID: "1"}, nil
	})
	defer pm1.Unpatch()

	body := apistructs.AutoTestSceneSetImportRequest{
		SpaceID:  1,
		FileType: apistructs.TestSceneSetFileTypeExcel,
	}
	bodyDat, _ := json.Marshal(body)
	r := &http.Request{Body: ioutil.NopCloser(bytes.NewReader(bodyDat)), ContentLength: 100}
	url, err := url.Parse("https://unit.test/api/autotests/scenesets/actions/import?projectID=1&spaceID=1&fileType=excel")
	if err != nil {
		t.Error(err)
	}
	r.URL = url
	autotestSvc := atv2.New()
	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(autotestSvc), "ImportSceneSet", func(svc *atv2.Service, req apistructs.AutoTestSceneSetImportRequest, r *http.Request) (uint64, error) {
		return 0, fmt.Errorf("failed to import")
	})
	defer pm2.Unpatch()

	ep := Endpoints{
		bdl:                bdl.Bdl,
		autotestV2:         autotestSvc,
		queryStringDecoder: schema.NewDecoder(),
	}
	_, err = ep.ImportAutotestSceneSet(context.Background(), r, map[string]string{})
	assert.NoError(t, err)
}
