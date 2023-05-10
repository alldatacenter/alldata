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

package wrapper

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/http/customhttp"
)

func DoRequest(client *http.Client, method, url string, body []byte, timeout int, headers ...map[string]string) ([]byte, *http.Response, error) {
	client.Timeout = time.Duration(timeout) * time.Second
	succ := false
	var resp *http.Response = nil
	var err error
	respBody := []byte("")
	defer func() {
		commonLog := fmt.Sprintf(
			"method[%s] url[%s] body[%s] header[%v]",
			method,
			url,
			body,
			headers,
		)
		if succ {
			resp.Body.Close()
			log.Infof("request succ: %s, resp: [%+v]", commonLog, resp)
		} else {
			log.Errorf("requset failed [%s]: %s, resp: [%+v] ", err, commonLog, resp)
		}
		log.Debugf("respBody: [%s]", respBody)
	}()
	if !strings.HasPrefix(url, "http://") && !strings.HasPrefix(url, "https://") &&
		!strings.HasPrefix(url, "inet://") {
		url = "http://" + url
	}
	req, err := customhttp.NewRequest(method, url, bytes.NewReader(body))
	if err != nil {
		return respBody, nil, errors.Wrap(err, "create http request failed")
	}
	for _, kv := range headers {
		for key, value := range kv {
			req.Header.Add(key, value)
			if strings.EqualFold(key, "host") {
				req.Host = value
			}
		}
	}
	resp, err = client.Do(req)
	if err != nil {
		return respBody, nil, errors.Wrap(err, "http client send request failed")
	}
	succ = true
	respBody, err = ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, nil, errors.Wrap(err, "read form response body failed")
	}
	return respBody, resp, nil
}

func Request(method, url string, body []byte, timeout int, headers ...map[string]string) ([]byte, *http.Response, error) {
	client := &http.Client{Timeout: time.Duration(timeout) * time.Second}
	return DoRequest(client, method, url, body, timeout, headers...)
}
