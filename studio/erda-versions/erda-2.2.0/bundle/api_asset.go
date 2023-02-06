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
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

const listRuntimesGroupByAppsAPI = "/api/runtimes/actions/group-by-apps"

func GetApplicationRuntimesAPI() string {
	return "/api/runtimes"
}

func GetRuntimeServicesAPI(runtimeID uint64) string {
	return "/api/runtimes/" + strconv.FormatUint(runtimeID, 10)
}

type CreateAPIAssetResponse struct {
	apistructs.Header
	Data apistructs.APIAssetID `json:"data"`
}

// CreateAPIAsset 创建 API Asset
func (b *Bundle) CreateAPIAsset(req apistructs.APIAssetCreateRequest) (apistructs.APIAssetID, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return "", err
	}
	hc := b.hc

	var createResp CreateAPIAssetResponse
	resp, err := hc.Post(host).Path("/api/api-assets").
		Header("Internal-Client", "bundle").
		Header("User-ID", req.IdentityInfo.UserID).
		JSONBody(&req).
		Do().JSON(&createResp)
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Header.Success {
		return "", toAPIError(resp.StatusCode(), createResp.Header.Error)
	}
	return createResp.Data, nil
}

type GetApplicationRuntimesResponse struct {
	apistructs.Header
	Data []*GetApplicationRuntimesDataEle
}

type GetApplicationRuntimesDataEle struct {
	ID                    uint64                                          `json:"id"`
	Name                  string                                          `json:"name"`
	ClusterID             uint64                                          `json:"clusterId"`
	ClusterName           string                                          `json:"clusterName"`
	ClusterType           string                                          `json:"clusterType"`
	CreatedAt             time.Time                                       `json:"createdAt"`
	DeleteStatus          string                                          `json:"deleteStatus"`
	DeployStatus          string                                          `json:"deployStatus"`
	Errors                interface{}                                     `json:"errors"`
	Extra                 *GetApplicationRuntimesDataEleExtra             `json:"extra"`
	LastMessage           interface{}                                     `json:"lastMessage"`
	LastOperateTime       time.Time                                       `json:"lastOperateTime"`
	LastOperator          string                                          `json:"lastOperator"`
	LastOperatorAvatar    string                                          `json:"lastOperatorAvatar"`
	LastOperatorName      string                                          `json:"lastOperatorName"`
	LastOperatorId        uint64                                          `json:"lastOperatorId"`
	ProjectID             uint64                                          `json:"projectId"`
	ReleaseID             string                                          `json:"releaseId"`
	ServiceGroupName      string                                          `json:"serviceGroupName"`
	ServiceGroupNamespace string                                          `json:"serviceGroupNamespace"`
	Services              map[string]*apistructs.RuntimeInspectServiceDTO `json:"services"`
	Source                string                                          `json:"source"`
	Status                string                                          `json:"status"`
	TimeCreated           time.Time                                       `json:"timeCreated"`
	UpdatedAt             time.Time                                       `json:"updatedAt"`
	Creator               string                                          `json:"creator"`
	ApplicationID         uint64                                          `json:"applicationId"`
	DeploymentOrderName   string                                          `json:"deploymentOrderName"`
	ReleaseVersion        string                                          `json:"releaseVersion"`
	RawStatus             string                                          `json:"rawStatus"`
	RawDeploymentStatus   string                                          `json:"rawDeploymentStatus"`
	// 忽略其他字段
}

type GetApplicationRuntimesDataEleExtra struct {
	ApplicationID uint64
	BuildID       uint64
	Workspace     string
}

// (cmdb) 获取 application 下的 runtimes
func (b *Bundle) GetApplicationRuntimes(applicationID uint64, orgID uint64, userID string) ([]*GetApplicationRuntimesDataEle, error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	var fetchResp GetApplicationRuntimesResponse

	hc := b.hc
	resp, err := hc.Get(host).
		Path(GetApplicationRuntimesAPI()).
		Param("applicationId", strconv.FormatUint(applicationID, 10)).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Do().
		JSON(&fetchResp)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}

