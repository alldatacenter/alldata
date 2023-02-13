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
	"io"
	"net/http"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/legacy/httpendpoints/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateAudits 创建审计事件
func (e *Endpoints) CreateAudits(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 鉴权，创建接口只允许内部调用
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		return apierrors.ErrCreateAudit.AccessDenied().ToResp(), nil
	}
	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrCreateAudit.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var auditCreateReq apistructs.AuditCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&auditCreateReq); err != nil {
		return apierrors.ErrCreateAudit.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("create request: %+v", auditCreateReq)
	// 检查事件创建请求是否合法
	if err := checkAuditCreateRequest(&auditCreateReq.Audit); err != nil {
		return apierrors.ErrCreateAudit.InvalidParameter(err).ToResp(), nil
	}

	// 创建信息至DB
	if err := e.audit.Create(auditCreateReq); err != nil {
		return apierrors.ErrCreateAudit.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("add audit succ")
}

// BatchCreateAudits 批量创建审计
func (e *Endpoints) BatchCreateAudits(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 鉴权，创建接口只允许内部调用
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		return apierrors.ErrCreateAudit.AccessDenied().ToResp(), nil
	}
	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrCreateAudit.MissingParameter("body").ToResp(), nil
	}
	var auditBatchCreateReq apistructs.AuditBatchCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&auditBatchCreateReq); err != nil {
		return apierrors.ErrCreateAudit.InvalidParameter(err).ToResp(), nil
	}

	var audits []apistructs.Audit
	for _, v := range auditBatchCreateReq.Audits {
		if err := checkAuditCreateRequest(&v); err != nil {
			return apierrors.ErrCreateAudit.InvalidParameter(err).ToResp(), nil
		}
		audits = append(audits, v)
	}

	// 创建信息至DB
	if err := e.audit.BatchCreateAudit(audits); err != nil {
		return apierrors.ErrCreateAudit.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("add audit succ")
}

// ListAudits 查看所有审计事件
func (e *Endpoints) ListAudits(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	var listReq apistructs.AuditsListRequest
	if err := e.queryStringDecoder.Decode(&listReq, r.URL.Query()); err != nil {
		return apierrors.ErrListAudit.InvalidParameter(err).ToResp(), nil
	}

	// 查询参数检查
	if err := listReq.Check(); err != nil {
		return apierrors.ErrListAudit.InvalidParameter(err).ToResp(), nil
	}

	// 权限检查
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListAudit.NotLogin().ToResp(), nil
	}

	if identityInfo.UserID != "" || !identityInfo.IsInternalClient() {
		access, err := e.permission.CheckPermission(getPermissionBody(&listReq, identityInfo))
		if err != nil {
			return apierrors.ErrListAudit.InternalError(err).ToResp(), nil
		}
		if !access {
			return apierrors.ErrListAudit.AccessDenied().ToResp(), nil
		}
	}

	total, audits, err := e.audit.List(&listReq)
	if err != nil {
		return apierrors.ErrListAudit.InternalError(err).ToResp(), nil
	}

	l := len(audits)
	auditList := make([]apistructs.Audit, 0, l)
	userIDs := make([]string, 0, l)
	for _, item := range audits {
		context := make(map[string]interface{})
		json.Unmarshal([]byte(item.Context), &context)
		audit := apistructs.Audit{
			ID:           item.ID,
			UserID:       item.UserID,
			ScopeType:    item.ScopeType,
			ScopeID:      item.ScopeID,
			OrgID:        item.OrgID,
			FDPProjectID: item.FDPProjectID,
			ProjectID:    item.ProjectID,
			AppID:        item.AppID,
			Context:      context,
			TemplateName: apistructs.TemplateName(item.TemplateName),
			AuditLevel:   item.AuditLevel,
			Result:       apistructs.Result(item.Result),
			ErrorMsg:     item.ErrorMsg,
			StartTime:    item.StartTime.Format("2006-01-02 15:04:05"),
			EndTime:      item.EndTime.Format("2006-01-02 15:04:05"),
			ClientIP:     item.ClientIP,
			UserAgent:    item.UserAgent,
		}
		auditList = append(auditList, audit)
		userIDs = append(userIDs, item.UserID)
	}

	return httpserver.OkResp(apistructs.AuditsListResponseData{
		Total: total,
		List:  auditList,
	}, userIDs)
}

// ExportExcelAudit 导出审计到 excel
func (e *Endpoints) ExportExcelAudit(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) (err error) {
	var listReq apistructs.AuditsListRequest
	if err := e.queryStringDecoder.Decode(&listReq, r.URL.Query()); err != nil {
		return apierrors.ErrExportExcelAudit.InvalidParameter(err)
	}

	// 查询参数检查
	if err := listReq.Check(); err != nil {
		return apierrors.ErrExportExcelAudit.InvalidParameter(err)
	}

	ctx = context.WithValue(ctx, "lang_codes", i18n.Language(r))
	// 权限检查
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrExportExcelAudit.NotLogin()
	}
	if identityInfo.UserID != "" || !identityInfo.IsInternalClient() {
		access, err := e.permission.CheckPermission(getPermissionBody(&listReq, identityInfo))
		if err != nil {
			return apierrors.ErrExportExcelAudit.InternalError(err)
		}
		if !access {
			return apierrors.ErrExportExcelAudit.AccessDenied()
		}
	}
	listReq.PageNo = 1
	listReq.PageSize = 99999

	_, audits, err := e.audit.List(&listReq)
	if err != nil {
		return apierrors.ErrExportExcelAudit.InternalError(err)
	}

	reader, tablename, err := e.audit.ExportExcel(ctx, audits)
	if err != nil {
		return apierrors.ErrExportExcelAudit.InternalError(err)
	}
	w.Header().Add("Content-Disposition", "attachment;fileName="+tablename+".xlsx")
	w.Header().Add("Content-Type", "application/vnd.ms-excel")

	if _, err := io.Copy(w, reader); err != nil {
		return apierrors.ErrExportExcelAudit.InternalError(err)
	}
	return nil
}

