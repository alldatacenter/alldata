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
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateCertificate 创建证书
func (e *Endpoints) CreateCertificate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取 body 信息
	var certificateCreateReq apistructs.CertificateCreateRequest
	if r.Body == nil {
		return apierrors.ErrCreateCertificate.MissingParameter("body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&certificateCreateReq); err != nil {
		return apierrors.ErrCreateCertificate.InvalidParameter(err).ToResp(), nil
	}
	logrus.Debugf("create certificate request body: %+v", certificateCreateReq)

	// 获取 orgID
	if certificateCreateReq.OrgID == 0 {
		orgIDStr := r.Header.Get(httputil.OrgHeader)
		orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
		if err != nil {
			return apierrors.ErrUpdateCertificate.InvalidParameter(err).ToResp(), nil
		}
		certificateCreateReq.OrgID = orgID
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  certificateCreateReq.OrgID,
			Resource: apistructs.CertificateResource,
			Action:   apistructs.CreateAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrCreateCertificate.AccessDenied().ToResp(), nil
		}
	}

	certificate, err := e.certificate.Create(identityInfo.UserID, &certificateCreateReq)
	if err != nil {
		return apierrors.ErrCreateCertificate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(certificate)
}

// UpdateCertificate 更新Certificate
func (e *Endpoints) UpdateCertificate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取 body 信息
	var certificateUpdateReq apistructs.CertificateUpdateRequest
	if r.Body == nil {
		return apierrors.ErrUpdateCertificate.MissingParameter("body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&certificateUpdateReq); err != nil {
		return apierrors.ErrUpdateCertificate.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("update certificate request body: %+v", certificateUpdateReq)

	certificateID, err := strutil.Atoi64(vars["certificateID"])
	if err != nil {
		return apierrors.ErrGetCertificate.InvalidParameter(err).ToResp(), nil
	}

	// 检查certificateID合法性
	if certificateID == 0 {
		return apierrors.ErrUpdateCertificate.InvalidParameter("need certificate id.").ToResp(), nil
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		orgIDStr := r.Header.Get(httputil.OrgHeader)
		orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
		if err != nil {
			return apierrors.ErrUpdateCertificate.InvalidParameter(err).ToResp(), nil
		}

		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgID,
			Resource: apistructs.CertificateResource,
			Action:   apistructs.UpdateAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrUpdateCertificate.AccessDenied().ToResp(), nil
		}
	}

	// 更新Certificate信息至DB
	err = e.certificate.Update(certificateID, &certificateUpdateReq)
	if err != nil {
		return apierrors.ErrUpdateCertificate.InternalError(err).ToResp(), nil
	}

	//获取 certificate
	certificate, err := e.certificate.Get(certificateID)
	if err != nil {
		return apierrors.ErrDeleteCertificate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(&certificate)
}

// GetCertificate 获取Certificate详情
func (e *Endpoints) GetCertificate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 检查certificateID合法性
	certificateID, err := strutil.Atoi64(vars["certificateID"])
	if err != nil {
		return apierrors.ErrGetCertificate.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	// 若用户传的 appID 参数不为空，则使用 app 鉴权，否则使用企业鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		var (
			req = apistructs.PermissionCheckRequest{}
		)
		appIDStr := r.URL.Query().Get("appID")
		if appIDStr != "" {
			appID, err := strconv.ParseUint(appIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrGetCertificate.InvalidParameter(err).ToResp(), nil
			}

			req = apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.AppScope,
				ScopeID:  appID,
				Resource: apistructs.CertificateResource,
				Action:   apistructs.GetAction,
			}
		}

		access, err := e.bdl.CheckPermission(&req)
		if err != nil || !access.Access {
			orgIDStr := r.Header.Get(httputil.OrgHeader)
			orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrUpdateCertificate.InvalidParameter(err).ToResp(), nil
			}

			req = apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.OrgScope,
				ScopeID:  orgID,
				Resource: apistructs.CertificateResource,
				Action:   apistructs.GetAction,
			}
			if access, err = e.bdl.CheckPermission(&req); err != nil || !access.Access {
				return apierrors.ErrUpdateCertificate.AccessDenied().ToResp(), nil
			}
		}
	}

	certificate, err := e.certificate.Get(certificateID)
	if err != nil {
		if err == dao.ErrNotFoundCertificate {
			return apierrors.ErrGetCertificate.NotFound().ToResp(), nil
		}
		return apierrors.ErrGetCertificate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(*certificate)
}

