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

package statesinformer

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func Test_mergeNodeSLOSpec(t *testing.T) {
	testingCustomNodeSLOSpec := slov1alpha1.NodeSLOSpec{
		ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
			CPUSuppressThresholdPercent: pointer.Int64Ptr(80),
		},
		ResourceQOSStrategy: &slov1alpha1.ResourceQOSStrategy{
			LSRClass: util.NoneResourceQOS(apiext.QoSLSR),
			LSClass:  util.NoneResourceQOS(apiext.QoSLS),
			BEClass: &slov1alpha1.ResourceQOS{
				CPUQOS: &slov1alpha1.CPUQOSCfg{
					Enable: pointer.BoolPtr(true),
				},
				MemoryQOS: &slov1alpha1.MemoryQOSCfg{
					Enable: pointer.BoolPtr(true),
				},
				ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
					Enable: pointer.BoolPtr(true),
					ResctrlQOS: slov1alpha1.ResctrlQOS{
						CATRangeEndPercent: pointer.Int64Ptr(50),
					},
				},
			},
		},
		CPUBurstStrategy: &slov1alpha1.CPUBurstStrategy{
			CPUBurstConfig:            slov1alpha1.CPUBurstConfig{},
			SharePoolThresholdPercent: nil,
		},
	}
	testingMergedNodeSLOSpec := util.DefaultNodeSLOSpecConfig()
	mergedInterface, err := util.MergeCfg(&testingMergedNodeSLOSpec, &testingCustomNodeSLOSpec)
	assert.NoError(t, err)
	testingMergedNodeSLOSpec = *mergedInterface.(*slov1alpha1.NodeSLOSpec)
	type args struct {
		nodeSLO *slov1alpha1.NodeSLO
	}
	type field struct {
		nodeSLO *slov1alpha1.NodeSLO
	}
	tests := []struct {
		name  string
		args  args
		field field
		want  *slov1alpha1.NodeSLO
	}{
		{
			name: "skip the merge if the old one is nil",
			args: args{
				nodeSLO: &slov1alpha1.NodeSLO{},
			},
			field: field{nodeSLO: nil},
			want:  nil,
		},
		{
			name: "skip the merge if the new one is nil",
			field: field{
				nodeSLO: &slov1alpha1.NodeSLO{},
			},
			want: &slov1alpha1.NodeSLO{},
		},
		{
			name: "use default and do not panic if the new is nil",
			field: field{
				nodeSLO: &slov1alpha1.NodeSLO{
					Spec: util.DefaultNodeSLOSpecConfig(),
				},
			},
			want: &slov1alpha1.NodeSLO{
				Spec: util.DefaultNodeSLOSpecConfig(),
			},
		},
		{
			name: "merge with the default",
			args: args{
				nodeSLO: &slov1alpha1.NodeSLO{
					Spec: testingCustomNodeSLOSpec,
				},
			},
			field: field{
				nodeSLO: &slov1alpha1.NodeSLO{
					Spec: slov1alpha1.NodeSLOSpec{
						ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
							CPUSuppressThresholdPercent: pointer.Int64Ptr(100),
							MemoryEvictThresholdPercent: pointer.Int64Ptr(100),
						},
						ResourceQOSStrategy: &slov1alpha1.ResourceQOSStrategy{
							LSRClass: &slov1alpha1.ResourceQOS{
								ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
									ResctrlQOS: slov1alpha1.ResctrlQOS{
										CATRangeStartPercent: pointer.Int64Ptr(0),
										CATRangeEndPercent:   pointer.Int64Ptr(100),
									},
								},
							},
							LSClass: &slov1alpha1.ResourceQOS{
								ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
									ResctrlQOS: slov1alpha1.ResctrlQOS{
										CATRangeStartPercent: pointer.Int64Ptr(0),
										CATRangeEndPercent:   pointer.Int64Ptr(100),
									},
								},
							},
							BEClass: &slov1alpha1.ResourceQOS{
								ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
									ResctrlQOS: slov1alpha1.ResctrlQOS{
										CATRangeStartPercent: pointer.Int64Ptr(0),
										CATRangeEndPercent:   pointer.Int64Ptr(40),
									},
								},
							},
						},
					},
				},
			},
			want: &slov1alpha1.NodeSLO{
				Spec: testingMergedNodeSLOSpec,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := nodeSLOInformer{nodeSLO: tt.field.nodeSLO}
			r.mergeNodeSLOSpec(tt.args.nodeSLO)
			assert.Equal(t, tt.want, r.nodeSLO)
		})
	}
}

