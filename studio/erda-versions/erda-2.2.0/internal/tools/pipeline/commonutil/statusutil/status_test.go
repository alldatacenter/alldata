// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package statusutil

import (
	"fmt"
	"testing"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func TestCalculatePipelineStatusV2(t *testing.T) {
	var datas = []struct {
		tasks        []*spec.PipelineTask
		expectStatus apistructs.PipelineStatus
		desc         string
	}{
		{
			tasks: []*spec.PipelineTask{
				{
					Status: apistructs.PipelineStatusAnalyzed,
				},
			},
			expectStatus: apistructs.PipelineStatusAnalyzed,
			desc:         "all analyzed -> analyzed",
		},
		{
			tasks: []*spec.PipelineTask{
				{
					Status: apistructs.PipelineStatusAnalyzed,
				},
				{
					Status: apistructs.PipelineStatusRunning,
				},
			},
			expectStatus: apistructs.PipelineStatusRunning,
			desc:         "at least one running -> running",
		},
		{
			tasks: []*spec.PipelineTask{
				{
					Status: apistructs.PipelineStatusAnalyzed,
				},
				{
					Status: apistructs.PipelineStatusAnalyzeFailed,
				},
			},
			expectStatus: apistructs.PipelineStatusRunning,
			desc:         "analyzed + analyzedFailed -> running",
		},
		{
			tasks: []*spec.PipelineTask{
				{
					Status: apistructs.PipelineStatusAnalyzed,
				},
				{
					Status: apistructs.PipelineStatusBorn,
				},
				{
					Status: apistructs.PipelineStatusSuccess,
				},
			},
			expectStatus: apistructs.PipelineStatusRunning,
			desc:         "not all done -> running",
		},
		{
			tasks: []*spec.PipelineTask{
				{
					Status: apistructs.PipelineStatusAnalyzed,
				},
				{
					Status: apistructs.PipelineStatusPaused,
				},
				{
					Status: apistructs.PipelineStatusSuccess,
				},
				{
					Status: apistructs.PipelineStatusFailed,
				},
			},
			expectStatus: apistructs.PipelineStatusPaused,
			desc:         "paused + analyzed + endStatus -> running",
		},
		{
			tasks: []*spec.PipelineTask{
				{
					Status: apistructs.PipelineStatusBorn,
				},
				{
					Status: apistructs.PipelineStatusPaused,
				},
				{
					Status: apistructs.PipelineStatusQueue,
				},
			},
			expectStatus: apistructs.PipelineStatusRunning,
			desc:         "queue -> running",
		},
		{
			tasks: []*spec.PipelineTask{
				{
					Status: apistructs.PipelineStatusFailed,
				},
				{
					Status: apistructs.PipelineStatusStopByUser,
				},
				{
					Status: apistructs.PipelineStatusSuccess,
				},
			},
			expectStatus: apistructs.PipelineStatusStopByUser,
			desc:         "all done, at least one stopByUser -> stopByUser",
		},
	}

	for index, v := range datas {
		pipelineStatus := CalculatePipelineStatusV2(v.tasks)
		if pipelineStatus != v.expectStatus {
			fmt.Println(fmt.Sprintf("[%v] desc %s. %s not equal to %s", index, v.desc, pipelineStatus, v.expectStatus))
			t.Fail()
		}
	}
}

func TestCalculatePipelineTaskAllDone(t *testing.T) {
	var datas = []struct {
		tasks         []*spec.PipelineTask
		expectAllDone bool
		desc          string
	}{
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusAnalyzed},
			},
			expectAllDone: false,
			desc:          "展示分析完成",
		},
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusAnalyzed},
				{Status: apistructs.PipelineStatusRunning},
			},
			expectAllDone: false,
			desc:          "展示运行中",
		},
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusAnalyzed},
				{Status: apistructs.PipelineStatusAnalyzeFailed},
			},
			expectAllDone: false,
			desc:          "有一个失败的状态，应该展示失败",
		},
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusSuccess},
				{Status: apistructs.PipelineStatusAnalyzeFailed},
				{Status: apistructs.PipelineStatusFailed},
			},
			expectAllDone: true,
			desc:          "所有都是终态",
		},
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusAnalyzed},
				{Status: apistructs.PipelineStatusBorn},
				{Status: apistructs.PipelineStatusSuccess},
			},
			expectAllDone: false,
			desc:          "有成功有运行中，应该展示运行中",
		},
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusBorn},
				{Status: apistructs.PipelineStatusPaused},
				{Status: apistructs.PipelineStatusSuccess},
			},
			expectAllDone: false,
			desc:          "有暂停有准备中，展示暂停",
		},
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusBorn},
				{Status: apistructs.PipelineStatusPaused},
				{Status: apistructs.PipelineStatusQueue},
			},
			expectAllDone: false,
			desc:          "有排队有等待有准备中，展示运行中",
		},
		{
			tasks: []*spec.PipelineTask{
				{Status: apistructs.PipelineStatusSuccess},
				{Status: apistructs.PipelineStatusFailed},
				{Status: apistructs.PipelineStatusDisabled},
			},
			expectAllDone: true,
			desc:          "成功 + 失败 + 禁用 = 终态",
		},
	}

	for index, v := range datas {
		allDone := CalculatePipelineTaskAllDone(v.tasks)
		if allDone != v.expectAllDone {
			fmt.Println(fmt.Sprintf("[%d] desc %s. %v (actual) not equal to %v (expected)", index, v.desc, allDone, v.expectAllDone))
			t.Fail()
		}
	}
}
