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
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/utils/pointer"

	ext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func initCPUSet(dirWithKube string, value string, helper *system.FileTestUtil) {
	helper.WriteCgroupFileContents(dirWithKube, system.CPUSet, value)
}

func getCPUSet(dirWithKube string, helper *system.FileTestUtil) string {
	return helper.ReadCgroupFileContents(dirWithKube, system.CPUSet)
}

func initCPUQuota(dirWithKube string, value string, helper *system.FileTestUtil) {
	helper.WriteCgroupFileContents(dirWithKube, system.CPUCFSQuota, value)
}

func getCPUQuota(dirWithKube string, helper *system.FileTestUtil) string {
	return helper.ReadCgroupFileContents(dirWithKube, system.CPUCFSQuota)
}

func Test_cpusetPlugin_Register(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		testRule := &cpusetRule{
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
		}
		p := &cpusetPlugin{
			rule: testRule,
		}

		p.Register()
	})
}

func Test_cpusetPlugin_SetContainerCPUSetAndUnsetCFS(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		testRule := &cpusetRule{
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
		}
		p := &cpusetPlugin{
			rule: testRule,
		}

		var testNilProto *protocol.ContainerContext
		err := p.SetContainerCPUSetAndUnsetCFS(testNilProto)
		assert.Error(t, err)

		testProto := &protocol.ContainerContext{
			Request: protocol.ContainerRequest{
				CgroupParent: "kubepods/test-pod/test-container/",
			},
		}
		err = p.SetContainerCPUSetAndUnsetCFS(testProto)
		assert.NoError(t, err)

		testInvalidProto := &protocol.ContainerContext{
			Request: protocol.ContainerRequest{
				CgroupParent: "kubepods/test-pod/test-container/",
				PodAnnotations: map[string]string{
					ext.AnnotationResourceStatus: "bad-format",
				},
			},
		}
		err = p.SetContainerCPUSetAndUnsetCFS(testInvalidProto)
		assert.Error(t, err)
	})
}

func Test_cpusetPlugin_SetContainerCPUSet(t *testing.T) {
	type fields struct {
		rule *cpusetRule
	}
	type args struct {
		podAlloc *ext.ResourceStatus
		proto    protocol.HooksProtocol
	}
	tests := []struct {
		name       string
		fields     fields
		args       args
		wantErr    bool
		wantCPUSet *string
	}{
		{
			name: "set cpu with nil protocol",
			fields: fields{
				rule: nil,
			},
			args: args{
				proto: nil,
			},
			wantErr:    true,
			wantCPUSet: nil,
		},
		{
			name: "set cpu by bad pod allocated format",
			fields: fields{
				rule: nil,
			},
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/test-pod/test-container/",
						PodAnnotations: map[string]string{
							ext.AnnotationResourceStatus: "bad-format",
						},
					},
				},
			},
			wantErr:    true,
			wantCPUSet: nil,
		},
		{
			name: "set cpu by pod allocated",
			fields: fields{
				rule: nil,
			},
			args: args{
				podAlloc: &ext.ResourceStatus{
					CPUSet: "2-4",
				},
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/test-pod/test-container/",
					},
				},
			},
			wantErr:    false,
			wantCPUSet: pointer.StringPtr("2-4"),
		},
		{
			name: "set cpu by pod allocated share pool with nil rule",
			fields: fields{
				rule: nil,
			},
			args: args{
				podAlloc: &ext.ResourceStatus{
					CPUSharedPools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
						},
					},
				},
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/test-pod/test-container/",
					},
				},
			},
			wantErr:    false,
			wantCPUSet: nil,
		},
		{
			name: "set cpu by pod allocated share pool",
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
				podAlloc: &ext.ResourceStatus{
					CPUSharedPools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
						},
					},
				},
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/test-pod/test-container/",
					},
				},
			},
			wantErr:    false,
			wantCPUSet: pointer.StringPtr("0-7"),
		},
		{
			name: "set cpu for origin besteffort pod",
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
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/besteffort/test-pod/test-container/",
					},
				},
			},
			wantErr:    false,
			wantCPUSet: pointer.StringPtr(""),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := system.NewFileTestUtil(t)
			var containerCtx *protocol.ContainerContext

			p := &cpusetPlugin{
				rule: tt.fields.rule,
			}
			if tt.args.proto != nil {
				containerCtx = tt.args.proto.(*protocol.ContainerContext)
				initCPUSet(containerCtx.Request.CgroupParent, "", testHelper)
				if tt.args.podAlloc != nil {
					podAllocJson := util.DumpJSON(tt.args.podAlloc)
					containerCtx.Request.PodAnnotations = map[string]string{
						ext.AnnotationResourceStatus: podAllocJson,
					}
				}
			}

			err := p.SetContainerCPUSet(containerCtx)
			if (err != nil) != tt.wantErr {
				t.Errorf("SetContainerCPUSet() error = %v, wantErr %v", err, tt.wantErr)
			}

			if containerCtx == nil {
				return
			}
			if tt.wantCPUSet == nil {
				assert.Nil(t, containerCtx.Response.Resources.CPUSet, "cpuset value should be nil")
			} else {
				containerCtx.ReconcilerDone()
				assert.Equal(t, *tt.wantCPUSet, *containerCtx.Response.Resources.CPUSet, "container cpuset should be equal")
				gotCPUSet := getCPUSet(containerCtx.Request.CgroupParent, testHelper)
				assert.Equal(t, *tt.wantCPUSet, gotCPUSet, "container cpuset should be equal")
			}
		})
	}
}

