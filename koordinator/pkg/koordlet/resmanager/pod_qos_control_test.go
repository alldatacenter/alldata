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

package resmanager

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	clientset "k8s.io/client-go/kubernetes"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resmanager/configextensions"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type cpuBurstGreyCtrlPlugin struct{}

func (p *cpuBurstGreyCtrlPlugin) Setup(kubeClient clientset.Interface) error {
	return nil
}

func (p *cpuBurstGreyCtrlPlugin) Run(stopCh <-chan struct{}) {
	return
}

func (p *cpuBurstGreyCtrlPlugin) InjectPodPolicy(pod *corev1.Pod, policyType configextensions.QOSPolicyType, greyCtlCfgIf *interface{}) (bool, error) {
	injected := false
	greyCtlCfg := &slov1alpha1.CPUBurstConfig{}
	if pod.Namespace == "allow-ns" {
		greyCtlCfg.Policy = slov1alpha1.CPUBurstAuto
		injected = true
	} else if pod.Namespace == "block-ns" {
		greyCtlCfg.Policy = slov1alpha1.CPUBurstNone
		injected = true
	}
	if injected {
		*greyCtlCfgIf = greyCtlCfg
	}
	return injected, nil
}

func (p *cpuBurstGreyCtrlPlugin) name() string {
	return "cpu-burst-test-plugin"
}

type memoryQOSGreyCtrlPlugin struct{}

func (p *memoryQOSGreyCtrlPlugin) Setup(kubeClient clientset.Interface) error {
	return nil
}

func (p *memoryQOSGreyCtrlPlugin) Run(stopCh <-chan struct{}) {
	return
}

func (p *memoryQOSGreyCtrlPlugin) InjectPodPolicy(pod *corev1.Pod, policyType configextensions.QOSPolicyType, greyCtlCfgIf *interface{}) (bool, error) {
	injected := false
	greyCtlCfg := &slov1alpha1.PodMemoryQOSConfig{}
	if pod.Namespace == "allow-ns" {
		greyCtlCfg.Policy = slov1alpha1.PodMemoryQOSPolicyAuto
		injected = true
	} else if pod.Namespace == "block-ns" {
		greyCtlCfg.Policy = slov1alpha1.PodMemoryQOSPolicyNone
		injected = true
	}
	if injected {
		*greyCtlCfgIf = greyCtlCfg
	}
	return injected, nil
}

func (p *memoryQOSGreyCtrlPlugin) name() string {
	return "memory-qos-test-plugin"
}

func Test_genPodBurstConfigWithPlugin(t *testing.T) {
	type args struct {
		pod            *corev1.Pod
		podCPUBurstCfg *slov1alpha1.CPUBurstConfig
		nodeCfg        slov1alpha1.CPUBurstConfig
	}
	tests := []struct {
		name string
		args args
		want *slov1alpha1.CPUBurstConfig
	}{
		{
			name: "inject by allowed ns list",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "allow-ns",
					},
				},
				podCPUBurstCfg: nil,
				nodeCfg:        util.DefaultCPUBurstConfig(),
			},
			want: &slov1alpha1.CPUBurstConfig{
				Policy:                     slov1alpha1.CPUBurstAuto,
				CPUBurstPercent:            pointer.Int64Ptr(1000),
				CFSQuotaBurstPercent:       pointer.Int64Ptr(300),
				CFSQuotaBurstPeriodSeconds: pointer.Int64Ptr(-1),
			},
		},
	}
	p := &cpuBurstGreyCtrlPlugin{}
	configextensions.ClearQOSGreyCtrlPlugin()
	configextensions.RegisterQOSGreyCtrlPlugin(p.name(), p)
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if tt.args.podCPUBurstCfg != nil {
				podCPUBurstCfgStr, _ := json.Marshal(tt.args.podCPUBurstCfg)
				tt.args.pod.Annotations = map[string]string{
					extension.AnnotationPodCPUBurst: string(podCPUBurstCfgStr),
				}
			}
			gotCfg := genPodBurstConfig(tt.args.pod, &tt.args.nodeCfg)
			assert.Equal(t, tt.want, gotCfg)
		})
	}
	configextensions.UnregisterQOSGreyCtrlPlugin(p.name())
}

func TestCgroupResourcesReconcile_mergePodResourceQoSForMemoryQoS(t *testing.T) {
	type args struct {
		pod *corev1.Pod
		cfg *slov1alpha1.ResourceQOS
	}
	type wants struct {
		memoryQOSCfg *slov1alpha1.MemoryQOSCfg
	}
	tests := []struct {
		name  string
		args  args
		wants wants
	}{
		{
			name: "inject by allow ns list",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "allow-ns",
						Labels: map[string]string{
							apiext.LabelPodQoS: string(apiext.QoSLS),
						},
					},
				},
				cfg: &slov1alpha1.ResourceQOS{},
			},
			wants: wants{
				memoryQOSCfg: &slov1alpha1.MemoryQOSCfg{
					MemoryQOS: *util.DefaultMemoryQOS(apiext.QoSLS),
				},
			},
		},
	}
	p := &memoryQOSGreyCtrlPlugin{}
	configextensions.ClearQOSGreyCtrlPlugin()
	configextensions.RegisterQOSGreyCtrlPlugin(p.name(), p)
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			m := &CgroupResourcesReconcile{
				resmanager: &resmanager{},
			}
			m.mergePodResourceQoSForMemoryQoS(tt.args.pod, tt.args.cfg)
			assert.Equal(t, tt.wants.memoryQOSCfg, tt.args.cfg.MemoryQOS)
		})
	}
	configextensions.UnregisterQOSGreyCtrlPlugin(p.name())
}
