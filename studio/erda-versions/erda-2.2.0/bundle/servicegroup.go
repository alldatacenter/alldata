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
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateServiceGroup create servicegroup
func (b *Bundle) CreateServiceGroup(sg apistructs.ServiceGroupCreateV2Request) error {
	var resp apistructs.ServiceGroupCreateV2Response
	if err := callOrchestrator(b, sg, &resp, "/api/servicegroup", b.hc.Post); err != nil {
		return err
	}
	if !resp.Success {
		return toAPIError(200, resp.Error)
	}
	return nil
}

// DeleteServiceGroup delete servicegroup
func (b *Bundle) DeleteServiceGroup(namespace, name string) error {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}
	var resp apistructs.ServiceGroupDeleteV2Response
	r, err := b.hc.Delete(host).Path("/api/servicegroup").
		Param("name", name).Param("namespace", namespace).
		Do().JSON(&resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() {
		return apierrors.ErrInvoke.InternalError(fmt.Errorf("statuscode: %d", r.StatusCode()))
	}
	if !resp.Success && resp.Error.Code == "404" {
		logrus.Errorf("deleteservicegroup: %s/%s not found", namespace, name)
		return nil
	}
	if !resp.Success {
		return toAPIError(200, resp.Error)
	}
	return nil
}

// ForceDeleteServiceGroup delete service group with force option
func (b *Bundle) ForceDeleteServiceGroup(req apistructs.ServiceGroupDeleteRequest) error {
	var force string
	if req.Force {
		force = "true"
	} else {
		force = "false"
	}
	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}
	var resp apistructs.ServiceGroupDeleteV2Response
	r, err := b.hc.Delete(host).Path("/api/servicegroup").
		Param("name", req.Name).Param("namespace", req.Namespace).Param("force", force).
		Do().JSON(&resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() {
		return apierrors.ErrInvoke.InternalError(fmt.Errorf("statuscode: %d", r.StatusCode()))
	}
	if !resp.Success && resp.Error.Code == "404" {
		logrus.Errorf("deleteservicegroup: %s/%s not found", req.Namespace, req.Name)
		return nil
	}
	if !resp.Success {
		return toAPIError(200, resp.Error)
	}
	return nil
}

// InspectServiceGroup get servicegroup info
func (b *Bundle) InspectServiceGroup(namespace, name string) (
	*apistructs.ServiceGroup, error) {
	sg := apistructs.ServiceGroupInfoRequest{Type: namespace, ID: name}
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}
	var resp apistructs.ServiceGroupInfoResponse
	r, err := b.hc.Get(host).Path("/api/servicegroup").Param("namespace", sg.Type).Param("name", sg.ID).Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// InspectServiceGroup get servicegroup info
func (b *Bundle) ServiceGroupConfigUpdate(sg apistructs.ServiceGroup) error {
	var resp apistructs.ServiceGroupConfigUpdateResponse
	if err := callOrchestrator(b, sg, &resp, "/api/servicegroup/actions/config", b.hc.Put); err != nil {
		return err
	}
	if !resp.Success {
		return toAPIError(200, resp.Error)
	}
	return nil
}

// TODO: an ugly hack, need refactor, it may cause goroutine explosion
func (b *Bundle) InspectServiceGroupWithTimeout(namespace, name string) (*apistructs.ServiceGroup, error) {
	var (
		sg  *apistructs.ServiceGroup
		err error
	)
	done := make(chan struct{}, 1)
	go func() {
		sg, err = b.InspectServiceGroup(namespace, name)
		done <- struct{}{}
	}()
	select {
	case <-done:
		return sg, err
	case <-time.After(time.Duration(conf.InspectServiceGroupTimeout()) * time.Second):
		return nil, apierrors.ErrInvoke.InternalError(fmt.Errorf("timeout for invoke getServiceGroup"))
	}
}

// GetInstanceInfo 实例状态 list
func (b *Bundle) GetInstanceInfo(req apistructs.InstanceInfoRequest) (*apistructs.InstanceInfoResponse, error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}
	params := make(url.Values)
	if req.Cluster != "" {
		params.Add("cluster", req.Cluster)
	}
	if req.OrgName != "" {
		params.Add("orgName", req.OrgName)
	}
	if req.OrgID != "" {
		params.Add("orgID", req.OrgID)
	}
	if req.ProjectName != "" {
		params.Add("projectName", req.ProjectName)
	}
	if req.ProjectID != "" {
		params.Add("projectID", req.ProjectID)
	}
	if req.ApplicationName != "" {
		params.Add("applicationName", req.ApplicationName)
	}
	if req.ApplicationID != "" {
		params.Add("applicationID", req.ApplicationID)
	}
	if req.RuntimeName != "" {
		params.Add("runtimeName", req.RuntimeName)
	}
	if req.RuntimeID != "" {
		params.Add("runtimeID", req.RuntimeID)
	}
	if req.ServiceName != "" {
		params.Add("serviceName", req.ServiceName)
	}
	if req.Workspace != "" {
		params.Add("workspace", req.Workspace)
	}
	if req.ContainerID != "" {
		params.Add("containerID", req.ContainerID)
	}
	if req.ServiceType != "" {
		params.Add("serviceType", req.ServiceType)
	}
	if req.AddonID != "" {
		params.Add("addonID", req.AddonID)
	}
	if req.InstanceIP != "" {
		params.Add("instanceIP", req.InstanceIP)
	}
	if req.HostIP != "" {
		params.Add("hostIP", req.HostIP)
	}
	if len(req.Phases) != 0 {
		params.Add("phases", strutil.Join(req.Phases, ",", true))
	}
	if req.Limit != 0 {
		params.Add("limit", strconv.Itoa(req.Limit))
	}
	var resp apistructs.InstanceInfoResponse
	r, err := b.hc.Get(host).Path(fmt.Sprintf("/api/instanceinfo")).Params(params).Do().JSON(&resp)
	if err != nil {
		return nil, err
	}
	if !r.IsOK() {
		return nil, fmt.Errorf("failed to get instanceinfo: req: %+v, statuscode: %d", req, r.StatusCode())
	}
	return &resp, nil
}

// PARAM: brief, 是否需要已使用资源信息
func (b *Bundle) ResourceInfo(clustername string, brief bool) (*apistructs.ClusterResourceInfoData, error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}
	params := make(url.Values)
	if brief {
		params.Add("brief", "true")
	}
	var resp apistructs.ClusterResourceInfoResponse
	r, err := b.hc.Get(host).
		Path(fmt.Sprintf("/api/resourceinfo/%s", clustername)).
		Params(params).
		Do().JSON(&resp)
	if err != nil {
		return nil, err
	}
	if !r.IsOK() {
		return nil, fmt.Errorf("failed to get resource info, statuscode: %d", r.StatusCode())
	}
	return &resp.Data, nil
}

func callOrchestrator(b *Bundle, req, resp interface{}, path string,
	httpfunc func(host string, retry ...httpclient.RetryOption) *httpclient.Request) error {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}
	r, err := httpfunc(host).Path(path).JSONBody(req).Do().JSON(resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() {
		return apierrors.ErrInvoke.InternalError(fmt.Errorf("statuscode: %d, %v", r.StatusCode(), string(r.Body())))
	}
	return nil
}
