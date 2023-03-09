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

package config

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/record"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func Test_syncColocationConfigIfChanged(t *testing.T) {
	clearDefaultColocationExtension()
	oldCfg := *NewDefaultColocationCfg()
	oldCfg.MemoryReclaimThresholdPercent = pointer.Int64Ptr(40)
	memoryCalcPolicyByUsage := extension.CalculateByPodUsage
	memoryCalcPolicyByRequest := extension.CalculateByPodRequest

	type fields struct {
		config *colocationCfgCache
	}
	type args struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name        string
		fields      fields
		args        args
		wantChanged bool
		wantField   *colocationCfgCache
	}{
		{
			name:        "configmap is nil,cache have no old cfg,  cfg will be changed to use default cfg",
			fields:      fields{config: &colocationCfgCache{}},
			args:        args{configMap: nil},
			wantChanged: true,
			wantField:   &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg(), available: true, errorStatus: true},
		},
		{
			name:        "configmap is nil,cache have old cfg,  cfg will be changed to use default cfg",
			fields:      fields{config: &colocationCfgCache{colocationCfg: oldCfg}},
			args:        args{configMap: nil},
			wantChanged: true,
			wantField:   &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg(), available: true, errorStatus: true},
		},
		{
			name:        "configmap is nil, cache has been set default cfg ,so not changed",
			fields:      fields{config: &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg()}},
			args:        args{configMap: nil},
			wantChanged: false,
			wantField:   &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg(), available: true, errorStatus: true},
		},
		{
			name:   "no colocation config in configmap, cache have no old cfg, cfg will be changed to use default cfg",
			fields: fields{config: &colocationCfgCache{}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{},
			}},
			wantChanged: true,
			wantField:   &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg(), available: true, errorStatus: false},
		},
		{
			name:   "no colocation config in configmap, cache have old cfg, cfg will be changed to use default cfg",
			fields: fields{config: &colocationCfgCache{colocationCfg: oldCfg}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{},
			}},
			wantChanged: true,
			wantField:   &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg(), available: true, errorStatus: false},
		},
		{
			name:   "no colocation config in configmap, cache has been set default cfg ,so not changed",
			fields: fields{config: &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg()}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{},
			}},
			wantChanged: false,
			wantField:   &colocationCfgCache{colocationCfg: *NewDefaultColocationCfg(), available: true, errorStatus: false},
		},
		{
			name:   "unmarshal failed, for cache unavailable",
			fields: fields{config: &colocationCfgCache{}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"metricAggregateDurationSeconds\":10, \"invalidKey\":\"invalidValue\",}",
				},
			}},
			wantChanged: false,
			wantField:   &colocationCfgCache{errorStatus: true},
		},
		{
			name: "unmarshal failed, keep the old",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: oldCfg,
				available:     true,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"metricAggregateDurationSeconds\":10, \"invalidKey\":\"invalidValue\",}",
				},
			}},
			wantChanged: false,
			wantField: &colocationCfgCache{
				colocationCfg: oldCfg,
				available:     true,
				errorStatus:   true,
			},
		},
		{
			name: "validate and merge partial config with the default",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: oldCfg,
				available:     true,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"metricAggregateDurationSeconds\":60,\"cpuReclaimThresholdPercent\":70," +
						"\"memoryReclaimThresholdPercent\":70,\"updateTimeThresholdSeconds\":100,\"metricReportIntervalSeconds\":20}",
				},
			}},
			wantChanged: true,
			wantField: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(false),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(70),
						MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
						DegradeTimeMinutes:             pointer.Int64Ptr(15),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(100),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
				},
				available:   true,
				errorStatus: false,
			},
		},
		{
			name:   "got invalid partial config, if restart and will unavailable",
			fields: fields{config: &colocationCfgCache{}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"metricAggregateDurationSeconds\":60,\"cpuReclaimThresholdPercent\":70," +
						"\"memoryReclaimThresholdPercent\":70,\"updateTimeThresholdSeconds\":-1,\"metricReportIntervalSeconds\":20}",
				},
			}},
			wantChanged: false,
			wantField:   &colocationCfgCache{errorStatus: true},
		},
		{
			name: "got invalid partial config, keep the old",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: oldCfg,
				available:     true,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"metricAggregateDurationSeconds\":60,\"cpuReclaimThresholdPercent\":70," +
						"\"memoryReclaimThresholdPercent\":70,\"updateTimeThresholdSeconds\":-1,\"metricReportIntervalSeconds\":20}",
				},
			}},
			wantChanged: false,
			wantField: &colocationCfgCache{
				colocationCfg: oldCfg,
				available:     true,
				errorStatus:   true,
			},
		},
		{
			name: "node config invalid, use cluster config",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: oldCfg,
				available:     true,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":30,\"metricReportIntervalSeconds\":20," +
						"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"updateTimeThresholdSeconds\":300," +
						"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1,\"nodeConfigs\":[{\"nodeSelector\":" +
						"{\"matchLabels\":{\"xxx\":\"yyy\"}},\"metricAggregateDurationSeconds\":-1}]}",
				},
			}},
			wantChanged: true,
			wantField: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(30),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"xxx": "yyy",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								Enable:                         pointer.BoolPtr(true),
								MetricAggregateDurationSeconds: pointer.Int64Ptr(30),
								MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
								MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
								CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
								MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
								MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
								DegradeTimeMinutes:             pointer.Int64Ptr(5),
								UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
								ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
							},
						},
					},
				},
				available:   true,
				errorStatus: false,
			},
		},
		{
			name: "only cluster config change successfully, set available",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: oldCfg,
				available:     false,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":60,\"metricReportIntervalSeconds\":20," +
						"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"updateTimeThresholdSeconds\":300," +
						"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1,\"metricReportIntervalSeconds\":20}",
				},
			}},
			wantChanged: true,
			wantField: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
				},
				available:   true,
				errorStatus: false,
			},
		},
		{
			name: "only cluster config with no change",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(60),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
				},
				available: true,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":60,\"metricAggregateDurationSeconds\":60," +
						"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"updateTimeThresholdSeconds\":300," +
						"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1}",
				},
			}},
			wantChanged: false,
			wantField: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(60),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
				},
				available:   true,
				errorStatus: false,
			},
		},
		{
			name: "full config change successfully",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(60),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"xxx": "yyy",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								Enable:                         pointer.BoolPtr(true),
								MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
								MetricReportIntervalSeconds:    pointer.Int64Ptr(60),
								MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
								CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
								MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
								DegradeTimeMinutes:             pointer.Int64Ptr(5),
								UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
								ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
							},
						},
					},
				},
				available:   true,
				errorStatus: false,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":60,\"metricReportIntervalSeconds\":20," +
						"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"updateTimeThresholdSeconds\":300," +
						"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1,\"nodeConfigs\":[{\"nodeSelector\":" +
						"{\"matchLabels\":{\"xxx\":\"yyy\"}},\"enable\":true}]}",
				},
			}},
			wantChanged: true,
			wantField: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"xxx": "yyy",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								Enable:                         pointer.BoolPtr(true),
								MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
								MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
								MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
								CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
								MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
								MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
								DegradeTimeMinutes:             pointer.Int64Ptr(5),
								UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
								ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
							},
						},
					},
				},
				available:   true,
				errorStatus: false,
			},
		},
		{
			name: "node config with change",
			fields: fields{config: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(60),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"aaa": "bbbb",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								Enable: pointer.BoolPtr(true),
							},
						},
					},
				},
				available: true,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      SLOCtrlConfigMap,
					Namespace: ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":30,\"metricReportIntervalSeconds\":20," +
						"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"memoryCalculatePolicy\":\"request\"," +
						"\"updateTimeThresholdSeconds\":300," +
						"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1,\"nodeConfigs\":[{\"nodeSelector\":" +
						"{\"matchLabels\":{\"xxx\":\"yyy\"}},\"enable\":true,\"cpuReclaimThresholdPercent\":60}]}",
				},
			}},
			wantChanged: true,
			wantField: &colocationCfgCache{
				colocationCfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                         pointer.BoolPtr(true),
						MetricAggregateDurationSeconds: pointer.Int64Ptr(30),
						MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
						MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
						CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
						MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
						MemoryCalculatePolicy:          &memoryCalcPolicyByRequest,
						DegradeTimeMinutes:             pointer.Int64Ptr(5),
						UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
						ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					},
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"xxx": "yyy",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								Enable:                         pointer.BoolPtr(true),
								MetricAggregateDurationSeconds: pointer.Int64Ptr(30),
								MetricReportIntervalSeconds:    pointer.Int64Ptr(20),
								MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
								MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
								MemoryCalculatePolicy:          &memoryCalcPolicyByRequest,
								DegradeTimeMinutes:             pointer.Int64Ptr(5),
								UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
								ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
								//change
								CPUReclaimThresholdPercent: pointer.Int64Ptr(60),
							},
						},
					},
				},
				available:   true,
				errorStatus: false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fakeClient := fake.NewClientBuilder().WithScheme(scheme.Scheme).Build()
			p := NewColocationHandlerForConfigMapEvent(fakeClient, *NewDefaultColocationCfg(), &record.FakeRecorder{})
			p.cfgCache = colocationCfgCache{available: tt.fields.config.available, colocationCfg: tt.fields.config.colocationCfg}
			got := p.syncColocationCfgIfChanged(tt.args.configMap)
			assert.Equal(t, tt.wantChanged, got)
			assert.Equal(t, tt.wantField.available, p.cfgCache.available)
			assert.Equal(t, tt.wantField.errorStatus, p.cfgCache.errorStatus)
			assert.Equal(t, tt.wantField.colocationCfg, p.cfgCache.colocationCfg)
		})
	}
}

