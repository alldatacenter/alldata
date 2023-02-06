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

// Package httpserver httpserver统一封装，使API注册更容易
package httpserver

import (
	"encoding/json"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver/ierror"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

// Responser is an interface for http response
type Responser interface {
	GetLocaledResp(locale *i18n.LocaleResource) HTTPResponse
	GetStatus() int
	GetContent() interface{}
}

// HTTPResponse is a struct contains status code and content body
type HTTPResponse struct {
	Error   ierror.IAPIError
	Status  int
	Content interface{}
}

// GetStatus returns http status code.
func (r HTTPResponse) GetStatus() int {
	return r.Status
}

// GetContent returns http content body
func (r HTTPResponse) GetContent() interface{} {
	return r.Content
}

// GetLocaledResp
func (r HTTPResponse) GetLocaledResp(locale *i18n.LocaleResource) HTTPResponse {
	if r.Error != nil {
		return HTTPResponse{
			Status: r.Status,
			Content: Resp{
				Success: false,
				Err: apistructs.ErrorResponse{
					Code: r.Error.Code(),
					Msg:  r.Error.Render(locale),
					Ctx:  r.Error.Ctx(),
				},
			},
		}
	}
	return r
}

// Resp dice平台http body返回结构
type Resp struct {
	Success bool                     `json:"success"`
	Data    interface{}              `json:"data,omitempty"`
	Err     apistructs.ErrorResponse `json:"err,omitempty"`
	UserIDs []string                 `json:"userIDs,omitempty"`
}

// ErrResp 采用httpserver框架时异常返回结果封装
func ErrResp(statusCode int, code, errMsg string) (Responser, error) {
	return HTTPResponse{
		Status: statusCode,
		Content: Resp{
			Success: false,
			Err: apistructs.ErrorResponse{
				Code: code,
				Msg:  errMsg,
			},
		},
	}, nil
}

// OkResp 采用httpserver框架时正常返回结果封装
//
// 在 `userIDs` 中设置需要由 openapi 注入的用户信息的 ID 列表
func OkResp(data interface{}, userIDs ...[]string) (Responser, error) {
	content := Resp{
		Success: true,
		Data:    data,
	}
	if len(userIDs) > 0 {
		content.UserIDs = strutil.DedupSlice(userIDs[0], true)
	}
	return HTTPResponse{
		Status:  http.StatusOK,
		Content: content,
	}, nil
}

// NotOkResp 采用httpserver框架时异常返回结果封装
//
// 在 `userIDs` 中设置需要由 openapi 注入的用户信息的 ID 列表
func NotOkResp(data interface{}, status int, userIDs ...[]string) (Responser, error) {
	content := Resp{
		Success: false,
		Data:    data,
	}
	if len(userIDs) > 0 {
		content.UserIDs = strutil.DedupSlice(userIDs[0], true)
	}
	return HTTPResponse{
		Status:  status,
		Content: content,
	}, nil
}

// WriteYAML 响应yaml结构
func WriteYAML(w http.ResponseWriter, v string) {
	w.Header().Set("Content-Type", "application/x-yaml; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, err := w.Write([]byte(v))
	if err != nil {
		logrus.Debugln(err)
	}
}

func WriteJSON(w http.ResponseWriter, v interface{}) {
	w.Header().Set("Content-Type", ContentTypeJSON)
	w.WriteHeader(http.StatusOK)
	b, err := json.Marshal(v)
	if err != nil {
		logrus.Debugln(err)
	}
	_, err = w.Write(b)
	if err != nil {
		logrus.Debugln(err)
	}
}

func WriteData(w http.ResponseWriter, v interface{}) {
	WriteJSON(w, Resp{
		Success: true,
		Data:    v,
	})
}

func WriteErr(w http.ResponseWriter, code, errMsg string) {
	WriteJSON(w, Resp{
		Success: false,
		Err: apistructs.ErrorResponse{
			Code: code,
			Msg:  errMsg,
		},
	})
}
