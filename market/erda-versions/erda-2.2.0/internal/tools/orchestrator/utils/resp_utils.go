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

package utils

import (
	"bytes"
	"encoding/json"
	"net/http"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

type ErrCode string

const (
	ErrCode0101 = "ORT0101" // Internal Error
	ErrCode0107 = "ORT0107" // Illegal Param
	ErrCode0108 = "ORT0108" // Illegal DiceYml
	ErrCode0109 = "ORT0109" // Cluster Not Found
	ErrCode0110 = "ORT0110" // 正在部署中，请不要重复部署
	ErrCode0111 = "ORT0111" // not login

	// TODO: fresh new code definitions
	RuntimeNotFound = "RuntimeNotFound"
)

type Resp struct {
	Success bool                      `json:"success"`
	Data    interface{}               `json:"data,omitempty"`
	Err     *apistructs.ErrorResponse `json:"err,omitempty"`
}

type RespForRead struct {
	Success bool                      `json:"success"`
	Data    json.RawMessage           `json:"data,omitempty"`
	Err     *apistructs.ErrorResponse `json:"err,omitempty"`
}

func DoJson(r *httpclient.Request, o interface{}) error {
	var b bytes.Buffer
	hr, err := r.Header("Content-Type", "application/json").
		Do().Body(&b)
	if err != nil {
		return errors.Wrap(err, "failed to request http")
	}
	if !hr.IsOK() {
		return errors.Errorf("failed to request http, status-code %d, content-type %s, raw body %s",
			hr.StatusCode(), hr.ResponseHeader("Content-Type"), b.String())
	}
	var resp RespForRead
	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return errors.Wrapf(err, "response not json, raw body %s", b.String())
	}
	if !resp.Success {
		return errors.Errorf("rest api not success, raw body %s, resp is %v", b.String(), resp)
	}
	if o == nil {
		return nil
	}
	if err := json.Unmarshal([]byte(resp.Data), o); err != nil {
		return errors.Wrapf(err, "resp.Data not json, raw body %s, data is %v", b.String(), string(resp.Data))
	}
	return nil
}

func ErrResp(status int, err *apistructs.ErrorResponse) (httpserver.Responser, error) {
	// make alert
	logrus.Errorf("[alert] ErrResp occur!, status-code is %v, err is %v", status, err)
	return httpserver.HTTPResponse{
		Status: status,
		Content: Resp{
			Success: false,
			Err:     err,
		},
	}, nil
}

func ErrResp0101(err error, msg string) (httpserver.Responser, error) {
	return ErrResp(http.StatusInternalServerError, &apistructs.ErrorResponse{Code: ErrCode0101, Msg: errors.Wrap(err, msg).Error()})
}

func ErrRespIllegalParam(err error, msg string) (httpserver.Responser, error) {
	return ErrResp(http.StatusBadRequest, &apistructs.ErrorResponse{Code: ErrCode0107, Msg: errors.Wrap(err, msg).Error()})
}
