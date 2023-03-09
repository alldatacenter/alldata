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

package batchresource

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
)

func Test_plugin_Register(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		p := &plugin{}
		p.Register()
	})
}

func TestObject(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		p := Object()
		assert.Equal(t, &plugin{}, p)
	})
}

func Test_plugin_SetPodResources(t *testing.T) {
	var testNilProto *protocol.PodContext
	testEmptySpec := &apiext.ExtendedResourceSpec{}
	testEmptySpecBytes, err := json.Marshal(testEmptySpec)
	assert.NoError(t, err)
	testSpec := &apiext.ExtendedResourceSpec{
		Containers: map[string]apiext.ExtendedResourceContainerSpec{
			"container-0": {
				Requests: corev1.ResourceList{
					apiext.BatchCPU:    resource.MustParse("500"),
					apiext.BatchMemory: resource.MustParse("2Gi"),
				},
				Limits: corev1.ResourceList{
					apiext.BatchCPU:    resource.MustParse("500"),
					apiext.BatchMemory: resource.MustParse("2Gi"),
				},
			},
		},
	}
	testSpecBytes, err := json.Marshal(testSpec)
	assert.NoError(t, err)
	testSpec1 := &apiext.ExtendedResourceSpec{
		Containers: map[string]apiext.ExtendedResourceContainerSpec{
			"container-0": {
				Requests: corev1.ResourceList{
					apiext.BatchCPU:    resource.MustParse("500"),
					apiext.BatchMemory: resource.MustParse("2Gi"),
				},
				Limits: corev1.ResourceList{
					apiext.BatchCPU:    resource.MustParse("500"),
					apiext.BatchMemory: resource.MustParse("2Gi"),
				},
			},
			"container-1": {
				Limits: corev1.ResourceList{
					apiext.BatchCPU:    resource.MustParse("1000"),
					apiext.BatchMemory: resource.MustParse("2Gi"),
				},
			},
		},
	}
	testSpecBytes1, err := json.Marshal(testSpec1)
	assert.NoError(t, err)
	type fields struct {
		rule *batchResourceRule
	}
	type args struct {
		proto protocol.HooksProtocol
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    protocol.HooksProtocol
		wantErr bool
	}{
		{
			name: "nil proto",
			args: args{
				proto: testNilProto,
			},
			want:    testNilProto,
			wantErr: true,
		},
		{
			name: "not a Batch pod",
			args: args{
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{},
				},
			},
			want: &protocol.PodContext{
				Request: protocol.PodRequest{},
			},
		},
		{
			name: "a Batch pod without requests",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						Labels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						Annotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testEmptySpecBytes),
						},
					},
				},
			},
			want: &protocol.PodContext{
				Request: protocol.PodRequest{
					Labels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					Annotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testEmptySpecBytes),
					},
				},
			},
		},
		{
			name: "a Batch pod with cfs quota unset",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: false,
				},
			},
			args: args{
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						Labels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						Annotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
						},
						ExtendedResources: testSpec,
					},
				},
			},
			want: &protocol.PodContext{
				Request: protocol.PodRequest{
					Labels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					Annotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
					},
					ExtendedResources: testSpec,
				},
				Response: protocol.PodResponse{
					Resources: protocol.Resources{
						CPUShares:   pointer.Int64Ptr(1024 * 500 / 1000),
						CFSQuota:    pointer.Int64Ptr(-1),
						MemoryLimit: pointer.Int64Ptr(2 * 1024 * 1024 * 1024),
					},
				},
			},
		},
		{
			name: "a Batch pod with cpu memory requests",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						Labels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						Annotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
						},
						ExtendedResources: testSpec,
					},
				},
			},
			want: &protocol.PodContext{
				Request: protocol.PodRequest{
					Labels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					Annotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
					},
					ExtendedResources: testSpec,
				},
				Response: protocol.PodResponse{
					Resources: protocol.Resources{
						CPUShares:   pointer.Int64Ptr(1024 * 500 / 1000),
						CFSQuota:    pointer.Int64Ptr(100000 * 500 / 1000),
						MemoryLimit: pointer.Int64Ptr(2 * 1024 * 1024 * 1024),
					},
				},
			},
		},
		{
			name: "a Batch pod with cpu memory requests 1",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						Labels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						Annotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes1),
						},
						ExtendedResources: testSpec1,
					},
				},
			},
			want: &protocol.PodContext{
				Request: protocol.PodRequest{
					Labels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					Annotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes1),
					},
					ExtendedResources: testSpec1,
				},
				Response: protocol.PodResponse{
					Resources: protocol.Resources{
						CPUShares:   pointer.Int64Ptr(1024 * 500 / 1000),
						CFSQuota:    pointer.Int64Ptr(100000 * 1500 / 1000),
						MemoryLimit: pointer.Int64Ptr(4 * 1024 * 1024 * 1024),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &plugin{
				rule: tt.fields.rule,
			}
			err := p.SetPodResources(tt.args.proto)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, tt.args.proto)
		})
	}
}

