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

// Package ticket 封装工单相关操作
package ticket

import (
	"encoding/json"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/permission"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

// Ticket 工单操作封装
type Ticket struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
}

// Option 定义 Ticket 配置选项
type Option func(*Ticket)

// New 新建 Ticket 实例
func New(options ...Option) *Ticket {
	t := &Ticket{}
	for _, op := range options {
		op(t)
	}
	return t
}

// WithDBClient 配置 Ticket 数据库选项
func WithDBClient(db *dao.DBClient) Option {
	return func(t *Ticket) {
		t.db = db
	}
}

// WithBundle 配置 Ticket bundle选项
func WithBundle(bdl *bundle.Bundle) Option {
	return func(t *Ticket) {
		t.bdl = bdl
	}
}

// Create 创建工单
func (t *Ticket) Create(userID user.ID, requestID string, req *apistructs.TicketCreateRequest) (int64, error) {
	// 请求参数检查
	if err := t.checkTicketCreateParam(userID, req); err != nil {
		return 0, err
	}

	var label string
	if len(req.Label) > 0 {
		labelBytes, _ := json.Marshal(req.Label)
		label = string(labelBytes)
	}

	if req.Key != "" { // 告警类工单
		ticket, _ := t.db.GetOpenTicketByKey(req.Key)
		if ticket != nil {
			ticket.Content = req.Content
			ticket.Label = label
			ticket.Count++

			if err := t.db.UpdateTicket(ticket); err != nil {
				logrus.Warnf("failed create ticket, (%v)", err)
			}
			return int64(ticket.ID), nil
		}

	}

	var (
		creator      string
		lastOperator string
	)
	if !t.IsSonarType(req.Type) && !t.IsAlertType(req.Type) {
		creator = req.UserID
		lastOperator = req.UserID
	}

	ticket := &model.Ticket{
		Title:        req.Title,
		Content:      req.Content,
		Type:         req.Type,
		Priority:     req.Priority,
		RequestID:    requestID,
		Key:          req.Key,
		OrgID:        req.OrgID,
		Metric:       req.Metric,
		MetricID:     req.MetricID,
		Count:        1,
		Creator:      creator,
		LastOperator: lastOperator,
		Status:       apistructs.TicketOpen,
		Label:        label,
		TargetType:   req.TargetType,
		TargetID:     req.TargetID,
	}
	if req.TriggeredAt > 0 { // 告警类工单有触发时间
		t := time.Unix(0, req.TriggeredAt*1000000)
		ticket.TriggeredAt = &t
	}
	if err := t.db.Create(ticket).Error; err != nil {
		return 0, err
	}

	return int64(ticket.ID), nil
}

// Update 更新工单
func (t *Ticket) Update(permission *permission.Permission, locale *i18n.LocaleResource, ticketID int64, userID user.ID, req *apistructs.TicketUpdateRequestBody) error {
	// 请求参数检查
	if err := t.checkTicketUpdateParam(req); err != nil {
		return err
	}

	ticket, err := t.db.GetTicket(ticketID)
	if err != nil {
		return err
	}
	if ticket == nil {
		return errors.Errorf("ticket %d is not found", ticketID)
	}

	// 鉴权
	if ticket.TargetType == apistructs.TicketApp && userID.String() != "" {
		access, err := t.checkPermission(permission, ticket, userID)
		if err != nil || !access {
			return apierrors.ErrDeleteProject.AccessDenied()
		}
	}

	ticket.Title = req.Title
	ticket.Content = req.Content
	ticket.Type = req.Type
	ticket.Priority = req.Priority
	if err := t.db.UpdateTicket(ticket); err != nil {
		return err
	}

	return nil
}

// Close 关闭工单
func (t *Ticket) Close(permission *permission.Permission, locale *i18n.LocaleResource, ticketID int64, userID user.ID) error {
	ticket, err := t.db.GetTicket(ticketID)
	if err != nil {
		return err
	}

	// 鉴权
	if ticket.TargetType == apistructs.TicketApp && userID.String() != "" {
		access, err := t.checkPermission(permission, ticket, userID)
		if err != nil || !access {
			return apierrors.ErrDeleteProject.AccessDenied()
		}
	}

	ticket.Status = apistructs.TicketClosed
	ticket.LastOperator = userID.String()
	now := time.Now()
	ticket.ClosedAt = &now

	return t.db.UpdateTicket(ticket)
}

func (t *Ticket) checkPermission(permission *permission.Permission, ticket *model.Ticket, userID user.ID) (bool, error) {
	appID, err := strconv.ParseUint(ticket.TargetID, 10, 64)
	if err != nil {
		return false, err
	}

	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: apistructs.TicketResource,
		Action:   apistructs.OperateAction,
	}

	access, err := t.bdl.CheckPermission(&req)
	if err != nil {
		return false, err
	}
	return access.Access, nil
}

