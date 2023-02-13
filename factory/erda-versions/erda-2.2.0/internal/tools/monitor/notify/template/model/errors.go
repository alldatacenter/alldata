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

package model

import "github.com/erda-project/erda-infra/providers/legacy/httpendpoints/errorresp"

var (
	ErrCreateNotify = err("ErrCreateNotify", "创建通知失败")
	ErrGetNotify    = err("ErrGetNotify", "获取通知失败")
	ErrDeleteNotify = err("ErrDeleteNotify", "删除通知失败")
	ErrUpdateNotify = err("ErrUpdateNotify", "更新通知失败")
	ErrNotifyEnable = err("ErrNotifyEnable", "启用通知失败")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