func Test_IsCfgAvailable(t *testing.T) {
	defaultConfig := DefaultColocationCfg()
	memoryCalcPolicyByUsage := extension.CalculateByPodUsage
	type fields struct {
		config    *colocationCfgCache
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name      string
		fields    fields
		want      bool
		wantField *extension.ColocationCfg
	}{
		{
			name: "directly return available",
			fields: fields{
				config: &colocationCfgCache{
					available: true,
				},
			},
			want:      true,
			wantField: &extension.ColocationCfg{},
		},
		{
			name: "set default when config is not found",
			fields: fields{
				config: &colocationCfgCache{
					colocationCfg: extension.ColocationCfg{
						ColocationStrategy: extension.ColocationStrategy{
							MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						},
					},
					available: false,
				},
			},
			want:      true,
			wantField: &defaultConfig,
		},
		{
			name: "use cluster config",
			fields: fields{
				config: &colocationCfgCache{
					colocationCfg: extension.ColocationCfg{
						ColocationStrategy: extension.ColocationStrategy{
							MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
						},
					},
					available: false,
				},
				configMap: &corev1.ConfigMap{
					TypeMeta: metav1.TypeMeta{
						Kind:       "ConfigMap",
						APIVersion: "v1",
					},
					ObjectMeta: metav1.ObjectMeta{
						Name:      SLOCtrlConfigMap,
						Namespace: ConfigNameSpace,
					},
					Data: map[string]string{
						extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":60," +
							"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"updateTimeThresholdSeconds\":300," +
							"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1}",
					},
				},
			},
			want: true,
			wantField: &extension.ColocationCfg{
				ColocationStrategy: extension.ColocationStrategy{
					Enable:                         pointer.BoolPtr(true),
					MetricAggregateDurationSeconds: pointer.Int64Ptr(60),
					MetricAggregatePolicy:          DefaultColocationStrategy().MetricAggregatePolicy,
					CPUReclaimThresholdPercent:     pointer.Int64Ptr(70),
					MemoryReclaimThresholdPercent:  pointer.Int64Ptr(80),
					MemoryCalculatePolicy:          &memoryCalcPolicyByUsage,
					DegradeTimeMinutes:             pointer.Int64Ptr(5),
					UpdateTimeThresholdSeconds:     pointer.Int64Ptr(300),
					ResourceDiffThreshold:          pointer.Float64Ptr(0.1),
					MetricReportIntervalSeconds:    pointer.Int64Ptr(60),
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fakeClient := fake.NewClientBuilder().WithScheme(scheme.Scheme).Build()
			if tt.fields.configMap != nil {
				fakeClient = fake.NewClientBuilder().WithScheme(scheme.Scheme).WithRuntimeObjects(tt.fields.configMap).Build()
			}
			p := NewColocationHandlerForConfigMapEvent(fakeClient, *NewDefaultColocationCfg(), &record.FakeRecorder{})
			p.cfgCache = colocationCfgCache{available: tt.fields.config.available, colocationCfg: tt.fields.config.colocationCfg}
			got := p.IsCfgAvailable()
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.wantField, &p.cfgCache.colocationCfg)
		})
	}
}
