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
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateTicket 创建工单
func (e *Endpoints) CreateTicket(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateTicket.InvalidParameter(err).ToResp(), nil
	}
	requestID := r.Header.Get(httputil.RequestIDHeader) // 用于幂等性校验

	// 检查body合法性
	if r.Body == nil {
		return apierrors.ErrCreateTicket.MissingParameter("body is nil").ToResp(), nil
	}
	var ticketCreateReq apistructs.TicketCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&ticketCreateReq); err != nil {
		logrus.Warnf("failed to decode body when create ticket, (%v)", err)
		return apierrors.ErrCreateTicket.InvalidParameter("can't decode body").ToResp(), nil
	}
	logrus.Infof("request body: %+v", ticketCreateReq)

	// TODO 鉴权
	if requestID != "" {
		// 幂等性校验
		ticket, err := e.ticket.GetByRequestID(requestID)
		if err != nil {
			return apierrors.ErrCreateTicket.InternalError(err).ToResp(), nil
		}
		if ticket != nil {
			return httpserver.OkResp(ticket.ID)
		}
	}

	// 添加ticket至DB
	ticketID, err := e.ticket.Create(userID, requestID, &ticketCreateReq)
	if err != nil {
		return apierrors.ErrCreateTicket.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(ticketID)
}

// UpdateTicket 更新工单
func (e *Endpoints) UpdateTicket(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	locale := e.GetLocale(r)
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateTicket.InvalidParameter(err).ToResp(), nil
	}

	ticketIDStr := vars["ticketID"]
	ticketID, err := strconv.ParseInt(ticketIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdateTicket.InvalidParameter(err).ToResp(), nil
	}

	// 检查body合法性
	if r.Body == nil {
		return apierrors.ErrUpdateTicket.MissingParameter("body is nil").ToResp(), nil
	}
	var ticketUpdateReq apistructs.TicketUpdateRequestBody
	if err := json.NewDecoder(r.Body).Decode(&ticketUpdateReq); err != nil {
		return apierrors.ErrUpdateTicket.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", ticketUpdateReq)

	// 更新ticket至DB
	if err = e.ticket.Update(e.permission, locale, ticketID, userID, &ticketUpdateReq); err != nil {
		return apierrors.ErrUpdateTicket.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(ticketID)
}

// CloseTicket 关闭工单
func (e *Endpoints) CloseTicket(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	locale := e.GetLocale(r)
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCloseTicket.InvalidParameter(err).ToResp(), nil
	}

	ticketIDStr := vars["ticketID"]
	ticketID, err := strutil.Atoi64(ticketIDStr)
	if err != nil {
		return apierrors.ErrCloseTicket.InvalidParameter(err).ToResp(), nil
	}

	// 将工单状态置为closed
	if err = e.ticket.Close(e.permission, locale, ticketID, userID); err != nil {
		return apierrors.ErrCloseTicket.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(ticketID)
}

// CloseTicket 关闭工单
func (e *Endpoints) DeleteTicket(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	ticketID := r.URL.Query().Get("targetID")
	ticketType := r.URL.Query().Get("ticketType")
	targetType := r.URL.Query().Get("targetType")

	if err := e.ticket.Delete(ticketID, targetType, ticketType); err != nil {
		return apierrors.ErrDeleteTicket.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}

// CloseTicketByKey 根据 key 关闭工单(告警使用, 仅对内)
func (e *Endpoints) CloseTicketByKey(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		return apierrors.ErrCloseTicket.MissingParameter("internal client header").ToResp(), nil
	}

	key := r.URL.Query().Get("key")
	if key == "" {
		return apierrors.ErrCloseTicket.MissingParameter("key").ToResp(), nil
	}

	// 将工单状态置为closed
	if err := e.ticket.CloseByKey(key); err != nil {
		return apierrors.ErrCloseTicket.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("close succ")
}

// ReopenTicket 重新打开工单
func (e *Endpoints) ReopenTicket(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	locale := e.GetLocale(r)
	if err != nil {
		return apierrors.ErrReopenTicket.InvalidParameter(err).ToResp(), nil
	}

	ticketIDStr := vars["ticketID"]
	ticketID, err := strconv.ParseInt(ticketIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrReopenTicket.InvalidParameter(err).ToResp(), nil
	}

	// 将工单状态置为open
	if err = e.ticket.Reopen(e.permission, locale, ticketID, userID); err != nil {
		return apierrors.ErrReopenTicket.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(ticketID)
}

// GetTicket 获取工单详情
func (e *Endpoints) GetTicket(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	locale := e.GetLocale(r)
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetTicket.InvalidParameter(err).ToResp(), nil
	}

	ticketIDStr := vars["ticketID"]
	ticketID, err := strconv.ParseInt(ticketIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetTicket.InvalidParameter(err).ToResp(), nil
	}

	ticketDTO, err := e.ticket.Get(e.permission, locale, ticketID, userID)
	if err != nil {
		return apierrors.ErrGetTicket.InternalError(err).ToResp(), nil
	}

	users := make([]string, 0, 2)
	if ticketDTO.Creator != "" {
		users = append(users, ticketDTO.Creator)
	}
	if ticketDTO.LastOperator != "" {
		users = append(users, ticketDTO.LastOperator)
	}

	return httpserver.OkResp(ticketDTO, users)
}

// ListTicket 工单列表
func (e *Endpoints) ListTicket(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	params, err := e.getTicketListParam(r)
	if err != nil {
		return apierrors.ErrListTicket.InvalidParameter(err).ToResp(), nil
	}
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListTicket.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		permissionReq := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Resource: apistructs.TicketResource,
		}
		if params.TargetType == apistructs.TicketApp && params.TargetID != "" {
			appID, err := strconv.ParseUint(params.TargetID, 10, 64)
			if err != nil {
				return apierrors.ErrListTicket.InvalidParameter("targetID").ToResp(), nil
			}
			permissionReq.Scope = apistructs.AppScope
			permissionReq.ScopeID = appID
			permissionReq.Action = apistructs.OperateAction
		} else {
			orgIDStr := r.Header.Get(httputil.OrgHeader)
			orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrListTicket.NotLogin().ToResp(), nil
			}
			permissionReq.Scope = apistructs.OrgScope
			permissionReq.ScopeID = orgID
			permissionReq.Action = apistructs.OperateAction
		}
		access, err := e.bdl.CheckPermission(&permissionReq)
		if err != nil {
			return apierrors.ErrListTicket.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrListTicket.AccessDenied().ToResp(), nil
		}
	}

	total, tickets, err := e.ticket.List(params)
	if err != nil {
		return apierrors.ErrListTicket.InternalError(err).ToResp(), nil
	}
	userIDs := make([]string, 0, len(tickets)*2)
	for i := range tickets {
		if tickets[i].Creator != "" {
			userIDs = append(userIDs, tickets[i].Creator)
		}
		if tickets[i].LastOperator != "" {
			userIDs = append(userIDs, tickets[i].LastOperator)
		}
	}

	return httpserver.OkResp(apistructs.TicketListResponseData{Total: total, Tickets: tickets}, userIDs)
}

