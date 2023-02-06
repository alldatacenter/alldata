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

package pod

import (
	"testing"

	"github.com/stretchr/testify/assert"
	apiv1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/k8s-tagger/metadata"
)

func TestCache_extractPodMetadata(t *testing.T) {
	type fields struct {
		aInclude, lInclude []string
	}
	type args struct {
		pod apiv1.Pod
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   Value
	}{
		{
			name: "podnameIndexer",
			args: args{pod: apiv1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Name:        "aaa",
					Namespace:   "default",
					UID:         "aaa-bbb-ccc-ddd",
					Labels:      map[string]string{},
					Annotations: map[string]string{},
				},
				Spec: apiv1.PodSpec{},
				Status: apiv1.PodStatus{
					PodIP: "1.1.1.1",
				},
			}},
			want: Value{
				Tags: map[string]string{
					metadata.PrefixPod + "name":      "aaa",
					metadata.PrefixPod + "namespace": "default",
					metadata.PrefixPod + "uid":       "aaa-bbb-ccc-ddd",
					metadata.PrefixPod + "ip":        "1.1.1.1",
				},
				Fields: map[string]interface{}{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c, err := NewCache(nil, tt.fields.aInclude, tt.fields.lInclude)
			assert.Nil(t, err)
			assert.Equal(t, tt.want, c.extractPodMetadata(tt.args.pod))
		})
	}
}

func TestCache_extractPodContainerMetadata(t *testing.T) {
	type fields struct {
		aInclude, lInclude []string
	}
	type args struct {
		pod       apiv1.Pod
		container apiv1.Container
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   Value
	}{
		{
			name: "podnamecontainerIndexer",
			fields: fields{
				aInclude: []string{"msp.erda.cloud/*"},
				lInclude: []string{"*.erda.cloud/*"},
			},
			args: args{pod: apiv1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "aaa",
					Namespace: "default",
					UID:       "aaa-bbb-ccc-ddd",
					Labels: map[string]string{
						"dop.erda.cloud/a": "b",
						"msp.erda.cloud/c": "d",
					},
					Annotations: map[string]string{
						"dop.erda.cloud/a": "b",
						"msp.erda.cloud/e": "f",
					},
				},
				Status: apiv1.PodStatus{
					PodIP: "1.1.1.1",
				},
			},
				container: apiv1.Container{
					Name: "nginx",
					Resources: apiv1.ResourceRequirements{
						Requests: apiv1.ResourceList{
							apiv1.ResourceCPU:    resource.MustParse("100m"),
							apiv1.ResourceMemory: resource.MustParse("512Mi"),
						},
						Limits: apiv1.ResourceList{
							apiv1.ResourceCPU:    resource.MustParse("100m"),
							apiv1.ResourceMemory: resource.MustParse("1Gi"),
						},
					},
				},
			},
			want: Value{
				Tags: map[string]string{},
				Fields: map[string]interface{}{
					"container_resources_cpu_request":    0.1,
					"container_resources_memory_request": int64(512 * 1024 * 1024),
					"container_resources_cpu_limit":      0.1,
					"container_resources_memory_limit":   int64(1024 * 1024 * 1024),
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c, err := NewCache(nil, tt.fields.aInclude, tt.fields.lInclude)
			assert.Nil(t, err)
			assert.Equal(t, tt.want, c.extractPodContainerMetadata(tt.args.container))
		})
	}
}
