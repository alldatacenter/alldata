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

package dcos

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
)

const (
	defaultMasterIP = "master.mesos"
)

func GetVersion(masterIP string) (string, error) {
	if masterIP == "" {
		masterIP = defaultMasterIP
	}
	res, err := http.Get("http://" + masterIP + "/dcos-metadata/dcos-version.json")
	if err != nil {
		return "", err
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK {
		return "", fmt.Errorf("malformed status code: %d", res.StatusCode)
	}
	b, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return "", err
	}
	var v struct {
		Version string `json:"version"`
	}
	err = json.Unmarshal(b, &v)
	if err != nil {
		return "", err
	}
	return v.Version, nil
}

func GetApps(masterIP string) ([]map[string]interface{}, error) {
	if masterIP == "" {
		masterIP = defaultMasterIP
	}
	res, err := http.Get("http://" + masterIP + "/service/marathon/v2/apps")
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("malformed status code: %d", res.StatusCode)
	}
	b, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return nil, err
	}
	var v struct {
		Apps []map[string]interface{} `json:"apps"`
	}
	err = json.Unmarshal(b, &v)
	if err != nil {
		return nil, err
	}
	return v.Apps, nil
}

func GetApp(masterIP, id string) (map[string]interface{}, error) {
	if masterIP == "" {
		masterIP = defaultMasterIP
	}
	if len(id) < 2 || id[0] != '/' {
		return nil, errors.New("illegal id")
	}
	res, err := http.Get("http://" + masterIP + "/service/marathon/v2/apps" + url.PathEscape(id))
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("malformed status code: %d", res.StatusCode)
	}
	b, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return nil, err
	}
	var v struct {
		App map[string]interface{} `json:"app"`
	}
	err = json.Unmarshal(b, &v)
	if err != nil {
		return nil, err
	}
	return v.App, nil
}

func PutApp(masterIP string, m map[string]interface{}) (string, error) {
	if masterIP == "" {
		masterIP = defaultMasterIP
	}
	id, ok := m["id"].(string)
	if !ok || len(id) < 2 || id[0] != '/' {
		return "", errors.New("illegal id")
	}
	b, err := json.Marshal(m)
	if err != nil {
		return "", err
	}
	u := "http://" + masterIP + "/service/marathon/v2/apps" + url.PathEscape(id) + "?force=true&partialUpdate=false"
	req, err := http.NewRequest("PUT", u, bytes.NewReader(b))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json; charset=utf-8")
	//req.Header.Set("Authorization", "Basic ")
	//req.Header.Set("Authorization", "token=")
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", err
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK && res.StatusCode != http.StatusCreated {
		return "", fmt.Errorf("malformed status code: %d", res.StatusCode)
	}
	b, err = ioutil.ReadAll(res.Body)
	if err != nil {
		return "", err
	}
	var v struct {
		DeploymentId string `json:"deploymentId"`
	}
	err = json.Unmarshal(b, &v)
	if err != nil {
		return "", err
	}
	return v.DeploymentId, nil
}

func RestartApp(masterIP, id string) (string, error) {
	if masterIP == "" {
		masterIP = defaultMasterIP
	}
	if len(id) < 2 || id[0] != '/' {
		return "", errors.New("illegal id")
	}
	u := "http://" + masterIP + "/service/marathon/v2/apps" + url.PathEscape(id) + "/restart?force=true"
	// #nosec G107
	res, err := http.Post(u, "application/json; charset=utf-8", nil)
	if err != nil {
		return "", err
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK {
		return "", fmt.Errorf("malformed status code: %d", res.StatusCode)
	}
	b, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return "", err
	}
	var v struct {
		DeploymentId string `json:"deploymentId"`
	}
	err = json.Unmarshal(b, &v)
	if err != nil {
		return "", err
	}
	return v.DeploymentId, nil
}

func DeleteApp(masterIP, id string) (string, error) {
	if masterIP == "" {
		masterIP = defaultMasterIP
	}
	if len(id) < 2 || id[0] != '/' {
		return "", errors.New("illegal id")
	}
	u := "http://" + masterIP + "/service/marathon/v2/apps" + url.PathEscape(id) + "?force=true"
	req, err := http.NewRequest("DELETE", u, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json; charset=utf-8")
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", err
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK {
		return "", fmt.Errorf("malformed status code: %d", res.StatusCode)
	}
	b, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return "", err
	}
	var v struct {
		DeploymentId string `json:"deploymentId"`
	}
	err = json.Unmarshal(b, &v)
	if err != nil {
		return "", err
	}
	return v.DeploymentId, nil
}
