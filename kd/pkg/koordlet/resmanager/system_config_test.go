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
	"strconv"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/utils/pointer"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	mock_statesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
	"github.com/koordinator-sh/koordinator/pkg/util/cache"
)

func Test_systemConfig_reconcile(t *testing.T) {
	defaultStrategy := util.DefaultSystemStrategy()
	nodeValidMemory := int64(512) * 1024 * 1024 * 1024
	initNode := getNode("80", strconv.FormatInt(nodeValidMemory, 10))
	tests := []struct {
		name         string
		initStrategy *slov1alpha1.SystemStrategy
		newStrategy  *slov1alpha1.SystemStrategy
		node         *corev1.Node
		expect       map[sysutil.Resource]string
	}{
		{
			name:         "testNodeNil",
			initStrategy: defaultStrategy,
			newStrategy:  &slov1alpha1.SystemStrategy{},
			expect: map[sysutil.Resource]string{
				sysutil.MinFreeKbytes:        strconv.FormatInt(nodeValidMemory/1024**defaultStrategy.MinFreeKbytesFactor/10000, 10),
				sysutil.WatermarkScaleFactor: strconv.FormatInt(*defaultStrategy.WatermarkScaleFactor, 10),
				sysutil.MemcgReapBackGround:  strconv.FormatInt(*defaultStrategy.MemcgReapBackGround, 10),
			},
		},
		{
			name:         "testNodeMemoryInvalid",
			initStrategy: defaultStrategy,
			newStrategy:  &slov1alpha1.SystemStrategy{},
			node:         getNode("80", "0"),
			expect: map[sysutil.Resource]string{
				sysutil.MinFreeKbytes:        strconv.FormatInt(nodeValidMemory/1024**defaultStrategy.MinFreeKbytesFactor/10000, 10),
				sysutil.WatermarkScaleFactor: strconv.FormatInt(*defaultStrategy.WatermarkScaleFactor, 10),
				sysutil.MemcgReapBackGround:  strconv.FormatInt(*defaultStrategy.MemcgReapBackGround, 10),
			},
		},
		{
			name:         "testNil",
			initStrategy: defaultStrategy,
			newStrategy:  &slov1alpha1.SystemStrategy{},
			node:         getNode("80", strconv.FormatInt(nodeValidMemory, 10)),
			expect: map[sysutil.Resource]string{
				sysutil.MinFreeKbytes:        strconv.FormatInt(nodeValidMemory/1024**defaultStrategy.MinFreeKbytesFactor/10000, 10),
				sysutil.WatermarkScaleFactor: strconv.FormatInt(*defaultStrategy.WatermarkScaleFactor, 10),
				sysutil.MemcgReapBackGround:  strconv.FormatInt(*defaultStrategy.MemcgReapBackGround, 10),
			},
		},
		{
			name:         "testInvalid",
			initStrategy: defaultStrategy,
			node:         getNode("80", strconv.FormatInt(nodeValidMemory, 10)),
			newStrategy:  &slov1alpha1.SystemStrategy{MinFreeKbytesFactor: pointer.Int64Ptr(-1), WatermarkScaleFactor: pointer.Int64Ptr(-1), MemcgReapBackGround: pointer.Int64Ptr(-1)},
			expect: map[sysutil.Resource]string{
				sysutil.MinFreeKbytes:        strconv.FormatInt(nodeValidMemory/1024**defaultStrategy.MinFreeKbytesFactor/10000, 10),
				sysutil.WatermarkScaleFactor: strconv.FormatInt(*defaultStrategy.WatermarkScaleFactor, 10),
				sysutil.MemcgReapBackGround:  strconv.FormatInt(*defaultStrategy.MemcgReapBackGround, 10),
			},
		},
		{
			name:         "testTooSmall",
			initStrategy: defaultStrategy,
			node:         getNode("80", strconv.FormatInt(nodeValidMemory, 10)),
			newStrategy:  &slov1alpha1.SystemStrategy{MinFreeKbytesFactor: pointer.Int64Ptr(0), WatermarkScaleFactor: pointer.Int64Ptr(5), MemcgReapBackGround: pointer.Int64Ptr(-1)},
			expect: map[sysutil.Resource]string{
				sysutil.MinFreeKbytes:        strconv.FormatInt(nodeValidMemory/1024**defaultStrategy.MinFreeKbytesFactor/10000, 10),
				sysutil.WatermarkScaleFactor: "150",
				sysutil.MemcgReapBackGround:  "0",
			},
		},
		{
			name:         "testValid",
			initStrategy: defaultStrategy,
			node:         getNode("80", strconv.FormatInt(nodeValidMemory, 10)),
			newStrategy:  &slov1alpha1.SystemStrategy{MinFreeKbytesFactor: pointer.Int64Ptr(88), WatermarkScaleFactor: pointer.Int64Ptr(99), MemcgReapBackGround: pointer.Int64Ptr(1)},
			expect: map[sysutil.Resource]string{
				sysutil.MinFreeKbytes:        strconv.FormatInt(nodeValidMemory/1024*88/10000, 10),
				sysutil.WatermarkScaleFactor: "99",
				sysutil.MemcgReapBackGround:  "1",
			},
		},
		{
			name:         "testToolarge",
			initStrategy: defaultStrategy,
			node:         getNode("80", strconv.FormatInt(nodeValidMemory, 10)),
			newStrategy:  &slov1alpha1.SystemStrategy{MinFreeKbytesFactor: pointer.Int64Ptr(400), WatermarkScaleFactor: pointer.Int64Ptr(500), MemcgReapBackGround: pointer.Int64Ptr(2)},
			expect: map[sysutil.Resource]string{
				sysutil.MinFreeKbytes:        strconv.FormatInt(nodeValidMemory/1024**defaultStrategy.MinFreeKbytesFactor/10000, 10),
				sysutil.WatermarkScaleFactor: "150",
				sysutil.MemcgReapBackGround:  "0",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			prepareFiles(helper, tt.initStrategy, initNode.Status.Capacity.Memory().Value())

			//prepareData: metaService pods node
			ctl := gomock.NewController(t)
			defer ctl.Finish()
			mockstatesinformer := mock_statesinformer.NewMockStatesInformer(ctl)
			mockstatesinformer.EXPECT().GetNode().Return(tt.node).AnyTimes()
			mockstatesinformer.EXPECT().GetNodeSLO().Return(getNodeSLOBySystemStrategy(tt.newStrategy)).AnyTimes()

			resmanager := &resmanager{
				statesInformer: mockstatesinformer,
				config:         NewDefaultConfig(),
			}

			reconcile := &SystemConfig{
				resmanager: resmanager,
				executor: &resourceexecutor.ResourceUpdateExecutorImpl{
					Config:        resourceexecutor.NewDefaultConfig(),
					ResourceCache: cache.NewCacheDefault(),
				},
			}
			stopCh := make(chan struct{})
			defer func() {
				close(stopCh)
			}()
			reconcile.executor.Run(stopCh)

			reconcile.reconcile()
			for file, expectValue := range tt.expect {
				got := helper.ReadFileContents(file.Path(""))
				assert.Equal(t, expectValue, got, file.Path(""))
			}
		})
	}
}

func prepareFiles(helper *sysutil.FileTestUtil, stragegy *slov1alpha1.SystemStrategy, nodeMemory int64) {
	helper.CreateFile(sysutil.MinFreeKbytes.Path(""))
	helper.WriteFileContents(sysutil.MinFreeKbytes.Path(""), strconv.FormatInt(*stragegy.MinFreeKbytesFactor*nodeMemory/1024/10000, 10))
	helper.CreateFile(sysutil.WatermarkScaleFactor.Path(""))
	helper.WriteFileContents(sysutil.WatermarkScaleFactor.Path(""), strconv.FormatInt(*stragegy.WatermarkScaleFactor, 10))
	helper.CreateFile(sysutil.MemcgReapBackGround.Path(""))
	helper.WriteFileContents(sysutil.MemcgReapBackGround.Path(""), strconv.FormatInt(*stragegy.MemcgReapBackGround, 10))

}

func getNodeSLOBySystemStrategy(stragegy *slov1alpha1.SystemStrategy) *slov1alpha1.NodeSLO {
	return &slov1alpha1.NodeSLO{
		Spec: slov1alpha1.NodeSLOSpec{
			SystemStrategy: stragegy,
		},
	}
}
