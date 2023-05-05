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
	"net/url"
	"reflect"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateTicket 创建工单
// - param: requestID http header，用于幂等性校验
func (b *Bundle) CreateTicket(requestID string, req *apistructs.TicketCreateRequest) (int64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var ticketResp apistructs.TicketCreateResponse
	resp, err := hc.Post(host).Path("/api/tickets").
		Header("Accept", "application/json").
		Header(httputil.UserHeader, req.UserID).
		Header(httputil.RequestIDHeader, requestID).
		JSONBody(req).
		Do().JSON(&ticketResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !ticketResp.Success {
		return 0, toAPIError(resp.StatusCode(), ticketResp.Error)
	}
	return ticketResp.Data, nil
}

// CloseTicket 关闭工单
func (b *Bundle) CloseTicket(ticketID int64, userID string) (int64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var ticketResp apistructs.TicketCloseResponse
	path := strutil.Concat("/api/tickets/", strconv.FormatInt(ticketID, 10), "/actions/close")
	resp, err := hc.Put(host).Path(path).
		Header("Accept", "application/json").
		Header(httputil.UserHeader, userID).
		Do().JSON(&ticketResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !ticketResp.Success {
		return 0, toAPIError(resp.StatusCode(), ticketResp.Error)
	}
	return ticketResp.Data, nil
}

// ReopenTicket 重新打开工单
func (b *Bundle) ReopenTicket(ticketID int64, userID string) (int64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var ticketResp apistructs.TicketReopenResponse
	path := strutil.Concat("/api/tickets/", strconv.FormatInt(ticketID, 10), "/actions/reopen")
	resp, err := hc.Put(host).Path(path).
		Header("Accept", "application/json").
		Header(httputil.UserHeader, userID).
		Do().JSON(&ticketResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !ticketResp.Success {
		return 0, toAPIError(resp.StatusCode(), ticketResp.Error)
	}
	return ticketResp.Data, nil
}

// ListTicket 工单列表
func (b *Bundle) ListTicket(req apistructs.TicketListRequest) (*apistructs.TicketListResponseData, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	t := reflect.TypeOf(req)
	v := reflect.ValueOf(req)
	params := make(url.Values, t.NumField())
	for i := 0; i < t.NumField(); i++ {
		name, ok := t.Field(i).Tag.Lookup("query")
		if !ok {
			continue
		}
		value := v.Field(i).String()
		params.Set(name, value)
	}
	logrus.Infof("params: %+v", params)

	var ticketResp apistructs.TicketListResponse
	resp, err := hc.Get(host).Path("/api/tickets").Params(params).
		Header("Accept", "application/json").
		Do().JSON(&ticketResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !ticketResp.Success {
		return nil, toAPIError(resp.StatusCode(), ticketResp.Error)
	}
	return &ticketResp.Data, nil
}

// DeleteTicket 删除工单
func (b *Bundle) DeleteTicket(ticketID int64) (int64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var ticketResp apistructs.TicketDeleteResponse
	path := strutil.Concat("/api/tickets/", strconv.FormatInt(ticketID, 10))
	resp, err := hc.Delete(host).Path(path).
		Header("Accept", "application/json").
		Do().JSON(&ticketResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !ticketResp.Success {
		return 0, toAPIError(resp.StatusCode(), ticketResp.Error)
	}
	return ticketResp.Data, nil
}

// DeleteTicketByTargetID 根据targetID删除工单
func (b *Bundle) DeleteTicketByTargetID(targetID int64, ticketType string, targetType string) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var ticketResp apistructs.TicketDeleteResponse
	path := strutil.Concat("/api/tickets/actions/batch-delete")
	resp, err := hc.Delete(host).Path(path).
		Header("Accept", "application/json").
		Param("ticketType", ticketType).
		Param("targetType", targetType).
		Param("targetID", strconv.FormatInt(targetID, 10)).
		Do().JSON(&ticketResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !ticketResp.Success {
		return toAPIError(resp.StatusCode(), ticketResp.Error)
	}
	return nil
}

// CreateComment 创建评论
func (b *Bundle) CreateComment(req *apistructs.CommentCreateRequest) (int64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var commentResp apistructs.CommentCreateResponse
	resp, err := hc.Post(host).Path("/api/comments").
		Header("Accept", "application/json").
		Header(httputil.UserHeader, req.UserID).
		JSONBody(req).
		Do().JSON(&commentResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !commentResp.Success {
		return 0, toAPIError(resp.StatusCode(), commentResp.Error)
	}
	return commentResp.Data, nil
}
