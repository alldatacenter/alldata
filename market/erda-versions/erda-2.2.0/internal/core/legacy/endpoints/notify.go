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
	"errors"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"unicode/utf8"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// CreateNotify 创建通知
func (e *Endpoints) CreateNotify(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	locale := e.GetLocale(r)
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrCreateNotify.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrCreateNotify.MissingParameter("body is nil").ToResp(), nil
	}

	var notifyCreateReq apistructs.CreateNotifyRequest
	if err := json.NewDecoder(r.Body).Decode(&notifyCreateReq); err != nil {
		return apierrors.ErrCreateNotify.InvalidParameter("can't decode body").ToResp(), nil
	}
	if strings.TrimSpace(notifyCreateReq.Name) == "" {
		return apierrors.ErrCreateNotify.InvalidParameter("name is empty").ToResp(), nil
	}
	if utf8.RuneCountInString(notifyCreateReq.Name) > 50 {
		return apierrors.ErrCreateNotify.InvalidParameter(locale.Get("ErrCreateNotifyGroup.NameTooLong")).ToResp(), nil
	}
	notifyCreateReq.Creator = r.Header.Get("User-Id")
	notifyCreateReq.OrgID = orgID
	if notifyCreateReq.WithGroup == false && notifyCreateReq.NotifyGroupID == 0 {
		return apierrors.ErrCreateNotify.InvalidParameter("notifyGroupId is null").ToResp(), nil
	}
	err = e.notifyGroup.CheckNotifyChannels(notifyCreateReq.Channels)
	if err != nil {
		return apierrors.ErrCreateNotify.InvalidParameter(err.Error()).ToResp(), nil
	}

	err = e.checkNotifyPermission(r, notifyCreateReq.ScopeType, notifyCreateReq.ScopeID, apistructs.CreateAction)
	if err != nil {
		return apierrors.ErrCreateNotify.AccessDenied().ToResp(), nil
	}

	notifyID, err := e.notifyGroup.CreateNotify(locale, &notifyCreateReq)
	if err != nil {
		return apierrors.ErrCreateNotify.InternalError(err).ToResp(), nil
	}

	notify, err := e.notifyGroup.GetNotify(notifyID, orgID)
	if err != nil {
		return apierrors.ErrCreateNotify.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(notify)
}

// GetNotify 获取通知详情
func (e *Endpoints) GetNotify(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	idStr := vars["notifyID"]
	notifyID, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetNotify.InvalidParameter(err).ToResp(), nil
	}

	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrGetNotify.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	notify, err := e.notifyGroup.GetNotify(notifyID, orgID)
	if err != nil {
		return apierrors.ErrGetNotify.InternalError(err).ToResp(), nil
	}

	err = e.checkNotifyPermission(r, notify.ScopeType, notify.ScopeID, apistructs.GetAction)
	if err != nil {
		return apierrors.ErrGetNotify.AccessDenied().ToResp(), nil
	}

	var userIDs []string
	if notify.NotifyGroup != nil {
		if notify.NotifyGroup.Creator != "" {
			userIDs = append(userIDs, notify.NotifyGroup.Creator)
		}
		for _, target := range notify.NotifyGroup.Targets {
			if target.Type == apistructs.UserNotifyTarget {
				for _, t := range target.Values {
					userIDs = append(userIDs, t.Receiver)
				}
			}
		}
	}
	if notify.Creator != "" {
		userIDs = append(userIDs, notify.Creator)
	}

	return httpserver.OkResp(notify, userIDs)
}

// DeleteNotify 删除通知
func (e *Endpoints) DeleteNotify(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	idStr := vars["notifyID"]
	notifyID, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeleteNotify.InvalidParameter(err).ToResp(), nil
	}

	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrDeleteNotify.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	notify, err := e.notifyGroup.GetNotify(notifyID, orgID)
	if err != nil {
		return apierrors.ErrDeleteNotify.InternalError(err).ToResp(), nil
	}
	err = e.checkNotifyPermission(r, notify.ScopeType, notify.ScopeID, apistructs.DeleteAction)
	if err != nil {
		return apierrors.ErrDeleteNotify.AccessDenied().ToResp(), nil
	}

	deleteGroup := r.URL.Query().Get("withGroup") == "true"
	if err = e.notifyGroup.DeleteNotify(notifyID, deleteGroup, orgID); err != nil {
		return apierrors.ErrDeleteNotify.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(notify)
}