// DeleteCertificate 删除Certificate
func (e *Endpoints) DeleteCertificate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 检查certificateID合法性
	certificateID, err := strutil.Atoi64(vars["certificateID"])
	if err != nil {
		return apierrors.ErrDeleteCertificate.InvalidParameter(err).ToResp(), nil
	}

	// 获取 orgID
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdateCertificate.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgID,
			Resource: apistructs.CertificateResource,
			Action:   apistructs.DeleteAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrUpdateCertificate.AccessDenied().ToResp(), nil
		}
	}

	//获取 certificate
	certificate, err := e.certificate.Get(certificateID)
	if err != nil {
		return apierrors.ErrDeleteCertificate.InternalError(err).ToResp(), nil
	}

	// 删除Certificate
	err = e.certificate.Delete(certificateID, int64(orgID))
	if err != nil {
		return apierrors.ErrDeleteCertificate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(&certificate)
}

// ListCertificates 所有Certificate列表
func (e *Endpoints) ListCertificates(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取请求参数
	params, err := getListCertificatesParam(r)
	if err != nil {
		return apierrors.ErrListCertificate.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		var (
			req = apistructs.PermissionCheckRequest{}
		)
		appIDStr := r.URL.Query().Get("appId")
		if appIDStr != "" {
			appID, err := strconv.ParseUint(appIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrListCertificate.InvalidParameter(err).ToResp(), nil
			}

			req = apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.AppScope,
				ScopeID:  appID,
				Resource: apistructs.CertificateResource,
				Action:   apistructs.ListAction,
			}
		}

		access, err := e.bdl.CheckPermission(&req)
		if err != nil || !access.Access {
			orgIDStr := r.Header.Get(httputil.OrgHeader)
			orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrListCertificate.InvalidParameter(err).ToResp(), nil
			}

			req = apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.OrgScope,
				ScopeID:  orgID,
				Resource: apistructs.CertificateResource,
				Action:   apistructs.GetAction,
			}
			if access, err = e.bdl.CheckPermission(&req); err != nil || !access.Access {
				return apierrors.ErrListCertificate.AccessDenied().ToResp(), nil
			}
		}
	}

	pagingCertificates, err := e.certificate.ListAllCertificates(params)
	if err != nil {
		return apierrors.ErrListCertificate.InternalError(err).ToResp(), nil
	}

	// userIDs
	userIDs := make([]string, 0, len(pagingCertificates.List))
	for _, n := range pagingCertificates.List {
		userIDs = append(userIDs, n.Creator, n.Operator)
	}
	userIDs = strutil.DedupSlice(userIDs, true)

	return httpserver.OkResp(*pagingCertificates, userIDs)
}

// Certificate列表时获取请求参数
func getListCertificatesParam(r *http.Request) (*apistructs.CertificateListRequest, error) {
	// 获取企业Id
	orgIDStr := r.URL.Query().Get("orgId")
	if orgIDStr == "" {
		orgIDStr = r.Header.Get(httputil.OrgHeader)
		if orgIDStr == "" {
			return nil, errors.Errorf("invalid param, orgId is empty")
		}
	}
	orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
	if err != nil {
		return nil, errors.Errorf("invalid param, orgId is invalid")
	}

	// 按Certificate名称搜索
	keyword := r.URL.Query().Get("q")

	// 获取pageSize
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageSize is invalid")
	}
	// 获取pageNo
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageNo is invalid")
	}

	return &apistructs.CertificateListRequest{
		OrgID:    uint64(orgID),
		Query:    keyword,
		Name:     r.URL.Query().Get("name"),
		Type:     r.URL.Query().Get("type"),
		Status:   r.URL.Query().Get("status"),
		PageNo:   pageNo,
		PageSize: pageSize,
	}, nil
}

// Certificate列表时获取请求参数
func listAppCertificatesParam(r *http.Request) (*apistructs.AppCertificateListRequest, error) {
	// 获取 appId
	appIDStr := r.URL.Query().Get("appId")
	appID, err := strconv.ParseInt(appIDStr, 10, 64)
	if err != nil {
		return nil, errors.Errorf("invalid param, appId is invalid")
	}

	// 获取pageSize
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageSize is invalid")
	}
	// 获取pageNo
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageNo is invalid")
	}

	return &apistructs.AppCertificateListRequest{
		AppID:    uint64(appID),
		Status:   r.URL.Query().Get("status"),
		PageNo:   pageNo,
		PageSize: pageSize,
	}, nil
}

