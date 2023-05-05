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

package scheduler

import (
	"bytes"
	"encoding/json"
	"os"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

// call this in form of goroutine
func GetDCOSTokenAuthPeriodically() {
	client := httpclient.New()
	waitTime := 10 * time.Millisecond

	for {
		select {
		case <-time.After(waitTime):
			token, err := getTokenAuthAndSetEnv(client)

			if err != nil {
				waitTime = 2 * time.Minute
				os.Setenv("AUTH_TOKEN", "")
				logrus.Errorf("get auth token error: %v", err)
				break
			}

			// Update every 24 hours
			waitTime = 24 * time.Hour

			if len(token) > 0 {
				os.Setenv("AUTH_TOKEN", token)
				logrus.Debugf("get auth token: %s", token)
			} else {
				// If err is nil and token is empty, it means that the user has not set token auth
				os.Unsetenv("AUTH_TOKEN")
				logrus.Debugf("clear auth token")
			}
		}
	}
}

func getTokenAuthAndSetEnv(client *httpclient.HTTPClient) (string, error) {
	dcosAddr := os.Getenv("DCOS_ADDR")
	id := os.Getenv("DCOS_UID")
	password := os.Getenv("DCOS_PASSWORD")

	// uid and password required
	if len(id) == 0 || len(password) == 0 {
		return "", nil
	}
	// dcosAddr is optional, default is internal dcos cluster addr
	if len(dcosAddr) == 0 {
		dcosAddr = "master.mesos"
	}

	logrus.Debugf("id: %v, password: %v, dcosAddr: %v", id, password, dcosAddr)
	var b bytes.Buffer
	type IdAndPassword struct {
		Uid      string `json:"uid"`
		Password string `json:"password"`
	}
	requestIdAndPwd := IdAndPassword{
		Uid:      id,
		Password: password,
	}

	type Token struct {
		AuthToken string `json:"token,omitempty"`
	}
	var token Token

	resp, err := client.Post(dcosAddr).
		Path("/acs/api/v1/auth/login").
		JSONBody(&requestIdAndPwd).
		Header("Content-Type", "application/json").
		Do().
		Body(&b)

	if err != nil {
		return "", err
	}

	if !resp.IsOK() {
		return "", errors.Errorf("get token auth status code: %v, body: %v", resp.StatusCode(), b.String())
	}

	r := bytes.NewReader(b.Bytes())
	if err := json.NewDecoder(r).Decode(&token); err != nil {
		return "", err
	}

	return token.AuthToken, nil
}
