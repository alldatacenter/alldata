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

package nodeslo

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func Test_getResourceThresholdSpec(t *testing.T) {
	defaultSLOCfg := DefaultSLOCfg()
	testingResourceThresholdCfg := &extension.ResourceThresholdCfg{
		ClusterStrategy: &slov1alpha1.ResourceThresholdStrategy{
			Enable:                      pointer.BoolPtr(true),
			CPUSuppressThresholdPercent: pointer.Int64Ptr(60),
		},
	}
	testingResourceThresholdCfg1 := &extension.ResourceThresholdCfg{
		ClusterStrategy: &slov1alpha1.ResourceThresholdStrategy{
			Enable:                      pointer.BoolPtr(true),
			CPUSuppressThresholdPercent: pointer.Int64Ptr(60),
		},
		NodeStrategies: []extension.NodeResourceThresholdStrategy{
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{"xxx": "yyy"},
				},
				ResourceThresholdStrategy: &slov1alpha1.ResourceThresholdStrategy{
					CPUSuppressThresholdPercent: pointer.Int64Ptr(50),
				},
			},
		},
	}
	type args struct {
		node *corev1.Node
		cfg  *extension.ResourceThresholdCfg
	}
	tests := []struct {
		name    string
		args    args
		want    *slov1alpha1.ResourceThresholdStrategy
		wantErr bool
	}{
		{
			name: "node empty ,use cluster config",
			args: args{
				node: &corev1.Node{},
				cfg:  &defaultSLOCfg.ThresholdCfgMerged,
			},
			want:    defaultSLOCfg.ThresholdCfgMerged.ClusterStrategy,
			wantErr: false,
		},
		{
			name: "get cluster config correctly",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
					},
				},
				cfg: testingResourceThresholdCfg,
			},
			want: testingResourceThresholdCfg.ClusterStrategy,
		},
		{
			name: "get node config correctly",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
						Labels: map[string]string{
							"xxx": "yyy",
						},
					},
				},
				cfg: testingResourceThresholdCfg1,
			},
			want: &slov1alpha1.ResourceThresholdStrategy{
				CPUSuppressThresholdPercent: pointer.Int64Ptr(50),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := getResourceThresholdSpec(tt.args.node, tt.args.cfg)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_calculateResourceThresholdCfgMerged(t *testing.T) {
	defaultSLOCfg := DefaultSLOCfg()

	oldSLOCfg := DefaultSLOCfg()
	oldSLOCfg.ThresholdCfgMerged.ClusterStrategy.CPUSuppressThresholdPercent = pointer.Int64Ptr(30)

	testingResourceThresholdCfg := &extension.ResourceThresholdCfg{
		ClusterStrategy: &slov1alpha1.ResourceThresholdStrategy{
			Enable:                      pointer.BoolPtr(true),
			CPUSuppressThresholdPercent: pointer.Int64Ptr(60),
		},
	}
	testingResourceThresholdCfgStr, _ := json.Marshal(testingResourceThresholdCfg)

	expectTestingResourceThresholdCfg := defaultSLOCfg.ThresholdCfgMerged.DeepCopy()
	expectTestingResourceThresholdCfg.ClusterStrategy.Enable = testingResourceThresholdCfg.ClusterStrategy.Enable
	expectTestingResourceThresholdCfg.ClusterStrategy.CPUSuppressThresholdPercent = testingResourceThresholdCfg.ClusterStrategy.CPUSuppressThresholdPercent

	testingResourceThresholdCfg1 := &extension.ResourceThresholdCfg{
		ClusterStrategy: &slov1alpha1.ResourceThresholdStrategy{
			Enable:                      pointer.BoolPtr(true),
			CPUSuppressThresholdPercent: pointer.Int64Ptr(60),
		},
		NodeStrategies: []extension.NodeResourceThresholdStrategy{
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{"xxx": "yyy"},
				},
				ResourceThresholdStrategy: &slov1alpha1.ResourceThresholdStrategy{
					CPUSuppressThresholdPercent: pointer.Int64Ptr(40),
				},
			},
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{"zzz": "zzz"},
				},
				ResourceThresholdStrategy: &slov1alpha1.ResourceThresholdStrategy{
					CPUSuppressThresholdPercent: pointer.Int64Ptr(50),
				},
			},
		},
	}
	testingResourceThresholdCfg1Str, _ := json.Marshal(testingResourceThresholdCfg1)

	expectTestingResourceThresholdCfg1 := defaultSLOCfg.ThresholdCfgMerged.DeepCopy()
	expectTestingResourceThresholdCfg1.ClusterStrategy.Enable = testingResourceThresholdCfg1.ClusterStrategy.Enable
	expectTestingResourceThresholdCfg1.ClusterStrategy.CPUSuppressThresholdPercent = testingResourceThresholdCfg1.ClusterStrategy.CPUSuppressThresholdPercent
	expectTestingResourceThresholdCfg1.NodeStrategies = []extension.NodeResourceThresholdStrategy{
		{
			NodeSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{"xxx": "yyy"},
			},
			ResourceThresholdStrategy: expectTestingResourceThresholdCfg1.ClusterStrategy.DeepCopy(),
		},
		{
			NodeSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{"zzz": "zzz"},
			},
			ResourceThresholdStrategy: expectTestingResourceThresholdCfg1.ClusterStrategy.DeepCopy(),
		},
	}
	expectTestingResourceThresholdCfg1.NodeStrategies[0].CPUSuppressThresholdPercent = pointer.Int64Ptr(40)
	expectTestingResourceThresholdCfg1.NodeStrategies[1].CPUSuppressThresholdPercent = pointer.Int64Ptr(50)

	type args struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name    string
		args    args
		want    *extension.ResourceThresholdCfg
		wantErr bool
	}{
		{
			name: "config contents is empty,then use default",
			args: args{
				configMap: &corev1.ConfigMap{},
			},
			want:    &defaultSLOCfg.ThresholdCfgMerged,
			wantErr: false,
		},
		{
			name: "throw error for configmap unmarshal failed",
			args: args{
				configMap: &corev1.ConfigMap{
					Data: map[string]string{
						extension.ResourceThresholdConfigKey: "invalid_content",
					},
				},
			},
			want:    &oldSLOCfg.ThresholdCfgMerged,
			wantErr: true,
		},
		{
			name: "only cluster config",
			args: args{
				configMap: &corev1.ConfigMap{
					Data: map[string]string{
						extension.ResourceThresholdConfigKey: string(testingResourceThresholdCfgStr),
					},
				},
			},
			want:    expectTestingResourceThresholdCfg,
			wantErr: false,
		},
		{
			name: "node config",
			args: args{
				configMap: &corev1.ConfigMap{
					ObjectMeta: metav1.ObjectMeta{
						Name:      config.SLOCtrlConfigMap,
						Namespace: config.ConfigNameSpace,
					},
					Data: map[string]string{
						extension.ResourceThresholdConfigKey: string(testingResourceThresholdCfg1Str),
					},
				},
			},
			want:    expectTestingResourceThresholdCfg1,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := calculateResourceThresholdCfgMerged(oldSLOCfg.ThresholdCfgMerged, tt.args.configMap)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, &got)
		})
	}
}

