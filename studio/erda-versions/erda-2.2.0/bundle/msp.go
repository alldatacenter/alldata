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

package bundle

import (
	"fmt"
	"net/url"
	"strconv"

	"github.com/erda-project/erda-proto-go/msp/tenant/pb"
	projpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// PutRuntimeService 部署runtime之后，orchestrator需要将服务域名信息通过此接口提交给hepa
func (b *Bundle) PutRuntimeService(req *apistructs.RuntimeServiceRequest) error {
	host, err := b.urls.Hepa()
	if err != nil {
		return err
	}
	hc := b.hc

	var resp apistructs.Header
	r, err := hc.Put(host).
		Path("/api/gateway/runtime-services").
		JSONBody(req).
		Do().
		JSON(&resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return toAPIError(r.StatusCode(), resp.Error)
	}
	return nil
}

// DeleteRuntimeService 删除runtime前，调用hepa
func (b *Bundle) DeleteRuntimeService(runtimeID uint64) error {
	host, err := b.urls.Hepa()
	if err != nil {
		return err
	}
	hc := b.hc

	var resp apistructs.Header
	r, err := hc.Delete(host).
		Path(fmt.Sprintf("/api/gateway/runtime-services/%d", runtimeID)).
		Do().
		JSON(&resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return toAPIError(r.StatusCode(), resp.Error)
	}
	return nil
}

// GetTenantGroupDetails .
func (b *Bundle) GetTenantGroupDetails(tenantGroup string) (*apistructs.TenantGroupDetails, error) {
	host, err := b.urls.TMC()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.TenantGroupDetailsResponse
	r, err := hc.Get(host).
		Path("/api/tmc/tenants/group/details").Param("tenantGroup", tenantGroup).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// GetMonitorStatusMetricDetails .
func (b *Bundle) GetMonitorStatusMetricDetails(metricID string) (*apistructs.MonitorStatusMetricDetails, error) {
	host, err := b.urls.MSP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.MonitorStatusMetricDetailsResponse
	r, err := hc.Get(host).
		Path(fmt.Sprintf("/api/v1/msp/checkers/metrics/%s", url.QueryEscape(metricID))).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

func (b *Bundle) CreateGatewayTenant(req *apistructs.GatewayTenantRequest) error {
	host, err := b.urls.Hepa()
	if err != nil {
		return err
	}
	hc := b.hc

	var resp apistructs.Header
	r, err := hc.Post(host, httpclient.NoRetry).
		Path("/api/gateway/tenants").
		JSONBody(req).
		Do().
		JSON(&resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return toAPIError(r.StatusCode(), resp.Error)
	}
	return nil
}

func (b *Bundle) CreateMSPTenant(projectID, workspace, tenantType, tenantGroup string) (string, error) {
	host := discover.MSP()
	hc := b.hc

	req := pb.CreateTenantRequest{
		ProjectID:  projectID,
		TenantType: tenantType,
		Workspaces: []string{workspace},
	}
	var resp apistructs.MSPTenantResponse
	r, err := hc.Post(host).
		Path("/api/msp/tenant").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().
		JSON(&resp)

	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return "", toAPIError(r.StatusCode(), resp.Error)
	}
	if len(resp.Data) <= 0 {
		// history project
		return tenantGroup, nil
	}
	return resp.Data[0].Id, nil
}

func (b *Bundle) GetMSPTenantProjects(userID, orgID string, withStats bool, projectIds []uint64) ([]*projpb.Project, error) {
	host := discover.MSP()
	hc := b.hc

	type GetTenantProjectsResponse struct {
		apistructs.Header
		projpb.GetProjectsResponse
	}

	var pidList []string
	for _, p := range projectIds {
		pidList = append(pidList, strconv.Itoa(int(p)))
	}

	var resp GetTenantProjectsResponse
	r, err := hc.Get(host).
		Path("/api/msp/tenant/projects").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, orgID).
		Param("withStats", strconv.FormatBool(withStats)).
		Params(map[string][]string{"projectId": pidList}).
		Do().
		JSON(&resp)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}

	return resp.Data, nil
}
