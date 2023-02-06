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
	"bytes"
	"fmt"
	"net/url"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/msp/menu/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// GetProject get project by id from core-services.
func (b *Bundle) GetProject(id uint64) (*apistructs.ProjectDTO, error) {
	return b.GetProjectWithSetter(id)
}

// GetProjectWithSetter get project by id from core-services.
func (b *Bundle) GetProjectWithSetter(id uint64, requestSetter ...httpclient.RequestSetter) (*apistructs.ProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.ProjectDetailResponse
	r := hc.Get(host, httpclient.RetryOption{}).Path(fmt.Sprintf("/api/projects/%d", id)).
		Header(httputil.InternalHeader, "bundle")
	for _, setter := range requestSetter {
		setter(r)
	}
	resp, err := r.Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}
	if fetchResp.Data.Name == "" {
		return nil, nil
	}
	return &fetchResp.Data, nil
}

// GetProjectByOrgIdAndName get project by orgId and name from cmdb.
func (b *Bundle) GetProjectByOrgIdAndName(orgId uint64, name string, userID string) (*apistructs.ProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.ProjectListResponse
	resp, err := hc.Get(host, httpclient.RetryOption{}).Path(fmt.Sprintf("/api/projects?name=%s&orgId=%d&joined=false&pageNo=1&pageSize=1", name, orgId)).
		Header("User-ID", userID).Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}
	if rsp.Data.Total == 0 {
		return nil, nil
	}
	return &rsp.Data.List[0], nil
}

func (b *Bundle) ListProject(userID string, req apistructs.ProjectListRequest) (*apistructs.PagingProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.ProjectListResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/projects")).
		Param("orgId", strconv.FormatUint(req.OrgID, 10)).
		Param("q", req.Query).
		Param("name", req.Name).
		Param("orderBy", req.OrderBy).
		Param("asc", strconv.FormatBool(req.Asc)).
		Param("joined", strconv.FormatBool(req.Joined)).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Param("isPublic", strconv.FormatBool(req.IsPublic)).
		Header("User-ID", userID).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}
	if rsp.Data.Total == 0 {
		return nil, nil
	}
	return &rsp.Data, nil
}

func (b *Bundle) ListDopProject(userID string, req apistructs.ProjectListRequest) (*apistructs.PagingProjectDTO, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.ProjectListResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/projects")).
		Param("orgId", strconv.FormatUint(req.OrgID, 10)).
		Param("q", req.Query).
		Param("name", req.Name).
		Param("orderBy", req.OrderBy).
		Param("asc", strconv.FormatBool(req.Asc)).
		Param("joined", strconv.FormatBool(req.Joined)).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Param("isPublic", strconv.FormatBool(req.IsPublic)).
		Header("User-ID", userID).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}
	if rsp.Data.Total == 0 {
		return nil, nil
	}
	return &rsp.Data, nil
}

// ListMyProject 获取用户加入的项目
func (b *Bundle) ListMyProject(userID string, req apistructs.ProjectListRequest) (*apistructs.PagingProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.ProjectListResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/projects/actions/list-my-projects")).
		Param("orgId", strconv.FormatUint(req.OrgID, 10)).
		Param("q", req.Query).
		Param("name", req.Name).
		Param("joined", strconv.FormatBool(req.Joined)).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Param("isPublic", strconv.FormatBool(req.IsPublic)).
		Param("orderBy", req.OrderBy).
		Param("asc", strconv.FormatBool(req.Asc)).
		Param("keepMsp", strconv.FormatBool(req.KeepMsp)).
		Header("User-ID", userID).
		Header("Org-ID", strconv.FormatUint(req.OrgID, 10)).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}
	if rsp.Data.Total == 0 {
		return nil, nil
	}
	return &rsp.Data, nil
}

// ListPublicProject 获取公开项目列表
func (b *Bundle) ListPublicProject(userID string, req apistructs.ProjectListRequest) (*apistructs.PagingProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.ProjectListResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/projects/actions/list-public-projects")).
		Param("q", req.Query).
		Param("name", req.Name).
		Param("joined", strconv.FormatBool(req.Joined)).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Param("isPublic", strconv.FormatBool(req.IsPublic)).
		Header("User-ID", userID).
		Header("Org-ID", strconv.FormatUint(req.OrgID, 10)).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}
	if rsp.Data.Total == 0 {
		return nil, nil
	}
	return &rsp.Data, nil
}

func (b *Bundle) ListProjectsEnvAndTenantId(userID, orgID, tenantId, Type string) ([]*apistructs.MenuItem, error) {
	host, err := b.urls.MSP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp = apistructs.GetMenuResponse{}
	var req = pb.GetMenuRequest{}
	req.Type = Type
	req.TenantId = tenantId
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/msp/tenant/menu")).
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}
	return rsp.Data, err
}

// UpdateProjectActiveTime 更新项目活跃时间
func (b *Bundle) UpdateProjectActiveTime(req apistructs.ProjectActiveTimeUpdateRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Put(host).Path("/api/projects/actions/update-active-time").
		Header(httputil.InternalHeader, "bundle").JSONBody(&req).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to update project active time, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}

	return nil
}

