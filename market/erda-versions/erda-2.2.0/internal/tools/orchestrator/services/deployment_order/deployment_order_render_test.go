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

package deployment_order

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	infrai18n "github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	release2 "github.com/erda-project/erda/internal/apps/dop/dicehub/release"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
)

func getFakeErdaYaml() []byte {
	return []byte(`
addons:
  fake-custom:
    plan: custom:basic
  fake-addon:
    plan: hello-world
envs: {}
jobs: {}
services:
  go-demo:
    binds: []
    deployments:
      replicas: 1
    envs: {}
    expose: []
    health_check: {}
    hosts: []
    image: registry.erda.io/erda-demo-erda-demo/go-web:go-demo-1642419767112914097
    ports:
    - 8080
    resources:
      cpu: 1
      mem: 1024
version: "2.0"
`)
}

func TestPreCheck(t *testing.T) {
	order := New()
	bdl := bundle.New()

	defer monkey.UnpatchAll()

	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "ListCustomInstancesByProjectAndEnv",
		func(*dbclient.DBClient, int64, string) ([]dbclient.AddonInstance, error) {
			return []dbclient.AddonInstance{}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CheckPermission",
		func(*bundle.Bundle, *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
			return &apistructs.PermissionCheckResponseData{Access: true}, nil
		},
	)

	monkey.Patch(i18n.LangCodesSprintf, func(infrai18n.LanguageCodes, string, ...interface{}) string {
		return ""
	})

	got, err := order.staticPreCheck([]*infrai18n.LanguageCode{{Code: "en", Quality: 1}}, "1", string(apistructs.WorkspaceDev), 1, 1, getFakeErdaYaml())
	assert.NoError(t, err)
	assert.Equal(t, len(got), 2)
}

func TestRenderDetail(t *testing.T) {
	order := New()
	bdl := bundle.New()
	releaseSvc := &release2.ReleaseService{}
	order.releaseSvc = releaseSvc

	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(releaseSvc), "GetRelease", func(*release2.ReleaseService, context.Context, *pb.ReleaseGetRequest) (*pb.ReleaseGetResponse, error) {
		return &pb.ReleaseGetResponse{Data: &pb.ReleaseGetResponseData{Diceyml: string(getFakeErdaYaml())}}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CheckPermission", func(*bundle.Bundle, *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
		return &apistructs.PermissionCheckResponseData{
			Access: true}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(order), "FetchDeploymentConfigDetail", func(*DeploymentOrder, string) ([]apistructs.EnvConfig, []apistructs.EnvConfig, error) {
		return nil, nil, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "ListCustomInstancesByProjectAndEnv",
		func(*dbclient.DBClient, int64, string) ([]dbclient.AddonInstance, error) {
			return []dbclient.AddonInstance{}, nil
		},
	)
	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "ListRuntimesByAppsName",
		func(*dbclient.DBClient, string, uint64, []string) (*[]dbclient.Runtime, error) {
			return &[]dbclient.Runtime{}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetApp", func(*bundle.Bundle, uint64) (*apistructs.ApplicationDTO, error) {
		return &apistructs.ApplicationDTO{
			ID: 1,
			Workspaces: []apistructs.ApplicationWorkspace{
				{Workspace: apistructs.WORKSPACE_PROD, ConfigNamespace: "1-198-PROD-480"},
			},
		}, nil
	})

	monkey.Patch(i18n.LangCodesSprintf, func(infrai18n.LanguageCodes, string, ...interface{}) string {
		return ""
	})

	_, err := order.RenderDetail(context.Background(), "", "1", "dd11727fc60945c998c2fcdf6487e9b0", "PROD", []string{"default"})
	assert.NoError(t, err)
}
