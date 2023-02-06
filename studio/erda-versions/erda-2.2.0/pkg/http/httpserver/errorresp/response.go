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
	"encoding/json"
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/i18n"
)

// ToResp 根据 APIError 转为一个 http error response.
func (e *APIError) ToResp() httpserver.Responser {
	return &httpserver.HTTPResponse{
		Error:  e,
		Status: e.httpCode,
		Content: httpserver.Resp{
			Success: false,
			Err: apistructs.ErrorResponse{
				Code: e.code,
				Msg:  e.msg,
				Ctx:  e.ctx,
			},
		},
	}
}

// ErrResp 根据 error 转为一个 http error response.
func ErrResp(e error) (httpserver.Responser, error) {
	switch t := e.(type) {
	case *APIError:
		return e.(*APIError).ToResp(), nil
	default:
		_ = t
		return New().InternalError(e).ToResp(), nil
	}
}

// Write 将错误写入 http.ResponseWriter
func (e *APIError) Write(w http.ResponseWriter) error {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(e.httpCode)
	return json.NewEncoder(w).Encode(httpserver.Resp{
		Success: false,
		Err: apistructs.ErrorResponse{
			Code: e.code,
			Msg:  e.Render(&i18n.LocaleResource{}),
		},
	})
}

// ErrWrite 根据 error 写入标准错误格式
func ErrWrite(e error, w http.ResponseWriter) error {
	switch t := e.(type) {
	case *APIError:
		return e.(*APIError).Write(w)
	default:
		_ = t
		return New().InternalError(e).Write(w)
	}
}
