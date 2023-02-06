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

package common

import (
	"bytes"
	"encoding/json"
	"io"
	"io/ioutil"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

// ExtResponse .
type ExtResponse struct {
	Success bool        `json:"success,omitempty"`
	Data    interface{} `json:"data,omitempty"`
	Err     *api.Error  `json:"err,omitempty"`
	UserIDs []string    `json:"userIDs,omitempty"`
	status  int
}

// Error .
func (r *ExtResponse) Error(ctx httpserver.Context) error { return nil }

// Status .
func (r *ExtResponse) Status(ctx httpserver.Context) int { return r.status }

// ReadCloser .
func (r *ExtResponse) ReadCloser(ctx httpserver.Context) io.ReadCloser {
	byts, err := json.Marshal(r)
	if err != nil {
		byts, _ = json.Marshal(&api.Response{
			Success: false,
			Err: &api.Error{
				Code: api.InternalError.Code(),
				Msg:  err.Error(),
			},
		})
		r.status = api.InternalError.Status()
	}
	return ioutil.NopCloser(bytes.NewBuffer(byts))
}

func (r *ExtResponse) Response(ctx httpserver.Context) httpserver.Response {
	if r.Err != nil {
		errMsg, ok := r.Err.Msg.(api.ErrorMessage)
		if ok {
			r.Err.Msg = errMsg.Message(ctx)
		}
	}
	header := ctx.ResponseWriter().Header()
	if header.Get("Content-Type") == "" {
		header.Set("Content-Type", "application/json; charset=UTF-8")
	}
	return r
}

// SuccessExt .
func SuccessExt(data interface{}, userIDs []string, status ...int) *ExtResponse {
	var statusCode int
	if len(status) > 0 {
		statusCode = status[0]
	}
	return &ExtResponse{
		Success: true,
		Data:    data,
		UserIDs: userIDs,
		status:  statusCode,
	}
}
