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

package apierrors

import "github.com/erda-project/erda/pkg/http/httpserver/errorresp"

var (
	// uc users
	ErrListUser                = err("ErrListUser", "获取用户列表失败")
	ErrGetUser                 = err("ErrGetUser", "获取用户详情失败")
	ErrCreateUser              = err("ErrCreateUser", "创建用户失败")
	ErrUpdateUserInfo          = err("ErrUpdateUserInfo", "更新用户信息失败")
	ErrFreezeUser              = err("ErrFreezeUser", "冻结用户失败")
	ErrBatchFreezeUser         = err("ErrBatchFreezeUser", "批量冻结用户失败")
	ErrUnfreezeUser            = err("ErrUnfreezeUser", "解冻用户失败")
	ErrBatchUnFreezeUser       = err("ErrBatchUnFreezeUser", "批量解冻用户失败")
	ErrGetPwdSecurityConfig    = err("ErrGetPwdSecurityConfig", "获取密码安全配置失败")
	ErrUpdatePwdSecurityConfig = err("ErrUpdatePwdSecurityConfig", "更新密码安全配置失败")
	ErrUpdateLoginMethod       = err("ErrUpdateLoginMethod", "更新用户登录方式失败")
	ErrListLoginMethod         = err("ErrListLoginMethod", "获取用户登录方式失败")
	ErrAdminUser               = err("ErrAdminUser", "用户操作失败")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
