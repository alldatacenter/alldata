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

	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func TestResourceToString(t *testing.T) {
	cpu := 1000.0
	mem := float64(1 << 30)
	resourceToString(cpu, "cpu")
	resourceToString(mem, "memory")
}

func TestGetRequestResources(t *testing.T) {
	containers := []corev1.Container{
		{
			Resources: corev1.ResourceRequirements{
				Requests: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewQuantity(1, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(1<<30, resource.DecimalSI),
				},
			},
		},
		{
			Resources: corev1.ResourceRequirements{
				Requests: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewMilliQuantity(500, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(100<<20, resource.BinarySI),
				},
			},
		},
	}
	cpu, mem := getRequestsResources(containers)
	if cpu != 1500 {
		t.Errorf("test failed, expected cpu is 1500, got %d", cpu)
	}
	if mem != 1178599424 {
		t.Errorf("test failed, expected mem is 1178599424, got %d", mem)
	}
}

func TestIsQuotaError(t *testing.T) {
	type args struct {
		err error
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			"case1",
			args{
				NewQuotaError("test"),
			},
			true,
		},
		{
			"case2",
			args{
				errors.New("test"),
			},
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsQuotaError(tt.args.err); got != tt.want {
				t.Errorf("IsQuotaError() = %v, want %v", got, tt.want)
			}
		})
	}
}
