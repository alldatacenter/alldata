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

package notify

import (
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"sync"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

type NotifyGroup struct {
	db  *dao.DBClient
	uc  *ucauth.UCClient
	bdl *bundle.Bundle
}

type Option func(*NotifyGroup)

func New(options ...Option) *NotifyGroup {
	o := &NotifyGroup{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(o *NotifyGroup) {
		o.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(o *NotifyGroup) {
		o.bdl = bdl
	}
}

func (o *NotifyGroup) Create(locale *i18n.LocaleResource, createReq *apistructs.CreateNotifyGroupRequest) (int64, error) {
	exist, err := o.db.CheckNotifyGroupNameExist(createReq.ScopeType, createReq.ScopeID, createReq.Name)
	if err != nil {
		return 0, err
	}
	if exist {
		return 0, errors.New(locale.Get("ErrCreateNotifyGroup.NameExist"))
	}
	err = o.CheckNotifyGroupTarget(createReq.Targets)
	if err != nil {
		return 0, err
	}
	return o.db.CreateNotifyGroup(createReq)
}

func (o *NotifyGroup) Update(updateReq *apistructs.UpdateNotifyGroupRequest) error {
	err := o.CheckNotifyGroupTarget(updateReq.Targets)
	if err != nil {
		return err
	}
	return o.db.UpdateNotifyGroup(updateReq)
}

func (o *NotifyGroup) Get(id int64, orgID int64) (*apistructs.NotifyGroup, error) {
	return o.db.GetNotifyGroupByID(id, orgID)
}

func (o *NotifyGroup) BatchGet(ids []int64) ([]*apistructs.NotifyGroup, error) {
	return o.db.BatchGetNotifyGroup(ids)
}

func (o *NotifyGroup) Delete(id int64, orgID int64) error {
	notify, err := o.db.GetNotifyByGroupID(id)
	if err == gorm.ErrRecordNotFound {
		return o.db.DeleteNotifyGroup(id, orgID)
	}
	if err != nil {
		return err
	}
	return fmt.Errorf("存在关联通知无法删除 关联通知ID:%d 通知名:%s ", notify.ID, notify.Name)
}

func (o *NotifyGroup) Query(queryReq *apistructs.QueryNotifyGroupRequest, orgID int64) (*apistructs.QueryNotifyGroupData, error) {
	return o.db.QueryNotifyGroup(queryReq, orgID)
}

func (o *NotifyGroup) GetDetail(id int64, orgID int64) (*apistructs.NotifyGroupDetail, error) {
	result := &apistructs.NotifyGroupDetail{}
	group, err := o.db.GetNotifyGroupByID(id, orgID)
	if err != nil {
		return nil, err
	}
	for _, target := range group.Targets {
		var err error
		switch target.Type {
		case apistructs.UserNotifyTarget:
			var userIDs []string
			for _, value := range target.Values {
				userIDs = append(userIDs, value.Receiver)
			}
			notifyUsers, err := getNotifyUsersByIDs(userIDs)
			if err != nil {
				return nil, err
			}
			result.Users = append(result.Users, notifyUsers...)
		case apistructs.ExternalUserNotifyTarget:
			// 外部用户
			for _, valueStr := range target.Values {
				var user apistructs.NotifyUser
				err := json.Unmarshal([]byte(valueStr.Receiver), &user)
				if err == nil {
					result.Users = append(result.Users, user)
				}
			}
		case apistructs.DingdingNotifyTarget:
			for _, dingding := range target.Values {
				result.DingdingList = append(result.DingdingList, dingding)
			}
		case apistructs.DingdingWorkNoticeNotifyTarget:
			for _, dingding := range target.Values {
				result.DingdingWorkNoticeList = append(result.DingdingWorkNoticeList, dingding)
			}
		case apistructs.WebhookNotifyTarget:
			for _, webhookUrl := range target.Values {
				result.WebHookList = append(result.WebHookList, webhookUrl.Receiver)
			}
		case apistructs.RoleNotifyTarget:
			var roles []string
			for _, r := range target.Values {
				roles = append(roles, r.Receiver)
			}
			var members []model.Member
			if group.ScopeType == apistructs.MSPScope {
				label := make(map[string]string)
				err = json.Unmarshal([]byte(group.Label), &label)
				if err != nil {
					return nil, err
				}
				scopeID, err := strconv.ParseInt(label[apistructs.MSPMemberScopeId], 10, 32)
				if err != nil {
					return nil, err
				}
				_, members, err = o.db.GetMembersByParam(&apistructs.MemberListRequest{
					ScopeType: apistructs.ScopeType(label[apistructs.MSPMemberScope]),
					ScopeID:   scopeID,
					Roles:     roles,
					Q:         "",
					PageNo:    1,
					PageSize:  1000,
				})
			} else {
				scopeID, err := strconv.ParseInt(group.ScopeID, 10, 32)
				if err != nil {
					return nil, err
				}
				_, members, err = o.db.GetMembersByParam(&apistructs.MemberListRequest{
					ScopeType: apistructs.ScopeType(group.ScopeType),
					ScopeID:   scopeID,
					Roles:     roles,
					Q:         "",
					PageNo:    1,
					PageSize:  1000,
				})
			}
			if err != nil {
				return nil, err
			}
			var userIDs []string
			for _, member := range members {
				userIDs = append(userIDs, member.UserID)
			}
			notifyUsers, err := getNotifyUsersByIDs(userIDs)
			if err != nil {
				return nil, err
			}
			result.Users = append(result.Users, notifyUsers...)
		}
		if err != nil {
			return nil, err
		}
	}

	result.ID = group.ID
	result.Name = group.Name
	result.ScopeType = group.ScopeType
	result.ScopeID = group.ScopeID
	result.Targets = group.Targets
	result.DingdingList = uniqueTargetList(result.DingdingList)
	result.DingdingWorkNoticeList = uniqueTargetList(result.DingdingWorkNoticeList)
	return result, nil
}

func uniqueTargetList(list []apistructs.Target) []apistructs.Target {
	var result []apistructs.Target
	keyMap := map[string]apistructs.Target{}
	for _, v := range list {
		keyMap[v.Receiver] = v
	}
	for _, target := range keyMap {
		result = append(result, target)
	}
	return result
}

var (
	once      sync.Once
	tokenAuth *ucauth.UCTokenAuth
	client    *httpclient.HTTPClient
)

type USERID string

// User 用户中心用户数据结构
type User struct {
	ID        USERID `json:"user_id"`
	Name      string `json:"username"`
	AvatarURL string `json:"avatar_url"`
	Phone     string `json:"phone_number"`
	Email     string `json:"email"`
	Nick      string `json:"nickname"`
}

// UnmarshalJSON maybe int or string, unmarshal them to string(USERID)
func (u *USERID) UnmarshalJSON(b []byte) error {
	var intid int
	if err := json.Unmarshal(b, &intid); err != nil {
		var stringid string
		if err := json.Unmarshal(b, &stringid); err != nil {
			return err
		}
		*u = USERID(stringid)
		return nil
	}
	*u = USERID(strconv.Itoa(intid))
	return nil
}

func getNotifyUsersByIDs(userIds []string) ([]apistructs.NotifyUser, error) {
	users, err := getUsers(userIds)
	if err != nil {
		return nil, err
	}
	var notifyUsers []apistructs.NotifyUser
	for _, user := range users {
		user := apistructs.NotifyUser{
			ID:       user.ID,
			Type:     "inner",
			Email:    user.Email,
			Mobile:   user.Phone,
			Username: user.Name,
		}
		notifyUsers = append(notifyUsers, user)
	}
	return notifyUsers, nil
}

func getUsers(IDs []string) (map[string]apistructs.UserInfo, error) {
	once.Do(func() {
		var err error
		tokenAuth, err = ucauth.NewUCTokenAuth(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
		if err != nil {
			panic(err)
		}
		client = httpclient.New(httpclient.WithDialerKeepAlive(10 * time.Second))
	})
	var (
		err   error
		token ucauth.OAuthToken
	)
	if token, err = tokenAuth.GetServerToken(false); err != nil {
		return nil, err
	}
	parts := make([]string, len(IDs))
	for i := range IDs {
		parts[i] = strutil.Concat("user_id:", IDs[i])
	}
	query := strutil.Join(parts, " OR ")
	var b []User
	resp, err := client.Get(discover.UC()).Path("/api/open/v1/users").Param("query", query).
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).Do().JSON(&b)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() {
		return nil, fmt.Errorf("failed to get users, status code: %d", resp.StatusCode())
	}
	users := make(map[string]apistructs.UserInfo, len(b))
	for i := range b {
		users[string(b[i].ID)] = apistructs.UserInfo{
			ID:     string(b[i].ID),
			Email:  b[i].Email,
			Phone:  b[i].Phone,
			Avatar: b[i].AvatarURL,
			Name:   b[i].Name,
			Nick:   b[i].Nick,
		}
	}
	return users, nil
}