func TestUnsetPodCPUQuota(t *testing.T) {
	type args struct {
		podAlloc *ext.ResourceStatus
		proto    protocol.HooksProtocol
	}
	tests := []struct {
		name         string
		args         args
		wantErr      bool
		wantCPUQuota *int64
	}{
		{
			name: "not change cfs quota with nil protocol",
			args: args{
				proto: nil,
			},
			wantErr:      true,
			wantCPUQuota: nil,
		},
		{
			name: "not change cfs quota by bad pod allocated format",
			args: args{
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						Labels: map[string]string{
							ext.LabelPodQoS: string(ext.QoSLS),
						},
						Annotations: map[string]string{
							ext.AnnotationResourceStatus: "bad-format",
						},
						CgroupParent: "kubepods/pod-guaranteed-test-uid/",
					},
					Response: protocol.PodResponse{},
				},
			},
			wantErr:      true,
			wantCPUQuota: nil,
		},
		{
			name: "set cfs quota by pod allocated",
			args: args{
				podAlloc: &ext.ResourceStatus{
					CPUSet: "2-4",
				},
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						Labels: map[string]string{
							ext.LabelPodQoS: string(ext.QoSLS),
						},
						CgroupParent: "kubepods/pod-guaranteed-test-uid/",
					},
					Response: protocol.PodResponse{},
				},
			},
			wantErr:      false,
			wantCPUQuota: pointer.Int64Ptr(-1),
		},
		{
			name: "not change cfs quota by pod allocated share pool",
			args: args{
				podAlloc: &ext.ResourceStatus{
					CPUSharedPools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
						},
					},
				},
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						Labels: map[string]string{
							ext.LabelPodQoS: string(ext.QoSLS),
						},
						CgroupParent: "kubepods/pod-guaranteed-test-uid/",
					},
					Response: protocol.PodResponse{},
				},
			},
			wantErr:      false,
			wantCPUQuota: nil,
		},
		{
			name: "not change cfs quota for origin besteffort pod",
			args: args{
				proto: &protocol.PodContext{
					Request: protocol.PodRequest{
						CgroupParent: "kubepods/besteffort/pod-besteffort-test-uid/",
					},
					Response: protocol.PodResponse{},
				},
			},
			wantErr:      false,
			wantCPUQuota: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := system.NewFileTestUtil(t)
			var podCtx *protocol.PodContext

			if tt.args.proto != nil {
				podCtx = tt.args.proto.(*protocol.PodContext)
				initCPUQuota(podCtx.Request.CgroupParent, "", testHelper)
				if tt.args.podAlloc != nil {
					podAllocJson := util.DumpJSON(tt.args.podAlloc)
					podCtx.Request.Annotations = map[string]string{
						ext.AnnotationResourceStatus: podAllocJson,
					}
				}
			}

			err := UnsetPodCPUQuota(podCtx)
			assert.Equal(t, err != nil, tt.wantErr)

			if podCtx == nil {
				return
			}
			if tt.wantCPUQuota == nil {
				assert.Nil(t, podCtx.Response.Resources.CFSQuota, "cfs quota value should be nil")
			} else {
				podCtx.ReconcilerDone()
				assert.Equal(t, *tt.wantCPUQuota, *podCtx.Response.Resources.CFSQuota, "pod cfs quota should be equal")
				gotCPUQuota := getCPUQuota(podCtx.Request.CgroupParent, testHelper)
				gotCPUQuotaStr, err := strconv.ParseInt(gotCPUQuota, 10, 64)
				assert.NoError(t, err)
				assert.Equal(t, *tt.wantCPUQuota, gotCPUQuotaStr, "pod cfs quota should be equal")
			}
		})
	}
}