func Test_createNodeSLO(t *testing.T) {
	testingNewNodeSLO := &slov1alpha1.NodeSLO{
		Spec: util.DefaultNodeSLOSpecConfig(),
	}
	testingNewNodeSLO.Spec.ResourceUsedThresholdWithBE = &slov1alpha1.ResourceThresholdStrategy{
		Enable:                      pointer.BoolPtr(true),
		CPUSuppressThresholdPercent: pointer.Int64Ptr(80),
	}

	testingNewNodeSLO.Spec.ResourceQOSStrategy.BEClass = &slov1alpha1.ResourceQOS{
		ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
			Enable: pointer.BoolPtr(true),
			ResctrlQOS: slov1alpha1.ResctrlQOS{
				CATRangeStartPercent: pointer.Int64Ptr(0),
				CATRangeEndPercent:   pointer.Int64Ptr(20),
			},
		},
	}

	testingCreatedNodeSLO := &slov1alpha1.NodeSLO{
		Spec: util.DefaultNodeSLOSpecConfig(),
	}
	testingCreatedNodeSLO.Spec.ResourceUsedThresholdWithBE.Enable = pointer.BoolPtr(true)
	testingCreatedNodeSLO.Spec.ResourceUsedThresholdWithBE.CPUSuppressThresholdPercent = pointer.Int64Ptr(80)
	testingCreatedNodeSLO.Spec.ResourceQOSStrategy.LSRClass = util.NoneResourceQOS(apiext.QoSLSR)
	testingCreatedNodeSLO.Spec.ResourceQOSStrategy.LSClass = util.NoneResourceQOS(apiext.QoSLS)
	testingCreatedNodeSLO.Spec.ResourceQOSStrategy.BEClass = util.NoneResourceQOS(apiext.QoSBE)
	testingCreatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.ResctrlQOS.Enable = pointer.BoolPtr(true)
	testingCreatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.ResctrlQOS.CATRangeStartPercent = pointer.Int64Ptr(0)
	testingCreatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.ResctrlQOS.CATRangeEndPercent = pointer.Int64Ptr(20)

	r := nodeSLOInformer{
		nodeSLO:        nil,
		callbackRunner: NewCallbackRunner(),
	}

	r.updateNodeSLOSpec(testingNewNodeSLO)
	assert.Equal(t, testingCreatedNodeSLO, r.nodeSLO)
}

func Test_updateNodeSLOSpec(t *testing.T) {
	testingNewNodeSLO := &slov1alpha1.NodeSLO{
		Spec: slov1alpha1.NodeSLOSpec{
			ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				CPUSuppressThresholdPercent: pointer.Int64Ptr(80),
			},
			ResourceQOSStrategy: &slov1alpha1.ResourceQOSStrategy{
				BEClass: &slov1alpha1.ResourceQOS{
					ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
						Enable: pointer.BoolPtr(true),
						ResctrlQOS: slov1alpha1.ResctrlQOS{
							CATRangeStartPercent: pointer.Int64Ptr(0),
							CATRangeEndPercent:   pointer.Int64Ptr(20),
						},
					},
				},
			},
		},
	}
	testingUpdatedNodeSLO := &slov1alpha1.NodeSLO{
		Spec: util.DefaultNodeSLOSpecConfig(),
	}
	testingUpdatedNodeSLO.Spec.ResourceUsedThresholdWithBE.Enable = pointer.BoolPtr(true)
	testingUpdatedNodeSLO.Spec.ResourceUsedThresholdWithBE.CPUSuppressThresholdPercent = pointer.Int64Ptr(80)
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.LSRClass.CPUQOS.CPUQOS = *util.NoneCPUQOS()
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.LSRClass.MemoryQOS.MemoryQOS = *util.NoneMemoryQOS()
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.LSRClass.ResctrlQOS.ResctrlQOS = *util.NoneResctrlQOS()

	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.LSClass.CPUQOS.CPUQOS = *util.NoneCPUQOS()
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.LSClass.MemoryQOS.MemoryQOS = *util.NoneMemoryQOS()
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.LSClass.ResctrlQOS.ResctrlQOS = *util.NoneResctrlQOS()

	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.CPUQOS.CPUQOS = *util.NoneCPUQOS()
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.MemoryQOS.MemoryQOS = *util.NoneMemoryQOS()
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.ResctrlQOS.Enable = pointer.BoolPtr(true)
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.ResctrlQOS.CATRangeStartPercent = pointer.Int64Ptr(0)
	testingUpdatedNodeSLO.Spec.ResourceQOSStrategy.BEClass.ResctrlQOS.CATRangeEndPercent = pointer.Int64Ptr(20)

	r := nodeSLOInformer{
		nodeSLO: &slov1alpha1.NodeSLO{
			Spec: slov1alpha1.NodeSLOSpec{
				ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
					CPUSuppressThresholdPercent: pointer.Int64Ptr(90),
					MemoryEvictThresholdPercent: pointer.Int64Ptr(90),
				},
			},
		},
		callbackRunner: NewCallbackRunner(),
	}

	r.updateNodeSLOSpec(testingNewNodeSLO)
	assert.Equal(t, testingUpdatedNodeSLO, r.nodeSLO)
}

