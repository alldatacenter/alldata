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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/core/legacy/utils"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/filehelper"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateOrg 创建企业
func (e *Endpoints) CreateOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateOrg.NotLogin().ToResp(), nil
	}

	isAdmin := e.member.IsAdmin(userID.String())

	// 操作鉴权, 只有系统管理员可创建企业
	// when env: create_org_enabled is true, allow all people create org
	if !conf.CreateOrgEnabled() {
		if !isAdmin {
			return apierrors.ErrCreateOrg.AccessDenied().ToResp(), nil
		}
	}

	// 检查body合法性
	if r.Body == nil {
		return apierrors.ErrCreateOrg.MissingParameter("body").ToResp(), nil
	}

	var orgCreateReq apistructs.OrgCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&orgCreateReq); err != nil {
		return apierrors.ErrCreateOrg.InvalidParameter(err).ToResp(), nil
	}

	// check the org name is invalid
	if !utils.IsValidOrgName(orgCreateReq.Name) {
		return apierrors.ErrCreateOrg.InvalidParameter(errors.Errorf("org name is invalid %s",
			orgCreateReq.Name)).ToResp(), nil
	}

	// check if it is free org, currently only admin can create paid organizations
	if orgCreateReq.Type != apistructs.FreeOrgType && !isAdmin {
		return apierrors.ErrCreateOrg.AccessDenied().ToResp(), nil
	}
	// compatible logic, delete this after perfecting the logic of organization creation
	if orgCreateReq.Type == "" && isAdmin {
		orgCreateReq.Type = apistructs.EnterpriseOrgType
	}

	logrus.Infof("request body: %+v", orgCreateReq)

	// 第一次创建企业的时候还没有集群，没有集群创建ingress会出错，先注释掉了
	// if err := e.bdl.CreateOrUpdateComponentIngress(apistructs.ComponentIngressUpdateRequest{
	// 	ComponentName: "ui",
	// 	ComponentPort: 80,
	// 	IngressName:   orgCreateReq.Name + "-org",
	// 	Routes: []apistructs.IngressRoute{
	// 		{
	// 			Domain: orgCreateReq.Name + "-org." + conf.RootDomain(),
	// 			Path:   "/",
	// 		},
	// 	},
	// 	RouteOptions: apistructs.RouteOptions{
	// 		Annotations: map[string]string{
	// 			"nginx.ingress.kubernetes.io/proxy-body-size":   "0",
	// 			"nginx.ingress.kubernetes.io/enable-access-log": "false",
	// 		},
	// 	},
	// }); err != nil {
	// 	return apierrors.ErrCreateOrg.InternalError(err).ToResp(), nil
	// }

	org, err := e.org.CreateWithEvent(userID.String(), orgCreateReq)
	if err != nil {
		return apierrors.ErrCreateOrg.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(e.convertToOrgDTO(*org))
}

// UpdateOrgIngress 更新企业Ingress
func (e *Endpoints) UpdateOrgIngress(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 检查orgID合法性
	orgID, err := strutil.Atoi64(vars["orgID"])
	if err != nil {
		return apierrors.ErrUpdateOrgIngress.InvalidParameter(err).ToResp(), nil
	}

	org, err := e.org.Get(orgID)
	if err != nil {
		return apierrors.ErrUpdateOrgIngress.InvalidParameter(err).ToResp(), nil
	}
	if err := e.bdl.CreateOrUpdateComponentIngress(apistructs.ComponentIngressUpdateRequest{
		ComponentName: "ui",
		ComponentPort: 80,
		IngressName:   org.Name + "-org",
		Routes: []apistructs.IngressRoute{
			{
				Domain: org.Name + "-org." + conf.RootDomain(),
				Path:   "/",
			},
		},
		RouteOptions: apistructs.RouteOptions{
			Annotations: map[string]string{
				"nginx.ingress.kubernetes.io/proxy-body-size":   "0",
				"nginx.ingress.kubernetes.io/enable-access-log": "false",
			},
		},
	}); err != nil {
		return apierrors.ErrCreateOrg.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(e.convertToOrgDTO(*org))
}

// UpdateOrg 更新企业
func (e *Endpoints) UpdateOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
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
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgID),
		Resource: apistructs.OrgResource,
		Action:   apistructs.UpdateAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrUpdateOrg.AccessDenied().ToResp(), nil
	}
	// 更新企业信息至DB
	org, auditMessage, err := e.org.UpdateWithEvent(orgID, orgUpdateReq)
	if err != nil {
		return apierrors.ErrUpdateOrg.InternalError(err).ToResp(), nil
	}
	orgDTO := e.convertToOrgDTO(*org)
	orgDTO.AuditMessage = auditMessage

	return httpserver.OkResp(orgDTO)
}

