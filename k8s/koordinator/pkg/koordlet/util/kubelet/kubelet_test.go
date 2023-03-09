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

package kubelet

import (
	"testing"

	"github.com/stretchr/testify/assert"
	kubeletconfiginternal "k8s.io/kubernetes/pkg/kubelet/apis/config"
)

func TestGetStaticCPUManagerPolicyReservedCPUs(t *testing.T) {
	tests := []struct {
		name             string
		config           *kubeletconfiginternal.KubeletConfiguration
		wantReservedCPUs string
	}{
		{
			name: "none policy",
			config: &kubeletconfiginternal.KubeletConfiguration{
				CPUManagerPolicy: "none",
			},
			wantReservedCPUs: "",
		},
		{
			name: "static policy",
			config: &kubeletconfiginternal.KubeletConfiguration{
				CPUManagerPolicy: "static",
				EvictionHard: map[string]string{
					"imagefs.available": "15%", "memory.available": "300Mi",
				},
				SystemReserved: map[string]string{
					"cpu": "200m", "memory": "2732Mi",
				},
				KubeReserved: map[string]string{
					"cpu": "1800m", "memory": "2732Mi",
				},
			},
			wantReservedCPUs: "0,6",
		},
		{
			name: "static policy with specified reserved cpus",
			config: &kubeletconfiginternal.KubeletConfiguration{
				CPUManagerPolicy:   "static",
				ReservedSystemCPUs: "1-4",
			},
			wantReservedCPUs: "1-4",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			reservedCPUS, err := GetStaticCPUManagerPolicyReservedCPUs(topoDualSocketHT, tt.config)
			assert.NoError(t, err)
			assert.Equal(t, tt.wantReservedCPUs, reservedCPUS.String())
		})
	}
}
