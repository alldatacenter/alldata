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

package ucclient

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/conf"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// User 用户中心用户数据结构
type User struct {
	ID        uint64 `json:"user_id"`
	Name      string `json:"username"`
	Nick      string `json:"nickname"`
	AvatarURL string `json:"avatar_url"`
	Phone     string `json:"phone_number"`
	Email     string `json:"email"`
}

// UCClient UC客户端
type UCClient struct {
	baseURL     string
	client      *httpclient.HTTPClient
	ucTokenAuth *ucauth.UCTokenAuth
}

// NewUCClient 初始化UC客户端
func NewUCClient() *UCClient {
	endpoint := discover.UC()
	clientID := conf.UCClientID()
	secret := conf.UCClientSecret()

	logrus.Debugf("initialize uc client, addr: %s, clientID: %s, secret: %s", endpoint, clientID, secret)

	tokenAuth, err := ucauth.NewUCTokenAuth(endpoint, clientID, secret)
	if err != nil {
		panic(err)
	}
	return &UCClient{
		baseURL:     endpoint,
		client:      httpclient.New(),
		ucTokenAuth: tokenAuth,
	}
}

// InvalidateServerToken 使 server token 失效
func (c *UCClient) InvalidateServerToken() {
	c.ucTokenAuth.ExpireServerToken()
}

// FindUsers 根据用户ID查找用户信息
func (c *UCClient) FindUsers(ids []string) ([]User, error) {
	if len(ids) == 0 {
		return nil, nil
	}
	parts := make([]string, len(ids))
	for _, id := range ids {
		parts = append(parts, "user_id:"+id)
	}
	query := strings.Join(parts, " OR ")

	// 保证返回的用户顺序为 id 列表顺序

	return c.findUsersByQuery(query, ids...)
}

// FindUsersByKey 根据key查找用户，key可匹配用户名/邮箱/手机号
func (c *UCClient) FindUsersByKey(key string) ([]User, error) {
	if key == "" {
		return nil, nil
	}
	query := fmt.Sprintf("username:%s OR nickname:%s OR phone_number:%s OR email:%s", key, key, key, key)

	return c.findUsersByQuery(query)
}

// GetUser 获取用户详情
func (c *UCClient) GetUser(userID string) (*User, error) {
	var (
		user *User
		err  error
	)
	// 增加重试机制，防止因 uc 升级 serverToken 格式不兼容，无法获取用户信息
	for i := 0; i < 3; i++ {
		user, err = c.getUser(userID)
		if err != nil {
			continue
		}
		return user, nil
	}

	return nil, err
}

// ListUCAuditsByLastID 根据lastID获取uc的审计事件
func (c *UCClient) ListUCAuditsByLastID(ucAuditReq apistructs.UCAuditsListRequest) (*apistructs.UCAuditsListResponse, error) {
	token, err := c.ucTokenAuth.GetServerToken(false)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to get token when finding user")
	}

	var getResp apistructs.UCAuditsListResponse
	resp, err := c.client.Post(c.baseURL).
		Path("/api/event-log/admin/list-last-event").
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		JSONBody(&ucAuditReq).Do().JSON(&getResp)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("failed to list uc audits, status-code: %d", resp.StatusCode())
	}

	return &getResp, nil
}

// ListUCAuditsByEventTime 根据时间获取uc的审计事件
func (c *UCClient) ListUCAuditsByEventTime(ucAuditReq apistructs.UCAuditsListRequest) (*apistructs.UCAuditsListResponse, error) {
	token, err := c.ucTokenAuth.GetServerToken(false)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to get token when finding user")
	}

	var getResp apistructs.UCAuditsListResponse
	resp, err := c.client.Post(c.baseURL).
		Path("/api/event-log/admin/list-event-time").
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		JSONBody(&ucAuditReq).Do().JSON(&getResp)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("failed to list uc audits, status-code: %d", resp.StatusCode())
	}

	return &getResp, nil
}

func (c *UCClient) getUser(userID string) (*User, error) {
	token, err := c.ucTokenAuth.GetServerToken(false)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to get token when finding user")
	}

	var user User
	r, err := c.client.Get(c.baseURL).
		Path(strutil.Concat("/api/open/v1/users/", userID)).
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		Do().JSON(&user)
	if err != nil {
		return nil, err
	}

	if !r.IsOK() {
		if r.StatusCode() == http.StatusUnauthorized {
			c.InvalidateServerToken()
		}
		return nil, errors.Errorf("failed to find user, status code: %d", r.StatusCode())
	}
	if user.ID == 0 {
		return nil, errors.Errorf("failed to find user %s", userID)
	}

	return &user, nil
}

func (c *UCClient) findUsersByQuery(query string, idOrder ...string) ([]User, error) {
	// 获取token
	token, err := c.ucTokenAuth.GetServerToken(false)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to get token when finding users")
	}

	// 批量查询用户
	var users []User
	var b bytes.Buffer
	r, err := c.client.Get(c.baseURL).
		Path("/api/open/v1/users").
		Param("query", query).
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		Do().Body(&b)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to find users")
	}
	if !r.IsOK() {
		return nil, errors.Errorf("failed to find users, status code: %d", r.StatusCode())
	}
	content := b.Bytes()
	if err := json.Unmarshal(content, &users); err != nil {
		return nil, fmt.Errorf("failed to unmarshal: %v", string(content))
	}

	// 保证顺序
	if len(idOrder) > 0 {
		userMap := make(map[string]User)
		for _, user := range users {
			userMap[strconv.FormatUint(user.ID, 10)] = user
		}
		var orderedUsers []User
		for _, id := range idOrder {
			orderedUsers = append(orderedUsers, userMap[id])
		}
		return orderedUsers, nil
	}

	return users, nil
}
