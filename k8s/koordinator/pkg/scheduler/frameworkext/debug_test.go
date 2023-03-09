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

package frameworkext

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"
)

func TestDebugScoresSetter(t *testing.T) {
	tests := []struct {
		name    string
		value   string
		wantErr bool
	}{
		{
			name:    "integer",
			value:   "123",
			wantErr: false,
		},
		{
			name:    "float",
			value:   "11.22",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := DebugScoresSetter(tt.value)
			if tt.wantErr && err == nil {
				t.Error("expected error but got nil")
			} else if !tt.wantErr && err != nil {
				t.Errorf("expected no error but got err: %v", err)
			}
		})
	}
}

func TestDebugFiltersSetter(t *testing.T) {
	tests := []struct {
		name    string
		value   string
		wantErr bool
		want    bool
	}{
		{
			name:    "valid bool",
			value:   "true",
			wantErr: false,
			want:    true,
		},
		{
			name:    "invalid",
			value:   "11.22",
			wantErr: true,
			want:    false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := DebugFiltersSetter(tt.value)
			if tt.wantErr && err == nil {
				t.Error("expected error but got nil")
			} else if !tt.wantErr && err != nil {
				t.Errorf("expected no error but got err: %v", err)
			}
			assert.Equal(t, tt.want, debugFilterFailure)
			debugFilterFailure = false
		})
	}
}

func TestDebugScores(t *testing.T) {
	pluginToNodeScores := map[string]framework.NodeScoreList{
		"ImageLocality": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 0},
		},
		"InterPodAffinity": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 0},
		},
		"LoadAwareScheduling": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 85},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 87},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 55},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 15},
		},
		"NodeAffinity": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 0},
		},
		"NodeNUMAResource": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 0},
		},
		"NodeResourcesBalancedAllocation": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 96},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 96},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 95},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 90},
		},
		"NodeResourcesFit": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 93},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 94},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 91},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 82},
		},
		"PodTopologySpread": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 200},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 200},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 200},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 200},
		},
		"Reservation": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 0},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 0},
		},
		"TaintToleration": {
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.50", Score: 100},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.51", Score: 100},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.19", Score: 100},
			framework.NodeScore{Name: "cn-hangzhou.10.0.4.18", Score: 100},
		},
	}
	nodes := []*corev1.Node{
		{ObjectMeta: metav1.ObjectMeta{Name: "cn-hangzhou.10.0.4.50"}},
		{ObjectMeta: metav1.ObjectMeta{Name: "cn-hangzhou.10.0.4.51"}},
		{ObjectMeta: metav1.ObjectMeta{Name: "cn-hangzhou.10.0.4.19"}},
		{ObjectMeta: metav1.ObjectMeta{Name: "cn-hangzhou.10.0.4.18"}},
	}

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "curlimage-545745d8f8-rngp7",
		},
	}

	w := debugScores(4, pod, pluginToNodeScores, nodes)
	expectedResult := `| # | Pod | Node | Score | ImageLocality | InterPodAffinity | LoadAwareScheduling | NodeAffinity | NodeNUMAResource | NodeResourcesBalancedAllocation | NodeResourcesFit | PodTopologySpread | Reservation | TaintToleration |
| --- | --- | --- | ---:| ---:| ---:| ---:| ---:| ---:| ---:| ---:| ---:| ---:| ---:|
| 0 | default/curlimage-545745d8f8-rngp7 | cn-hangzhou.10.0.4.51 | 577 | 0 | 0 | 87 | 0 | 0 | 96 | 94 | 200 | 0 | 100 |
| 1 | default/curlimage-545745d8f8-rngp7 | cn-hangzhou.10.0.4.50 | 574 | 0 | 0 | 85 | 0 | 0 | 96 | 93 | 200 | 0 | 100 |
| 2 | default/curlimage-545745d8f8-rngp7 | cn-hangzhou.10.0.4.19 | 541 | 0 | 0 | 55 | 0 | 0 | 95 | 91 | 200 | 0 | 100 |
| 3 | default/curlimage-545745d8f8-rngp7 | cn-hangzhou.10.0.4.18 | 487 | 0 | 0 | 15 | 0 | 0 | 90 | 82 | 200 | 0 | 100 |`
	assert.Equal(t, expectedResult, w.RenderMarkdown())
}
