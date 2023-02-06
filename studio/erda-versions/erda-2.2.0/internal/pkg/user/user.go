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

// Package user 定义通用的 user 逻辑.
package user

import (
	"net/http"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// ID 定义 user id.
type ID string

// String 返回 user id 字符串类型.
func (i ID) String() string {
	return string(i)
}

// Invalid 返回 user id 是否有效.
func (i ID) Invalid() bool {
	return string(i) == ""
}

// GetUserID 从 http request 的 header 中读取 user id.
func GetUserID(r *http.Request) (ID, error) {
	v := r.Header.Get("USER-ID")
	id := ID(v)

	if id.Invalid() {
		return id, errors.New("invalid user id")
	}
	return id, nil
}

// GetIdentityInfo 从 http.Request 中获取用户 ID && Internal-Client
//
// return: IdentityInfo, error
func GetIdentityInfo(r *http.Request) (apistructs.IdentityInfo, error) {
	// 尝试从 Header 中获取用户信息
	headerUserID, headerUserErr := GetUserID(r)

	// 尝试从 Header 中获取 Internal-Client
	internalClient := r.Header.Get(httputil.InternalHeader)

	// 未登录
	if headerUserErr != nil && internalClient == "" {
		return apistructs.IdentityInfo{}, errors.Errorf("invalid identity info")
	}

	identity := apistructs.IdentityInfo{
		UserID:         string(headerUserID),
		InternalClient: internalClient,
	}

	return identity, nil
}
