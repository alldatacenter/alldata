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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/record"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
)

func Test_syncNodeSLOSpecIfChanged(t *testing.T) {
	oldSLOCfg := DefaultSLOCfg()
	testingConfigMap1 := &corev1.ConfigMap{
		TypeMeta: metav1.TypeMeta{
			Kind:       "ConfigMap",
			APIVersion: "v1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      config.SLOCtrlConfigMap,
			Namespace: config.ConfigNameSpace,
		},
		Data: map[string]string{
			extension.ResourceThresholdConfigKey: "{\"clusterStrategy\":{\"enable\":true,\"cpuSuppressThresholdPercent\":60}}",
			extension.ResourceQOSConfigKey: `
{
  "clusterStrategy": {
    "beClass": {
      "cpuQOS": {
        "groupIdentity": 0
      }
    }
  }
}
`,
			extension.CPUBurstConfigKey: "{\"clusterStrategy\":{\"cfsQuotaBurstPeriodSeconds\":60}}",
		},
	}

	expectTestingCfg1 := oldSLOCfg.DeepCopy()
	expectTestingCfg1.ThresholdCfgMerged.ClusterStrategy.Enable = pointer.BoolPtr(true)
	expectTestingCfg1.ThresholdCfgMerged.ClusterStrategy.CPUSuppressThresholdPercent = pointer.Int64Ptr(60)

	expectTestingCfg1.ResourceQOSCfgMerged.ClusterStrategy = &slov1alpha1.ResourceQOSStrategy{
		BEClass: &slov1alpha1.ResourceQOS{
			CPUQOS: &slov1alpha1.CPUQOSCfg{
				CPUQOS: slov1alpha1.CPUQOS{
					GroupIdentity: pointer.Int64Ptr(0),
				},
			},
		},
	}

	expectTestingCfg1.CPUBurstCfgMerged.ClusterStrategy.CFSQuotaBurstPeriodSeconds = pointer.Int64Ptr(60)

	type fields struct {
		oldCfg *sLOCfgCache
	}
	type args struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name        string
		fields      fields
		args        args
		wantChanged bool
		wantField   *sLOCfgCache
	}{
		{
			name:        "configmap is nil, use default cfg",
			fields:      fields{oldCfg: &sLOCfgCache{}},
			args:        args{configMap: nil},
			wantChanged: true,
			wantField:   &sLOCfgCache{sloCfg: DefaultSLOCfg(), available: true},
		},
		{
			name:        "configmap is nil, old is default, then not changed",
			fields:      fields{oldCfg: &sLOCfgCache{sloCfg: DefaultSLOCfg(), available: true}},
			args:        args{configMap: nil},
			wantChanged: false,
			wantField:   &sLOCfgCache{sloCfg: DefaultSLOCfg(), available: true},
		},
		{
			name: "no slo config in configmap, keep the old,and set available",
			fields: fields{oldCfg: &sLOCfgCache{
				sloCfg:    DefaultSLOCfg(),
				available: false,
			}},
			args:        args{configMap: &corev1.ConfigMap{}},
			wantChanged: false,
			wantField: &sLOCfgCache{
				sloCfg:    DefaultSLOCfg(),
				available: true,
			},
		},
		{
			name: "unmarshal config failed, keep the old",
			fields: fields{oldCfg: &sLOCfgCache{
				sloCfg:    *oldSLOCfg.DeepCopy(),
				available: true,
			}},
			args: args{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      config.SLOCtrlConfigMap,
					Namespace: config.ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ResourceThresholdConfigKey: "invalid_content",
					extension.ResourceQOSConfigKey:       "invalid_content",
					extension.CPUBurstConfigKey:          "invalid_content",
				},
			}},
			wantChanged: false,
			wantField: &sLOCfgCache{
				sloCfg:    DefaultSLOCfg(),
				available: true,
			},
		},
		{
			name: "config with no change",
			fields: fields{oldCfg: &sLOCfgCache{
				sloCfg:    *expectTestingCfg1,
				available: true,
			}},
			args:        args{configMap: testingConfigMap1},
			wantChanged: false,
			wantField: &sLOCfgCache{
				sloCfg:    *expectTestingCfg1,
				available: true,
			},
		},
		{
			name: "config changed",
			fields: fields{oldCfg: &sLOCfgCache{
				sloCfg:    oldSLOCfg,
				available: true,
			}},
			args:        args{configMap: testingConfigMap1},
			wantChanged: true,
			wantField: &sLOCfgCache{
				sloCfg:    *expectTestingCfg1,
				available: true,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fakeClient := fake.NewClientBuilder().WithScheme(scheme.Scheme).Build()
			p := NewSLOCfgHandlerForConfigMapEvent(fakeClient, tt.fields.oldCfg.sloCfg, &record.FakeRecorder{})
			p.cfgCache = sLOCfgCache{available: tt.fields.oldCfg.available, sloCfg: tt.fields.oldCfg.sloCfg}
			p.cfgCache.available = tt.fields.oldCfg.available
			got := p.syncNodeSLOSpecIfChanged(tt.args.configMap)
			assert.Equal(t, tt.wantChanged, got)
			assert.Equal(t, tt.wantField.available, p.cfgCache.available)
			assert.Equal(t, tt.wantField.sloCfg, p.cfgCache.sloCfg)
		})
	}
}
