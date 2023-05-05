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

// Package response release api 响应格式
package response

import (
	"encoding/json"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/apps/dop/dicehub/errcode"
)

// Response 内部约定的Resposne格式, 更多，请参考: https://yuque.antfin-inc.com/terminus_paas_dev/paas/fozwef
type Response struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data,omitempty"`
	Err     ErrorMsg    `json:"err,omitempty"`
}

// ErrorMsg release api 错误消息格式
type ErrorMsg struct {
	Code    string      `json:"code,omitempty"`
	Msg     string      `json:"msg,omitempty"`
	Context interface{} `json:"ctx,omitempty"`
}

// Success release api 成功时响应
func Success(w http.ResponseWriter, data interface{}) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.WriteHeader(http.StatusOK)

	response := Response{
		Success: true,
		Data:    data,
	}

	jsonResponse, err := json.Marshal(response)
	if err != nil {
		logrus.Errorf("response parse to json err: %v", err)
	}

	w.Write(jsonResponse)
}

// Error release api 失败时响应
func Error(w http.ResponseWriter, statusCode int, code errcode.ImageErrorCode, errorMsg string) {
	w.Header().Set("Content-Type", "applicaton/json; charset=utf-8")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.WriteHeader(statusCode)

	response := &Response{
		Success: false,
		Err: ErrorMsg{
			Code: string(code),
			Msg:  errorMsg,
		},
	}

	jsonResponse, err := json.Marshal(response)
	if err != nil {
		logrus.Errorf("response parse to json err: %v", err)
	}

	w.Write(jsonResponse)
}