// UpdateNotify 更新通知
func (e *Endpoints) UpdateNotify(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	idStr := vars["notifyID"]
	notifyID, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdateNotify.InvalidParameter(err).ToResp(), nil
	}

	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrUpdateNotify.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrUpdateNotify.MissingParameter("body is nil").ToResp(), nil
	}

	var notifyUpdateReq apistructs.UpdateNotifyRequest
	if err := json.NewDecoder(r.Body).Decode(&notifyUpdateReq); err != nil {
		return apierrors.ErrUpdateNotify.InvalidParameter("can't decode body").ToResp(), nil
	}

	err = e.notifyGroup.CheckNotifyChannels(notifyUpdateReq.Channels)
	if err != nil {
		return apierrors.ErrUpdateNotify.InvalidParameter(err.Error()).ToResp(), nil
	}

	notifyUpdateReq.ID = notifyID
	notifyUpdateReq.OrgID = orgID

	notify, err := e.notifyGroup.GetNotify(notifyID, orgID)
	if err != nil {
		return apierrors.ErrUpdateNotify.InternalError(err).ToResp(), nil
	}
	err = e.checkNotifyPermission(r, notify.ScopeType, notify.ScopeID, apistructs.UpdateAction)
	if err != nil {
		return apierrors.ErrUpdateNotify.AccessDenied().ToResp(), nil
	}

	err = e.notifyGroup.UpdateNotify(&notifyUpdateReq)

	if err != nil {
		return apierrors.ErrUpdateNotify.InternalError(err).ToResp(), nil
	}
	notify, err = e.notifyGroup.GetNotify(notifyID, orgID)
	if err != nil {
		return apierrors.ErrUpdateNotify.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(notify)
}

// NotifyEnable 启用通知
func (e *Endpoints) NotifyEnable(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	idStr := vars["notifyID"]
	notifyID, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrNotifyEnable.InvalidParameter(err).ToResp(), nil
	}

	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrNotifyEnable.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	notify, err := e.notifyGroup.GetNotify(notifyID, orgID)
	if err != nil {
		return apierrors.ErrNotifyEnable.InternalError(err).ToResp(), nil
	}
	err = e.checkNotifyPermission(r, notify.ScopeType, notify.ScopeID, apistructs.OperateAction)
	if err != nil {
		return apierrors.ErrNotifyEnable.AccessDenied().ToResp(), nil
	}

	err = e.notifyGroup.UpdateNotifyEnable(notifyID, true, orgID)
	if err != nil {
		return apierrors.ErrNotifyEnable.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(notify)
}

// NotifyDisable 禁用通知
func (e *Endpoints) NotifyDisable(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	idStr := vars["notifyID"]
	notifyID, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrNotifyDisable.InvalidParameter(err).ToResp(), nil
	}

	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrNotifyDisable.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	notify, err := e.notifyGroup.GetNotify(notifyID, orgID)
	if err != nil {
		return apierrors.ErrNotifyDisable.InternalError(err).ToResp(), nil
	}
	err = e.checkNotifyPermission(r, notify.ScopeType, notify.ScopeID, apistructs.OperateAction)
	if err != nil {
		return apierrors.ErrNotifyDisable.AccessDenied().ToResp(), nil
	}

	err = e.notifyGroup.UpdateNotifyEnable(notifyID, false, orgID)
	if err != nil {
		return apierrors.ErrNotifyDisable.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(notify)
}

