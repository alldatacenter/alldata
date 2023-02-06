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

package nacos

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"

	"github.com/erda-project/erda-proto-go/msp/configcenter/pb"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// SearchMode .
type SearchMode string

var (
	// SearchModeBlur .
	SearchModeBlur SearchMode = "BLUR"
	// SearchModeAccurate .
	SearchModeAccurate SearchMode = "ACCURATE"
)

// Adapter .
type Adapter struct {
	ClusterName string
	Addr        string
	User        string
	Password    string
	client      *httpclient.HTTPClient
}

// NewAdapter .
func NewAdapter(clusterName, addr, user, password string) *Adapter {
	return &Adapter{
		ClusterName: clusterName,
		Addr:        addr,
		User:        user,
		Password:    password,
		client:      httpclient.New(httpclient.WithClusterDialer(clusterName)),
	}
}

// SearchResponse .
type SearchResponse struct {
	Total       int64         `json:"totalCount"`
	Pages       int64         `json:"pagesAvailable"`
	ConfigItems []*ConfigItem `json:"pageItems"`
}

// ConfigItem .
type ConfigItem struct {
	DataID  string `json:"dataId"`
	Group   string `json:"group"`
	Content string `json:"content"`
}

// ToConfigCenterGroups .
func (s *SearchResponse) ToConfigCenterGroups() *pb.Groups {
	list := make([]string, 0, len(s.ConfigItems))
	for _, item := range s.ConfigItems {
		if item == nil {
			continue
		}
		list = append(list, item.Group)
	}
	return &pb.Groups{
		Total: s.Total,
		List:  list,
	}
}

// SearchConfig .
func (a *Adapter) SearchConfig(mode SearchMode, tenantName, groupName, dataID string, page, pageSize int) (*SearchResponse, error) {
	auth, err := a.loginNacos()
	if err != nil {
		return nil, err
	}
	params := url.Values{}
	params.Set("search", strings.ToLower(string(mode)))
	params.Set("dataId", dataID)
	params.Set("group", groupName)
	params.Set("tenant", tenantName)
	params.Set("pageNo", strconv.Itoa(page))
	params.Set("pageSize", strconv.Itoa(pageSize))
	resp, err := a.client.Get(a.Addr).Path("/nacos/v1/cs/configs").Params(params).Header("Authorization", auth).Do().RAW()
	if err != nil {
		return nil, err
	}
	var body SearchResponse
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		err = json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return nil, err
		}
	}
	return &body, nil
}

// SaveConfig .
func (a *Adapter) SaveConfig(tenantName, groupName, dataID, content string) error {
	auth, err := a.loginNacos()
	if err != nil {
		return err
	}
	params := url.Values{}
	params.Set("dataId", dataID)
	params.Set("group", groupName)
	params.Set("tenant", tenantName)
	params.Set("content", content)
	resp, err := a.client.Post(a.Addr).Path("/nacos/v1/cs/configs").Params(params).Header("Authorization", auth).Do().RAW()
	if err != nil {
		return err
	}
	if resp.StatusCode < http.StatusOK || http.StatusMultipleChoices < resp.StatusCode {
		return fmt.Errorf("nacos response status error: %d %s", resp.StatusCode, resp.Status)
	}
	return nil
}

// loginNacos .
func (a *Adapter) loginNacos() (string, error) {
	params := url.Values{}
	params.Set("username", a.User)
	params.Set("password", a.Password)
	resp, err := a.client.Post(a.Addr).Path("/nacos/v1/auth/login").Params(params).Do().RAW()
	if err != nil {
		return "", err
	}
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		var body struct {
			AccessToken string `json:"accessToken"`
			Data        string `json:"data"`
		}
		err := json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return "", err
		}
		if len(body.AccessToken) > 0 {
			return body.AccessToken, nil
		} else if len(body.Data) > 0 {
			return body.Data, nil
		}
	}
	return "", nil
}