// CloseByKey 根据 key 关闭告警工单
func (t *Ticket) CloseByKey(key string) error {
	ticket, err := t.db.GetOpenTicketByKey(key)
	if err != nil {
		return err
	}

	ticket.Status = apistructs.TicketClosed
	now := time.Now()
	ticket.ClosedAt = &now
	if err := t.db.UpdateTicket(ticket); err != nil {
		return err
	}
	return nil
}

// Reopen 重新打开工单
func (t *Ticket) Reopen(permission *permission.Permission, locale *i18n.LocaleResource, ticketID int64, userID user.ID) error {
	ticket, err := t.db.GetTicket(ticketID)
	if err != nil {
		return err
	}

	// 鉴权
	if ticket.TargetType == apistructs.TicketApp {
		access, err := t.checkPermission(permission, ticket, userID)
		if err != nil || !access {
			return apierrors.ErrDeleteProject.AccessDenied()
		}
	}

	ticket.Status = apistructs.TicketOpen
	ticket.LastOperator = userID.String()
	if err := t.db.UpdateTicket(ticket); err != nil {
		return err
	}

	return nil
}

// Get 获取工单详情
func (t *Ticket) Get(permission *permission.Permission, locale *i18n.LocaleResource, ticketID int64, userID user.ID) (*apistructs.Ticket, error) {
	ticket, err := t.db.GetTicket(ticketID)
	if err != nil {
		return nil, err
	}

	// 鉴权
	if ticket.TargetType == apistructs.TicketApp && userID.String() != "" {
		access, err := t.checkPermission(permission, ticket, userID)
		if err != nil || !access {
			return nil, apierrors.ErrDeleteProject.AccessDenied()
		}
	}

	return t.convertToTicketDTO(ticket, false), nil
}

// GetByRequestID 根据requestID header获取工单
func (t *Ticket) GetByRequestID(requestID string) (*model.Ticket, error) {
	return t.db.GetTicketByRequestID(requestID)
}

// List 工单列表/查询
func (t *Ticket) List(param *apistructs.TicketListRequest) (int64, []apistructs.Ticket, error) {
	var (
		total   int64
		tickets []model.Ticket
		endedAt time.Time
	)
	db := t.db.DB
	if len(param.Type) != 0 {
		db = db.Where("type in (?)", param.Type)
	}
	if param.Priority != "" {
		db = db.Where("priority = ?", param.Priority)
	}
	if param.Key != "" {
		db = db.Where("key = ?", param.Key)
	}
	if param.Status != "" {
		db = db.Where("status = ?", param.Status)
	}
	if param.TargetType != "" {
		db = db.Where("target_type = ?", param.TargetType)
	}
	if param.TargetID != "" {
		targetIDs := strings.Split(param.TargetID, ",")
		db = db.Where("target_id in (?)", targetIDs)
	} else if param.OrgID != 0 {
		relations, _ := t.bdl.GetOrgClusterRelationsByOrg(uint64(param.OrgID))
		clusterNames := make([]string, 0, len(relations))
		for _, v := range relations {
			clusterNames = append(clusterNames, v.ClusterName)
		}
		db = db.Where("target_id in (?)", clusterNames)
	}
	if param.Metric != "" {
		db = db.Where("metric = ?", param.Metric)
	}
	if len(param.MetricID) != 0 {
		db = db.Where("metric_id in (?)", param.MetricID)
	}
	if param.EndTime == 0 {
		endedAt = time.Now()
	} else {
		endedAt = time.Unix(0, param.EndTime*1000000)
	}
	db = db.Where("created_at < ?", endedAt)
	if param.StartTime != 0 {
		startedAt := time.Unix(0, param.StartTime*1000000)
		db = db.Where("created_at > ?", startedAt)
	}
	if param.Q != "" {
		db = db.Where("title LIKE ?", strutil.Concat("%", param.Q, "%"))
	}
	if err := db.Order("updated_at DESC").
		Offset((param.PageNo - 1) * param.PageSize).
		Limit(param.PageSize).Find(&tickets).Error; err != nil {
		return 0, nil, err
	}
	// 符合条件的 ticket 总量
	if err := db.Model(&model.Ticket{}).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	ticketsDTO := make([]apistructs.Ticket, 0, len(tickets))
	for i := range tickets {
		ticketDTO := t.convertToTicketDTO(&tickets[i], param.Comment)
		ticketsDTO = append(ticketsDTO, *ticketDTO)
	}

	return total, ticketsDTO, nil
}