func (e *Endpoints) getTicketListParam(r *http.Request) (*apistructs.TicketListRequest, error) {
	// 获取type参数
	types := r.URL.Query()["type"]
	ticketTypes := make([]apistructs.TicketType, 0, len(types))
	for _, v := range types {
		ticketType := apistructs.TicketType(v)
		if err := e.ticket.CheckTicketType(ticketType); err != nil {
			return nil, errors.Errorf("invalid param, (%v)", err)
		}
		ticketTypes = append(ticketTypes, ticketType)
	}
	// 获取priority参数
	ticketPriority := r.URL.Query().Get("priority")
	if ticketPriority != "" {
		if err := e.ticket.CheckTicketPriority(apistructs.TicketPriority(ticketPriority)); err != nil {
			return nil, errors.Errorf("invalid param, (%v)", err)
		}
	}
	// 获取status参数
	status := r.URL.Query().Get("status")
	switch apistructs.TicketStatus(status) {
	case "", apistructs.TicketOpen, apistructs.TicketClosed:
	default:
		return nil, errors.Errorf("invalid param, status: %v", status)
	}
	// 获取targetType参数
	targetType := r.URL.Query().Get("targetType")
	if targetType != "" {
		if err := e.ticket.CheckTicketTarget(apistructs.TicketTarget(targetType)); err != nil {
			return nil, errors.Errorf("invalid param, (%v)", err)
		}
	}
	targetIDs := r.URL.Query()["targetID"]

	key := r.URL.Query().Get("key")

	var orgID int64
	orgIDStr := r.URL.Query().Get("orgID")
	if orgIDStr != "" {
		orgID, _ = strutil.Atoi64(orgIDStr)
	}
	metric := r.URL.Query().Get("metric")
	metricID := r.URL.Query()["metricID"]

	// 获取时间范围
	var (
		startTime int64
		endTime   int64
		err       error
	)
	startTimeStr := r.URL.Query().Get("startTime")
	if startTimeStr != "" {
		startTime, err = strutil.Atoi64(startTimeStr)
		if err != nil {
			return nil, err
		}
	}
	endTimeStr := r.URL.Query().Get("endTime")
	if endTimeStr != "" {
		endTime, err = strutil.Atoi64(endTimeStr)
		if err != nil {
			return nil, err
		}
	}

	// 是否包含工单最新评论
	var comment bool
	commentStr := r.URL.Query().Get("comment")
	if commentStr == "true" || commentStr == "1" {
		comment = true
	}

	// 获取q参数
	keyword := r.URL.Query().Get("q")

	// 获取pageNo参数
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageNo: %v", pageNoStr)
	}
	// 获取pageSize参数
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageSize: %v", pageSizeStr)
	}

	return &apistructs.TicketListRequest{
		Type:       ticketTypes,
		Priority:   apistructs.TicketPriority(ticketPriority),
		Status:     apistructs.TicketStatus(status),
		TargetType: apistructs.TicketTarget(targetType),
		TargetID:   strings.Join(targetIDs, ","),
		Key:        key,
		OrgID:      orgID,
		Metric:     metric,
		MetricID:   metricID,
		StartTime:  startTime,
		EndTime:    endTime,
		Comment:    comment,
		Q:          keyword,
		PageNo:     pageNo,
		PageSize:   pageSize,
	}, nil
}