// GetOrg 获取企业详情
func (e *Endpoints) GetOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		userID user.ID
		org    *model.Org
		err    error
	)
	// 检查orgID合法性
	orgStr := vars["idOrName"]
	orgID, _ := strutil.Atoi64(orgStr)
	if orgID == 0 { // 按 orgName 查询
		org, err = e.org.GetByName(orgStr)
		if err != nil {
			return apierrors.ErrGetOrg.InternalError(err).ToResp(), nil
		}
	} else { // 按 orgID 查询
		org, err = e.org.Get(orgID)
		if err != nil {
			return apierrors.ErrGetOrg.InternalError(err).ToResp(), nil
		}
	}

	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		userID, err = user.GetUserID(r)
		if err != nil {
			return apierrors.ErrGetOrg.NotLogin().ToResp(), nil
		}
		// 操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(org.ID),
			Resource: apistructs.OrgResource,
			Action:   apistructs.GetAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			return apierrors.ErrGetOrg.AccessDenied().ToResp(), nil
		}
	}

	result := e.convertToOrgDTO(*org)
	// 不是内部调用不返回配置信息
	if internalClient == "" {
		result.HidePassword()
	}
	// 封装org返回结构
	return httpserver.OkResp(result)
}

// DeleteOrg 删除企业
func (e *Endpoints) DeleteOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteOrg.NotLogin().ToResp(), nil
	}

	var org *model.Org
	// 检查orgID合法性
	orgStr := vars["idOrName"]
	orgID, _ := strutil.Atoi64(orgStr)
	if orgID == 0 { // 按 orgName 查询
		org, err = e.org.GetByName(orgStr)
		if err != nil {
			return apierrors.ErrDeleteOrg.InternalError(err).ToResp(), nil
		}
	} else { // 按 orgID 查询
		org, err = e.org.Get(orgID)
		if err != nil {
			return apierrors.ErrDeleteOrg.InternalError(err).ToResp(), nil
		}
	}

	if !identityInfo.IsInternalClient() {
		// 操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(org.ID),
			Resource: apistructs.OrgResource,
			Action:   apistructs.DeleteAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			return apierrors.ErrDeleteOrg.AccessDenied().ToResp(), nil
		}
	}

	// 删除企业
	if err = e.org.Delete(org.ID); err != nil {
		return errorresp.ErrResp(err)
	}

	// 封装org返回结构
	return httpserver.OkResp(e.convertToOrgDTO(*org))
}