func Test_getResourceQOSSpec(t *testing.T) {
	defaultSLOCfg := DefaultSLOCfg()
	testingResourceQOSCfg := &extension.ResourceQOSCfg{
		ClusterStrategy: &slov1alpha1.ResourceQOSStrategy{
			BEClass: &slov1alpha1.ResourceQOS{
				CPUQOS: &slov1alpha1.CPUQOSCfg{
					CPUQOS: slov1alpha1.CPUQOS{
						GroupIdentity: pointer.Int64Ptr(0),
					},
				},
			},
		},
	}
	testingResourceQOSCfg1 := &extension.ResourceQOSCfg{
		ClusterStrategy: &slov1alpha1.ResourceQOSStrategy{
			BEClass: &slov1alpha1.ResourceQOS{
				CPUQOS: &slov1alpha1.CPUQOSCfg{
					CPUQOS: slov1alpha1.CPUQOS{
						GroupIdentity: pointer.Int64Ptr(0),
					},
				},
			},
		},
		NodeStrategies: []extension.NodeResourceQOSStrategy{
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"xxx": "yyy",
					},
				},
				ResourceQOSStrategy: &slov1alpha1.ResourceQOSStrategy{
					BEClass: &slov1alpha1.ResourceQOS{
						CPUQOS: &slov1alpha1.CPUQOSCfg{
							CPUQOS: slov1alpha1.CPUQOS{
								GroupIdentity: pointer.Int64Ptr(1),
							},
						},
					},
				},
			},
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"zzz": "zzz",
					},
				},
				ResourceQOSStrategy: &slov1alpha1.ResourceQOSStrategy{
					BEClass: &slov1alpha1.ResourceQOS{
						CPUQOS: &slov1alpha1.CPUQOSCfg{
							CPUQOS: slov1alpha1.CPUQOS{
								GroupIdentity: pointer.Int64Ptr(2),
							},
						},
					},
				},
			},
		},
	}
	type args struct {
		node *corev1.Node
		cfg  *extension.ResourceQOSCfg
	}
	tests := []struct {
		name    string
		args    args
		want    *slov1alpha1.ResourceQOSStrategy
		wantErr bool
	}{
		{
			name: "node empty, use cluster config",
			args: args{
				node: &corev1.Node{},
				cfg:  &defaultSLOCfg.ResourceQOSCfgMerged,
			},
			want:    &slov1alpha1.ResourceQOSStrategy{},
			wantErr: false,
		},
		{
			name: "get cluster config correctly",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
					},
				},
				cfg: testingResourceQOSCfg,
			},
			want: testingResourceQOSCfg.ClusterStrategy,
		},
		{
			name: "get node config correctly",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
						Labels: map[string]string{
							"zzz": "zzz",
						},
					},
				},
				cfg: testingResourceQOSCfg1,
			},
			want: testingResourceQOSCfg1.NodeStrategies[1].ResourceQOSStrategy,
		},
		{
			name: "get firstly-matched node config",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
						Labels: map[string]string{
							"xxx": "yyy",
						},
					},
				},
				cfg: testingResourceQOSCfg1,
			},
			want: testingResourceQOSCfg1.NodeStrategies[0].ResourceQOSStrategy,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := getResourceQOSSpec(tt.args.node, tt.args.cfg)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_calculateResourceQOSCfgMerged(t *testing.T) {
	defaultSLOCfg := DefaultSLOCfg().ResourceQOSCfgMerged
	oldSLOConfig := &extension.ResourceQOSCfg{
		ClusterStrategy: &slov1alpha1.ResourceQOSStrategy{
			BEClass: &slov1alpha1.ResourceQOS{
				CPUQOS: &slov1alpha1.CPUQOSCfg{
					CPUQOS: slov1alpha1.CPUQOS{
						GroupIdentity: pointer.Int64Ptr(2),
					},
				},
				MemoryQOS: &slov1alpha1.MemoryQOSCfg{
					MemoryQOS: slov1alpha1.MemoryQOS{
						MinLimitPercent: pointer.Int64Ptr(40),
					},
				},
			},
		},
	}

	testingOnlyCluster := &extension.ResourceQOSCfg{
		ClusterStrategy: &slov1alpha1.ResourceQOSStrategy{
			BEClass: &slov1alpha1.ResourceQOS{
				CPUQOS: &slov1alpha1.CPUQOSCfg{
					CPUQOS: slov1alpha1.CPUQOS{
						GroupIdentity: pointer.Int64Ptr(0),
					},
				},
			},
		},
	}
	testingOnlyClusterStr, _ := json.Marshal(testingOnlyCluster)
	expectTestingOnlyCluster := testingOnlyCluster.DeepCopy()

	testingResourceQOSCfg1 := &extension.ResourceQOSCfg{
		ClusterStrategy: &slov1alpha1.ResourceQOSStrategy{
			BEClass: &slov1alpha1.ResourceQOS{
				CPUQOS: &slov1alpha1.CPUQOSCfg{
					CPUQOS: slov1alpha1.CPUQOS{
						GroupIdentity: pointer.Int64Ptr(0),
					},
				},
			},
		},
		NodeStrategies: []extension.NodeResourceQOSStrategy{
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"xxx": "yyy",
					},
				},
				ResourceQOSStrategy: &slov1alpha1.ResourceQOSStrategy{
					BEClass: &slov1alpha1.ResourceQOS{
						CPUQOS: &slov1alpha1.CPUQOSCfg{
							CPUQOS: slov1alpha1.CPUQOS{
								GroupIdentity: pointer.Int64Ptr(0),
							},
						},
					},
				},
			},
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"zzz": "zzz",
					},
				},
				ResourceQOSStrategy: &slov1alpha1.ResourceQOSStrategy{
					BEClass: &slov1alpha1.ResourceQOS{
						CPUQOS: &slov1alpha1.CPUQOSCfg{
							CPUQOS: slov1alpha1.CPUQOS{
								GroupIdentity: pointer.Int64Ptr(-1),
							},
						},
					},
				},
			},
		},
	}
	testingResourceQOSCfgStr1, _ := json.Marshal(testingResourceQOSCfg1)
	expectTestingResourceQOSCfg1 := testingResourceQOSCfg1.DeepCopy()
	expectTestingResourceQOSCfg1.NodeStrategies[0].BEClass.CPUQOS.GroupIdentity = pointer.Int64Ptr(0)
	expectTestingResourceQOSCfg1.NodeStrategies[1].BEClass.CPUQOS.GroupIdentity = pointer.Int64Ptr(-1)

	type args struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name    string
		args    args
		want    *extension.ResourceQOSCfg
		wantErr bool
	}{
		{
			name: "config is null! use old",
			args: args{
				configMap: &corev1.ConfigMap{},
			},
			want:    &defaultSLOCfg,
			wantErr: false,
		},
		{
			name: "throw error for configmap unmarshal failed",
			args: args{
				configMap: &corev1.ConfigMap{
					Data: map[string]string{
						extension.ResourceQOSConfigKey: "invalid_content",
					},
				},
			},
			want:    oldSLOConfig,
			wantErr: true,
		},
		{
			name: "get cluster config correctly",
			args: args{
				configMap: &corev1.ConfigMap{
					ObjectMeta: metav1.ObjectMeta{
						Name:      config.SLOCtrlConfigMap,
						Namespace: config.ConfigNameSpace,
					},
					Data: map[string]string{
						extension.ResourceQOSConfigKey: string(testingOnlyClusterStr),
					},
				},
			},
			want: expectTestingOnlyCluster,
		},
		{
			name: "get node config correctly",
			args: args{
				configMap: &corev1.ConfigMap{
					ObjectMeta: metav1.ObjectMeta{
						Name:      config.SLOCtrlConfigMap,
						Namespace: config.ConfigNameSpace,
					},
					Data: map[string]string{
						extension.ResourceQOSConfigKey: string(testingResourceQOSCfgStr1),
					},
				},
			},
			want: expectTestingResourceQOSCfg1,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := calculateResourceQOSCfgMerged(*oldSLOConfig, tt.args.configMap)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, &got)
		})
	}
}

