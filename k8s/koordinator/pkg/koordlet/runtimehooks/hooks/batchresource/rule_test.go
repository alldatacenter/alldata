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
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_plugin_parseRule(t *testing.T) {
	type fields struct {
		rule *batchResourceRule
	}
	type args struct {
		mergedNodeSLO *slov1alpha1.NodeSLOSpec
	}
	tests := []struct {
		name     string
		fields   fields
		args     args
		want     bool
		wantErr  bool
		wantRule *batchResourceRule
	}{
		{
			name:    "initialize true",
			want:    true,
			wantErr: false,
			wantRule: &batchResourceRule{
				enableCFSQuota: true,
			},
		},
		{
			name: "initialize false",
			args: args{
				mergedNodeSLO: &slov1alpha1.NodeSLOSpec{
					ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
						Enable:            pointer.Bool(true),
						CPUSuppressPolicy: slov1alpha1.CPUCfsQuotaPolicy,
					},
				},
			},
			want:    true,
			wantErr: false,
			wantRule: &batchResourceRule{
				enableCFSQuota: false,
			},
		},
		{
			name: "rule change to false",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				mergedNodeSLO: &slov1alpha1.NodeSLOSpec{
					ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
						Enable:            pointer.Bool(true),
						CPUSuppressPolicy: slov1alpha1.CPUCfsQuotaPolicy,
					},
				},
			},
			want:    true,
			wantErr: false,
			wantRule: &batchResourceRule{
				enableCFSQuota: false,
			},
		},
		{
			name: "rule change to true",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: false,
				},
			},
			args: args{
				mergedNodeSLO: &slov1alpha1.NodeSLOSpec{},
			},
			want:    true,
			wantErr: false,
			wantRule: &batchResourceRule{
				enableCFSQuota: true,
			},
		},
		{
			name: "rule not change as true",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				mergedNodeSLO: &slov1alpha1.NodeSLOSpec{},
			},
			want:    false,
			wantErr: false,
			wantRule: &batchResourceRule{
				enableCFSQuota: true,
			},
		},
		{
			name: "rule not change as false",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: false,
				},
			},
			args: args{
				mergedNodeSLO: &slov1alpha1.NodeSLOSpec{
					ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
						Enable:            pointer.Bool(true),
						CPUSuppressPolicy: slov1alpha1.CPUCfsQuotaPolicy,
					},
				},
			},
			want:    false,
			wantErr: false,
			wantRule: &batchResourceRule{
				enableCFSQuota: false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := plugin{
				rule: tt.fields.rule,
			}
			got, err := p.parseRule(tt.args.mergedNodeSLO)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.wantRule, p.rule)
		})
	}
}

func Test_plugin_ruleUpdateCb(t *testing.T) {
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
	type fields struct {
		rule *batchResourceRule
	}
	type args struct {
		pods []*statesinformer.PodMeta
	}
	type want struct {
		cfsQuota string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
		want    []want
	}{
		{
			name:    "rule is nil",
			wantErr: false,
			want: []want{
				{
					cfsQuota: "-1",
				},
			},
		},
		{
			name: "update no Batch pods",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				pods: []*statesinformer.PodMeta{
					{
						Pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								UID: "123456",
							},
							Status: corev1.PodStatus{
								ContainerStatuses: []corev1.ContainerStatus{
									{
										ContainerID: "docker://abcdef",
									},
								},
							},
						},
						CgroupDir: "/kubepods-besteffort.slice/kubepods-besteffort-pod123456.slice/",
					},
				},
			},
			wantErr: false,
			want: []want{
				{
					cfsQuota: "-1",
				},
			},
		},
		{
			name: "update a Batch pods",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: true,
				},
			},
			args: args{
				pods: []*statesinformer.PodMeta{
					{
						Pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								UID: "123456",
								Labels: map[string]string{
									apiext.LabelPodQoS: string(apiext.QoSBE),
								},
								Annotations: map[string]string{
									apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
								},
							},
							Spec: corev1.PodSpec{
								Containers: []corev1.Container{
									{
										Name: "container-0",
										Resources: corev1.ResourceRequirements{
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
								},
							},
							Status: corev1.PodStatus{
								ContainerStatuses: []corev1.ContainerStatus{
									{
										Name:        "container-0",
										ContainerID: "docker://abcdef",
									},
								},
							},
						},
						CgroupDir: "/kubepods-besteffort.slice/kubepods-besteffort-pod123456.slice/",
					},
				},
			},
			wantErr: false,
			want: []want{
				{
					cfsQuota: "50000",
				},
			},
		},
		{
			name: "update a Batch pods for unset cfs quota",
			fields: fields{
				rule: &batchResourceRule{
					enableCFSQuota: false,
				},
			},
			args: args{
				pods: []*statesinformer.PodMeta{
					{
						Pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								UID: "123456",
								Labels: map[string]string{
									apiext.LabelPodQoS: string(apiext.QoSBE),
								},
								Annotations: map[string]string{
									apiext.AnnotationExtendedResourceSpec: string(testSpecBytes),
								},
							},
							Spec: corev1.PodSpec{
								Containers: []corev1.Container{
									{
										Name: "container-0",
										Resources: corev1.ResourceRequirements{
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
								},
							},
							Status: corev1.PodStatus{
								ContainerStatuses: []corev1.ContainerStatus{
									{
										Name:        "container-0",
										ContainerID: "docker://abcdef",
									},
								},
							},
						},
						CgroupDir: "/kubepods-besteffort.slice/kubepods-besteffort-pod123456.slice/",
					},
				},
			},
			wantErr: false,
			want: []want{
				{
					cfsQuota: "-1",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			// init cgroups cpuset file
			for _, podMeta := range tt.args.pods {
				cgroupDir := util.GetPodCgroupDirWithKube(podMeta.CgroupDir)
				helper.WriteCgroupFileContents(cgroupDir, sysutil.CPUCFSQuota, "-1")
				for _, containerStat := range podMeta.Pod.Status.ContainerStatuses {
					cgroupDir, err := util.GetContainerCgroupPathWithKubeByID(podMeta.CgroupDir, containerStat.ContainerID)
					assert.NoError(t, err, "container "+containerStat.Name)
					helper.WriteCgroupFileContents(cgroupDir, sysutil.CPUCFSQuota, "-1")
				}
			}

			p := plugin{
				rule: tt.fields.rule,
			}
			err := p.ruleUpdateCb(tt.args.pods)
			assert.Equal(t, tt.wantErr, err != nil)
			// init cgroups cpuset file
			for i, podMeta := range tt.args.pods {
				w := tt.want[i]
				cgroupDir := util.GetPodCgroupDirWithKube(podMeta.CgroupDir)
				assert.Equal(t, w.cfsQuota, helper.ReadCgroupFileContents(cgroupDir, sysutil.CPUCFSQuota))
				for _, containerStat := range podMeta.Pod.Status.ContainerStatuses {
					cgroupDir, err := util.GetContainerCgroupPathWithKubeByID(podMeta.CgroupDir, containerStat.ContainerID)
					assert.NoError(t, err, "container "+containerStat.Name)
					assert.Equal(t, w.cfsQuota, helper.ReadCgroupFileContents(cgroupDir, sysutil.CPUCFSQuota))
				}
			}
		})
	}
}
