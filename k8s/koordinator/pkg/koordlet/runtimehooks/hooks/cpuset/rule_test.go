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

package cpuset

import (
	"testing"

	topov1alpha1 "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/apis/topology/v1alpha1"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	ext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func Test_cpusetRule_getContainerCPUSet(t *testing.T) {
	type fields struct {
		kubeletPoicy string
		sharePools   []ext.CPUSharedPool
	}
	type args struct {
		podAlloc     *ext.ResourceStatus
		containerReq *protocol.ContainerRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *string
		wantErr bool
	}{
		{
			name: "get cpuset fqrom bad annotation",
			fields: fields{
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
				},
			},
			args: args{
				containerReq: &protocol.ContainerRequest{
					PodMeta:       protocol.PodMeta{},
					ContainerMeta: protocol.ContainerMeta{},
					PodLabels:     map[string]string{},
					PodAnnotations: map[string]string{
						ext.AnnotationResourceStatus: "bad-alloc-fmt",
					},
					CgroupParent: "burstable/test-pod/test-container",
				},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "get cpuset from annotation share pool",
			fields: fields{
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			args: args{
				containerReq: &protocol.ContainerRequest{
					PodMeta:        protocol.PodMeta{},
					ContainerMeta:  protocol.ContainerMeta{},
					PodLabels:      map[string]string{},
					PodAnnotations: map[string]string{},
					CgroupParent:   "burstable/test-pod/test-container",
				},
				podAlloc: &ext.ResourceStatus{
					CPUSharedPools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
						},
					},
				},
			},
			want:    pointer.String("0-7"),
			wantErr: false,
		},
		{
			name: "get all share pools for ls pod",
			fields: fields{
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			args: args{
				containerReq: &protocol.ContainerRequest{
					PodMeta:       protocol.PodMeta{},
					ContainerMeta: protocol.ContainerMeta{},
					PodLabels: map[string]string{
						ext.LabelPodQoS: string(ext.QoSLS),
					},
					PodAnnotations: map[string]string{},
					CgroupParent:   "burstable/test-pod/test-container",
				},
			},
			want:    pointer.String("0-7,8-15"),
			wantErr: false,
		},
		{
			name: "get all share pools for origin burstable pod under none policy",
			fields: fields{
				kubeletPoicy: ext.KubeletCPUManagerPolicyNone,
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			args: args{
				containerReq: &protocol.ContainerRequest{
					PodMeta:        protocol.PodMeta{},
					ContainerMeta:  protocol.ContainerMeta{},
					PodLabels:      map[string]string{},
					PodAnnotations: map[string]string{},
					CgroupParent:   "burstable/test-pod/test-container",
				},
			},
			want:    pointer.String("0-7,8-15"),
			wantErr: false,
		},
		{
			name: "do nothing for origin burstable pod under static policy",
			fields: fields{
				kubeletPoicy: ext.KubeletCPUManagerPolicyStatic,
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			args: args{
				containerReq: &protocol.ContainerRequest{
					PodMeta:        protocol.PodMeta{},
					ContainerMeta:  protocol.ContainerMeta{},
					PodLabels:      map[string]string{},
					PodAnnotations: map[string]string{},
					CgroupParent:   "burstable/test-pod/test-container",
				},
			},
			want:    nil,
			wantErr: false,
		},
		{
			name: "empty string for origin besteffort pod",
			fields: fields{
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			args: args{
				containerReq: &protocol.ContainerRequest{
					PodMeta:        protocol.PodMeta{},
					ContainerMeta:  protocol.ContainerMeta{},
					PodLabels:      map[string]string{},
					PodAnnotations: map[string]string{},
					CgroupParent:   "besteffort/test-pod/test-container",
				},
			},
			want:    pointer.String(""),
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &cpusetRule{
				kubeletPolicy: ext.KubeletCPUManagerPolicy{
					Policy: tt.fields.kubeletPoicy,
				},
				sharePools: tt.fields.sharePools,
			}
			if tt.args.podAlloc != nil {
				podAllocJson := util.DumpJSON(tt.args.podAlloc)
				tt.args.containerReq.PodAnnotations[ext.AnnotationResourceStatus] = podAllocJson
			}
			got, err := r.getContainerCPUSet(tt.args.containerReq)
			if (err != nil) != tt.wantErr {
				t.Errorf("getCPUSet() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.Equal(t, tt.want, got, "cpuset of container should be equal")
		})
	}
	// node.koordinator.sh/cpu-shared-pools: '[{"cpuset":"2-7"}]'
	// scheduling.koordinator.sh/resource-status: '{"cpuset":"0-1"}'
}

func Test_cpusetPlugin_parseRuleBadIf(t *testing.T) {
	type fields struct {
		rule *cpusetRule
	}
	type args struct {
		nodeTopo interface{}
	}
	tests := []struct {
		name        string
		fields      fields
		args        args
		wantUpdated bool
		wantRule    *cpusetRule
		wantErr     bool
	}{
		{
			name: "update rule with bad format",
			fields: fields{
				rule: &cpusetRule{
					sharePools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
							CPUSet: "0-7",
						},
						{
							Socket: 1,
							Node:   0,
							CPUSet: "8-15",
						},
					},
				},
			},
			args: args{
				nodeTopo: corev1.Pod{},
			},
			wantUpdated: false,
			wantRule: &cpusetRule{
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &cpusetPlugin{
				rule: tt.fields.rule,
			}
			got, err := p.parseRule(tt.args.nodeTopo)
			if (err != nil) != tt.wantErr {
				t.Errorf("parseRule() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.wantUpdated {
				t.Errorf("parseRule() got = %v, wantUpdated %v", got, tt.wantUpdated)
			}
			assert.Equal(t, tt.wantRule, p.rule, "after plugin rule parse")
		})
	}
}

func Test_cpusetPlugin_parseRule(t *testing.T) {
	type fields struct {
		rule *cpusetRule
	}
	type args struct {
		nodeTopo   *topov1alpha1.NodeResourceTopology
		cpuPolicy  *ext.KubeletCPUManagerPolicy
		sharePools []ext.CPUSharedPool
	}
	tests := []struct {
		name        string
		fields      fields
		args        args
		wantUpdated bool
		wantRule    *cpusetRule
		wantErr     bool
	}{
		{
			name: "update rule with bad format",
			fields: fields{
				rule: &cpusetRule{
					sharePools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
							CPUSet: "0-7",
						},
						{
							Socket: 1,
							Node:   0,
							CPUSet: "8-15",
						},
					},
				},
			},
			args: args{
				nodeTopo: &topov1alpha1.NodeResourceTopology{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
						Annotations: map[string]string{
							ext.AnnotationNodeCPUSharedPools: "bad-fmt",
						},
					},
				},
			},
			wantUpdated: false,
			wantRule: &cpusetRule{
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			wantErr: true,
		},
		{
			name: "update rule with same",
			fields: fields{
				rule: &cpusetRule{
					sharePools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
							CPUSet: "0-7",
						},
						{
							Socket: 1,
							Node:   0,
							CPUSet: "8-15",
						},
					},
				},
			},
			args: args{
				nodeTopo: &topov1alpha1.NodeResourceTopology{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
					},
				},
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			wantUpdated: false,
			wantRule: &cpusetRule{
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			wantErr: false,
		},
		{
			name: "update rule success",
			fields: fields{
				rule: nil,
			},
			args: args{
				nodeTopo: &topov1alpha1.NodeResourceTopology{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node",
					},
				},
				cpuPolicy: &ext.KubeletCPUManagerPolicy{
					Policy: ext.KubeletCPUManagerPolicyNone,
				},
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			wantUpdated: true,
			wantRule: &cpusetRule{
				kubeletPolicy: ext.KubeletCPUManagerPolicy{
					Policy: ext.KubeletCPUManagerPolicyNone,
				},
				sharePools: []ext.CPUSharedPool{
					{
						Socket: 0,
						Node:   0,
						CPUSet: "0-7",
					},
					{
						Socket: 1,
						Node:   0,
						CPUSet: "8-15",
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &cpusetPlugin{
				rule: tt.fields.rule,
			}
			if tt.args.nodeTopo.Annotations == nil {
				tt.args.nodeTopo.Annotations = map[string]string{}
			}
			if tt.args.cpuPolicy != nil {
				cpuPolicyJson := util.DumpJSON(tt.args.cpuPolicy)
				tt.args.nodeTopo.Annotations[ext.AnnotationKubeletCPUManagerPolicy] = cpuPolicyJson
			}
			if len(tt.args.sharePools) != 0 {
				sharePoolJson := util.DumpJSON(tt.args.sharePools)
				tt.args.nodeTopo.Annotations[ext.AnnotationNodeCPUSharedPools] = sharePoolJson
			}
			got, err := p.parseRule(tt.args.nodeTopo)
			if (err != nil) != tt.wantErr {
				t.Errorf("parseRule() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.wantUpdated {
				t.Errorf("parseRule() got = %v, wantUpdated %v", got, tt.wantUpdated)
			}
			assert.Equal(t, tt.wantRule, p.rule, "after plugin rule parse")
		})
	}
}

func Test_cpusetPlugin_ruleUpdateCb(t *testing.T) {
	type args struct {
		pods      []*statesinformer.PodMeta
		podAllocs map[string]ext.ResourceStatus
	}
	type wants struct {
		containersCPUSet map[string]string
	}
	tests := []struct {
		name    string
		args    args
		wants   wants
		wantErr bool
	}{
		{
			name: "set container cpuset",
			args: args{
				pods: []*statesinformer.PodMeta{
					{
						Pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								UID: "pod-with-cpuset-alloc-uid",
							},
							Spec: corev1.PodSpec{
								Containers: []corev1.Container{
									{
										Name: "container-with-cpuset-alloc-name",
									},
								},
							},
							Status: corev1.PodStatus{
								ContainerStatuses: []corev1.ContainerStatus{
									{
										Name:        "container-with-cpuset-alloc-name",
										ContainerID: "containerd://container-with-cpuset-alloc-uid",
									},
								},
							},
						},
					},
					{
						Pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								UID: "pod-with-bad-cpuset-alloc-uid",
								Annotations: map[string]string{
									ext.AnnotationResourceStatus: "bad-format",
								},
							},
							Spec: corev1.PodSpec{
								Containers: []corev1.Container{
									{
										Name: "container-with-bad-cpuset-alloc-name",
									},
								},
							},
							Status: corev1.PodStatus{
								ContainerStatuses: []corev1.ContainerStatus{
									{
										Name:        "container-with-bad-cpuset-alloc-name",
										ContainerID: "containerd://container-with-bad-cpuset-alloc-uid",
									},
								},
							},
						},
					},
				},
				podAllocs: map[string]ext.ResourceStatus{
					"pod-with-cpuset-alloc-uid": {
						CPUSet: "2-4",
					},
				},
			},
			wants: wants{
				containersCPUSet: map[string]string{
					"container-with-cpuset-alloc-name":     "2-4",
					"container-with-bad-cpuset-alloc-name": "",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := system.NewFileTestUtil(t)
			// init cgroups cpuset file
			for _, podMeta := range tt.args.pods {
				for _, containerStat := range podMeta.Pod.Status.ContainerStatuses {
					containerPath, err := koordletutil.GetContainerCgroupPathWithKubeByID(podMeta.CgroupDir, containerStat.ContainerID)
					assert.NoError(t, err, "get contaienr cgorup path during init container cpuset")
					initCPUSet(containerPath, "", testHelper)
				}
			}

			// init pod annotations
			for _, podMeta := range tt.args.pods {
				podUID := string(podMeta.Pod.UID)
				podAlloc, exist := tt.args.podAllocs[podUID]
				if !exist {
					continue
				}
				podAllocJson := util.DumpJSON(podAlloc)
				podMeta.Pod.Annotations = map[string]string{
					ext.AnnotationResourceStatus: podAllocJson,
				}
			}

			p := &cpusetPlugin{}
			if err := p.ruleUpdateCb(tt.args.pods); (err != nil) != tt.wantErr {
				t.Errorf("ruleUpdateCb() error = %v, wantErr %v", err, tt.wantErr)
			}

			for _, podMeta := range tt.args.pods {
				for _, containerStat := range podMeta.Pod.Status.ContainerStatuses {
					containerPath, err := koordletutil.GetContainerCgroupPathWithKubeByID(podMeta.CgroupDir, containerStat.ContainerID)
					assert.NoError(t, err, "get contaienr cgorup path during check container cpuset")
					gotCPUSEt := getCPUSet(containerPath, testHelper)
					assert.Equal(t, tt.wants.containersCPUSet[containerStat.Name], gotCPUSEt,
						"cpuset after callback should be equal")
				}
			}
		})
	}
}
