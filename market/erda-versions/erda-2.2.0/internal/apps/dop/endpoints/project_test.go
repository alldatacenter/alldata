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
	"context"
	"net/http"
	"net/url"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/gorilla/schema"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/services/project"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/user"

	"github.com/erda-project/erda/apistructs"
)

func TestAddOnsFilterIn(t *testing.T) {
	addOns := []apistructs.AddonFetchResponseData{
		{
			ID:                  "1",
			PlatformServiceType: 1,
		},
		{
			ID:                  "2",
			PlatformServiceType: 0,
		},
		{
			ID:                  "3",
			PlatformServiceType: 1,
		},
	}
	newAddOns := addOnsFilterIn(addOns, func(addOn *apistructs.AddonFetchResponseData) bool {
		return addOn.PlatformServiceType == 0
	})
	assert.Equal(t, 1, len(newAddOns))
}

func TestExportProjectTemplate(t *testing.T) {
	pm1 := monkey.Patch(user.GetIdentityInfo, func(r *http.Request) (apistructs.IdentityInfo, error) {
		return apistructs.IdentityInfo{UserID: "1"}, nil
	})
	defer pm1.Unpatch()

	proSvc := &project.Project{}
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
	ep := Endpoints{
		bdl:                bdl.Bdl,
		project:            proSvc,
		queryStringDecoder: queryStringDecoder,
	}
	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(ep.bdl), "CheckPermission", func(b *bundle.Bundle, req *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
		return nil, errors.Errorf("invalid permission")
	})
	defer pm3.Unpatch()

	r := http.Request{URL: &url.URL{}}
	_, err := ep.ExportProjectTemplate(context.Background(), &r, map[string]string{})
	assert.NoError(t, err)
}

func TestImportProjectTemplate(t *testing.T) {
	pm1 := monkey.Patch(user.GetIdentityInfo, func(r *http.Request) (apistructs.IdentityInfo, error) {
		return apistructs.IdentityInfo{UserID: "1"}, nil
	})
	defer pm1.Unpatch()

	proSvc := &project.Project{}
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
	ep := Endpoints{
		bdl:                bdl.Bdl,
		project:            proSvc,
		queryStringDecoder: queryStringDecoder,
	}
	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(ep.bdl), "CheckPermission", func(b *bundle.Bundle, req *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
		return nil, errors.Errorf("invalid permission")
	})
	defer pm3.Unpatch()

	r := http.Request{URL: &url.URL{}}
	_, err := ep.ImportProjectTemplate(context.Background(), &r, map[string]string{})
	assert.NoError(t, err)
}

func Test_getProjectID(t *testing.T) {
	vars := map[string]string{
		"projectID": "1",
	}
	ep := &Endpoints{}
	projectID, err := ep.getProjectID(vars)
	assert.NoError(t, err)
	assert.Equal(t, uint64(1), projectID)
}

func Test_getOrgID(t *testing.T) {
	vars := map[string]string{
		"orgID": "1",
	}
	ep := &Endpoints{}
	orgID, err := ep.getOrgID(vars)
	assert.NoError(t, err)
	assert.Equal(t, int64(1), orgID)
}
