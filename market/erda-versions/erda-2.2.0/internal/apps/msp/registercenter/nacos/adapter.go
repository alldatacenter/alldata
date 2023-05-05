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
	"io/ioutil"
	"net"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"

	"github.com/erda-project/erda-proto-go/msp/registercenter/pb"
	"github.com/erda-project/erda/pkg/http/httpclient"
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

// GetDubboInterfaceList .
func (a *Adapter) GetDubboInterfaceList(namespace string) ([]*pb.Interface, error) {
	count, err := a.countInterface(namespace, "")
	if err != nil {
		return nil, err
	}
	result, err := a.getInterfaceDetail(namespace, "", 1, count)
	if err != nil {
		return nil, err
	}

	var keys []string
	values := make(map[string]*pb.Interface)
	for _, item := range result {
		if len(item.getSide()) <= 0 {
			continue
		}
		iname := item.getInterfaceName()
		if exist, ok := values[iname]; ok {
			values[iname] = mergeInterface(exist, item.ToInterface())
		} else {
			keys = append(keys, iname)
			values[iname] = item.ToInterface()
		}
	}
	sort.Strings(keys)
	list := make([]*pb.Interface, 0, len(result))
	for _, key := range keys {
		list = append(list, values[key])
	}
	return list, nil
}

func mergeInterface(a *pb.Interface, b *pb.Interface) *pb.Interface {
	if a == nil {
		return b
	}
	if b != nil && a.Interfacename == b.Interfacename {
		providers := make(map[string]struct{})
		for _, p := range a.Providerlist {
			providers[p] = struct{}{}
		}
		for _, p := range b.Providerlist {
			if _, ok := providers[p]; !ok {
				a.Providerlist = append(a.Providerlist, p)
			}
		}
		if len(b.Providermap) > 0 && a.Providermap == nil {
			a.Providermap = make(map[string]*pb.InterfaceOwner)
		}
		for k, v := range b.Providermap {
			if _, ok := a.Providermap[k]; !ok {
				a.Providermap[k] = v
			}
		}
		consumers := make(map[string]struct{})
		for _, p := range a.Consumerlist {
			consumers[p] = struct{}{}
		}
		for _, p := range b.Consumerlist {
			if _, ok := consumers[p]; !ok {
				a.Consumerlist = append(a.Consumerlist, p)
			}
		}
		if len(b.Consumermap) > 0 && a.Consumermap == nil {
			a.Consumermap = make(map[string]*pb.InterfaceOwner)
		}
		for k, v := range b.Consumermap {
			if _, ok := a.Consumermap[k]; !ok {
				a.Consumermap[k] = v
			}
		}
	}
	return a
}

func (a *Adapter) GetHTTPInterfaceList(namespace string) ([]*pb.HTTPService, error) {
	count, err := a.countInterface(namespace, "")
	if err != nil {
		return nil, err
	}
	result, err := a.getInterfaceDetail(namespace, "", 1, count)
	if err != nil {
		return nil, err
	}
	list := make([]*pb.HTTPService, 0, len(result))
	for _, item := range result {
		if len(item.getSide()) > 0 {
			continue
		}
		list = append(list, item.ToHTTPService())
	}
	return list, nil
}

func (a *Adapter) EnableHTTPService(namespace string, service *pb.EnableHTTPService) (*pb.EnableHTTPService, error) {
	count, err := a.countInterface(namespace, service.ServiceName)
	if err != nil {
		return nil, err
	}
	results, err := a.getInterfaceDetail(namespace, service.ServiceName, 1, count)
	if err != nil {
		return nil, err
	}
	serviceIP, servicePort, err := net.SplitHostPort(service.Address)
	if err != nil {
		serviceIP = service.Address
	}
	var result *ServiceSearchResult
	for _, r := range results {
		if !strings.EqualFold(r.ServiceName, service.ServiceName) {
			continue
		}
		for _, ip := range r.getIPs() {
			if ip == serviceIP {
				result = r
				break
			}
		}
	}
	if result == nil {
		return nil, nil
	}
	h := result.getHostByIP(serviceIP)
	if h == nil {
		return nil, nil
	}
	params := url.Values{}
	params.Set("serviceName", service.ServiceName)
	params.Set("clusterName", "DEFAULT")
	params.Set("ip", serviceIP)
	params.Set("port", servicePort)
	params.Set("ephemeral", "true")
	params.Set("weight", strconv.FormatFloat(h.Weight, 'f', -1, 64))
	params.Set("enabled", strconv.FormatBool(service.Online))
	params.Set("namespaceId", namespace)
	byts, _ := json.Marshal(h.MetaData)
	params.Set("metadata", string(byts))
	resp, err := a.client.Put(a.Addr).Path("/nacos/v1/ns/instance").Params(params).Do().RAW()
	if err != nil {
		return nil, err
	}
	byts, err = ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		if strings.EqualFold(string(byts), "ok") {
			return service, nil
		}
	}
	return nil, fmt.Errorf("unexpect status=%d, body=%q", resp.StatusCode, string(byts))
}

func (a *Adapter) countInterface(namespace, keyword string) (int64, error) {
	params := url.Values{}
	params.Set("hasIpCount", "false")
	params.Set("withInstances", "false")
	params.Set("pageNo", "1")
	params.Set("pageSize", "10")
	params.Set("serviceNameParam", keyword)
	params.Set("namespaceId", namespace)
	resp, err := a.client.Get(a.Addr).Path("/nacos/v1/ns/catalog/services").Params(params).Do().RAW()
	if err != nil {
		return 0, err
	}
	var body struct {
		Count int64 `json:"count"`
	}
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		err = json.NewDecoder(resp.Body).Decode(&body)
		if err != nil {
			return 0, err
		}
	}
	return body.Count, nil
}

func (a *Adapter) getInterfaceDetail(namespace, keyword string, pageNo, pageSize int64) ([]*ServiceSearchResult, error) {
	params := url.Values{}
	params.Set("hasIpCount", "false")
	params.Set("withInstances", "true")
	params.Set("pageNo", strconv.FormatInt(pageNo, 10))
	params.Set("pageSize", strconv.FormatInt(pageSize, 10))
	params.Set("serviceNameParam", keyword)
	params.Set("namespaceId", namespace)
	resp, err := a.client.Get(a.Addr).Path("/nacos/v1/ns/catalog/services").Params(params).Do().RAW()
	if err != nil {
		return nil, err
	}
	var list []*ServiceSearchResult
	if http.StatusOK <= resp.StatusCode && resp.StatusCode < http.StatusMultipleChoices {
		err = json.NewDecoder(resp.Body).Decode(&list)
		if err != nil {
			return nil, err
		}
	}
	filtered := make([]*ServiceSearchResult, 0, len(list))
	for _, item := range list {
		if item == nil || len(item.ClusterMap) <= 0 || item.ClusterMap["DEFAULT"] == nil || len(item.ClusterMap["DEFAULT"].Hosts) <= 0 {
			continue
		}
		filtered = append(filtered, item)
	}
	return filtered, nil
}