func Test_getCPBurstConfigSpec(t *testing.T) {
	defaultConfig := DefaultSLOCfg()
	testingCPUBurstCfg := &extension.CPUBurstCfg{
		ClusterStrategy: &slov1alpha1.CPUBurstStrategy{
			CPUBurstConfig: slov1alpha1.CPUBurstConfig{
				CFSQuotaBurstPeriodSeconds: pointer.Int64Ptr(120),
			},
		},
	}
	testingCPUBurstCfg1 := &extension.CPUBurstCfg{
		ClusterStrategy: &slov1alpha1.CPUBurstStrategy{
			CPUBurstConfig: slov1alpha1.CPUBurstConfig{
				CPUBurstPercent: pointer.Int64Ptr(200),
			},
		},
		NodeStrategies: []extension.NodeCPUBurstCfg{
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"xxx": "yyy",
					},
				},
				CPUBurstStrategy: &slov1alpha1.CPUBurstStrategy{
					CPUBurstConfig: slov1alpha1.CPUBurstConfig{
						CPUBurstPercent: pointer.Int64Ptr(200),
					},
				},
			},
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"zzz": "zzz",
					},
				},
				CPUBurstStrategy: &slov1alpha1.CPUBurstStrategy{
					CPUBurstConfig: slov1alpha1.CPUBurstConfig{
						CPUBurstPercent: pointer.Int64Ptr(100),
					},
				},
			},
		},
	}
	type args struct {
		node *corev1.Node
		cfg  *extension.CPUBurstCfg
	}
	tests := []struct {
		name    string
		args    args
		want    *slov1alpha1.CPUBurstStrategy
		wantErr bool
	}{
		{
			name: "default value for empty config",
			args: args{
				node: &corev1.Node{},
				cfg:  &defaultConfig.CPUBurstCfgMerged,
			},
			want:    util.DefaultCPUBurstStrategy(),
			wantErr: false,
		},
		{
			name: "get cluster config correctly",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
					},
				},
				cfg: testingCPUBurstCfg,
			},
			want: testingCPUBurstCfg.ClusterStrategy,
		},
		{
			name: "get node config correctly",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
						Labels: map[string]string{
							"xxx": "yyy",
						},
					},
				},
				cfg: testingCPUBurstCfg1,
			},
			want: testingCPUBurstCfg1.NodeStrategies[0].CPUBurstStrategy,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := getCPUBurstConfigSpec(tt.args.node, tt.args.cfg)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_calculateCPUBurstCfgMerged(t *testing.T) {

	defaultSLOCfg := DefaultSLOCfg()

	oldSLOConfig := DefaultSLOCfg()
	oldSLOConfig.CPUBurstCfgMerged.ClusterStrategy.CFSQuotaBurstPercent = pointer.Int64Ptr(30)

	testingCfgClusterOnly := &extension.CPUBurstCfg{
		ClusterStrategy: &slov1alpha1.CPUBurstStrategy{
			CPUBurstConfig: slov1alpha1.CPUBurstConfig{
				CFSQuotaBurstPeriodSeconds: pointer.Int64Ptr(120),
			},
		},
	}
	testingCfgClusterOnlyStr, _ := json.Marshal(testingCfgClusterOnly)

	expectTestingCfgClusterOnly := defaultSLOCfg.CPUBurstCfgMerged.DeepCopy()
	expectTestingCfgClusterOnly.ClusterStrategy.CPUBurstConfig.CFSQuotaBurstPeriodSeconds = testingCfgClusterOnly.ClusterStrategy.CPUBurstConfig.CFSQuotaBurstPeriodSeconds

	testingCPUBurstCfg1 := &extension.CPUBurstCfg{
		ClusterStrategy: testingCfgClusterOnly.ClusterStrategy,
		NodeStrategies: []extension.NodeCPUBurstCfg{
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"xxx": "yyy",
					},
				},
				CPUBurstStrategy: &slov1alpha1.CPUBurstStrategy{
					CPUBurstConfig: slov1alpha1.CPUBurstConfig{
						CPUBurstPercent: pointer.Int64Ptr(100),
					},
				},
			},
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"zzz": "zzz",
					},
				},
				CPUBurstStrategy: &slov1alpha1.CPUBurstStrategy{
					CPUBurstConfig: slov1alpha1.CPUBurstConfig{
						CPUBurstPercent: pointer.Int64Ptr(200),
					},
				},
			},
		},
	}
	testingCPUBurstCfgStr1, _ := json.Marshal(testingCPUBurstCfg1)

	expectTestingCPUBurstCfg1 := &extension.CPUBurstCfg{
		ClusterStrategy: expectTestingCfgClusterOnly.ClusterStrategy.DeepCopy(),
		NodeStrategies: []extension.NodeCPUBurstCfg{
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"xxx": "yyy",
					},
				},
				CPUBurstStrategy: expectTestingCfgClusterOnly.ClusterStrategy.DeepCopy(),
			},
			{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"zzz": "zzz",
					},
				},
				CPUBurstStrategy: expectTestingCfgClusterOnly.ClusterStrategy.DeepCopy(),
			},
		},
	}
	expectTestingCPUBurstCfg1.NodeStrategies[0].CPUBurstPercent = testingCPUBurstCfg1.NodeStrategies[0].CPUBurstPercent
	expectTestingCPUBurstCfg1.NodeStrategies[1].CPUBurstPercent = testingCPUBurstCfg1.NodeStrategies[1].CPUBurstPercent

	type args struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name    string
		args    args
		want    *extension.CPUBurstCfg
		wantErr bool
	}{
		{
			name: "config is null! use cluster config",
			args: args{
				configMap: &corev1.ConfigMap{},
			},
			want:    &defaultSLOCfg.CPUBurstCfgMerged,
			wantErr: false,
		},
		{
			name: "throw error for configmap unmarshal failed",
			args: args{
				configMap: &corev1.ConfigMap{
					Data: map[string]string{
						extension.CPUBurstConfigKey: "invalid_content",
					},
				},
			},
			want:    &oldSLOConfig.CPUBurstCfgMerged,
			wantErr: true,
		},
		{
			name: "get cluster config correctly",
			args: args{
				configMap: &corev1.ConfigMap{
					ObjectMeta: metav1.ObjectMeta{
						Name:      config.SLOCtrlConfigMap,
						Namespace: config.ConfigNameSpace,
					},
					Data: map[string]string{
						extension.CPUBurstConfigKey: string(testingCfgClusterOnlyStr),
					},
				},
			},
			want: expectTestingCfgClusterOnly,
		},
		{
			name: "get config merged correctly",
			args: args{
				configMap: &corev1.ConfigMap{
					ObjectMeta: metav1.ObjectMeta{
						Name:      config.SLOCtrlConfigMap,
						Namespace: config.ConfigNameSpace,
					},
					Data: map[string]string{
						extension.CPUBurstConfigKey: string(testingCPUBurstCfgStr1),
					},
				},
			},
			want: expectTestingCPUBurstCfg1,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := calculateCPUBurstCfgMerged(oldSLOConfig.CPUBurstCfgMerged, tt.args.configMap)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, &got)
		})
	}
}
