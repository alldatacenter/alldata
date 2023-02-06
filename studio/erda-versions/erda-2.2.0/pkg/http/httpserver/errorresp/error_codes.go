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

package errorresp

import (
	"fmt"
	"net/http"

	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

// Error Codes i18n templates
var (
	templateMissingParameter      = i18n.NewTemplate("MissingParameter", "缺少参数 %s")
	templateInvalidParameter      = i18n.NewTemplate("InvalidParameter", "参数错误 %s")
	templateInvalidState          = i18n.NewTemplate("InvalidState", "状态异常 %s")
	templateNotLogin              = i18n.NewTemplate("NotLogin", "未登录")
	templateAccessDenied          = i18n.NewTemplate("AccessDenied", "无权限")
	templateNotFound              = i18n.NewTemplate("NotFound", "资源不存在")
	templateAlreadyExists         = i18n.NewTemplate("AlreadyExists", "资源已存在")
	templateInternalError         = i18n.NewTemplate("InternalError", "异常 %s")
	templateErrorVerificationCode = i18n.NewTemplate("ErrorVerificationCode", "验证码错误 %s")
	templateAuthorizedRoles       = i18n.NewTemplate("AuthorizedRoles", "有权限的角色: %s")
)

// MissingParameter 缺少参数
func (e *APIError) MissingParameter(err string) *APIError {
	return e.dup().appendCode(http.StatusBadRequest, "MissingParameter").
		appendLocaleTemplate(templateMissingParameter, err)
}

// InvalidParameter 参数错误
func (e *APIError) InvalidParameter(err interface{}) *APIError {
	return e.dup().appendCode(http.StatusBadRequest, "InvalidParameter").
		appendLocaleTemplate(templateInvalidParameter, toString(err))
}

// InvalidState 状态异常
func (e *APIError) InvalidState(err string) *APIError {
	return e.dup().appendCode(http.StatusBadRequest, "InvalidState").
		appendLocaleTemplate(templateInvalidState, err)
}

// NotLogin 未登录
func (e *APIError) NotLogin() *APIError {
	return e.dup().appendCode(http.StatusUnauthorized, "NotLogin").
		appendLocaleTemplate(templateNotLogin)
}

// AccessDenied 无权限
func (e *APIError) AccessDenied() *APIError {
	return e.dup().appendCode(http.StatusForbidden, "AccessDenied").
		appendLocaleTemplate(templateAccessDenied)
}

// NotFound 资源不存在
func (e *APIError) NotFound() *APIError {
	return e.dup().appendCode(http.StatusNotFound, "NotFound").
		appendLocaleTemplate(templateNotFound)
}

// Error prompts who has permission for the request
func (e *APIError) AppendAuthorizedRoles(roles []string) *APIError {
	return e.dup().appendLocaleTemplate(templateAuthorizedRoles, strutil.Join(roles, ", "))
}

func IsNotFound(e error) bool {
	return getCode(e) == "NotFound"
}

// AlreadyExists 资源已存在
func (e *APIError) AlreadyExists() *APIError {
	return e.dup().appendCode(http.StatusConflict, "AlreadyExists").
		appendLocaleTemplate(templateAlreadyExists)
}

// InternalError 异常
func (e *APIError) InternalError(err error) *APIError {
	return e.dup().appendCode(http.StatusInternalServerError, "InternalError").
		appendLocaleTemplate(templateInternalError, err.Error())
}

// ErrorVerificationCode 验证码错误
func (e *APIError) ErrorVerificationCode(err error) *APIError {
	return e.dup().appendCode(http.StatusInternalServerError, "ErrorVerificationCode").
		appendLocaleTemplate(templateErrorVerificationCode, err.Error())
}

func toString(err interface{}) string {
	switch t := err.(type) {
	case string:
		return err.(string)
	case error:
		return err.(error).Error()
	default:
		_ = t
		return fmt.Sprintf("%v", err)
	}
}

func getCode(e error) string {
	switch t := e.(type) {
	case *APIError:
		return t.code
	default:
		return ""
	}
}
