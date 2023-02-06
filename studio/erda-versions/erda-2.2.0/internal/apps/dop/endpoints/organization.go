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
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/legacy/httpendpoints/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateOrg 创建企业
func (e *Endpoints) CreateOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateOrg.NotLogin().ToResp(), nil
	}

	// 检查body合法性
	if r.Body == nil {
		return apierrors.ErrCreateOrg.MissingParameter("body").ToResp(), nil
	}

	var orgCreateReq apistructs.OrgCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&orgCreateReq); err != nil {
		return apierrors.ErrCreateOrg.InvalidParameter(err).ToResp(), nil
	}

	logrus.Infof("request body: %+v", orgCreateReq)

	// create org
	org, err := e.bdl.CreateOrg(identityInfo.UserID, &orgCreateReq)
	if err != nil {
		return apierrors.ErrCreateOrg.InternalError(err).ToResp(), nil
	}

	// create publisher
	if orgCreateReq.PublisherName != "" {
		pub := &apistructs.PublisherCreateRequest{
			Name:          orgCreateReq.PublisherName,
			PublisherType: "ORG",
			OrgID:         org.ID,
		}

		// 将企业管理员作为 publisher 管理员
		// 若无企业管理员，则将创建企业者作为管理员
		var managerID = identityInfo.UserID
		if len(orgCreateReq.Admins) > 0 {
			managerID = orgCreateReq.Admins[0]
		}
		pubID, err := e.publisher.Create(managerID, pub)
		if err != nil {
			return apierrors.ErrCreateOrg.InternalError(err).ToResp(), nil
		}
		org.PublisherID = pubID
	}

	// 异步保证企业级别 nexus group repo
	go func() {
		if err := e.org.EnsureNexusOrgGroupRepos(org); err != nil {
			logrus.Errorf("[alert] org nexus: failed to ensure org group repo when create org, orgID: %d, err: %v", org.ID, err)
			return
		}
	}()

	// create issueStage
	stageName := []string{
		"设计", "开发", "测试", "实施", "部署", "运维",
		"需求设计", "架构设计", "代码研发",
	}
	stage := []string{
		"design", "dev", "test", "implement", "deploy", "operator",
		"demandDesign", "architectureDesign", "codeDevelopment",
	}
	// stage
	var stages []dao.IssueStage
	for i := 0; i < 9; i++ {
		if i < 6 {
			stages = append(stages, dao.IssueStage{
				OrgID:     int64(org.ID),
				IssueType: apistructs.IssueTypeTask,
				Name:      stageName[i],
				Value:     stage[i],
			})
		} else {
			stages = append(stages, dao.IssueStage{
				OrgID:     int64(org.ID),
				IssueType: apistructs.IssueTypeBug,
				Name:      stageName[i],
				Value:     stage[i],
			})
		}
	}
	err = e.db.CreateIssueStage(stages)
	if err != nil {
		return apierrors.ErrCreateOrg.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(org)
}

// UpdateOrg update org
func (e *Endpoints) UpdateOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateOrg.NotLogin().ToResp(), nil
	}

	// 检查orgID合法性
	orgID, err := strutil.Atoi64(vars["orgID"])
	if err != nil {
		return apierrors.ErrUpdateOrg.InvalidParameter(err).ToResp(), nil
	}

	// 检查body合法性
	if r.Body == nil {
		return apierrors.ErrUpdateOrg.MissingParameter("body").ToResp(), nil
	}
	var orgUpdateReq apistructs.OrgUpdateRequestBody
	if err := json.NewDecoder(r.Body).Decode(&orgUpdateReq); err != nil {
		return apierrors.ErrUpdateOrg.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", orgUpdateReq)
	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   identityInfo.UserID,
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgID),
		Resource: apistructs.OrgResource,
		Action:   apistructs.UpdateAction,
	}
	if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
		return apierrors.ErrUpdateOrg.AccessDenied().ToResp(), nil
	}
	// update org
	org, err := e.bdl.UpdateOrg(identityInfo.UserID, orgID, &orgUpdateReq)
	if err != nil {
		return apierrors.ErrUpdateOrg.InternalError(err).ToResp(), nil
	}
	org.PublisherID = e.org.GetPublisherID(int64(org.ID))
	// 传递了publisherName并且当前org没有publisher才创建
	if orgUpdateReq.PublisherName != "" && org.PublisherID == 0 {
		publisherID, err := e.publisher.Create(identityInfo.UserID, &apistructs.PublisherCreateRequest{
			Name:          orgUpdateReq.PublisherName,
			PublisherType: "ORG",
			OrgID:         org.ID,
		})
		if err != nil {
			return apierrors.ErrUpdateOrg.InternalError(err).ToResp(), err
		}
		org.PublisherID = publisherID
	}
	// 异步保证企业级别 nexus group repo
	go func() {
		if err := e.org.EnsureNexusOrgGroupRepos(org); err != nil {
			logrus.Errorf("[alert] org nexus: failed to ensure org group repo when update org, orgID: %d, err: %v", org.ID, err)
			return
		}
		// TODO 写入 etcd 记录
	}()

	return httpserver.OkResp(org)
}

