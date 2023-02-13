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
	"fmt"
	"net/url"
	"strings"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

type authType int

const (
	AccessToken = authType(0)
	Bearer      = authType(1)
)

func NewNacosClient(clusterName, addr, user, password string) *NacosClient {
	return &NacosClient{
		addr:        addr,
		user:        user,
		password:    password,
		clusterName: clusterName,
		tokenType:   AccessToken,
	}
}

type NacosClient struct {
	clusterName string
	addr        string
	user        string
	password    string
	accessToken string
	tokenType   authType
}

func (c *NacosClient) Login() (string, error) {
	loginUrl := "/nacos/v1/auth/login?username=" + c.user + "&password=" + c.password
	var result map[string]interface{}
	resp, err := httpclient.New(httpclient.WithClusterDialer(c.clusterName)).
		Post(c.addr).Path(loginUrl).Do().JSON(&result)
	if err != nil {
		return "", err
	}
	if !resp.IsOK() {
		return "", fmt.Errorf("nacos login response code error[%d], body:%s", resp.StatusCode(), string(resp.Body()))
	}
	accessToken, ok := result["accessToken"]
	if !ok {
		accessToken = result["data"]
	}

	c.accessToken = fmt.Sprint(accessToken)

	tokenParts := strings.Split(c.accessToken, " ")
	if len(tokenParts) == 2 && tokenParts[0] == "Bearer" {
		c.tokenType = Bearer
		c.accessToken = tokenParts[1]
	}

	fmt.Printf("nacos login token: %s, type: %d \n", c.accessToken, c.tokenType)

	return c.accessToken, nil
}

func (c *NacosClient) GetNamespaceId(namespaceName string) (string, error) {

	if len(c.accessToken) == 0 {
		c.Login()
	}

	path := "/nacos/v1/console/namespaces"
	var result struct {
		Data []struct {
			NamespaceShowName string
			Namespace         string
		}
	}
	hc := httpclient.New(httpclient.WithClusterDialer(c.clusterName))
	if c.tokenType == Bearer {
		hc = hc.BearerTokenAuth(c.accessToken)
	} else {
		hc = hc.TokenAuth(c.accessToken)
	}
	resp, err := hc.Get(c.addr).Path(path).Do().JSON(&result)
	if err != nil {
		return "", err
	}
	if !resp.IsOK() {
		return "", fmt.Errorf("nacos get namespaceid response code error[%d], body:%s", resp.StatusCode(), string(resp.Body()))
	}

	for _, namespace := range result.Data {
		if namespace.NamespaceShowName == namespaceName {
			return namespace.Namespace, nil
		}
	}

	return "", nil
}

func (c *NacosClient) CreateNamespace(namespaceName string) (string, error) {

	if len(c.accessToken) == 0 {
		c.Login()
	}

	path := "/nacos/v1/console/namespaces" + "?namespaceName=" + namespaceName + "&namespaceDesc=" + namespaceName + "&customNamespaceId=" + namespaceName
	hc := httpclient.New(httpclient.WithClusterDialer(c.clusterName))
	if c.tokenType == Bearer {
		hc = hc.BearerTokenAuth(c.accessToken)
	} else {
		hc = hc.TokenAuth(c.accessToken)
	}
	resp, err := hc.Post(c.addr).Path(path).Do().DiscardBody()
	if err != nil {
		return "", err
	}
	if !resp.IsOK() {
		return "", fmt.Errorf("nacos create namespace response code error[%d], body:%s", resp.StatusCode(), string(resp.Body()))
	}

	return c.GetNamespaceId(namespaceName)
}

func (c *NacosClient) SaveConfig(tenantName string, groupName string, dataId string, content string) error {
	if len(c.accessToken) == 0 {
		c.Login()
	}

	path := "/nacos/v1/cs/configs?dataId=" + dataId + "&group=" + groupName + "&content=" + url.QueryEscape(content) + "&tenant=" + tenantName
	hc := httpclient.New(httpclient.WithClusterDialer(c.clusterName))
	if c.tokenType == Bearer {
		hc = hc.BearerTokenAuth(c.accessToken)
	} else {
		hc = hc.TokenAuth(c.accessToken)
	}
	resp, err := hc.Post(c.addr).Path(path).Do().DiscardBody()
	if err != nil {
		return err
	}
	if !resp.IsOK() {
		return fmt.Errorf("nacos save config response code error[%d], body:%s", resp.StatusCode(), string(resp.Body()))
	}
	return nil
}

func (c *NacosClient) DeleteConfig(tenantName string, groupName string) error {
	if len(c.accessToken) == 0 {
		c.Login()
	}

	path := "/nacos/v1/cs/configs?dataId=application.yml&group=" + groupName + "&tenant=" + tenantName
	hc := httpclient.New(httpclient.WithClusterDialer(c.clusterName))
	if c.tokenType == Bearer {
		hc = hc.BearerTokenAuth(c.accessToken)
	} else {
		hc = hc.TokenAuth(c.accessToken)
	}
	resp, err := hc.Delete(c.addr).Path(path).Do().DiscardBody()
	if err != nil {
		return err
	}
	if !resp.IsOK() {
		return fmt.Errorf("nacos delete config response code error[%d], body:%s", resp.StatusCode(), string(resp.Body()))
	}

	return nil
}