// QueryNotifies 查询通知
func (e *Endpoints) QueryNotifies(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrQueryNotify.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	pageNo := getInt(r.URL, "pageNo", 1)
	pageSize := getInt(r.URL, "pageSize", 10)
	queryReq := apistructs.QueryNotifyRequest{
		PageSize:    pageSize,
		PageNo:      pageNo,
		ScopeType:   r.URL.Query().Get("scopeType"),
		ScopeID:     r.URL.Query().Get("scopeId"),
		Label:       r.URL.Query().Get("label"),
		ClusterName: r.URL.Query().Get("clusterName"),
		OrgID:       orgID,
	}

	err = e.checkNotifyPermission(r, queryReq.ScopeType, queryReq.ScopeID, apistructs.ListAction)
	if err != nil {
		return apierrors.ErrQueryNotify.AccessDenied().ToResp(), nil
	}

	result, err := e.notifyGroup.QueryNotifies(&queryReq)
	if err != nil {
		return apierrors.ErrQueryNotify.InternalError(err).ToResp(), nil
	}

	var userIDs []string
	for _, notify := range result.List {
		if notify.Creator != "" {
			userIDs = append(userIDs, notify.Creator)
		}
		if notify.NotifyGroup != nil {
			for _, target := range notify.NotifyGroup.Targets {
				if target.Type == apistructs.UserNotifyTarget {
					for _, t := range target.Values {
						userIDs = append(userIDs, t.Receiver)
					}
				}
			}
		}
	}
	return httpserver.OkResp(result, userIDs)
}

// QueryNotifiesBySource 根据source关联的通知
func (e *Endpoints) QueryNotifiesBySource(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	sourceType := r.URL.Query().Get("sourceType")
	sourceID := r.URL.Query().Get("sourceId")
	itemName := r.URL.Query().Get("itemName")
	clusterName := r.URL.Query().Get("clusterName")
	orgIdStr := r.URL.Query().Get("orgId")
	label := r.URL.Query().Get("label")

	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return apierrors.ErrQueryNotify.InternalError(err).ToResp(), nil
	}
	localeName := ""
	orgInfo, err := e.org.Get(orgId)
	if err == nil {
		localeName = orgInfo.Locale
	}
	locale := e.bdl.GetLocale(localeName)
	result, err := e.notifyGroup.QueryNotifiesBySource(locale, sourceType, sourceID, itemName, orgId, clusterName, label)
	if err != nil {
		return apierrors.ErrQueryNotify.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// FuzzyQueryNotifiesBySource 模糊查询根据source关联的通知
func (e *Endpoints) FuzzyQueryNotifiesBySource(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgIDStr := r.URL.Query().Get("orgId")
	orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrQueryNotify.InternalError(err).ToResp(), nil
	}
	localeName := ""
	orgInfo, err := e.org.Get(orgID)
	if err == nil {
		localeName = orgInfo.Locale
	}
	locale := e.bdl.GetLocale(localeName)
	req := apistructs.FuzzyQueryNotifiesBySourceRequest{
		SourceType:  r.URL.Query().Get("sourceType"),
		OrgID:       orgID,
		Label:       r.URL.Query().Get("label"),
		Locale:      locale,
		PageNo:      getInt(r.URL, "pageNo", 1),
		PageSize:    getInt(r.URL, "pageSize", 10),
		ClusterName: r.URL.Query().Get("clusterName"),
		SourceName:  r.URL.Query().Get("sourceName"),
		NotifyName:  r.URL.Query().Get("notifyName"),
		ItemName:    r.URL.Query().Get("itemName"),
		Channel:     r.URL.Query().Get("channel"),
	}

	result, err := e.notifyGroup.FuzzyQueryNotifiesBySource(req)
	if err != nil {
		return apierrors.ErrQueryNotify.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) checkNotifyPermission(r *http.Request, scopeType, scopeID, action string) error {
	if scopeType == apistructs.MSPScope {
		return nil
	}
	userID := r.Header.Get("User-ID")
	if userID == "" {
		return errors.New("failed to get permission(User-ID is empty)")
	}
	var scope apistructs.ScopeType
	if scopeType == "org" {
		scope = apistructs.OrgScope
	}
	if scopeType == "project" {
		scope = apistructs.ProjectScope
	}
	if scopeType == "app" {
		scope = apistructs.AppScope
	}
	id, err := strconv.ParseInt(scopeID, 10, 64)
	if err != nil {
		return err
	}
	access, err := e.permission.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    scope,
		ScopeID:  uint64(id),
		Action:   action,
		Resource: apistructs.NotifyResource,
	})
	if err != nil {
		return err
	}
	if !access {
		return errors.New("no permission")
	}
	return nil
}

func getInt(url *url.URL, key string, defaultValue int64) int64 {
	valueStr := url.Query().Get(key)
	value, err := strconv.ParseInt(valueStr, 10, 32)
	if err != nil {
		return defaultValue
	}
	return value
}