// GetOrg 获取企业详情
func (e *Endpoints) GetOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateOrg.NotLogin().ToResp(), nil
	}

	orgStr := vars["idOrName"]
	org, err := e.bdl.GetOrg(orgStr)
	if err != nil {
		return apierrors.ErrGetOrg.InternalError(err).ToResp(), nil
	}
	org.PublisherID = e.org.GetPublisherID(int64(org.ID))

	return httpserver.OkResp(org)
}

// DeleteOrg 删除企业
func (e *Endpoints) DeleteOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteOrg.NotLogin().ToResp(), nil
	}
	orgStr := vars["idOrName"]

	org, err := e.bdl.GetOrg(orgStr)
	if err != nil {
		return apierrors.ErrDeleteOrg.InternalError(err).ToResp(), nil
	}
	org.PublisherID = e.org.GetPublisherID(int64(org.ID))

	_, err = e.bdl.DeleteOrg(orgStr)
	if err != nil {
		return apierrors.ErrDeleteOrg.InternalError(err).ToResp(), nil
	}
	org.PublisherID = e.org.GetPublisherID(int64(org.ID))

	return httpserver.OkResp(org)
}

// ListOrg list org
func (e *Endpoints) ListOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListOrg.NotLogin().ToResp(), nil
	}
	// 获取请求参数
	req, err := getOrgListParam(r)
	if err != nil {
		return apierrors.ErrListOrg.InvalidParameter(err).ToResp(), nil
	}
	orgResp, err := e.bdl.ListOrgs(req, r.Header.Get(httputil.OrgHeader))
	if err != nil {
		return apierrors.ErrListOrg.InternalError(err).ToResp(), nil
	}
	// this may cause database pressure and this interface currently does not need these data
	// maybe add some cache when we need these data
	// for i := range orgResp.List {
	// 	orgResp.List[i].PublisherID = e.org.GetPublisherID(int64(orgResp.List[i].ID))
	// }
	return httpserver.OkResp(orgResp)
}

// ListPublicOrg list public org
func (e *Endpoints) ListPublicOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListPublicOrg.NotLogin().ToResp(), nil
	}

	req, err := getOrgListParam(r)
	if err != nil {
		return apierrors.ErrListPublicOrg.InvalidParameter(err).ToResp(), nil
	}

	orgResp, err := e.bdl.ListPublicOrgs(req)
	if err != nil {
		return apierrors.ErrListOrg.InternalError(err).ToResp(), nil
	}
	// this may cause database pressure and this interface currently does not need these data
	// maybe add some cache when we need these data
	// for i := range orgResp.List {
	// 	orgResp.List[i].PublisherID = e.org.GetPublisherID(int64(orgResp.List[i].ID))
	// }

	return httpserver.OkResp(orgResp)
}

// GetOrgByDomain 通过域名查询企业
func (e *Endpoints) GetOrgByDomain(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListPublicOrg.NotLogin().ToResp(), nil
	}
	domain := r.URL.Query().Get("domain")
	orgName := r.URL.Query().Get("orgName")
	if domain == "" {
		return apierrors.ErrGetOrg.MissingParameter("domain").ToResp(), nil
	}

	org, err := e.bdl.GetOrgByDomain(domain, orgName, identity.UserID)
	if err != nil {
		return apierrors.ErrGetOrg.InternalError(err).ToResp(), nil
	}
	if org == nil {
		return httpserver.OkResp(nil)
	}
	org.PublisherID = e.org.GetPublisherID(int64(org.ID))
	return httpserver.OkResp(org)
}

