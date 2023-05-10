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
	apiv1 "k8s.io/api/core/v1"
	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sservice"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestCreateOrPutService(t *testing.T) {
	k8s := &Kubernetes{
		service: k8sservice.New(),
	}

	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(k8s.service), "Get", func(*k8sservice.Service, string, string) (*apiv1.Service, error) {
		return &apiv1.Service{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k8s), "UpdateK8sService", func(*Kubernetes, *v1.Service, *apistructs.Service, map[string]string) error {
		return nil
	})

	err := k8s.CreateOrPutService(&apistructs.Service{
		Name:      "fake-service",
		Namespace: apiv1.NamespaceDefault,
		Ports: []diceyml.ServicePort{
			{Port: 80, Protocol: "TCP"},
		},
	}, map[string]string{})
	assert.NoError(t, err)
}