// ListOrg 查询企业 GET /api/orgs?key=xxx(按企业名称过滤)
func (e *Endpoints) ListOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	all, orgIDs, err := e.getOrgPermissions(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	// 获取请求参数
	req, err := getOrgListParam(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	var (
		total int
		orgs  []model.Org
	)
	total, orgs, err = e.org.ListOrgs(orgIDs, req, all)
	if err != nil {
		logrus.Warnf("failed to get orgs, (%v)", err)
		return apierrors.ErrListOrg.InternalError(err).ToResp(), nil
	}

	orgDTOs, err := e.coverOrgsToDto(r, orgs)
	if err != nil {
		return apierrors.ErrListOrg.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(apistructs.PagingOrgDTO{
		List:  orgDTOs,
		Total: total,
	})
}

func (e *Endpoints) coverOrgsToDto(r *http.Request, orgs []model.Org) ([]apistructs.OrgDTO, error) {
	var currentOrgID int64
	var err error
	v := r.Header.Get(httputil.OrgHeader)
	if v != "" {
		// ignore convert error
		currentOrgID, err = strutil.Atoi64(v)
		if err != nil {
			return nil, err
		}
	}
	if currentOrgID == 0 {
		// compatible TODO: refactor it
		userID, _ := user.GetUserID(r)
		currentOrgID, _ = e.org.GetCurrentOrgByUser(userID.String())
	}

	// 封装成API所需格式
	orgDTOs := make([]apistructs.OrgDTO, 0, len(orgs))
	var selected bool // 是否已选中某企业flag
	for _, org := range orgs {
		orgDTO := e.convertToOrgDTO(org)
		if (currentOrgID == 0 || currentOrgID == org.ID) && !selected {
			orgDTO.Selected = true
			selected = true
		}
		orgDTO.HidePassword()
		orgDTOs = append(orgDTOs, orgDTO)
	}
	if !selected && len(orgDTOs) > 0 { // 用户选中某个企业后被踢出此企业后
		orgDTOs[0].Selected = true
	}

	return orgDTOs, nil
}

// ListPublicOrg Get public orgs
func (e *Endpoints) ListPublicOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListPublicOrg.NotLogin().ToResp(), nil
	}

	req, err := getOrgListParam(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	total, orgs, err := e.org.SearchPublicOrgsByName(req.Q, req.PageNo, req.PageSize)
	if err != nil {
		return apierrors.ErrListPublicOrg.InternalError(err).ToResp(), nil
	}

	orgDTOs := make([]apistructs.OrgDTO, 0, len(orgs))
	for _, org := range orgs {
		orgDTO := e.convertToOrgDTO(org)
		orgDTO.HidePassword()
		orgDTOs = append(orgDTOs, orgDTO)
	}

	return httpserver.OkResp(apistructs.PagingOrgDTO{
		List:  orgDTOs,
		Total: total,
	})
}

// GetOrgByDomain 通过域名查询企业
func (e *Endpoints) GetOrgByDomain(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	domain := r.URL.Query().Get("domain")
	orgName := r.URL.Query().Get("orgName")
	if domain == "" {
		return apierrors.ErrGetOrg.MissingParameter("domain").ToResp(), nil
	}

	org, err := e.org.GetOrgByDomainAndOrgName(domain, orgName)
	if err != nil {
		return apierrors.ErrGetOrg.InternalError(err).ToResp(), nil
	}
	if org == nil {
		return httpserver.OkResp(nil)
	}
	return httpserver.OkResp(e.convertToOrgDTO(*org, domain))
}

// Deprecated: no need to store selection
// ChangeCurrentOrg 切换当前企业
func (e *Endpoints) ChangeCurrentOrg(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrChangeOrg.InvalidParameter(err).ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrChangeOrg.MissingParameter("body is nil").ToResp(), nil
	}
	var req apistructs.OrgChangeRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrChangeOrg.InvalidParameter(err).ToResp(), nil
	}

	if err = e.org.ChangeCurrentOrg(userID.String(), &req); err != nil {
		return apierrors.ErrChangeOrg.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(true)
}

// CreateOrgClusterRelation 创建企业集群关联关系
func (e *Endpoints) CreateOrgClusterRelation(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrRelateCluster.NotLogin().ToResp(), nil
	}

	// 解析body参数
	if r.Body == nil {
		return apierrors.ErrRelateCluster.MissingParameter("body").ToResp(), nil
	}
	var req apistructs.OrgClusterRelationCreateRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrRelateCluster.InvalidParameter(err).ToResp(), nil
	}

	if req.OrgID == 0 {
		return apierrors.ErrRelateCluster.MissingParameter("org id").ToResp(), nil
	}
	if req.ClusterName == "" {
		return apierrors.ErrRelateCluster.MissingParameter("cluster name").ToResp(), nil
	}

	if err = e.org.RelateCluster(userID.String(), &req); err != nil {
		return apierrors.ErrRelateCluster.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("success")
}

