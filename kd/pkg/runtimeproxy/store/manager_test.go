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

package store

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
)

func generateSimplePodSandbox() *PodSandboxInfo {
	return &PodSandboxInfo{
		PodSandboxHookRequest: &v1alpha1.PodSandboxHookRequest{
			PodMeta: &v1alpha1.PodSandboxMetadata{
				Name: fmt.Sprintf("name"),
			},
		},
	}
}

func TestReadWritePodSandboxInfo(t *testing.T) {
	type args struct {
		podKeyId               string
		checkPointedPodSandbox *PodSandboxInfo
	}
	type want struct {
		poolSize        int
		expectedSandbox *PodSandboxInfo
	}
	tests := []struct {
		name string
		args args
		want want
	}{
		{
			name: "key is null, there should be one item in pool",
			args: args{
				podKeyId:               "",
				checkPointedPodSandbox: generateSimplePodSandbox(),
			},
			want: want{
				poolSize:        1,
				expectedSandbox: generateSimplePodSandbox(),
			},
		},
		{
			name: "pod info is null",
			args: args{
				podKeyId:               "key1",
				checkPointedPodSandbox: nil,
			},
			want: want{
				poolSize:        1,
				expectedSandbox: nil,
			},
		},
		{
			name: "regular pod store",
			args: args{
				podKeyId:               "key1",
				checkPointedPodSandbox: generateSimplePodSandbox(),
			},
			want: want{
				poolSize:        1,
				expectedSandbox: generateSimplePodSandbox(),
			},
		},
	}
	for _, tt := range tests {
		m.reset()
		WritePodSandboxInfo(tt.args.podKeyId, tt.args.checkPointedPodSandbox)
		realPod := GetPodSandboxInfo(tt.args.podKeyId)
		assert.Equal(t, len(m.podInfos), tt.want.poolSize)
		assert.Equal(t, realPod, tt.want.expectedSandbox)
	}
}

func generateSimpleContainer() *ContainerInfo {
	return &ContainerInfo{
		ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
			ContainerMeta: &v1alpha1.ContainerMetadata{
				Name: fmt.Sprintf("name"),
			},
		},
	}
}

func TestReadWriteContainerInfo(t *testing.T) {
	type args struct {
		containerId           string
		checkPointedContainer *ContainerInfo
	}
	type want struct {
		poolSize          int
		expectedContainer *ContainerInfo
	}
	tests := []struct {
		name string
		args args
		want want
	}{
		{
			name: "key is null, there should be one item in pool",
			args: args{
				containerId:           "",
				checkPointedContainer: generateSimpleContainer(),
			},
			want: want{
				poolSize:          1,
				expectedContainer: generateSimpleContainer(),
			},
		},
		{
			name: "container info is null",
			args: args{
				containerId:           "key1",
				checkPointedContainer: nil,
			},
			want: want{
				poolSize:          1,
				expectedContainer: nil,
			},
		},
		{
			name: "regular pod store",
			args: args{
				containerId:           "key1",
				checkPointedContainer: generateSimpleContainer(),
			},
			want: want{
				poolSize:          1,
				expectedContainer: generateSimpleContainer(),
			},
		},
	}
	for _, tt := range tests {
		m.reset()
		WriteContainerInfo(tt.args.containerId, tt.args.checkPointedContainer)
		realContainer := GetContainerInfo(tt.args.containerId)
		assert.Equal(t, len(m.containerInfos), tt.want.poolSize)
		assert.Equal(t, realContainer, tt.want.expectedContainer)
	}
}
