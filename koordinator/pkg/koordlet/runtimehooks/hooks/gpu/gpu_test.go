/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package gpu

import (
	"testing"

	"github.com/stretchr/testify/assert"

	ext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
)

func Test_InjectContainerGPUEnv(t *testing.T) {
	tests := []struct {
		name             string
		expectedAllocStr string
		expectedError    bool
		proto            protocol.HooksProtocol
	}{
		{
			"test empty proto",
			"",
			true,
			nil,
		},
		{
			"test normal gpu alloc",
			"0,1",
			false,
			&protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodAnnotations: map[string]string{
						ext.AnnotationDeviceAllocated: "{\"gpu\": [{\"minor\": 0},{\"minor\": 1}]}",
					},
				},
			},
		},
		{
			"test empty gpu alloc",
			"",
			false,
			&protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodAnnotations: map[string]string{
						ext.AnnotationDeviceAllocated: "{\"fpga\": [{\"minor\": 0},{\"minor\": 1}]}",
					},
				},
			},
		},
	}
	plugin := gpuPlugin{}
	for _, tt := range tests {
		var containerCtx *protocol.ContainerContext
		if tt.proto != nil {
			containerCtx = tt.proto.(*protocol.ContainerContext)
		}
		err := plugin.InjectContainerGPUEnv(containerCtx)
		assert.Equal(t, tt.expectedError, err != nil, tt.name)
		if tt.proto != nil {
			containerCtx := tt.proto.(*protocol.ContainerContext)
			assert.Equal(t, containerCtx.Response.AddContainerEnvs[GpuAllocEnv], tt.expectedAllocStr, tt.name)
		}
	}
}