// ListOrgClusterRelation 获取所有企业对应集群关系
func (e *Endpoints) ListOrgClusterRelation(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		err  error
		rels []model.OrgClusterRelation
	)
	_, err = user.GetUserID(r)
	if err != nil {
		return apierrors.ErrRelateCluster.NotLogin().ToResp(), nil
	}

	cluster := r.URL.Query().Get("cluster")
	if cluster == "" {
		rels, err = e.org.ListAllOrgClusterRelation()
		if err != nil {
			return apierrors.ErrGetOrgClusterRelation.InternalError(err).ToResp(), nil
		}
	} else {
		rels, err = e.db.GetOrgClusterRelationsByCluster(cluster)
		if err != nil {
			return apierrors.ErrGetOrgClusterRelationsByOrg.InvalidParameter(err).ToResp(), nil
		}
	}

	var relDTOs []apistructs.OrgClusterRelationDTO
	for _, rel := range rels {
		relDTOs = append(relDTOs, convertToOrgClusterRelationDTO(rel))
	}
	return httpserver.OkResp(relDTOs)
}

func (e *Endpoints) convertToOrgDTO(org model.Org, domains ...string) apistructs.OrgDTO {
	domain := ""
	if len(domains) > 0 {
		domain = domains[0]
	}
	domainAndPort := strutil.Split(domain, ":", true)
	port := ""
	if len(domainAndPort) > 1 {
		port = domainAndPort[1]
	}
	concatDomain := conf.UIDomain()
	if port != "" {
		concatDomain = strutil.Concat(conf.UIDomain(), ":", port)
	}

	orgDto := apistructs.OrgDTO{
		ID:          uint64(org.ID),
		Name:        org.Name,
		Desc:        org.Desc,
		Logo:        filehelper.APIFileUrlRetriever(org.Logo),
		Locale:      org.Locale,
		Domain:      concatDomain,
		Creator:     org.UserID,
		OpenFdp:     org.OpenFdp,
		DisplayName: org.DisplayName,
		Type:        org.Type,
		Config: &apistructs.OrgConfig{
			EnablePersonalMessageEmail: org.Config.EnablePersonalMessageEmail,
			EnableMS:                   org.Config.EnableMS,
			SMTPHost:                   org.Config.SMTPHost,
			SMTPUser:                   org.Config.SMTPUser,
			SMTPPassword:               org.Config.SMTPPassword,
			SMTPPort:                   org.Config.SMTPPort,
			SMTPIsSSL:                  org.Config.SMTPIsSSL,
			SMSKeyID:                   org.Config.SMSKeyID,
			SMSKeySecret:               org.Config.SMSKeySecret,
			SMSSignName:                org.Config.SMSSignName,
			SMSMonitorTemplateCode:     org.Config.SMSMonitorTemplateCode, // 监控单独的短信模版
			VMSKeyID:                   org.Config.VMSKeyID,
			VMSKeySecret:               org.Config.VMSKeySecret,
			VMSMonitorTtsCode:          org.Config.VMSMonitorTtsCode, // 监控单独的语音模版
			VMSMonitorCalledShowNumber: org.Config.VMSMonitorCalledShowNumber,
		},
		IsPublic: org.IsPublic,
		BlockoutConfig: apistructs.BlockoutConfig{
			BlockDEV:   org.BlockoutConfig.BlockDEV,
			BlockTEST:  org.BlockoutConfig.BlockTEST,
			BlockStage: org.BlockoutConfig.BlockStage,
			BlockProd:  org.BlockoutConfig.BlockProd,
		},
		EnableReleaseCrossCluster: org.Config.EnableReleaseCrossCluster,
		CreatedAt:                 org.CreatedAt,
		UpdatedAt:                 org.UpdatedAt,
	}
	if orgDto.DisplayName == "" {
		orgDto.DisplayName = orgDto.Name
	}
	return orgDto
}

func convertToOrgClusterRelationDTO(rel model.OrgClusterRelation) apistructs.OrgClusterRelationDTO {
	return apistructs.OrgClusterRelationDTO{
		ID:          uint64(rel.ID),
		OrgID:       rel.OrgID,
		OrgName:     rel.OrgName,
		ClusterID:   rel.ClusterID,
		ClusterName: rel.ClusterName,
		Creator:     rel.Creator,
		CreatedAt:   rel.CreatedAt,
		UpdatedAt:   rel.UpdatedAt,
	}
}

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
	}, nil
}

