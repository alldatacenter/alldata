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

// Package approve 封装Approve资源相关操作
package approve

import (
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/services/member"
	"github.com/erda-project/erda/internal/core/legacy/utils"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Approve 资源对象操作封装
type Approve struct {
	db     *dao.DBClient
	bdl    *bundle.Bundle
	uc     *ucauth.UCClient
	member *member.Member
}

// Option 定义 Approve 对象的配置选项
type Option func(*Approve)

// New 新建 Approve 实例，通过 Approve 实例操作企业资源
func New(options ...Option) *Approve {
	p := &Approve{}
	for _, op := range options {
		op(p)
	}
	return p
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(p *Approve) {
		p.db = db
	}
}

// WithUCClient 配置 uc client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(p *Approve) {
		p.uc = uc
	}
}

// WithMember 配置 member
func WithMember(m *member.Member) Option {
	return func(p *Approve) {
		p.member = m
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(p *Approve) {
		p.bdl = bdl
	}
}

// Create 创建Approve
func (a *Approve) Create(userID string, createReq *apistructs.ApproveCreateRequest) (*apistructs.ApproveDTO, error) {
	// 参数合法性检查
	if createReq.TargetID == 0 {
		return nil, errors.Errorf("failed to create approve(targetId is empty)")
	}
	if createReq.Type == "" {
		return nil, errors.Errorf("failed to create approve(type is empty)")
	}

	if createReq.Type == apistructs.ApproveCeritficate && createReq.EntityID == 0 {
		return nil, errors.Errorf("failed to create approve(entityId is empty)")
	}
	if len(createReq.Desc) > 1000 {
		return nil, errors.Errorf("too long desc (>1000)")
	}
	approve, err := a.db.GetApproveByOrgAndID(createReq.Type, createReq.OrgID, createReq.TargetID, createReq.EntityID)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	if approve != nil && approve.Type != string(apistructs.ApproveUnblockAppication) {
		return nil, errors.Errorf("failed to create approve(approve:%d already exists)", approve.ID)
	}

	// 添加Approve至DB
	approve = &model.Approve{
		OrgID:        createReq.OrgID,
		TargetID:     createReq.TargetID,
		EntityID:     createReq.EntityID,
		TargetName:   createReq.TargetName,
		Status:       string(apistructs.ApprovalStatusPending),
		Desc:         createReq.Desc,
		ApprovalTime: &[]time.Time{time.Now()}[0],
		Submitter:    userID,
		Priority:     createReq.Priority,
		Title:        createReq.Title,
		Type:         string(createReq.Type),
	}

	if approve.Type == string(apistructs.ApproveUnblockAppication) {
		start, err := time.Parse(time.RFC3339, createReq.Extra["start"])
		if err != nil {
			return nil, errors.Errorf("failed to parse 'start' timestamp: %v", err)
		}
		end, err := time.Parse(time.RFC3339, createReq.Extra["end"])
		if err != nil {
			return nil, errors.Errorf("failed to parse 'end' timestamp: %v", err)
		}
		approve.Title = fmt.Sprintf("%s项目，申请在时间段(%s~%s)内的部署权限",
			createReq.TargetName, start.Format("2006-01-02 15:04:05"), end.Format("2006-01-02 15:04:05"))
	}
	if createReq.Extra != nil {
		extra, err := json.Marshal(createReq.Extra)
		if err != nil {
			return nil, err
		}
		approve.Extra = string(extra)
	}
	if err = a.db.CreateApprove(approve); err != nil {
		return nil, errors.Errorf("failed to insert approve to db")
	}
	if approve.Type == string(apistructs.ApproveUnblockAppication) {
		memberlist, err := a.uc.FindUsers([]string{userID})
		if err != nil {
			return nil, errors.Errorf("failed to get user(%s): %v", userID, err)
		}
		member := memberlist[0].Name
		start, err := time.Parse(time.RFC3339, createReq.Extra["start"])
		if err != nil {
			return nil, errors.Errorf("failed to parse 'start' timestamp: %v", err)
		}
		end, err := time.Parse(time.RFC3339, createReq.Extra["end"])
		if err != nil {
			return nil, errors.Errorf("failed to parse 'end' timestamp: %v", err)
		}
		_, approvers, err := a.member.List(&apistructs.MemberListRequest{
			ScopeType: "org",
			ScopeID:   int64(approve.OrgID),
			Roles:     []string{"Owner", "Lead", "Manager"},
			PageSize:  99,
		})
		if err != nil {
			return nil, err
		}
		if err := a.mkMboxEmailNotify(approve.ID, "undone", approve.OrgID, approve.TargetName, member,
			start, end, createReq.Desc, approvers); err != nil {
			return nil, err
		}
	}

	return a.convertToApproveDTO(approve), nil
}

func (a *Approve) mkMboxEmailNotify(id int64, done string, orgid uint64, projectname string,
	member string, start, end time.Time, desc string, receivers []model.Member) error {
	protocol := utils.GetProtocol()
	domain := conf.UIDomain()
	org, err := a.db.GetOrg(int64(orgid))
	if err != nil {
		logrus.Errorf("failed to getorg(%v):%v", orgid, err)
		return err
	}

	url := fmt.Sprintf("%s://%s/%s/orgCenter/approval?id=%d&type=%s",
		protocol, domain, org.Name, id, done)

	var approverIDs []string
	var emails []string
	for _, approver := range receivers {
		approverIDs = append(approverIDs, approver.UserID)
		emails = append(emails, approver.Email)
	}
	template := "notify.unblockapproval.launch.markdown_template"
	if done == "done" {
		template = "notify.unblockapproval.done.markdown_template"
	}
	if err := a.bdl.CreateMboxNotify(template,
		map[string]string{
			"title": fmt.Sprintf("【重要】 %s 项目 生产环境部署申请%s", projectname,
				map[string]string{"undone": "", "done": "通过"}[done]),
			"member":      member,
			"projectname": projectname,
			"start":       start.Format("2006-01-02 15:04"),
			"end":         end.Format("2006-01-02 15:04"),
			"desc":        desc,
			"url":         url,
		}, "zh-CN", orgid, approverIDs); err != nil {
		logrus.Errorf("failed to send mbox notify: %v", err)
	}
	go func() {
		// 由于加了域名黑白名单限制以后，个人邮箱域名发送很容易被拦，然后请求超时，前端504，所以换成异步
		if err := a.bdl.CreateEmailNotify(template,
			map[string]string{
				"title": fmt.Sprintf("【重要】 %s 项目 生产环境部署申请%s", projectname,
					map[string]string{"undone": "", "done": "通过"}[done]),
				"member":      member,
				"projectname": projectname,
				"start":       start.Format("2006-01-02 15:04"),
				"end":         end.Format("2006-01-02 15:04"),
				"desc":        desc,
				"url":         url,
			}, "zh-CN", orgid, emails); err != nil {
			logrus.Errorf("failed to send email notify: %v", err)
		}
	}()
	return nil
}

// Update 更新Approve
func (a *Approve) Update(approveID int64, updateReq *apistructs.ApproveUpdateRequest) error {
	// 检查待更新的approve是否存在
	approve, err := a.db.GetApproveByID(approveID)
	if err != nil {
		return errors.Wrap(err, "not exist approve")
	}

	if updateReq.Status == "" {
		return errors.Wrap(err, "need status")
	}

	if updateReq.Status != apistructs.ApprovalStatusPending &&
		updateReq.Status != apistructs.ApprovalStatusApproved &&
		updateReq.Status != apistructs.ApprovalStatusDeined {
		return errors.New("status error")
	}

	if updateReq.Approver != "" {
		approve.Approver = updateReq.Approver
	}

	if updateReq.Priority != "" {
		approve.Priority = updateReq.Priority
	}

	if updateReq.Desc != "" {
		approve.Desc = updateReq.Desc
	}

	if updateReq.Extra != nil {
		extra, err := json.Marshal(updateReq.Extra)
		if err != nil {
			return err
		}
		approve.Extra = string(extra)
	}

	approve.Status = string(updateReq.Status)

	if err = a.db.UpdateApprove(&approve); err != nil {
		logrus.Errorf("failed to update approve, (%v)", err)
		return errors.Errorf("failed to update approve")
	}

	// 解封申请
	if approve.Type == string(apistructs.ApproveUnblockAppication) &&
		approve.Status == string(apistructs.ApprovalStatusApproved) {
		extra := map[string]string{}
		if err := json.Unmarshal([]byte(approve.Extra), &extra); err != nil {
			return err
		}
		if err := a.updateApplicationWhenUnblock(extra); err != nil {
			logrus.Errorf("failed to updateApplicationWhenUnblock: %v", err)
		}
		memberlist, err := a.uc.FindUsers([]string{approve.Submitter})
		if err != nil {
			return errors.Errorf("failed to get user(%s): %v", approve.Submitter, err)
		}
		member := memberlist[0].Name
		start, err := time.Parse(time.RFC3339, extra["start"])
		if err != nil {
			return errors.Errorf("failed to parse 'start' timestamp: %v", err)
		}
		end, err := time.Parse(time.RFC3339, extra["end"])
		if err != nil {
			return errors.Errorf("failed to parse 'end' timestamp: %v", err)
		}
		if err := a.mkMboxEmailNotify(approve.ID, "done", approve.OrgID, approve.TargetName, member, start, end, approve.Desc,
			[]model.Member{{
				UserID: approve.Submitter,
				Email:  memberlist[0].Email,
			}}); err != nil {
			return err
		}

	}

	// 创建状态变更事件
	eventContent := &apistructs.ApprovalStatusChangedEventData{
		ApprovalID:     uint64(approve.ID),
		ApprovalStatus: updateReq.Status,
		ApprovalType:   apistructs.ApproveType(approve.Type),
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:     bundle.ApprovalStatusChangedEvent,
			Action:    bundle.UpdateAction,
			OrgID:     strconv.FormatUint(updateReq.OrgID, 10),
			TimeStamp: time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderCoreServices,
		Content: eventContent,
	}
	// 发送应用创建事件
	if err = a.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send approve create event, (%v)", err)
	}

	return nil
}

