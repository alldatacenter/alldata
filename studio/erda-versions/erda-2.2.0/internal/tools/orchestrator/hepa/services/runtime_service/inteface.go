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

package runtime_service

import (
	"context"
	"strings"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/endpoint"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

type RuntimeEndpointInfo struct {
	RuntimeService *orm.GatewayRuntimeService
	Endpoints      []diceyml.Endpoint
}

var Service GatewayRuntimeServiceService

type GatewayRuntimeServiceService interface {
	Clone(context.Context) GatewayRuntimeServiceService
	GetRegisterAppInfo(string, string) (dto.RegisterAppsDto, error)
	TouchRuntime(*dto.RuntimeServiceReqDto) (bool, error)
	DeleteRuntime(string) error
	GetServiceRuntimes(projectId, env, app, service string) ([]orm.GatewayRuntimeService, error)
	// 获取指定服务的API前缀
	GetServiceApiPrefix(*dto.ApiPrefixReqDto) ([]string, error)
}

func MakeEndpointMaterial(runtimeService *orm.GatewayRuntimeService) (endpoint.EndpointMaterial, error) {
	material := endpoint.EndpointMaterial{}
	material.ServiceName = runtimeService.ServiceName
	material.ServicePort = runtimeService.ServicePort
	material.ServiceGroupNamespace = runtimeService.GroupNamespace
	material.ServiceGroupName = runtimeService.GroupName
	material.ProjectNamespace = runtimeService.ProjectNamespace
	// enable tls by default
	material.K8SRouteOptions.EnableTLS = true
	switch strings.ToLower(runtimeService.BackendProtocol) {
	case "https":
		material.K8SRouteOptions.BackendProtocol = &[]k8s.BackendProtocl{k8s.HTTPS}[0]
	case "grpc":
		material.K8SRouteOptions.BackendProtocol = &[]k8s.BackendProtocl{k8s.GRPC}[0]
	case "grpcs":
		material.K8SRouteOptions.BackendProtocol = &[]k8s.BackendProtocl{k8s.GRPCS}[0]
	case "fastcgi":
		material.K8SRouteOptions.BackendProtocol = &[]k8s.BackendProtocl{k8s.FCGI}[0]
	}
	return material, nil
}
