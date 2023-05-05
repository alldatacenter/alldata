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
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func TestKubernetes_InspectStateful(t *testing.T) {

	kubernetes := &Kubernetes{}

	serviceName := "fake-service"

	sg := &apistructs.ServiceGroup{
		Dice: apistructs.Dice{
			Type: "service",
			ID:   "fakeTest",
			Services: []apistructs.Service{
				{
					Name: "fake-service",
					Ports: []diceyml.ServicePort{
						{
							Port:       1234,
							Protocol:   "HTTPS",
							L4Protocol: "TCP",
						},
						{
							Port:       5678,
							Protocol:   "UDP",
							L4Protocol: "UDP",
						},
					},
				},
			},
		},
	}
	hostName := strutil.Join([]string{serviceName, "service--fakeTest", "svc.cluster.local"}, ".")
	sg, err := kubernetes.inspectStateless(sg)
	assert.Nil(t, err)
	assert.Equal(t, sg.Services[0].ProxyIp, hostName)
	assert.Equal(t, sg.Services[0].Vip, hostName)
	assert.Equal(t, sg.Services[0].ShortVIP, hostName)
	assert.Equal(t, sg.Services[0].ProxyPorts, []int{1234, 5678})
}
