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

package component_protocol

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"time"

	jsi "github.com/json-iterator/go"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/types"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	proxyErrorCode = "Proxy Error"
)

func modifyProxyResponse(proxyConfig types.ProxyConfig) func(*http.Response) error {
	return func(resp *http.Response) error {
		logrus.Infof("[DEBUG] start wrap erda style resp at %s", time.Now().Format(time.StampNano))
		if err := wrapErdaStyleResponse(proxyConfig, resp); err != nil {
			logrus.Errorf("failed to wrap erda style response when modify proxied response of component-protocol: %v", err)
			return err
		}
		logrus.Infof("[DEBUG] end wrap erda style resp at %s", time.Now().Format(time.StampNano))
		return nil
	}
}

// response .
type response struct {
	Success  bool                           `json:"success,omitempty"`
	Data     interface{}                    `json:"data,omitempty"`
	Err      apistructs.ErrorResponse       `json:"err,omitempty"`
	UserIDs  []string                       `json:"userIDs,omitempty"`
	UserInfo map[string]apistructs.UserInfo `json:"userInfo,omitempty"`
}

type cpErrResponse struct {
	Code int    `json:"code,omitempty"`
	Err  string `json:"err,omitempty"`
}

// wrapErdaStyleResponse wrap response by erda response.
func wrapErdaStyleResponse(proxyConfig types.ProxyConfig, resp *http.Response) (wErr error) {
	if resp.Header.Get("X-NEED-USER-INFO") != "true" {
		logrus.Info("resp doesn't have need user info header, skip inject user info")
		resp.Header.Set("Content-Type", "application/json")
		return
	}
	content, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return err
	}
	defer func() {
		if r := recover(); r != nil {
			resp.Body = ioutil.NopCloser(bytes.NewReader(content))
			wErr = fmt.Errorf("err: %v, responseBody: %s", r, string(content))
		}
		resp.Header.Set("Content-Type", "application/json")
	}()

	// construct erda style response
	var erdaResp response
	if err := jsi.Unmarshal(content, &erdaResp); err != nil {
		panic(err)
	}
	renderResponse, ok := erdaResp.Data.(map[string]interface{})
	if !ok {
		logrus.Infof("data in response is not map[string]interface{}, skip inject user info")
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}
	protocol, ok := renderResponse["protocol"]
	if !ok {
		logrus.Infof("protocol is nil in response, skip inject user info")
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}
	obj, ok := protocol.(map[string]interface{})
	if !ok {
		logrus.Infof("protocol in response is not map[string]interface{}, skip inject user info")
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}
	globalState, ok := obj["state"]
	if !ok {
		logrus.Infof("globalState is nil in response, skip inject user info")
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}
	obj, ok = globalState.(map[string]interface{})
	if !ok {
		logrus.Infof("globalState is response is not map[string]interface{}, skip inject user info")
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}

	userIDsValue, ok := obj[cptype.GlobalInnerKeyUserIDs.String()]
	if !ok {
		logrus.Infof("userIDsValue is nil, skip inject user info")
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}

	var userIDs []string
	if err := cputil.ObjJSONTransfer(userIDsValue, &userIDs); err != nil {
		panic(err)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	// inject to response body
	erdaResp.UserIDs = userIDs

	// update response body
	newErdaBody, err := jsi.Marshal(erdaResp)
	if err != nil {
		panic(err)
	}
	resp.Body = ioutil.NopCloser(bytes.NewReader(newErdaBody))
	resp.Header.Set("Content-Length", fmt.Sprint(len(newErdaBody)))

	return nil
}
