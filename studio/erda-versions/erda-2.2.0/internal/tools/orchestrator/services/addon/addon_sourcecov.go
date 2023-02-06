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

import (
	"os"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

type SourcecovAddonManagementDeps interface {
	GetProjectNamespaceInfo(projectID uint64) (*apistructs.ProjectNameSpaceInfo, error)
	GetOrg(idOrName interface{}) (*apistructs.OrgDTO, error)
	GetOAuth2Token(req apistructs.OAuth2TokenGetRequest) (*apistructs.OAuth2Token, error)
}

type SourcecovAddonManagement struct {
	bdl SourcecovAddonManagementDeps
}

func (sam *SourcecovAddonManagement) BuildSourceCovServiceItem(
	params *apistructs.AddonHandlerCreateItem,
	addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension,
	addonDice *diceyml.Object,
	_ *apistructs.ClusterInfoData) (err error) {
	var (
		projectID   int64
		orgInfo     *apistructs.OrgDTO
		projectInfo *apistructs.ProjectNameSpaceInfo
		token       string
	)

	projectID, err = strconv.ParseInt(addonIns.ProjectID, 10, 64)
	if err != nil {
		err = errors.Wrapf(err, "failed to parse project id to int %s", addonIns.ProjectID)
		return
	}

	projectInfo, err = sam.bdl.GetProjectNamespaceInfo(uint64(projectID))
	if err != nil {
		return
	}

	if orgInfo, err = sam.bdl.GetOrg(addonIns.OrgID); err != nil {
		return
	}

	if token, err = sam.getSourcecovToken(addonIns.OrgID); err != nil {
		return
	}

	addonDeployPlan := addonSpec.Plan[params.Plan]

	for _, service := range addonDice.Services {
		if service.Envs == nil {
			service.Envs = make(map[string]string)
		}

		service.Envs["PROJECT_ID"] = addonIns.ProjectID
		service.Envs["WORKSPACE"] = addonIns.Workspace
		service.Envs["PROJECT_NS"] = projectInfo.Namespaces[addonIns.Workspace]
		service.Envs["ORG_NAME"] = orgInfo.Name
		service.Envs["CENTER_HOST"] = os.Getenv("OPENAPI_PUBLIC_URL")
		service.Envs["CENTER_TOKEN"] = token
		service.Resources = diceyml.Resources{
			CPU: addonDeployPlan.CPU,
			Mem: addonDeployPlan.Mem,

			MaxCPU: addonDeployPlan.MaxCPU,
			MaxMem: addonDeployPlan.MaxMem,
		}

		if service.Labels == nil {
			service.Labels = make(map[string]string)
		}

		SetlabelsFromOptions(params.Options, service.Labels)

		//  主要目的是传递 卷配置信息以及标签给后端 sourcecov-agent 对象，本身不会创建卷
		vol01 := SetAddonVolumes(params.Options, "/for-agent", false)
		service.Volumes = diceyml.Volumes{vol01}
	}

	return nil
}

func (sam *SourcecovAddonManagement) getSourcecovToken(orgID string) (token string, err error) {
	resp, err := sam.bdl.GetOAuth2Token(apistructs.OAuth2TokenGetRequest{
		ClientID:     conf.TokenClientID(),
		ClientSecret: conf.TokenClientSecret(),
		Payload: apistructs.OAuth2TokenPayload{
			AccessTokenExpiredIn: "0",
			AccessibleAPIs: []apistructs.AccessibleAPI{{
				Path:   "/api/code-coverage/actions/end-callBack",
				Method: "POST",
				Schema: "http",
			}, {
				Path:   "/api/code-coverage/actions/status",
				Method: "GET",
				Schema: "http",
			}, {
				Path:   "/api/code-coverage/actions/report-callBack",
				Method: "POST",
				Schema: "http",
			}, {
				Path:   "/api/code-coverage/actions/ready-callBack",
				Method: "POST",
				Schema: "http",
			}, {
				Path:   "/api/files",
				Method: "POST",
				Schema: "http",
			}},

			Metadata: map[string]string{
				httputil.InternalHeader: "orchestrator",
				httputil.OrgHeader:      orgID,
			},
		},
	})

	if err != nil {
		return "", err
	}

	return resp.AccessToken, nil
}

func (sam *SourcecovAddonManagement) DeployStatus(ins *dbclient.AddonInstance, group *apistructs.ServiceGroup) (map[string]string, error) {
	return map[string]string{
		"SOURCECOV_ENABLED": "true",
		"OPEN_JACOCO_AGENT": "true",
	}, nil
}