func Test_mergeSLOSpecResourceUsedThresholdWithBE(t *testing.T) {
	testingDefaultSpec := util.DefaultResourceThresholdStrategy()
	testingNewSpec := &slov1alpha1.ResourceThresholdStrategy{
		Enable:                      pointer.BoolPtr(true),
		CPUSuppressThresholdPercent: pointer.Int64Ptr(80),
		MemoryEvictThresholdPercent: pointer.Int64Ptr(75),
	}
	testingNewSpec1 := &slov1alpha1.ResourceThresholdStrategy{
		Enable:                      pointer.BoolPtr(true),
		CPUSuppressThresholdPercent: pointer.Int64Ptr(80),
	}
	testingMergedSpec := &slov1alpha1.ResourceThresholdStrategy{
		Enable:                      pointer.BoolPtr(true),
		CPUSuppressThresholdPercent: pointer.Int64Ptr(80),
		MemoryEvictThresholdPercent: pointer.Int64Ptr(70),
		CPUSuppressPolicy:           slov1alpha1.CPUSetPolicy,
	}
	type args struct {
		defaultSpec *slov1alpha1.ResourceThresholdStrategy
		newSpec     *slov1alpha1.ResourceThresholdStrategy
	}
	tests := []struct {
		name string
		args args
		want *slov1alpha1.ResourceThresholdStrategy
	}{
		{
			name: "both empty",
			args: args{
				defaultSpec: &slov1alpha1.ResourceThresholdStrategy{},
				newSpec:     &slov1alpha1.ResourceThresholdStrategy{},
			},
			want: &slov1alpha1.ResourceThresholdStrategy{},
		},
		{
			name: "totally use new",
			args: args{
				defaultSpec: &slov1alpha1.ResourceThresholdStrategy{},
				newSpec:     testingNewSpec,
			},
			want: testingNewSpec,
		},
		{
			name: "totally use new 1",
			args: args{
				defaultSpec: testingDefaultSpec,
				newSpec:     testingNewSpec,
			},
			want: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				CPUSuppressThresholdPercent: pointer.Int64Ptr(80),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(75),
				CPUSuppressPolicy:           slov1alpha1.CPUSetPolicy,
			},
		},
		{
			name: "partially use new, merging with the default",
			args: args{
				defaultSpec: testingDefaultSpec,
				newSpec:     testingNewSpec1,
			},
			want: testingMergedSpec,
		},
		{
			name: "new overwrite a nil",
			args: args{
				defaultSpec: testingDefaultSpec,
			},
			want: testingDefaultSpec,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := mergeSLOSpecResourceUsedThresholdWithBE(tt.args.defaultSpec, tt.args.newSpec)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_mergeSLOSpecResourceQOSStrategy(t *testing.T) {
	testingDefaultSpec := util.DefaultResourceQOSStrategy()

	testingNewSpec := testingDefaultSpec.DeepCopy()
	testingNewSpec.BEClass.MemoryQOS.WmarkRatio = pointer.Int64Ptr(0)

	testingNewSpec1 := &slov1alpha1.ResourceQOSStrategy{
		BEClass: &slov1alpha1.ResourceQOS{
			MemoryQOS: &slov1alpha1.MemoryQOSCfg{
				Enable: pointer.BoolPtr(true),
				MemoryQOS: slov1alpha1.MemoryQOS{
					WmarkRatio: pointer.Int64Ptr(90),
				},
			},
		},
	}

	testingMergedSpec := testingDefaultSpec.DeepCopy()
	testingMergedSpec.BEClass.MemoryQOS.Enable = pointer.BoolPtr(true)
	testingMergedSpec.BEClass.MemoryQOS.WmarkRatio = pointer.Int64Ptr(90)

	type args struct {
		defaultSpec *slov1alpha1.ResourceQOSStrategy
		newSpec     *slov1alpha1.ResourceQOSStrategy
	}
	tests := []struct {
		name string
		args args
		want *slov1alpha1.ResourceQOSStrategy
	}{
		{
			name: "both empty",
			args: args{
				defaultSpec: &slov1alpha1.ResourceQOSStrategy{},
				newSpec:     &slov1alpha1.ResourceQOSStrategy{},
			},
			want: &slov1alpha1.ResourceQOSStrategy{},
		},
		{
			name: "totally use new",
			args: args{
				defaultSpec: &slov1alpha1.ResourceQOSStrategy{},
				newSpec:     testingNewSpec,
			},
			want: testingNewSpec,
		},
		{
			name: "totally use new 1",
			args: args{
				defaultSpec: testingDefaultSpec,
				newSpec:     testingNewSpec,
			},
			want: testingNewSpec,
		},
		{
			name: "partially use new, merging with the default",
			args: args{
				defaultSpec: testingDefaultSpec,
				newSpec:     testingNewSpec1,
			},
			want: testingMergedSpec,
		},
		{
			name: "new overwrite a nil",
			args: args{
				defaultSpec: testingDefaultSpec,
			},
			want: testingDefaultSpec,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := mergeSLOSpecResourceQOSStrategy(tt.args.defaultSpec, tt.args.newSpec)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_mergeNoneResourceQOSIfDisabled(t *testing.T) {
	testDefault := util.DefaultResourceQOSStrategy()
	testAllNone := util.NoneResourceQOSStrategy()

	testLSMemQOSEnabled := testDefault.DeepCopy()
	testLSMemQOSEnabled.LSClass.MemoryQOS.Enable = pointer.BoolPtr(true)
	testLSMemQOSEnabledResult := util.NoneResourceQOSStrategy()
	testLSMemQOSEnabledResult.LSClass.MemoryQOS.Enable = pointer.BoolPtr(true)
	testLSMemQOSEnabledResult.LSClass.MemoryQOS.MemoryQOS = *util.DefaultMemoryQOS(apiext.QoSLS)

	type args struct {
		nodeCfg *slov1alpha1.NodeSLO
	}
	tests := []struct {
		name string
		args args
		want *slov1alpha1.ResourceQOSStrategy
	}{
		{
			name: "all disabled",
			args: args{
				nodeCfg: &slov1alpha1.NodeSLO{
					Spec: slov1alpha1.NodeSLOSpec{
						ResourceQOSStrategy: testDefault,
					},
				},
			},
			want: testAllNone,
		},
		{
			name: "only ls memory qos enabled",
			args: args{
				nodeCfg: &slov1alpha1.NodeSLO{
					Spec: slov1alpha1.NodeSLOSpec{
						ResourceQOSStrategy: testLSMemQOSEnabled,
					},
				},
			},
			want: testLSMemQOSEnabledResult,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mergeNoneResourceQOSIfDisabled(tt.args.nodeCfg.Spec.ResourceQOSStrategy)
			assert.Equal(t, tt.want, tt.args.nodeCfg.Spec.ResourceQOSStrategy)
		})
	}
}
