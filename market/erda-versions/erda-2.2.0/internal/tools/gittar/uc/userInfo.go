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

package uc

import (
	"fmt"
	"reflect"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

type UserInfo struct {
	Name      string      `json:"name,omitempty"`
	Username  string      `json:"username,omitempty"`
	Nickname  string      `json:"nickname,omitempty"`
	AvatarUrl string      `json:"avatar_url,omitempty"`
	UserId    interface{} `json:"user_id,omitempty"`
}

// uc 2.0
type UserInfoDto struct {
	AvatarUrl string      `json:"avatarUrl,omitempty"`
	Email     string      `json:"email,omitempty"`
	UserId    interface{} `json:"id,omitempty"`
	NickName  string      `json:"nickName,omitempty"`
	Phone     string      `json:"phone,omitempty"`
	RealName  string      `json:"realName,omitempty"`
	Username  string      `json:"username,omitempty"`
}

func (u *UserInfoDto) Convert() (string, error) {
	switch u.UserId.(type) {
	case string:
		return u.UserId.(string), nil
	case int:
		return strconv.Itoa(u.UserId.(int)), nil
	case int64:
		return strconv.FormatInt(u.UserId.(int64), 10), nil
	case float64:
		return fmt.Sprintf("%g", u.UserId.(float64)), nil
	default:
		return "", errors.Errorf("invalid type of %v", reflect.TypeOf(u.UserId))
	}
}

func (u *UserInfoDto) GetUsername() string {
	if u.NickName != "" {
		return u.NickName
	}
	if u.RealName != "" {
		return u.RealName
	}
	if u.Phone != "" {
		return u.Phone
	}
	if u.Email != "" {
		return u.Email
	}

	// 由 uc 2.0 生成
	return u.Username
}

// Deprecated
func (u *UserInfo) Convert() (string, error) {
	switch u.UserId.(type) {
	case string:
		return u.UserId.(string), nil
	case int:
		return strconv.Itoa(u.UserId.(int)), nil
	case int64:
		return strconv.FormatInt(u.UserId.(int64), 10), nil
	case float64:
		return fmt.Sprintf("%g", u.UserId.(float64)), nil
	default:
		return "", errors.Errorf("invalid type of %v", reflect.TypeOf(u.UserId))
	}
}

// Deprecated
func (u *UserInfo) UserName() string {
	if u == nil {
		return ""
	}
	if len(u.Name) != 0 {
		return u.Name
	}
	if len(u.Nickname) != 0 {
		return u.Nickname
	}
	if u.Username != "" {
		return u.Username
	}
	logrus.Errorf("can not cat any user name from user=%v", u)
	return ""
}
