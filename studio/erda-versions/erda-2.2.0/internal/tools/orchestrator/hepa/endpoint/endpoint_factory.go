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

package endpoint

import (
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
)

type Route struct {
	Host string
	Path string
}

type EndpointMaterial struct {
	K8SNamespace          string
	ServiceName           string
	ServicePort           int
	Routes                []Route
	ServiceGroupNamespace string
	ServiceGroupName      string
	ProjectNamespace      string
	IngressName           string
	K8SRouteOptions       k8s.RouteOptions
}

type EndpointFactory interface {
	TouchRoute(EndpointMaterial) error
	ClearRoute(EndpointMaterial) error
	TouchComponentRoute(EndpointMaterial) error
	ClearComponentRoute(EndpointMaterial) error
}

func TouchEndpoint(factory EndpointFactory, material EndpointMaterial) error {
	if len(material.Routes) == 0 {
		return factory.ClearRoute(material)
	}
	return factory.TouchRoute(material)
}

func TouchComponentEndpoint(factory EndpointFactory, material EndpointMaterial) error {
	if len(material.Routes) == 0 {
		return factory.ClearComponentRoute(material)
	}
	return factory.TouchComponentRoute(material)
}