func (a *Approve) updateApplicationWhenUnblock(approveExtra map[string]string) error {
	appids_str, ok := approveExtra["appIDs"]
	if !ok || appids_str == "" {
		return nil
	}
	start, err := time.Parse(time.RFC3339, approveExtra["start"])
	if err != nil {
		return err
	}
	end, err := time.Parse(time.RFC3339, approveExtra["end"])
	if err != nil {
		return err
	}
	appids := strutil.Split(appids_str, ",", true)
	for _, id_s := range appids {
		id, err := strconv.ParseInt(id_s, 10, 64)
		if err != nil {
			return err
		}
		app, err := a.db.GetApplicationByID(id)
		if err != nil {
			return err
		}
		app.UnblockStart = &start
		app.UnblockEnd = &end
		if err := a.db.UpdateApplication(&app); err != nil {
			return err
		}
	}
	return nil
}

// Get 获取Approve
func (c *Approve) Get(approveID int64) (*apistructs.ApproveDTO, error) {
	approve, err := c.db.GetApproveByID(approveID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to get approve info")
	}
	return c.convertToApproveDTO(&approve), nil
}

// ListAllApproves 企业管理员可查看当前企业下所有Approve，包括未加入的Approve
func (c *Approve) ListAllApproves(params *apistructs.ApproveListRequest) (
	*apistructs.PagingApproveDTO, error) {
	var total int
	var approves []model.Approve
	var err error
	if params.ID != nil {
		approve, err := c.db.GetApproveByID(*params.ID)
		if err != nil {
			return nil, errors.Errorf("failed to get approves, (%v)", err)
		}
		total = 1
		approves = []model.Approve{approve}
	} else {
		total, approves, err = c.db.GetApprovesByOrgIDAndStatus(params)
		if err != nil {
			return nil, errors.Errorf("failed to get approves, (%v)", err)
		}
	}

	// 转换成所需格式
	approveDTOs := make([]apistructs.ApproveDTO, 0, len(approves))
	for i := range approves {
		approveDTOs = append(approveDTOs, *(c.convertToApproveDTO(&approves[i])))
	}

	return &apistructs.PagingApproveDTO{Total: total, List: approveDTOs}, nil
}

func (p *Approve) convertToApproveDTO(approve *model.Approve) *apistructs.ApproveDTO {
	approveDto := &apistructs.ApproveDTO{
		ID:           uint64(approve.ID),
		Type:         apistructs.ApproveType(approve.Type),
		Desc:         approve.Desc,
		OrgID:        uint64(approve.OrgID),
		Title:        approve.Title,
		Priority:     approve.Priority,
		Submitter:    approve.Submitter,
		Approver:     approve.Approver,
		ApprovalTime: approve.ApprovalTime,
		Status:       apistructs.ApprovalStatus(approve.Status),
		TargetName:   approve.TargetName,
		EntityID:     approve.EntityID,
		TargetID:     approve.TargetID,
		Extra:        make(map[string]string),
		CreatedAt:    approve.CreatedAt,
		UpdatedAt:    approve.UpdatedAt,
	}

	if approve.Extra != "" {
		json.Unmarshal([]byte(approve.Extra), &approveDto.Extra)
	}

	return approveDto
}
