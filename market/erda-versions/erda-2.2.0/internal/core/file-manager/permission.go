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

package file_manager

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

func (p *provider) getScopeByHTTPRequest(ctx httpserver.Context) (string, error) {
	switch ctx.Request().URL.Query().Get("scope") {
	case "org":
		return permission.ScopeOrg, nil
	case "app":
		return permission.ScopeApp, nil
	}
	return "", fmt.Errorf("invalid scope")
}

func (p *provider) checkScopeIDByHTTPRequest(ctx httpserver.Context) (string, error) {
	params := ctx.Request().URL.Query()
	setInstance := func(instance *apistructs.InstanceInfoData) *apistructs.InstanceInfoData {
		ctx.SetAttribute(instanceKey, instance)
		return instance
	}
	if params.Get("scope") == "org" {
		return p.checkOrgScopeID(ctx.Param("containerID"), params.Get("hostIP"), permission.ScopeOrg, setInstance)
	}
	return p.checkOrgScopeID(ctx.Param("containerID"), params.Get("hostIP"), permission.ScopeApp, setInstance)
}

func (p *provider) getScopeByRequest(ctx context.Context, req interface{}) (string, error) {
	r, ok := req.(reuqestForPermission)
	if !ok {
		return "", fmt.Errorf("invalid reuqest")
	}
	switch r.GetScope() {
	case "org":
		return perm.ScopeOrg, nil
	case "app":
		return perm.ScopeApp, nil
	}
	return "", fmt.Errorf("invalid scope")
}

func (p *provider) checkScopeID(ctx context.Context, req interface{}) (string, error) {
	r, ok := req.(reuqestForPermission)
	if !ok {
		return "", fmt.Errorf("invalid reuqest")
	}
	setInstance := func(instance *apistructs.InstanceInfoData) *apistructs.InstanceInfoData {
		perm.SetPermissionDataFromContext(ctx, instanceKey, instance)
		return instance
	}
	if r.GetScope() == "org" {
		return p.checkOrgScopeID(r.GetContainerID(), r.GetHostIP(), permission.ScopeOrg, setInstance)
	}
	return p.checkOrgScopeID(r.GetContainerID(), r.GetHostIP(), permission.ScopeOrg, setInstance)
}

type reuqestForPermission interface {
	GetContainerID() string
	GetHostIP() string
	GetScope() string
}

func (p *provider) checkOrgScopeID(containerID, hostIP, scope string, fn func(instance *apistructs.InstanceInfoData) *apistructs.InstanceInfoData) (string, error) {
	resp, err := p.bdl.GetInstanceInfo(apistructs.InstanceInfoRequest{
		ContainerID: containerID,
		HostIP:      hostIP,
		Limit:       1,
	})
	if err != nil {
		return "", fmt.Errorf("failed to GetInstanceInfo: %s", err)
	}
	if !resp.Success {

		return "", fmt.Errorf("failed to GetInstanceInfo: code=%s, msg=%s", resp.Error.Code, resp.Error.Msg)
	}
	if len(resp.Data) <= 0 {
		return "", fmt.Errorf("not found instance %s/%s", hostIP, containerID)
	}
	instance := &resp.Data[0]
	if fn != nil {
		instance = fn(instance)
	}
	switch scope {
	case "org":
		return instance.OrgID, nil
	case "app":
		return instance.ApplicationID, nil
	default:
		return "", fmt.Errorf("invalid scope")
	}
}
