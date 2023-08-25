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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func TestIsResourceDiff(t *testing.T) {
	type args struct {
		old           corev1.ResourceList
		new           corev1.ResourceList
		resourceName  corev1.ResourceName
		diffThreshold float64
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "the new resource has big enough difference with the old one",
			args: args{
				old: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewQuantity(1, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(0, resource.BinarySI),
				},
				new: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewQuantity(9, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(0, resource.BinarySI),
				},
				resourceName:  corev1.ResourceCPU,
				diffThreshold: 2,
			},
			want: true,
		},
		{
			name: "the new resource doesn't have big enough difference with the old one",
			args: args{
				old: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewQuantity(1, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(0, resource.BinarySI),
				},
				new: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewQuantity(2, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(0, resource.BinarySI),
				},
				resourceName:  corev1.ResourceCPU,
				diffThreshold: 2,
			},
			want: false,
		},
		{
			name: "the old resource doesn't have queryed resource type",
			args: args{
				old: corev1.ResourceList{
					// corev1.ResourceCPU:    *resource.NewQuantity(1, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(0, resource.BinarySI),
				},
				new: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewQuantity(2, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(0, resource.BinarySI),
				},
				resourceName:  corev1.ResourceCPU,
				diffThreshold: 2,
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsResourceDiff(tt.args.old, tt.args.new, tt.args.resourceName, tt.args.diffThreshold); got != tt.want {
				t.Errorf("IsResourceDiff() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestQuantityPtr(t *testing.T) {
	testQuantity := resource.MustParse("1000")
	testQuantityPtr := &testQuantity
	testQuantity1 := resource.MustParse("20Gi")
	testQuantityPtr1 := &testQuantity1
	testQuantityPtr2 := resource.NewQuantity(1000, resource.DecimalSI)
	testQuantity2 := *testQuantityPtr2
	tests := []struct {
		name string
		arg  resource.Quantity
		want *resource.Quantity
	}{
		{
			name: "quantity 0",
			arg:  testQuantity,
			want: testQuantityPtr,
		},
		{
			name: "quantity 1",
			arg:  testQuantity1,
			want: testQuantityPtr1,
		},
		{
			name: "quantity 2",
			arg:  testQuantity2,
			want: testQuantityPtr2,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := QuantityPtr(tt.arg)
			assert.Equal(t, tt.want, got)
		})
	}
}