// GetWorkspaceClusterByAppBranch 根据 appID 和 branch 返回环境和集群
func (b *Bundle) GetWorkspaceClusterByAppBranch(appID uint64, gitRef string) (
	app *apistructs.ApplicationDTO,
	project *apistructs.ProjectDTO,
	branchRule *apistructs.ValidBranch,
	workspace apistructs.DiceWorkspace,
	clusterName string,
	err error,
) {
	// app
	app, err = b.GetApp(appID)
	if err != nil {
		return
	}
	// get project branch rule for workspace
	branchRule, err = b.GetBranchWorkspaceConfigByProject(app.ProjectID, gitRef)
	if err != nil {
		return
	}
	workspace = apistructs.DiceWorkspace(branchRule.Workspace)
	// get clusterName from project
	project, err = b.GetProject(app.ProjectID)
	if err != nil {
		return
	}
	for _ws, _clusterName := range project.ClusterConfig {
		if strutil.Equal(_ws, workspace.String(), true) {
			clusterName = _clusterName
			break
		}
	}
	return
}

// GetProjectNamespaceInfo 获取项目级命名空间信息
func (b *Bundle) GetProjectNamespaceInfo(projectID uint64) (*apistructs.ProjectNameSpaceInfo, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.ProjectNameSpaceInfoResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/projects/%d/actions/get-ns-info", projectID)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}

	return &rsp.Data, nil
}

// IsEnabledProjectNamespaceWithoutErr 忽略错误获取项目级命名空间是否开启
func (b *Bundle) IsEnabledProjectNamespaceWithoutErr(projectID uint64) bool {
	prjNs, err := b.GetProjectNamespaceInfo(projectID)
	if err != nil {
		logrus.Errorf("[project namespace] check project namespace is enabled err: %v", err)
		return false
	}

	return prjNs.Enabled
}

// GetProjectNamespaceWithoutErr 忽略错误根据workspace获取项目级命名空间
func (b *Bundle) GetProjectNamespaceWithoutErr(projectID uint64, workspace string) string {
	var isValidWS bool
	for _, v := range apistructs.DiceWorkspaceSlice {
		if workspace == v.String() {
			isValidWS = true
			break
		}
	}

	if !isValidWS {
		return ""
	}

	prjNs, err := b.GetProjectNamespaceInfo(projectID)
	if err != nil {
		logrus.Errorf("[project namespace] get project namespace err: %v", err)
		return ""
	}

	if !prjNs.Enabled {
		return ""
	}

	return prjNs.Namespaces[workspace]
}

func (b *Bundle) GetMSProjects(orgID, userID string, params url.Values) ([]apistructs.MicroServiceProjectResponseData, error) {
	hc := b.hc
	var getResp *apistructs.MicroServiceProjectResponse
	resp, err := hc.Get(discover.Orchestrator()).
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		Path("/api/microservice/projects").
		Params(params).Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return getResp.Data, nil
}

// GetMyProjectIDs get projectIDs by orgID adn userID from core-services.
func (b *Bundle) GetMyProjectIDs(orgID uint64, userID string) ([]uint64, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.MyProjectIDsResponse
	resp, err := hc.Get(host, httpclient.RetryOption{}).
		Path(fmt.Sprintf("/api/projects/actions/list-my-projectIDs")).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, strconv.FormatInt(int64(orgID), 10)).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}
	return fetchResp.Data, nil
}

// GetProjectListByStates list projects by states
func (b *Bundle) GetProjectListByStates(req apistructs.GetProjectIDListByStatesRequest) (*apistructs.GetProjectIDListByStatesData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.GetProjectIDListByStatesResponse
	resp, err := hc.Get(host).Path("/api/projects/actions/list-by-states").
		Header(httputil.InternalHeader, "bundle").JSONBody(&req).Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

// GetAllProjects get all projects
func (b *Bundle) GetAllProjects() ([]apistructs.ProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.GetAllProjectsResponse
	resp, err := hc.Get(host).Path("/api/projects/actions/list-all").
		Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}

// CreateProject create project
func (b *Bundle) CreateProject(req apistructs.ProjectCreateRequest, userID string) (uint64, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var fetchResp apistructs.ProjectCreateResponse
	resp, err := hc.Post(host).Path("/api/projects").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(&req).Do().JSON(&fetchResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return 0, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}

// DeleteProject delete project
func (b *Bundle) DeleteProject(id, orgID uint64, userID string) (*apistructs.ProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.ProjectDeleteResponse
	resp, err := hc.Delete(host).Path(fmt.Sprintf("/api/projects/%d", id)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

func (b *Bundle) UpdateProject(req apistructs.ProjectUpdateRequest, orgID uint64, userID string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var updateResp apistructs.ProjectUpdateResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/projects/%d", req.ProjectID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		JSONBody(&req.Body).Do().JSON(&updateResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return toAPIError(resp.StatusCode(), updateResp.Error)
	}

	return nil
}

// Get projects map
func (b *Bundle) GetProjectsMap(req apistructs.GetModelProjectsMapRequest) (map[uint64]apistructs.ProjectDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.GetModelProjectsMapResponse
	resp, err := hc.Get(host).Path("/api/projects/actions/get-projects-map").
		Header(httputil.InternalHeader, "bundle").JSONBody(&req).Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}

	return rsp.Data, nil
}
