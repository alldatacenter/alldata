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

package util

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
)

func Test_FindContainerIdAndStatusByName(t *testing.T) {
	type args struct {
		name                  string
		podStatus             *corev1.PodStatus
		containerName         string
		expectContainerStatus *corev1.ContainerStatus
		expectContainerId     string
		expectErr             bool
	}

	tests := []args{
		{
			name: "testValidContainerId",
			podStatus: &corev1.PodStatus{
				ContainerStatuses: []corev1.ContainerStatus{
					{
						Name:        "main",
						ContainerID: fmt.Sprintf("docker://%s", "main"),
					},
					{
						Name:        "sidecar",
						ContainerID: fmt.Sprintf("docker://%s", "sidecar"),
					},
				},
			},
			containerName: "main",
			expectContainerStatus: &corev1.ContainerStatus{
				Name:        "main",
				ContainerID: fmt.Sprintf("docker://%s", "main"),
			},
			expectContainerId: "main",
			expectErr:         false,
		},
		{
			name: "testInValidContainerId",
			podStatus: &corev1.PodStatus{
				ContainerStatuses: []corev1.ContainerStatus{
					{
						Name:        "main",
						ContainerID: "main",
					},
					{
						Name:        "sidecar",
						ContainerID: "sidecar",
					},
				},
			},
			containerName:         "main",
			expectContainerStatus: nil,
			expectContainerId:     "",
			expectErr:             true,
		},
		{
			name: "testNotfoundContainer",
			podStatus: &corev1.PodStatus{
				ContainerStatuses: []corev1.ContainerStatus{
					{
						Name:        "sidecar",
						ContainerID: fmt.Sprintf("docker://%s", "sidecar"),
					},
				},
			},
			containerName:         "main",
			expectContainerStatus: nil,
			expectContainerId:     "",
			expectErr:             true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotContainerId, gotContainerStatus, gotErr := FindContainerIdAndStatusByName(tt.podStatus, tt.containerName)
			assert.Equal(t, tt.expectContainerId, gotContainerId, "checkContainerId")
			assert.Equal(t, tt.expectContainerStatus, gotContainerStatus, "checkContainerStatus")
			assert.Equal(t, tt.expectErr, gotErr != nil, "checkError")
		})
	}
}