// CreateOrgPublisher 创建发布商
func (e *Endpoints) CreateOrgPublisher(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateOrgPublisher.NotLogin().ToResp(), nil
	}

	orgID, err := strconv.ParseUint(vars["orgID"], 10, 64)
	if err != nil {
		return apierrors.ErrCreateOrgPublisher.InvalidParameter(err).ToResp(), nil
	}

	// check permission
	if access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  orgID,
		Resource: apistructs.OrgResource,
		Action:   apistructs.CreateAction,
	}); err != nil || !access.Access {
		if err != nil {
			logrus.Errorf("failed to check permission when create org publisher, err: %v", err)
		}
		return apierrors.ErrCreateOrgPublisher.AccessDenied().ToResp(), nil
	}

	pubID := e.org.GetPublisherID(int64(orgID))
	if pubID != 0 {
		return apierrors.ErrUpdateOrg.InvalidParameter(errors.New("org already has publisher")).ToResp(), nil
	}

	publisherName := ""
	if r.Method == "POST" {
		var publisherCreateReq apistructs.CreateOrgPublisherRequest
		if err := json.NewDecoder(r.Body).Decode(&publisherCreateReq); err != nil {
			return apierrors.ErrCreateOrg.InvalidParameter(err).ToResp(), nil
		}
		publisherName = publisherCreateReq.Name
	} else {
		publisherName = r.URL.Query().Get("name")
	}

	if publisherName == "" {
		return apierrors.ErrUpdateOrg.InvalidParameter(errors.New("name is empty")).ToResp(), nil
	}

	pub := &apistructs.PublisherCreateRequest{
		Name:          publisherName,
		PublisherType: "ORG",
		OrgID:         orgID,
	}

	_, err = e.publisher.Create(userID.String(), pub)
	if err != nil {
		return apierrors.ErrUpdateOrg.InvalidParameter(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}

func (e *Endpoints) FetchOrgResources(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	langCodes := i18n.Language(r)
	ctx = context.WithValue(ctx, "lang_codes", langCodes)

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFetchOrgResources.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrFetchOrgResources.NotLogin().ToResp(), nil
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrFetchOrgResources.InvalidParameter("org id header").ToResp(), nil
	}

	if !identityInfo.IsInternalClient() {
		// 操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgID,
			Resource: apistructs.ResourceInfoResource,
			Action:   apistructs.GetAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrFetchOrgResources.AccessDenied().ToResp(), nil
		}
	}

	resource, err := e.org.FetchOrgClusterResource(ctx, orgID)
	if err != nil {
		return apierrors.ErrFetchOrgResources.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(resource)
}

// getOrgListParam get org params
func getOrgListParam(r *http.Request) (*apistructs.OrgSearchRequest, error) {
	q := r.URL.Query().Get("q")
	if q == "" {
		// Deprecated: TODO: remove it
		q = r.URL.Query().Get("key")
	}
	// Get paging info
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageNumStr := r.URL.Query().Get("pageNo")
	if pageNumStr == "" {
		pageNumStr = "1"
	}
	size, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, apierrors.ErrListOrg.InvalidParameter(strutil.Concat("PageSize: ", pageSizeStr))
	}
	num, err := strconv.Atoi(pageNumStr)
	if err != nil {
		return nil, apierrors.ErrListOrg.InvalidParameter(strutil.Concat("PageNo: ", pageNumStr))
	}
	if num <= 0 {
		num = 1
	}
	if size <= 0 {
		size = 20
	}
	return &apistructs.OrgSearchRequest{
		Q:        q,
		PageNo:   num,
		PageSize: size,
		Org:      r.Header.Get("org"),
		IdentityInfo: apistructs.IdentityInfo{
			UserID: r.Header.Get(httputil.UserHeader),
		},
	}, nil
}