// QuoteCertificate 应用引用证书
func (e *Endpoints) QuoteCertificate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取 body 信息
	var certificateQuoteReq apistructs.CertificateQuoteRequest
	if r.Body == nil {
		return apierrors.ErrQuoteCertificate.MissingParameter("body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&certificateQuoteReq); err != nil {
		return apierrors.ErrQuoteCertificate.InvalidParameter(err).ToResp(), nil
	}
	logrus.Debugf("create certificate request body: %+v", certificateQuoteReq)

	// 参数校验
	if certificateQuoteReq.AppID == 0 || certificateQuoteReq.CertificateID == 0 {
		return apierrors.ErrQuoteCertificate.InvalidParameter(errors.New("nil appId or certificateId")).ToResp(), nil
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrQuoteCertificate.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  certificateQuoteReq.AppID,
			Resource: apistructs.QuoteCertificateResource,
			Action:   apistructs.CreateAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrQuoteCertificate.AccessDenied().ToResp(), nil
		}
	}

	orgIDStr := r.Header.Get(httputil.OrgHeader)
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrQuoteCertificate.InvalidParameter(err).ToResp(), nil
	}

	err = e.appCertificate.Create(identityInfo.UserID, orgID, &certificateQuoteReq)
	if err != nil {
		return apierrors.ErrQuoteCertificate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("OK")
}

// CancelQuoteCertificate 应用删除引用Certificate
func (e *Endpoints) CancelQuoteCertificate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取 appID 和 certificateID
	appIDStr := r.URL.Query().Get("appId")
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCancelQuoteCertificate.InvalidParameter(err).ToResp(), nil
	}

	cerIDStr := r.URL.Query().Get("certificateId")
	cerID, err := strconv.ParseInt(cerIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCancelQuoteCertificate.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  appID,
			Resource: apistructs.QuoteCertificateResource,
			Action:   apistructs.DeleteAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrCancelQuoteCertificate.AccessDenied().ToResp(), nil
		}
	}

	err = e.appCertificate.Delete(int64(appID), cerID)
	if err != nil {
		return apierrors.ErrCancelQuoteCertificate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(cerID)
}

// ListQuoteCertificates 所有Certificate列表
func (e *Endpoints) ListQuoteCertificates(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取请求参数
	params, err := listAppCertificatesParam(r)
	if err != nil {
		return apierrors.ErrListQuoteCertificate.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}

	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  params.AppID,
			Resource: apistructs.CertificateResource,
			Action:   apistructs.ListAction,
		}

		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrListQuoteCertificate.AccessDenied().ToResp(), nil
		}

	}

	pagingCertificates, err := e.appCertificate.ListAllAppCertificates(params)
	if err != nil {
		return apierrors.ErrListQuoteCertificate.InternalError(err).ToResp(), nil
	}

	// userIDs
	userIDs := make([]string, 0, len(pagingCertificates.List))
	for _, n := range pagingCertificates.List {
		userIDs = append(userIDs, n.Creator, n.Operator)
	}
	userIDs = strutil.DedupSlice(userIDs, true)

	return httpserver.OkResp(*pagingCertificates, userIDs)
}

// PushCertificateConfig 推送证书配置到配置管理
func (e *Endpoints) PushCertificateConfig(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取 body 信息
	var certificatePushReq apistructs.PushCertificateConfigsRequest
	if r.Body == nil {
		return apierrors.ErrPushCertificateConfigs.MissingParameter("body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&certificatePushReq); err != nil {
		return apierrors.ErrPushCertificateConfigs.InvalidParameter(err).ToResp(), nil
	}
	logrus.Debugf("create certificate config request body: %+v", certificatePushReq)

	// 参数校验
	if certificatePushReq.AppID == 0 ||
		certificatePushReq.CertificateID == 0 {
		return apierrors.ErrPushCertificateConfigs.MissingParameter("need appId and certificateId and key").ToResp(), nil
	}

	// 操作鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  certificatePushReq.AppID,
			Resource: apistructs.QuoteCertificateResource,
			Action:   apistructs.CreateAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrPushCertificateConfigs.AccessDenied().ToResp(), nil
		}
	}

	err = e.appCertificate.PushConfigs(&certificatePushReq)
	if err != nil {
		return apierrors.ErrPushCertificateConfigs.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("OK")
}