func Test_plugin_SetContainerResources(t *testing.T) {
	var testNilProto *protocol.ContainerContext
	testEmptySpec := &apiext.ExtendedResourceSpec{}
	testEmptySpecBytes, err := json.Marshal(testEmptySpec)
	assert.NoError(t, err)
	testContainerSpec := &apiext.ExtendedResourceContainerSpec{
		Requests: corev1.ResourceList{
			apiext.BatchCPU:    resource.MustParse("500"),
			apiext.BatchMemory: resource.MustParse("2Gi"),
		},
		Limits: corev1.ResourceList{
			apiext.BatchCPU:    resource.MustParse("500"),
			apiext.BatchMemory: resource.MustParse("2Gi"),
		},
	}
	testSpec := &apiext.ExtendedResourceSpec{
		Containers: map[string]apiext.ExtendedResourceContainerSpec{
			"container-0": *testContainerSpec,
		},
	}
	testSpecBytes, err := json.Marshal(testSpec)
	assert.NoError(t, err)
	testContainerSpec1 := &apiext.ExtendedResourceContainerSpec{
		Limits: corev1.ResourceList{
			apiext.BatchCPU:    resource.MustParse("1000"),
			apiext.BatchMemory: resource.MustParse("2Gi"),
		},
	}
	testSpec1 := &apiext.ExtendedResourceSpec{
		Containers: map[string]apiext.ExtendedResourceContainerSpec{
			"container-0": *testContainerSpec,
			"container-1": *testContainerSpec1,
		},
	}
	testSpecBytes1, err := json.Marshal(testSpec1)
	assert.NoError(t, err)
	type fields struct {
		rule *batchResourceRule
	}
	type args struct {
		proto protocol.HooksProtocol
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    protocol.HooksProtocol
		wantErr bool
	}{
		{
			name: "nil proto",
			args: args{
				proto: testNilProto,
			},
			want:    testNilProto,
			wantErr: true,
		},
		{
			name: "not a Batch container",
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{},
				},
			},
			want: &protocol.ContainerContext{
				Request: protocol.ContainerRequest{},
			},
		},
		{
			name: "a Batch container without requests",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						PodLabels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						PodAnnotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testEmptySpecBytes),
						},
					},
				},
			},
			want: &protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodLabels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					PodAnnotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testEmptySpecBytes),
					},
				},
			},
		},
		{
			name: "a Batch container with cfs quota unset",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: false,
				},
			},
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						PodLabels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						PodAnnotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
						},
						ContainerMeta: protocol.ContainerMeta{
							Name: "container-0",
						},
						ExtendedResources: testContainerSpec,
					},
				},
			},
			want: &protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodLabels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					PodAnnotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
					},
					ContainerMeta: protocol.ContainerMeta{
						Name: "container-0",
					},
					ExtendedResources: testContainerSpec,
				},
				Response: protocol.ContainerResponse{
					Resources: protocol.Resources{
						CPUShares:   pointer.Int64Ptr(1024 * 500 / 1000),
						CFSQuota:    pointer.Int64Ptr(-1),
						MemoryLimit: pointer.Int64Ptr(2 * 1024 * 1024 * 1024),
					},
				},
			},
		},
		{
			name: "a Batch container with requests not matched",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						PodLabels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						PodAnnotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
						},
						ContainerMeta: protocol.ContainerMeta{
							Name: "container-1",
						},
					},
				},
			},
			want: &protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodLabels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					PodAnnotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
					},
					ContainerMeta: protocol.ContainerMeta{
						Name: "container-1",
					},
				},
			},
		},
		{
			name: "a Batch container with requests",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						PodLabels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						PodAnnotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
						},
						ContainerMeta: protocol.ContainerMeta{
							Name: "container-0",
						},
						ExtendedResources: testContainerSpec,
					},
				},
			},
			want: &protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodLabels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					PodAnnotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
					},
					ContainerMeta: protocol.ContainerMeta{
						Name: "container-0",
					},
					ExtendedResources: testContainerSpec,
				},
				Response: protocol.ContainerResponse{
					Resources: protocol.Resources{
						CPUShares:   pointer.Int64Ptr(1024 * 500 / 1000),
						CFSQuota:    pointer.Int64Ptr(100000 * 500 / 1000),
						MemoryLimit: pointer.Int64Ptr(2 * 1024 * 1024 * 1024),
					},
				},
			},
		},
		{
			name: "a Batch container with requests not matched",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						PodLabels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						PodAnnotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
						},
						ContainerMeta: protocol.ContainerMeta{
							Name: "container-1",
						},
					},
				},
			},
			want: &protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodLabels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					PodAnnotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
					},
					ContainerMeta: protocol.ContainerMeta{
						Name: "container-1",
					},
				},
			},
		},
		{
			name: "a Batch container with requests 1",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						PodLabels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSBE),
						},
						PodAnnotations: map[string]string{
							apiext.AnnotationExtendedResourceSpec: string(testSpecBytes1),
						},
						ContainerMeta: protocol.ContainerMeta{
							Name: "container-1",
						},
						ExtendedResources: testContainerSpec1,
					},
				},
			},
			want: &protocol.ContainerContext{
				Request: protocol.ContainerRequest{
					PodLabels: map[string]string{
						apiext.LabelPodQoS: string(apiext.QoSBE),
					},
					PodAnnotations: map[string]string{
						apiext.AnnotationExtendedResourceSpec: string(testSpecBytes1),
					},
					ContainerMeta: protocol.ContainerMeta{
						Name: "container-1",
					},
					ExtendedResources: testContainerSpec1,
				},
				Response: protocol.ContainerResponse{
					Resources: protocol.Resources{
						CPUShares:   pointer.Int64Ptr(2),
						CFSQuota:    pointer.Int64Ptr(100000 * 1000 / 1000),
						MemoryLimit: pointer.Int64Ptr(2 * 1024 * 1024 * 1024),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &plugin{
				rule: tt.fields.rule,
			}
			err := p.SetContainerResources(tt.args.proto)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, tt.args.proto)
		})
	}
}

func Test_isPodQoSBEByAttr(t *testing.T) {
	tests := []struct {
		name string
		arg  map[string]string
		arg1 map[string]string
		want bool
	}{
		{
			name: "qos is BE",
			arg: map[string]string{
				apiext.LabelPodQoS: string(apiext.QoSBE),
			},
			want: true,
		},
		{
			name: "qos is not BE",
			arg: map[string]string{
				apiext.LabelPodQoS: string(apiext.QoSLS),
			},
			want: false,
		},
		{
			name: "qos is not BE 1",
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := isPodQoSBEByAttr(tt.arg, tt.arg1)
			assert.Equal(t, tt.want, got)
		})
	}
}
