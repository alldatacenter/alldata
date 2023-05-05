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

package factories

import (
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/endpoint"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
)

type EdasFactory struct {
	k8sAdapter k8s.K8SAdapter
}

func NewEdasFactory(clusterKey string) (endpoint.EndpointFactory, error) {
	adapter, err := k8s.NewAdapter(clusterKey)
	if err != nil {
		return nil, err
	}
	return EdasFactory{
		k8sAdapter: adapter,
	}, nil
}

func (impl EdasFactory) serviceName(serviceGroupNamespace, serviceGroupName, serviceName string) string {
	return serviceGroupNamespace + "-" + serviceGroupName + "-" + serviceName
}

func (impl EdasFactory) TouchRoute(material endpoint.EndpointMaterial) error {
	var k8sRoutes []k8s.IngressRoute
	for _, route := range material.Routes {
		k8sRoutes = append(k8sRoutes, k8s.IngressRoute{
			Domain: route.Host,
			Path:   route.Path,
		})
	}
	serviceName := impl.serviceName(material.ServiceGroupNamespace, material.ServiceGroupName, material.ServiceName)
	backend := k8s.IngressBackend{
		ServiceName: serviceName,
		ServicePort: material.ServicePort,
	}
	_, err := impl.k8sAdapter.CreateOrUpdateIngress("default", serviceName,
		k8sRoutes, backend, material.K8SRouteOptions)
	if err != nil {
		return err
	}
	return nil
}

func (impl EdasFactory) TouchComponentRoute(material endpoint.EndpointMaterial) error {
	var k8sRoutes []k8s.IngressRoute
	for _, route := range material.Routes {
		k8sRoutes = append(k8sRoutes, k8s.IngressRoute{
			Domain: route.Host,
			Path:   route.Path,
		})
	}
	backend := k8s.IngressBackend{
		ServiceName: material.ServiceName,
		ServicePort: material.ServicePort,
	}
	namespace := "default"
	if material.K8SNamespace != "" {
		namespace = material.K8SNamespace
	}
	_, err := impl.k8sAdapter.CreateOrUpdateIngress(namespace, material.IngressName, k8sRoutes, backend, material.K8SRouteOptions)
	if err != nil {
		return err
	}
	return nil
}

func (impl EdasFactory) ClearComponentRoute(material endpoint.EndpointMaterial) error {
	namespace := "default"
	if material.K8SNamespace != "" {
		namespace = material.K8SNamespace
	}
	return impl.k8sAdapter.DeleteIngress(namespace, material.IngressName)
}
func (impl EdasFactory) ClearRoute(material endpoint.EndpointMaterial) error {
	return impl.k8sAdapter.DeleteIngress("default", impl.serviceName(material.ServiceGroupNamespace, material.ServiceGroupName, material.ServiceName))
}
