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

package k8s

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/namespace"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestCreateNamespace(t *testing.T) {
	k8s := &Kubernetes{
		namespace: &namespace.Namespace{},
	}
	service := &apistructs.Service{
		Name:          "test-service",
		Namespace:     "test",
		Image:         "test",
		ImageUsername: "test",
		ImagePassword: "test",
		Cmd:           "",
		Ports:         nil,
		ProxyPorts:    nil,
		Vip:           "",
		ShortVIP:      "",
		ProxyIp:       "",
		PublicIp:      "",
		Scale:         0,
		Resources: apistructs.Resources{
			Cpu: 0.1,
			Mem: 512,
		},
		Depends:            nil,
		Env:                nil,
		Labels:             nil,
		DeploymentLabels:   nil,
		Selectors:          nil,
		Binds:              nil,
		Volumes:            nil,
		Hosts:              nil,
		HealthCheck:        nil,
		NewHealthCheck:     nil,
		SideCars:           nil,
		InitContainer:      nil,
		InstanceInfos:      nil,
		MeshEnable:         nil,
		TrafficSecurity:    diceyml.TrafficSecurity{},
		WorkLoad:           "",
		ProjectServiceName: "",
		K8SSnippet:         nil,
		StatusDesc:         apistructs.StatusDesc{},
	}
	servicegroup := &apistructs.ServiceGroup{
		Dice: apistructs.Dice{
			ID:                   "test",
			Type:                 "service",
			Labels:               map[string]string{},
			Services:             []apistructs.Service{*service},
			ServiceDiscoveryKind: "",
			ServiceDiscoveryMode: "",
			ProjectNamespace:     "test-111-test",
		},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(k8s), "NotfoundNamespace", func(k8s *Kubernetes, ns string) (bool, error) {
		return false, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k8s), "UpdateImageSecret", func(k8s *Kubernetes, namespace string, infos []apistructs.RegistryInfo) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k8s.namespace), "Create", func(namespace *namespace.Namespace, ns string, labels map[string]string) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k8s), "NewRuntimeImageSecret", func(k8s *Kubernetes, namespace string, sg *apistructs.ServiceGroup) error {
		return nil
	})
	err := k8s.CreateNamespace("test", servicegroup)
	assert.Equal(t, err, nil)
}
