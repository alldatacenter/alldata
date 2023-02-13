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

package util

import (
	"encoding/json"
	"net/http"
	"reflect"
	"strings"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/wrapper"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
)

func DoCommonRequest(client *http.Client, method, url string, data interface{}, headers ...map[string]string) (int, []byte, error) {
	log.Debugf("%+v", headers) // output for debug
	var reqBody []byte
	var err error
	v := reflect.ValueOf(data)
	k := v.Kind()
	if data == nil || ((k == reflect.Ptr || k == reflect.Interface) &&
		reflect.ValueOf(data).IsNil()) {
		reqBody = []byte("")
	} else {
		switch data := data.(type) {
		case []byte:
			reqBody = data
		case string:
			reqBody = []byte(data)
		default:
			// dto object
			reqBody, err = json.Marshal(data)
			if err != nil {
				return 0, nil, errors.Wrap(err, "json marshal failed")
			}
		}
	}
	if len(reqBody) > 0 {
		contentTypeHeaderExist := false
		for _, kv := range headers {
			for key := range kv {
				if strings.EqualFold(key, "content-type") {
					contentTypeHeaderExist = true
				}
			}
		}
		if !contentTypeHeaderExist {
			headers = append(headers, map[string]string{
				"Content-Type": "application/json;charset=UTF-8"})
		}
	}
	respBody, resp, err := wrapper.DoRequest(client, method, url, reqBody, config.ServerConf.ReqTimeout, headers...)
	if err != nil {
		return 0, nil, errors.Wrap(err, "http wrapper request failed")
	}
	if err != nil {
		return resp.StatusCode, nil, errors.Wrap(err, "read from response failed")
	}
	return resp.StatusCode, respBody, nil
}

func CommonRequest(method, url string, data interface{}, headers ...map[string]string) (int, []byte, error) {
	client := &http.Client{}
	return DoCommonRequest(client, method, url, data, headers...)
}