func (t *Ticket) checkTicketCreateParam(userID user.ID, req *apistructs.TicketCreateRequest) error {
	if req.Title == "" {
		return errors.Errorf("title is empty")
	}
	if err := t.CheckTicketType(req.Type); err != nil {
		return err
	}
	if err := t.CheckTicketPriority(req.Priority); err != nil {
		return err
	}
	if req.Type == apistructs.TicketTask && req.UserID != userID.String() {
		return errors.Errorf("user id doesn't match")
	}
	if err := t.CheckTicketTarget(req.TargetType); err != nil {
		return err
	}
	return nil
}

func (t *Ticket) checkTicketUpdateParam(req *apistructs.TicketUpdateRequestBody) error {
	if req.Title == "" {
		return errors.Errorf("title is empty")
	}
	if err := t.CheckTicketType(req.Type); err != nil {
		return err
	}
	if err := t.CheckTicketPriority(req.Priority); err != nil {
		return err
	}
	return nil
}

// CheckTicketType 检查工单类型
func (t *Ticket) CheckTicketType(ticketType apistructs.TicketType) error {
	ticketTypeMap := map[apistructs.TicketType]struct{}{
		apistructs.TicketTask:                {},
		apistructs.TicketBug:                 {},
		apistructs.TicketVulnerability:       {},
		apistructs.TicketCodeSmell:           {},
		apistructs.TicketMergeRequest:        {},
		apistructs.TicketMachineAlert:        {},
		apistructs.TicketKubernetesAlert:     {},
		apistructs.TicketDiceAddOnAlert:      {},
		apistructs.TicketComponentAlert:      {},
		apistructs.TicketAddOnAlert:          {},
		apistructs.TicketAppStatusAlert:      {},
		apistructs.TicketAppResourceAlert:    {},
		apistructs.TicketExceptionAlert:      {},
		apistructs.TicketAppTransactionAlert: {},
	}
	if _, ok := ticketTypeMap[ticketType]; !ok {
		return errors.Errorf("unsupported ticket type")
	}
	return nil
}

// IsSonarType 判断 ticketType 是否为sonar类型
func (t *Ticket) IsSonarType(ticketType apistructs.TicketType) bool {
	switch ticketType {
	case apistructs.TicketBug, apistructs.TicketVulnerability, apistructs.TicketCodeSmell:
		return true
	default:
		return false
	}
}

// IsAlertType 判断 ticketType 是否为告警类型
func (t *Ticket) IsAlertType(ticketType apistructs.TicketType) bool {
	ticketTypeMap := map[apistructs.TicketType]struct{}{
		apistructs.TicketMachineAlert:        {},
		apistructs.TicketKubernetesAlert:     {},
		apistructs.TicketDiceAddOnAlert:      {},
		apistructs.TicketComponentAlert:      {},
		apistructs.TicketAddOnAlert:          {},
		apistructs.TicketAppStatusAlert:      {},
		apistructs.TicketAppResourceAlert:    {},
		apistructs.TicketExceptionAlert:      {},
		apistructs.TicketAppTransactionAlert: {},
	}
	if _, ok := ticketTypeMap[ticketType]; ok {
		return true
	}
	return false
}

// CheckTicketPriority 检查 ticketPriority 合法性
func (t *Ticket) CheckTicketPriority(ticketPriority apistructs.TicketPriority) error {
	switch ticketPriority {
	case apistructs.TicketHigh, apistructs.TicketMedium, apistructs.TicketLow:
	default:
		return errors.Errorf("unsupported ticket priority")
	}
	return nil
}

// CheckTicketTarget 检查 ticketTarget 合法性
func (t *Ticket) CheckTicketTarget(ticketTarget apistructs.TicketTarget) error {
	switch ticketTarget {
	case "", apistructs.TicketCluster, apistructs.TicketProject, apistructs.TicketApp, apistructs.TicketOrg, apistructs.TicketMicroService:
	default:
		return errors.Errorf("unsupported ticket target")
	}
	return nil
}

