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

package zkproxy

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/erda-project/erda-proto-go/msp/registercenter/pb"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// Adapter .
type Adapter struct {
	ClusterName string
	Addr        string
	client      *httpclient.HTTPClient
}

// NewAdapter .
func NewAdapter(clusterName, addr string) *Adapter {
	return &Adapter{
		ClusterName: clusterName,
		Addr:        addr,
		client:      httpclient.New(httpclient.WithClusterDialer(clusterName)),
	}
}

func (a *Adapter) GetAllInterfaceList(projectID, env, namespace string) ([]*pb.Interface, error) {
	resp, err := a.client.Get(a.Addr).Path(httputil.JoinPathR("listinterface", projectID, env)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	list := make([]*pb.Interface, 0)
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		err = json.NewDecoder(resp.Body).Decode(&list)
		if err != nil {
			return nil, err
		}
	}
	return list, nil
}

func (a *Adapter) GetRouteRule(interfaceName, projectID, env, namespace string) (*pb.RequestRule, error) {
	resp, err := a.client.Get(a.Addr).Path(httputil.JoinPathR("listinterface", "route", interfaceName, projectID, env)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getRequestRuleFromResponse(resp)
}

func (a *Adapter) CreateRouteRule(interfaceName, projectID, env, namespace string, rule *pb.RequestRule) (*pb.RequestRule, error) {
	resp, err := a.client.Post(a.Addr).Path(httputil.JoinPathR("listinterface", "route", interfaceName, projectID, env)).Header("namespace", namespace).JSONBody(rule).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getRequestRuleFromResponse(resp)
}

func (a *Adapter) DeleteRouteRule(interfaceName, projectID, env, namespace string) (*pb.RequestRule, error) {
	resp, err := a.client.Delete(a.Addr).Path(httputil.JoinPathR("listinterface", "route", interfaceName, projectID, env)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getRequestRuleFromResponse(resp)
}

func getRequestRuleFromResponse(resp *http.Response) (*pb.RequestRule, error) {
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		var body = struct {
			Success bool            `json:"success"`
			Data    *pb.RequestRule `json:"data"`
			Err     interface{}     `json:"err"`
		}{}
		err := json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return nil, err
		}
		if !body.Success {
			return nil, fmt.Errorf("failed request: %v", body.Err)
		}
		return body.Data, nil
	}
	// return nil, fmt.Errorf("unexpect status=%d", resp.StatusCode)
	return nil, nil
}

func (a *Adapter) GetHostRule(projectID, env, appID, namespace string) (*pb.HostRules, error) {
	resp, err := a.client.Get(a.Addr).Path(httputil.JoinPathR("rule", "branch", projectID, env, appID)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getHostRulesFromResponse(resp)
}

func (a *Adapter) CreateHostRoute(projectID, env, appID, namespace string, rules *pb.HostRules) (*pb.HostRules, error) {
	resp, err := a.client.Post(a.Addr).Path(httputil.JoinPathR("rule", "branch", projectID, env, appID)).Header("namespace", namespace).JSONBody(rules).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getHostRulesFromResponse(resp)
}

func (a *Adapter) DeleteHostRoute(projectID, env, appID, namespace string) (*pb.HostRules, error) {
	resp, err := a.client.Delete(a.Addr).Path(httputil.JoinPathR("rule", "branch", projectID, env, appID)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getHostRulesFromResponse(resp)
}

func getHostRulesFromResponse(resp *http.Response) (*pb.HostRules, error) {
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		var body = struct {
			Success bool          `json:"success"`
			Data    *pb.HostRules `json:"data"`
			Err     interface{}   `json:"err"`
		}{}
		err := json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return nil, err
		}
		if !body.Success {
			return nil, fmt.Errorf("failed request: %v", body.Err)
		}
		return body.Data, nil
	}
	// return nil, fmt.Errorf("unexpect status=%d", resp.StatusCode)
	return nil, nil
}

func (a *Adapter) GetHostRuntimeRule(host, projectID, env, namespace string) (*pb.HostRuntimeRules, error) {
	resp, err := a.client.Get(a.Addr).Path(httputil.JoinPathR("rule", "host", projectID, env, host)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getHostRuntimeRulesFromResponse(resp)
}

func (a *Adapter) CreateHostRuntimeRule(projectID, env, host, namespace string, rules *pb.HostRuntimeRules) (*pb.HostRuntimeRules, error) {
	resp, err := a.client.Post(a.Addr).Path(httputil.JoinPathR("rule", "host", projectID, env, host)).Header("namespace", namespace).JSONBody(rules).Do().RAW()
	if err != nil {
		return nil, err
	}
	return getHostRuntimeRulesFromResponse(resp)
}

func getHostRuntimeRulesFromResponse(resp *http.Response) (*pb.HostRuntimeRules, error) {
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		var body = struct {
			Success bool                 `json:"success"`
			Data    *pb.HostRuntimeRules `json:"data"`
			Err     interface{}          `json:"err"`
		}{}
		err := json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return nil, err
		}
		if !body.Success {
			return nil, fmt.Errorf("failed request: %v", body.Err)
		}
		return body.Data, nil
	}
	// return nil, fmt.Errorf("unexpect status=%d", resp.StatusCode)
	return nil, nil
}

func (a *Adapter) GetAllHostRuntimeRules(projectID, env, appID, namespace string) (*pb.HostRuntimeInterfaces, error) {
	resp, err := a.client.Get(a.Addr).Path(httputil.JoinPathR("listhostinterface", "timestamp", projectID, env, appID)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		var body = struct {
			Success bool                      `json:"success"`
			Data    *pb.HostRuntimeInterfaces `json:"data"`
			Err     interface{}               `json:"err"`
		}{}
		err := json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return nil, err
		}
		if !body.Success {
			return nil, fmt.Errorf("failed request: %v", body.Err)
		}
		return body.Data, nil
	}
	// return nil, fmt.Errorf("unexpect status=%d", resp.StatusCode)
	return nil, nil
}

func (a *Adapter) GetDubboInterfaceTime(interfaceName, projectID, env, namespace string) (*pb.DubboInterfaceTime, error) {
	resp, err := a.client.Get(a.Addr).Path(httputil.JoinPathR("interface", "timestamp", interfaceName, projectID, env)).Header("namespace", namespace).Do().RAW()
	if err != nil {
		return nil, err
	}
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		var body = struct {
			Success bool                   `json:"success"`
			Data    *pb.DubboInterfaceTime `json:"data"`
			Err     interface{}            `json:"err"`
		}{}
		err := json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return nil, err
		}
		if !body.Success {
			return nil, fmt.Errorf("failed request: %v", body.Err)
		}
		return body.Data, nil
	}
	// return nil, fmt.Errorf("unexpect status=%d", resp.StatusCode)
	return nil, nil
}
