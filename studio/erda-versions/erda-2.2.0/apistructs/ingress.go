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

package apistructs

import (
	"os"

	"github.com/pkg/errors"
)

type ComponentIngressUpdateResponse struct {
	Header
}

type ComponentIngressUpdateRequest struct {
	K8SNamespace  string `json:"k8sNamespace"`
	ComponentName string `json:"componentName"`
	ComponentPort int    `json:"componentPort"`
	// 若为空，则使用当前集群名称
	ClusterName string `json:"clusterName"`
	// 若为空，则使用ComponentName
	IngressName string `json:"ingressName"`
	// 若为空，则清除ingress
	Routes       []IngressRoute `json:"routes"`
	RouteOptions RouteOptions   `json:"routeOptions"`
}

type IngressRoute struct {
	Domain string `json:"domain"`
	Path   string `json:"path"`
}

type RouteOptions struct {
	// 重写转发域名
	RewriteHost *string `json:"rewriteHost"`
	// 重写转发路径
	RewritePath *string `json:"rewritePath"`
	// Path中是否使用了正则
	UseRegex bool `json:"useRegex"`
	// 是否开启TLS，不填时，默认为true
	EnableTLS *bool `json:"enableTls"`
	// 参考: https://kubernetes.github.io/ingress-nginx/user-guide/nginx-configuration/annotations/
	Annotations map[string]string `json:"annotations"`
	// 参考: https://kubernetes.github.io/ingress-nginx/user-guide/nginx-configuration/annotations/#configuration-snippet
	LocationSnippet *string `json:"locationSnippet"`
}

func (req *ComponentIngressUpdateRequest) CheckValid() error {
	if req.ComponentName == "" {
		return errors.New("component name is empty")
	}
	if req.ComponentPort == 0 {
		return errors.New("component port is empty")
	}
	for _, route := range req.Routes {
		if route.Domain == "" || route.Path == "" {
			return errors.New("invalid route")
		}
	}
	if req.RouteOptions.EnableTLS == nil {
		enabled := true
		req.RouteOptions.EnableTLS = &enabled
	}
	if req.IngressName == "" {
		req.IngressName = req.ComponentName
	}
	if req.ClusterName == "" {
		req.ClusterName = os.Getenv("DICE_CLUSTER_NAME")
	}
	return nil
}
