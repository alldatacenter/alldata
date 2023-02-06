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
	"net/http"
	"net/url"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GetApp get app by id from core-service.
func (b *Bundle) GetApp(id uint64) (*apistructs.ApplicationDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.ApplicationFetchResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/applications/%d", id)).Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

func (b *Bundle) GetMyApps(userid string, orgid uint64) (*apistructs.ApplicationListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var listResp apistructs.ApplicationListResponse
	resp, err := hc.Get(host).
		Path("/api/applications/actions/list-my-applications").
		Header(httputil.OrgHeader, strconv.FormatUint(orgid, 10)).
		Header(httputil.UserHeader, userid).
		Param("pageSize", "9999").
		Param("pageNo", "1").
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}
	return &listResp.Data, nil
}

func (b *Bundle) GetMyAppsByProject(userid string, orgid, projectID uint64, appName string) (*apistructs.ApplicationListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var listResp apistructs.ApplicationListResponse
	resp, err := hc.Get(host).
		Path("/api/applications/actions/list-my-applications").
		Header(httputil.OrgHeader, strconv.FormatUint(orgid, 10)).
		Header(httputil.UserHeader, userid).
		Param("pageSize", "9999").
		Param("pageNo", "1").
		Param("projectId", strconv.FormatUint(projectID, 10)).
		Param("name", appName).
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}
	return &listResp.Data, nil
}

// GetAppsByProject 根据 projectID 获取应用列表
func (b *Bundle) GetAppsByProject(projectID, orgID uint64, userID string) (*apistructs.ApplicationListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var listResp apistructs.ApplicationListResponse
	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/api/applications")).
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Header(httputil.UserHeader, userID).
		Param("projectId", strconv.FormatUint(projectID, 10)).
		Param("pageSize", "10000").
		Param("pageNo", "1").
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return &listResp.Data, nil
}

func (b *Bundle) GetAppList(orgID, userID string, req apistructs.ApplicationListRequest) (*apistructs.ApplicationListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var appIDs []string
	for _, v := range req.ApplicationID {
		appIDs = append(appIDs, strconv.Itoa(int(v)))
	}

	var listResp apistructs.ApplicationListResponse
	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/api/applications")).
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("name", req.Name).
		Param("mode", req.Mode).
		Param("q", req.Query).
		Param("public", req.Public).
		Param("projectId", strconv.FormatUint(req.ProjectID, 10)).
		Param("isSimple", strconv.FormatBool(req.IsSimple)).
		Param("orderBy", req.OrderBy).
		Params(map[string][]string{"applicationID": appIDs}).
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return &listResp.Data, nil
}

// get applications by projectID and app name
func (b *Bundle) GetAppsByProjectAndAppName(projectID, orgID uint64, userID string, appName string, header ...http.Header) (*apistructs.ApplicationListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var listResp apistructs.ApplicationListResponse
	q := hc.Get(host).
		Path("/api/applications").
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Header(httputil.UserHeader, userID).
		Param("projectId", strconv.FormatUint(projectID, 10)).
		Param("pageSize", "1").
		Param("pageNo", "1").
		Param("name", appName)

	if len(header) > 0 {
		q.Headers(header[0])
	}

	resp, err := q.Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return &listResp.Data, nil
}

// GetAppsByProjectSimple 根据 projectID 获取应用列表简单信息
func (b *Bundle) GetAppsByProjectSimple(projectID, orgID uint64, userID string) (*apistructs.ApplicationListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var listResp apistructs.ApplicationListResponse
	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/api/applications")).
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Header(httputil.UserHeader, userID).
		Param("projectId", strconv.FormatUint(projectID, 10)).
		Param("pageSize", "9999").
		Param("pageNo", "1").
		Param("isSimple", "true").
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return &listResp.Data, nil
}

// Deprecated
type AbilityAppReq struct {
	OrgId           uint64 `json:"orgId"`
	ClusterId       uint64 `json:"clusterId"`
	ClusterName     string `json:"clusterName"`
	ApplicationName string `json:"applicationName"`
	Operator        string `json:"operator"`
}

// GetAppPublishItemRelationsGroupByENV 根据 appID 获取应用关联的发布内容
func (b *Bundle) GetAppPublishItemRelationsGroupByENV(appID uint64) (*apistructs.QueryAppPublishItemRelationGroupByENVResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var relationResp apistructs.QueryAppPublishItemRelationGroupByENVResponse
	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/api/applications/%d/actions/get-publish-item-relations", appID)).
		Do().JSON(&relationResp)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("failed to fetch publish item relation, statusCode: %d", resp.StatusCode())
	}
	if !relationResp.Success {
		return nil, errors.Errorf("failed to fetch publish item relation, err: %s", relationResp.Error.Msg)
	}
	return &relationResp, nil
}

// QueryAppPublishItemRelations 查询应用关联的发布内容
func (b *Bundle) QueryAppPublishItemRelations(req *apistructs.QueryAppPublishItemRelationRequest) (*apistructs.QueryAppPublishItemRelationResponse, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var relationResp apistructs.QueryAppPublishItemRelationResponse
	resp, err := hc.Get(host).
		Path("/api/applications/actions/query-publish-item-relations").
		Param("publishItemID", strconv.FormatInt(req.PublishItemID, 10)).
		Param("appID", strconv.FormatInt(req.AppID, 10)).
		Param("ak", req.AK).Param("ai", req.AI).
		Do().JSON(&relationResp)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("failed to fetch publish item relation, statusCode: %d", resp.StatusCode())
	}
	if !relationResp.Success {
		return nil, errors.Errorf("failed to fetch publish item relation, err: %s", relationResp.Error.Msg)
	}
	return &relationResp, nil
}