// ListRuntimesGroupByApps queries the runtimes for the given applications ids
// if workspace not specified ,return all runtimes.
func (b *Bundle) ListRuntimesGroupByApps(orgID uint64, userID string, applicationsIDs []uint64, workspace string) (map[uint64][]*GetApplicationRuntimesDataEle, error) {
	l := logrus.WithField("func", "*Bundle.ListRuntimesGroupByApps")
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	type response struct {
		apistructs.Header
		Data map[uint64][]*GetApplicationRuntimesDataEle `json:"data"`
	}
	var fetchResp response
	request := b.hc.Get(host).
		Path(listRuntimesGroupByAppsAPI).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", strconv.FormatUint(orgID, 10))
	for _, appID := range applicationsIDs {
		request.Param("applicationID", strconv.FormatUint(appID, 10))
	}
	request.Param("workspace", workspace)
	resp, err := request.Do().JSON(&fetchResp)
	if err != nil {
		return nil, err
	}
	l.Debugf("body: %s", string(resp.Body()))
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}

type GetRuntimeServicesResponse struct {
	apistructs.Header
	Data *GetRuntimeServicesResponseData
}

type GetBatchRuntimeServicesResponse struct {
	apistructs.Header
	Data map[uint64]*GetRuntimeServicesResponseData
}

type GetRuntimeServicesResponseData struct {
	ID                    uint64                                            `json:"id"`
	Name                  string                                            `json:"name"`
	ServiceGroupName      string                                            `json:"serviceGroupName"`
	ServiceGroupNamespace string                                            `json:"serviceGroupNamespace"`
	Source                string                                            `json:"source"`
	Status                string                                            `json:"status"`
	DeployStatus          string                                            `json:"deployStatus"`
	DeleteStatus          string                                            `json:"deleteStatus"`
	ReleaseId             string                                            `json:"releaseId"`
	ClusterId             uint64                                            `json:"clusterId"`
	ClusterName           string                                            `json:"clusterName"`
	ClusterType           string                                            `json:"clusterType"`
	Extra                 *GetRuntimeServicesResponseDataExtra              `json:"extra"`
	ProjectID             uint64                                            `json:"projectId"`
	Services              map[string]*GetRuntimeServicesResponseDataService `json:"services"`
	// 其他字段略
}

type GetRuntimeServicesResponseDataExtra struct {
	ApplicationId uint64 `json:"applicationId"`
	BuildId       uint64 `json:"buildId"`
	Workspace     string `json:"workspace"`
}

type GetRuntimeServicesResponseDataService struct {
	Status string   `json:"status"`
	Addrs  []string `json:"addrs"`
	Expose []string `json:"expose"`
	// 其他字段略
}

func (b *Bundle) GetRuntimeServices(runtimeID uint64, orgID uint64, userID string) (*GetRuntimeServicesResponseData, error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}
	var (
		fetchResp GetRuntimeServicesResponse
		hc        = b.hc
	)

	resp, err := hc.Get(host).
		Path(GetRuntimeServicesAPI(runtimeID)).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Do().
		JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	logrus.Infof("GetRuntimeServices: respBody: %s", string(resp.Body()))

	return fetchResp.Data, nil
}

func (b *Bundle) BatchGetRuntimeServices(runtimeIDs []uint64, orgID, userID string) (map[uint64]*GetRuntimeServicesResponseData, error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}
	var (
		fetchResp GetBatchRuntimeServicesResponse
		hc        = b.hc
	)

	req := hc.Get(host).
		Path("/api/runtimesServices").
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", orgID)
	for i := 0; i < len(runtimeIDs); i++ {
		req.Param("runtimeID", strconv.FormatUint(runtimeIDs[i], 10))
	}
	resp, err := req.Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	logrus.Infof("BatchGetRuntimeServices: respBody: %s", string(resp.Body()))

	return fetchResp.Data, nil
}
