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

package addon

//func TestSourcecovAddonManagement_BuildSourceCovServiceItem(t *testing.T) {
//	assert := require.New(t)
//	ctrl := gomock.NewController(t)
//	defer ctrl.Finish()
//
//	bdl := mock.NewMockSourcecovAddonManagementDeps(ctrl)
//	bdl.EXPECT().GetOrg("1").Return(&apistructs.OrgDTO{
//		Name: "org",
//	}, nil).Times(1)
//	bdl.EXPECT().GetOpenapiOAuth2Token(gomock.Any()).Return(&apistructs.OAuth2Token{
//		AccessToken: "token",
//	}, nil).Times(1)
//	bdl.EXPECT().GetProjectNamespaceInfo(uint64(1)).Return(&apistructs.ProjectNameSpaceInfo{
//		Namespaces: map[string]string{
//			"DEV": "dev-ns",
//		},
//	}, nil).Times(1)
//
//	diceYml := &diceyml.Object{
//		Services: map[string]*diceyml.Service{"app": {}},
//	}
//	addIns := &dbclient.AddonInstance{ProjectID: "1", OrgID: "1", Workspace: "DEV"}
//	sam := &SourcecovAddonManagement{bdl: bdl}
//	err := sam.BuildSourceCovServiceItem(&apistructs.AddonHandlerCreateItem{
//		Plan: "ultimate",
//	}, addIns, &apistructs.AddonExtension{
//		Plan: map[string]apistructs.AddonPlanItem{
//			"ultimate": {
//				CPU:   float64(1),
//				Mem:   1024,
//				Nodes: 1,
//
//				MaxCPU: float64(2),
//				MaxMem: 1025,
//			},
//		},
//	}, diceYml, nil)
//
//	assert.Nil(err)
//	assert.Equal("token", diceYml.Services["app"].Envs["CENTER_TOKEN"])
//	assert.Equal("dev-ns", diceYml.Services["app"].Envs["PROJECT_NS"])
//	assert.Equal("DEV", diceYml.Services["app"].Envs["WORKSPACE"])
//	assert.Equal("1", diceYml.Services["app"].Envs["PROJECT_ID"])
//	assert.Equal("org", diceYml.Services["app"].Envs["ORG_NAME"])
//	assert.Equal("", diceYml.Services["app"].Envs["CENTER_HOST"])
//	assert.Equal(1025, diceYml.Services["app"].Resources.MaxMem)
//	assert.Equal(float64(2), diceYml.Services["app"].Resources.MaxCPU)
//	status, err := sam.DeployStatus(nil, nil)
//	assert.Nil(err)
//	assert.Equal("true", status["SOURCECOV_ENABLED"])
//}