// PutAuditsSettings 对企业设置事件清理周期
func (e *Endpoints) PutAuditsSettings(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 检查body格式
	var auditSetReq apistructs.AuditSetCleanCronRequest
	if err := json.NewDecoder(r.Body).Decode(&auditSetReq); err != nil {
		return apierrors.ErrCreateAudit.InvalidParameter(err).ToResp(), nil
	}

	orgID, interval, err := checkSetAuditParam(auditSetReq)
	if err != nil {
		return apierrors.ErrCreateAudit.InvalidParameter(err).ToResp(), nil
	}

	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateAuditSettings.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		access, err := e.permission.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  auditSetReq.OrgID,
			Resource: apistructs.AuditResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return apierrors.ErrUpdateAuditSettings.InternalError(err).ToResp(), nil
		}
		if !access {
			return apierrors.ErrUpdateAuditSettings.AccessDenied().ToResp(), nil
		}
	}

	// 创建信息至DB
	if err := e.audit.UpdateAuditCleanCron(orgID, interval); err != nil {
		return apierrors.ErrUpdateAuditSettings.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(auditSetReq.OrgID)
}

// GetAuditsSettings 获取企业设置事件清理周期
func (e *Endpoints) GetAuditsSettings(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgIDStr := r.URL.Query().Get("orgId")
	if orgIDStr == "" {
		return nil, errors.Errorf("invalid request, orgId is empty")
	}
	orgID, err := strutil.Atoi64(orgIDStr)
	if err != nil {
		return nil, errors.Errorf("invalid request, orgId is invalid")
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListAuditSettings.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		access, err := e.permission.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(orgID),
			Resource: apistructs.AuditResource,
			Action:   apistructs.ListAction,
		})
		if err != nil {
			return apierrors.ErrListAuditSettings.InternalError(err).ToResp(), nil
		}
		if !access {
			return apierrors.ErrListAuditSettings.AccessDenied().ToResp(), nil
		}
	}

	audit, err := e.audit.GetAuditCleanCron(orgID)
	if err != nil {
		return apierrors.ErrListAuditSettings.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(audit)
}

func getPermissionBody(listReq *apistructs.AuditsListRequest, identityInfo apistructs.IdentityInfo) *apistructs.PermissionCheckRequest {
	pcr := &apistructs.PermissionCheckRequest{
		UserID:   identityInfo.UserID,
		Resource: apistructs.AuditResource,
		Action:   apistructs.ListAction,
	}

	if listReq.Sys {
		pcr.Scope = apistructs.SysScope
	} else {
		pcr.Scope = apistructs.OrgScope
		pcr.ScopeID = uint64(listReq.OrgID)
	}

	return pcr
}

func checkAuditCreateRequest(req *apistructs.Audit) error {
	// 事件发生时间不能为空，转换成字符串
	if req.StartTime == "" {
		return errors.Errorf("invalid request, StartTime couldn't be empty")
	}
	sTime, err := strutil.Atoi64(req.StartTime)
	if err != nil {
		return err
	}
	req.StartTime = time.Unix(sTime, 0).Format("2006-01-02 15:04:05")
	//事件结束时间不为空，转换成字符串
	if req.EndTime == "" {
		return errors.Errorf("invalid request, EndTime couldn't be empty")
	}
	eTime, err := strutil.Atoi64(req.EndTime)
	if err != nil {
		return err
	}
	req.EndTime = time.Unix(eTime, 0).Format("2006-01-02 15:04:05")
	// 事件主角不能为空
	if req.UserID == "" {
		return errors.Errorf("invalid request, UserID couldn't be empty")
	}
	// 事件 scope 类型不能为空
	if req.ScopeType == "" {
		return errors.Errorf("invalid request, ScopeType couldn't be empty")
	}
	// 事件 scope id 不能为空
	if req.ScopeID == 0 && req.ScopeType != apistructs.SysScope {
		return errors.Errorf("invalid request, ScopeID couldn't be empty")
	}
	// 事件模版名不能为空
	if req.TemplateName == "" {
		return errors.Errorf("invalid request, TemplateName couldn't be empty")
	}

	return nil
}

func checkSetAuditParam(auditSetReq apistructs.AuditSetCleanCronRequest) (int64, int64, error) {
	var orgID, interval int64
	if auditSetReq.Interval < 1 || auditSetReq.Interval > conf.OrgAuditMaxRetentionDays() {
		return orgID, interval, errors.Errorf("invalid request, max retention days should be between 1 and %d", conf.OrgAuditMaxRetentionDays())
	}

	interval, orgID = int64(-auditSetReq.Interval), int64(auditSetReq.OrgID)

	return orgID, interval, nil
}