func (e *Endpoints) getOrgPermissions(r *http.Request) (bool, []int64, error) {
	// first check user
	userID, _ := user.GetUserID(r)

	//verify calls from bundle
	if userID == "" && r.Header.Get(httputil.InternalHeader) != "" {
		return true, nil, nil
	}
	// 操作鉴权, 系统管理员可查询企业
	// Found that org is passed in the request header, even admin does not query all organizations
	if e.member.IsAdmin(userID.String()) && (r.URL.Query().Get("org") == "" || r.URL.Query().Get("org") == "-") { // 系统管理员可查看所有企业列表
		return true, nil, nil
	} else { // 非系统管理员只能查看有权限的企业列表
		members, err := e.member.ListByScopeTypeAndUser(apistructs.OrgScope, userID.String())
		if err != nil {
			return false, nil, apierrors.ErrListOrg.InternalError(err)
		}
		var orgIDs []int64
		for i := range members {
			orgIDs = append(orgIDs, members[i].ScopeID)
		}
		return false, orgIDs, nil
	}
}

// TODO: need refactor
func (e *Endpoints) getPermOrgs(r *http.Request) (bool, []int64, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return false, nil, apierrors.ErrListOrg.NotLogin()
	}
	// 操作鉴权, 系统管理员可查询企业
	if e.member.IsAdmin(userID.String()) { // 系统管理员可查看所有企业列表
		return true, nil, nil
	} else { // 非系统管理员只能查看有权限的企业列表
		members, err := e.member.ListByScopeTypeAndUser(apistructs.OrgScope, userID.String())
		if err != nil {
			return false, nil, apierrors.ErrListOrg.InternalError(err)
		}
		var orgIDs []int64
		for i := range members {
			orgIDs = append(orgIDs, members[i].ScopeID)
		}
		return false, orgIDs, nil
	}
}

func (e *Endpoints) SetReleaseCrossCluster(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrSetReleaseCrossCluster.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.SysScope,
			Resource: apistructs.OrgResource,
			Action:   apistructs.UpdateAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			return apierrors.ErrSetReleaseCrossCluster.AccessDenied().ToResp(), nil
		}
	}
	enable, err := strconv.ParseBool(r.URL.Query().Get("enable"))
	if err != nil {
		return apierrors.ErrSetReleaseCrossCluster.InvalidParameter("invalid bool query: enable").ToResp(), nil
	}
	orgID, err := strconv.ParseUint(vars["orgID"], 10, 64)
	if err != nil {
		return apierrors.ErrSetReleaseCrossCluster.InvalidParameter("orgID").ToResp(), nil
	}
	if err := e.org.SetReleaseCrossCluster(orgID, enable); err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(nil)
}

// GenVerifiCode 生成邀请成员加入企业的验证码
func (e *Endpoints) GenVerifiCode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetOrgVerifiCode.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrGetOrgVerifiCode.MissingParameter("org-id is empty").ToResp(), nil
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetOrgVerifiCode.InvalidParameter("org id header").ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgID,
			Resource: apistructs.MemberResource,
			Action:   apistructs.CreateAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			return apierrors.ErrGetOrgVerifiCode.AccessDenied().ToResp(), nil
		}
	}

	// 获取验证码
	verifiCode, err := e.org.GenVerifiCode(identityInfo, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(map[string]string{"verifyCode": verifiCode})
}

// SetNotifyConfig 设置通知配置
func (e *Endpoints) SetNotifyConfig(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		userID user.ID
		err    error
	)
	// 检查orgID合法性
	orgID, err := strutil.Atoi64(vars["orgID"])
	if err != nil {
		return apierrors.ErrGetNotifyConfig.InvalidParameter(err).ToResp(), nil
	}

	// 检查body合法性
	if r.Body == nil {
		return apierrors.ErrUpdateOrg.MissingParameter("body").ToResp(), nil
	}
	var nCfgUpdateReq apistructs.NotifyConfigUpdateRequestBody
	if err := json.NewDecoder(r.Body).Decode(&nCfgUpdateReq); err != nil {
		return apierrors.ErrUpdateOrg.InvalidParameter(err).ToResp(), nil
	}

	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		userID, err = user.GetUserID(r)
		if err != nil {
			return apierrors.ErrGetNotifyConfig.NotLogin().ToResp(), nil
		}
		// 操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(orgID),
			Resource: apistructs.NotifyConfigResource,
			Action:   apistructs.UpdateAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			return apierrors.ErrGetNotifyConfig.AccessDenied().ToResp(), nil
		}
	}

	if err := e.org.SetNotifyConfig(orgID, nCfgUpdateReq); err != nil {
		return apierrors.ErrGetNotifyConfig.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("succes")
}

