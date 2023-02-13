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
	"io/ioutil"
	"net/http"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	atv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
	"github.com/erda-project/erda/internal/pkg/user"
)

func TestCopyAutoTestSpaceV2(t *testing.T) {
	pm1 := monkey.Patch(user.GetIdentityInfo, func(r *http.Request) (apistructs.IdentityInfo, error) {
		return apistructs.IdentityInfo{UserID: "1"}, nil
	})
	defer pm1.Unpatch()

	body := apistructs.AutoTestSpace{
		ID: 1,
	}
	bodyDat, _ := json.Marshal(body)
	r := &http.Request{Body: ioutil.NopCloser(bytes.NewReader(bodyDat)), ContentLength: 100}

	autotestSvc := atv2.New()
	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(autotestSvc), "GetSpace", func(svc *atv2.Service, id uint64) (*apistructs.AutoTestSpace, error) {
		return &apistructs.AutoTestSpace{ID: 1}, nil
	})
	defer pm2.Unpatch()
	ep := Endpoints{
		bdl:        bdl.Bdl,
		autotestV2: autotestSvc,
	}
	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(ep.bdl), "CheckPermission", func(b *bundle.Bundle, req *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
		return nil, errors.Errorf("invalid permission")
	})
	defer pm3.Unpatch()

	_, err := ep.CopyAutoTestSpaceV2(context.Background(), r, map[string]string{})
	assert.NoError(t, err)
}

func TestExportAutoTestSpace(t *testing.T) {
	pm1 := monkey.Patch(user.GetIdentityInfo, func(r *http.Request) (apistructs.IdentityInfo, error) {
		return apistructs.IdentityInfo{UserID: "1"}, nil
	})
	defer pm1.Unpatch()

	body := apistructs.AutoTestSpaceExportRequest{
		ID: 1,
	}
	bodyDat, _ := json.Marshal(body)
	r := &http.Request{Body: ioutil.NopCloser(bytes.NewReader(bodyDat)), ContentLength: 100}

	autotestSvc := atv2.New()
	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(autotestSvc), "GetSpace", func(svc *atv2.Service, id uint64) (*apistructs.AutoTestSpace, error) {
		return &apistructs.AutoTestSpace{ID: 1}, nil
	})
	defer pm2.Unpatch()
	ep := Endpoints{
		bdl:        bdl.Bdl,
		autotestV2: autotestSvc,
	}
	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(ep.bdl), "CheckPermission", func(b *bundle.Bundle, req *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
		return nil, errors.Errorf("invalid permission")
	})
	defer pm3.Unpatch()

	_, err := ep.ExportAutoTestSpace(context.Background(), r, map[string]string{})
	assert.NoError(t, err)
}