func (b *Bundle) RemoveAppPublishItemRelations(publishItemID int64) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	req := apistructs.RemoveAppPublishItemRelationsRequest{
		PublishItemId: publishItemID,
	}
	hc := b.hc
	var getResp apistructs.RemoveAppPublishItemRelationsResponse
	resp, err := hc.Post(host).Path("/api/applications/actions/remove-publish-item-relations").
		Header("Internal-Client", "bundle").JSONBody(req).
		Do().JSON(&getResp)
	if err != nil {
		apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return toAPIError(resp.StatusCode(), getResp.Error)
	}
	return nil
}

// get my apps by paging
func (b *Bundle) GetAllMyApps(userid string, orgid uint64, req apistructs.ApplicationListRequest) (*apistructs.ApplicationListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var appIDs []string
	for _, v := range req.ApplicationID {
		appIDs = append(appIDs, strconv.Itoa(int(v)))
	}

	var listResp apistructs.ApplicationListResponse
	resp, err := hc.Get(host).
		Path("/api/applications/actions/list-my-applications").
		Header(httputil.OrgHeader, strconv.FormatUint(orgid, 10)).
		Header(httputil.UserHeader, userid).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("name", req.Name).
		Param("mode", req.Mode).
		Param("q", req.Query).
		Param("public", req.Public).
		Param("projectId", strconv.FormatUint(req.ProjectID, 10)).
		Param("isSimple", strconv.FormatBool(req.IsSimple)).
		Param("orderBy", req.OrderBy).
		Params(map[string][]string{"applicationID": appIDs}).
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}
	return &listResp.Data, nil
}

// CreateApp create app
// This will no longer create gittar repo
func (b *Bundle) CreateApp(req apistructs.ApplicationCreateRequest, userID string) (*apistructs.ApplicationDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.ApplicationCreateResponse
	resp, err := hc.Post(host).Path("/api/applications").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(&req).Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

// CreateAppWithRepo Create app with gittar repo
func (b *Bundle) CreateAppWithRepo(req apistructs.ApplicationCreateRequest, userID string) (*apistructs.ApplicationDTO, error) {
	appDto, err := b.CreateApp(req, userID)
	if err != nil {
		return nil, err
	}
	defer func() {
		if err != nil {
			logrus.Infof("delete app, appID: %d", appDto.ID)
			_, deleteAppErr := b.DeleteApp(appDto.ID, userID)
			if deleteAppErr != nil {
				logrus.Errorf("failed to delete app, err: %v", deleteAppErr)
			}
			if deleteRepoErr := b.DeleteRepo(int64(appDto.ID)); deleteRepoErr != nil {
				logrus.Errorf("failed to delete repo, err: %v", deleteRepoErr)
			}
		}
	}()
	repoReq := apistructs.CreateRepoRequest{
		OrgID:       int64(appDto.OrgID),
		OrgName:     appDto.OrgName,
		ProjectID:   int64(appDto.ProjectID),
		ProjectName: appDto.ProjectName,
		AppName:     appDto.Name,
		IsExternal:  req.IsExternalRepo,
		Config:      req.RepoConfig,
	}
	if req.IsExternalRepo {
		repoReq.OnlyCheck = true
		_, err = b.CreateRepo(repoReq)
		if err != nil {
			return nil, errors.Errorf("failed to create repo, err: %v", err)
		}
	}
	repoReq.OnlyCheck = false
	repoReq.AppID = int64(appDto.ID)
	_, err = b.CreateRepo(repoReq)
	if err != nil {
		return nil, errors.Errorf("failed to create repo, err: %v", err)
	}
	return appDto, nil
}

// UpdateApp update app
func (b *Bundle) UpdateApp(req apistructs.ApplicationUpdateRequestBody, appID uint64, userID string) (interface{}, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.ApplicationUpdateResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/applications/%d", appID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(&req).Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

// DeleteApp delete app
func (b *Bundle) DeleteApp(appID uint64, userID string) (*apistructs.ApplicationDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.ApplicationDeleteResponse
	resp, err := hc.Delete(host).Path(fmt.Sprintf("/api/applications/%d", appID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

// CountAppByProID count app by proID
func (b *Bundle) CountAppByProID(proID uint64) (int64, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var fetchResp apistructs.CountAppResponse
	resp, err := hc.Get(host).Path("/api/applications/actions/count").
		Header(httputil.InternalHeader, "bundle").
		Param("projectID", strconv.FormatUint(proID, 10)).
		Do().JSON(&fetchResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return 0, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}

func (b *Bundle) GetAppIDByNames(projectID uint64, userID string, names []string) (*apistructs.GetAppIDByNamesResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getAppIDByNamesResp apistructs.GetAppIDByNamesResponse
	resp, err := hc.Get(host).Path("/api/applications/actions/get-id-by-names").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Param("projectID", strconv.FormatUint(projectID, 10)).
		Params(url.Values{"name": names}).
		Do().JSON(&getAppIDByNamesResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), getAppIDByNamesResp.Error)
	}
	return &getAppIDByNamesResp.Data, nil
}