func (t *Ticket) convertToTicketDTO(ticket *model.Ticket, comment bool) *apistructs.Ticket {
	var label map[string]interface{}
	if ticket.Label != "" {
		if err := json.Unmarshal([]byte(ticket.Label), &label); err != nil {
			logrus.Errorf("ticket label unmarshal error: %v", err)
			label = make(map[string]interface{})
		}
	}

	var lastComment *apistructs.Comment
	if comment {
		c, _ := t.db.GetLastCommentByTicket(int64(ticket.ID))
		if c != nil {
			lastComment = &apistructs.Comment{
				CommentID: int64(c.ID),
				TicketID:  c.TicketID,
				Content:   c.Content,
				UserID:    c.UserID,
				CreatedAt: c.CreatedAt,
				UpdatedAt: c.UpdatedAt,
			}
		}
	}

	ti := apistructs.Ticket{
		TicketID:     int64(ticket.ID),
		Title:        ticket.Title,
		Content:      ticket.Content,
		Type:         ticket.Type,
		Priority:     ticket.Priority,
		Status:       ticket.Status,
		Key:          ticket.Key,
		OrgID:        ticket.OrgID,
		Metric:       ticket.Metric,
		MetricID:     ticket.MetricID,
		Count:        ticket.Count,
		Creator:      ticket.Creator,
		LastOperator: ticket.LastOperator,
		Label:        label,
		TargetType:   ticket.TargetType,
		TargetID:     ticket.TargetID,
		LastComment:  lastComment,
		CreatedAt:    ticket.CreatedAt,
		UpdatedAt:    ticket.UpdatedAt,
	}
	if ticket.TriggeredAt != nil {
		ti.TriggeredAt = *ticket.TriggeredAt
	}
	if ticket.ClosedAt != nil {
		ti.ClosedAt = *ticket.ClosedAt
	}

	return &ti
}

// TODO deprecated
func (t *Ticket) GetClusterTicketsNum(ticketType, targetType, targetID string) (uint64, error) {
	return t.db.GetClusterOpenTicketsNum(ticketType, targetType, targetID)
}

// sendAlertMessage 发送告警消息(钉钉消息/工作通知两种方式)
// more info about dingding, please refer: https://open-doc.dingtalk.com/microapp/serverapi2/pgoxpy
func (t *Ticket) sendAlertMessage(req *apistructs.TicketCreateRequest) error {
	msgReq := apistructs.MessageCreateRequest{
		Sender:  bundle.SenderDOP,
		Content: req.Content,
	}

	msgLabels := make(map[apistructs.MessageLabel]interface{})

	if req.Label["sendWay"] == apistructs.DingdingWorkNoticeLabel { // 工作通知方式
		url := req.Label["ddHook"]
		agentID := req.Label["agentId"]
		users, ok := req.Label["users"].(string)
		if !ok {
			return errors.Errorf("invalid user label")
		}

		labels := make([]map[string]interface{}, 0, 1)
		item := make(map[string]interface{})
		item["url"] = url
		item["agent_id"] = agentID
		item["userid_list"] = strutil.Split(users, ",")
		labels = append(labels, item)

		msgLabels[apistructs.DingdingWorkNoticeLabel] = labels
	} else { // 钉钉消息方式
		url, err := t.getDingTalkURL(req)
		if err != nil {
			return err
		}
		msgLabels[apistructs.DingdingLabel] = []string{url}
	}

	msgReq.Labels = msgLabels

	return t.bdl.CreateMessage(&msgReq)
}

// TODO deprecated
// 获取钉钉消息/工作通知地址
func (t *Ticket) getDingTalkURL(req *apistructs.TicketCreateRequest) (string, error) {
	//if url, ok := req.Label["ddHook"]; ok { // 钉钉地址优先从label获取
	//	return url.(string), nil
	//}
	//switch req.TargetType {
	//case apistructs.TicketCluster:
	//	c, err := t.db.GetClusterByName(req.TargetID)
	//	if err != nil {
	//		return "", err
	//	}
	//	var urls map[string]string
	//	if err := json.Unmarshal([]byte(c.URLs), &urls); err != nil {
	//		return "", err
	//	}
	//	v, ok := urls["dingDingWarning"]
	//	if !ok {
	//		return "", errors.Errorf("dingDingWarning is not configured in cluster: %s", req.TargetID)
	//	}
	//	return v, nil
	//case apistructs.TicketProject:
	//	projectID, err := strutil.Atoi64(req.TargetID)
	//	if err != nil {
	//		return "", err
	//	}
	//	p, err := t.db.GetProjectByID(projectID)
	//	if err != nil {
	//		return "", err
	//	}
	//	return p.DDHook, nil
	//case apistructs.TicketApp:
	//	appID, err := strutil.Atoi64(req.TargetID)
	//	if err != nil {
	//		return "", err
	//	}
	//	a, err := t.db.GetApplicationByID(appID)
	//	if err != nil {
	//		return "", err
	//	}
	//	// 获取应用配置的钉钉地址
	//	var config map[string]interface{}
	//	if err := json.Unmarshal([]byte(a.Config), &config); err != nil {
	//		config = make(map[string]interface{})
	//	}
	//	v, ok := config["ddHookUrl"]
	//	if !ok {
	//		return "", errors.Errorf("ddHookUrl is not configured in app: %d", appID)
	//	}
	//	return v.(string), nil
	//default:
	//	return "", errors.Errorf("unknown target type")
	//}
	return "", nil
}

// Delete 删除工单
func (t *Ticket) Delete(targetID, targetType, ticketType string) error {
	return t.db.DeleteTicket(targetID, targetType, ticketType)
}