// GetNotifyConfig 获取通知配置
func (e *Endpoints) GetNotifyConfig(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// var (
	// userID user.ID
	// 	err error
	// )
	// 检查orgID合法性
	orgID, err := strutil.Atoi64(vars["orgID"])
	if err != nil {
		return apierrors.ErrGetNotifyConfig.InvalidParameter(err).ToResp(), nil
	}

	// internalClient := r.Header.Get(httputil.InternalHeader)
	// if internalClient == "" {
	// 	userID, err = user.GetUserID(r)
	// 	if err != nil {
	// 		return apierrors.ErrGetNotifyConfig.NotLogin().ToResp(), nil
	// 	}
	// 	// 操作鉴权
	// 	req := apistructs.PermissionCheckRequest{
	// 		UserID:   userID.String(),
	// 		Scope:    apistructs.OrgScope,
	// 		ScopeID:  uint64(orgID),
	// 		Resource: apistructs.NotifyConfigResource,
	// 		Action:   apistructs.GetAction,
	// 	}
	// 	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
	// 		return apierrors.ErrGetNotifyConfig.AccessDenied().ToResp(), nil
	// 	}
	// }

	result, err := e.org.GetNotifyConfig(orgID)
	if err != nil {
		return apierrors.ErrGetNotifyConfig.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(apistructs.NotifyConfigUpdateRequestBody{
		Config: result,
	})
}

// GetOrgClusterRelationsByOrg get orgClusters relation by orgID
func (e *Endpoints) GetOrgClusterRelationsByOrg(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetOrgClusterRelationsByOrg.NotLogin().ToResp(), nil
	}

	orgID, err := strutil.Atoi64(vars["orgID"])
	if err != nil {
		return apierrors.ErrGetOrgClusterRelationsByOrg.InvalidParameter(err).ToResp(), nil
	}

	orgList, err := e.db.GetOrgClusterRelationsByOrg(orgID)
	if err != nil {
		return apierrors.ErrGetOrgClusterRelationsByOrg.InvalidParameter(err).ToResp(), nil
	}

	orgDtoList := make([]apistructs.OrgClusterRelationDTO, 0, len(orgList))
	for _, v := range orgList {
		orgDtoList = append(orgDtoList, convertToOrgClusterRelationDTO(v))
	}
	return httpserver.OkResp(orgDtoList)
}

// DereferenceCluster 解除关联集群关系
func (e *Endpoints) DereferenceCluster(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDereferenceCluster.NotLogin().ToResp(), nil
	}
	orgIDStr := r.URL.Query().Get("orgID")
	if orgIDStr == "" {
		return apierrors.ErrDereferenceCluster.MissingParameter("orgID").ToResp(), nil
	}
	var orgID int64
	if orgID, err = strutil.Atoi64(orgIDStr); err != nil {
		return apierrors.ErrListCluster.InvalidParameter(err).ToResp(), nil
	}
	clusterName := r.URL.Query().Get("clusterName")
	if clusterName == "" {
		return apierrors.ErrDereferenceCluster.MissingParameter("clusterName").ToResp(), nil
	}
	req := apistructs.DereferenceClusterRequest{
		OrgID:   orgID,
		Cluster: clusterName,
	}
	if err := e.member.CheckPermission(identity.UserID, apistructs.OrgScope, req.OrgID); err != nil {
		return apierrors.ErrDereferenceCluster.InternalError(err).ToResp(), nil
	}
	if err := e.org.DereferenceCluster(identity.UserID, &req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp("delete succ")
}