func TestUnsetContainerCPUQuota(t *testing.T) {
	type args struct {
		podAlloc *ext.ResourceStatus
		proto    protocol.HooksProtocol
	}
	tests := []struct {
		name         string
		args         args
		wantErr      bool
		wantCPUQuota *int64
	}{
		{
			name: "not change cfs quota with nil protocol",
			args: args{
				proto: nil,
			},
			wantErr:      true,
			wantCPUQuota: nil,
		},
		{
			name: "not change cfs quota by bad pod allocated format",
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/test-pod/test-container/",
						PodAnnotations: map[string]string{
							ext.AnnotationResourceStatus: "bad-format",
						},
					},
				},
			},
			wantErr:      true,
			wantCPUQuota: nil,
		},
		{
			name: "set cfs quota by pod allocated",
			args: args{
				podAlloc: &ext.ResourceStatus{
					CPUSet: "2-4",
				},
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/test-pod/test-container/",
					},
				},
			},
			wantErr:      false,
			wantCPUQuota: pointer.Int64Ptr(-1),
		},
		{
			name: "not change cfs quota by pod allocated share pool",
			args: args{
				podAlloc: &ext.ResourceStatus{
					CPUSharedPools: []ext.CPUSharedPool{
						{
							Socket: 0,
							Node:   0,
						},
					},
				},
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/test-pod/test-container/",
					},
				},
			},
			wantErr:      false,
			wantCPUQuota: nil,
		},
		{
			name: "not change cfs quota for origin besteffort pod",
			args: args{
				proto: &protocol.ContainerContext{
					Request: protocol.ContainerRequest{
						CgroupParent: "kubepods/besteffort/test-pod/test-container/",
					},
				},
			},
			wantErr:      false,
			wantCPUQuota: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := system.NewFileTestUtil(t)
			var containerCtx *protocol.ContainerContext

			if tt.args.proto != nil {
				containerCtx = tt.args.proto.(*protocol.ContainerContext)
				initCPUQuota(containerCtx.Request.CgroupParent, "", testHelper)
				if tt.args.podAlloc != nil {
					podAllocJson := util.DumpJSON(tt.args.podAlloc)
					containerCtx.Request.PodAnnotations = map[string]string{
						ext.AnnotationResourceStatus: podAllocJson,
					}
				}
			}

			err := UnsetContainerCPUQuota(containerCtx)
			assert.Equal(t, err != nil, tt.wantErr)

			if containerCtx == nil {
				return
			}
			if tt.wantCPUQuota == nil {
				assert.Nil(t, containerCtx.Response.Resources.CFSQuota, "cfs quota value should be nil")
			} else {
				containerCtx.ReconcilerDone()
				assert.Equal(t, *tt.wantCPUQuota, *containerCtx.Response.Resources.CFSQuota, "container cfs quota should be equal")
				gotCPUQuota := getCPUQuota(containerCtx.Request.CgroupParent, testHelper)
				gotCPUQuotaStr, err := strconv.ParseInt(gotCPUQuota, 10, 64)
				assert.NoError(t, err)
				assert.Equal(t, *tt.wantCPUQuota, gotCPUQuotaStr, "container cfs quota should be equal")
			}
		})
	}
}
