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

package testplan_after

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	testplanpb "github.com/erda-project/erda-proto-go/core/dop/autotest/testplan/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func Test_filterPipelineTask(t *testing.T) {
	id1 := uint64(1)
	id2 := uint64(2)

	alltasks := []*spec.PipelineTask{
		{
			Type: apistructs.ActionTypeAPITest,
			Extra: spec.PipelineTaskExtra{
				Action: pipelineyml.Action{
					Version: "2.0",
				},
			},
		},
		{
			Type: apistructs.ActionTypeAPITest,
			Extra: spec.PipelineTaskExtra{
				Action: pipelineyml.Action{
					Version: "1.0",
				},
			},
		},
		{
			Type:              apistructs.ActionTypeSnippet,
			SnippetPipelineID: &id1,
		},
		{
			Type:              apistructs.ActionTypeSnippet,
			SnippetPipelineID: &id2,
		},
		{
			Type:              apistructs.ActionTypeCustomScript,
			SnippetPipelineID: &id1,
		},
	}
	want1 := alltasks[0:1]
	want2 := []uint64{1, 2}
	list1, list2 := filterPipelineTask(alltasks)
	assert.Equal(t, list1, want1)
	assert.Equal(t, list2, want2)
}

func Test_convertReport(t *testing.T) {
	var want = ApiReportMeta{
		ApiTotalNum:   2,
		ApiSuccessNum: 1,
	}
	meta, err := convertReport(uint64(1), spec.PipelineReport{
		Meta: map[string]interface{}{
			"apiTotalNum":   2,
			"apiSuccessNum": 1,
		},
	})
	assert.NoError(t, err)
	assert.Equal(t, meta, want)
}

func Test_sendMessage(t *testing.T) {
	bdl := &bundle.Bundle{}
	p := &provider{
		Bundle: bdl,
	}
	req := testplanpb.Content{
		TestPlanID:  1,
		ExecuteTime: "",
		PassRate:    10,
		ApiTotalNum: 100,
	}
	want := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:         bundle.AutoTestPlanExecuteEvent,
			Action:        bundle.UpdateAction,
			OrgID:         "1",
			ProjectID:     "13",
			ApplicationID: "-1",
			TimeStamp:     "2020-10-10 11:11:11",
		},
		Sender:  bundle.SenderDOP,
		Content: req,
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateEvent",
		func(b *bundle.Bundle, ev *apistructs.EventCreateRequest) error {
			want.TimeStamp = ev.TimeStamp
			assert.Equal(t, want, ev)
			return nil
		})
	defer monkey.UnpatchAll()
	ctx := &aoptypes.TuneContext{}
	ctx.SDK.Pipeline.Labels = map[string]string{
		apistructs.LabelProjectID: "13",
		apistructs.LabelOrgID:     "1",
	}
	err := p.sendMessage(req, ctx)
	assert.NoError(t, err)
}

func TestStatistics(t *testing.T) {
	var db *dbclient.Client

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetPipelineWithTasks", func(*dbclient.Client, uint64) (*spec.PipelineWithTasks, error) {
		var id uint64 = 2
		return &spec.PipelineWithTasks{
			Pipeline: nil,
			Tasks: []*spec.PipelineTask{
				{
					ID:     1,
					Name:   "1",
					Type:   "api-test",
					Status: "Success",
					Extra: spec.PipelineTaskExtra{
						Action: pipelineyml.Action{
							Version: "2.0",
						},
					},
				},
				{
					ID:     2,
					Name:   "2",
					Type:   "api-test",
					Status: "fail",
					Extra: spec.PipelineTaskExtra{
						Action: pipelineyml.Action{
							Version: "2.0",
						},
					},
				},
				{
					ID:                3,
					Name:              "3",
					Type:              "snippet",
					Status:            "fail",
					SnippetPipelineID: &id,
				},
			},
		}, nil
	})

	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "BatchListPipelineReportsByPipelineID", func(*dbclient.Client, []uint64, []string, ...dbclient.SessionOption) (map[uint64][]spec.PipelineReport, error) {
		meta := apistructs.PipelineReportMeta{}
		meta["apiTotalNum"] = 2
		meta["apiSuccessNum"] = 1
		return map[uint64][]spec.PipelineReport{
			1: {{
				Meta: meta,
			}},
		}, nil
	})

	ctx := aoptypes.TuneContext{
		Context: context.Background(),
		SDK: aoptypes.SDK{
			DBClient: db,
		},
	}
	numStatistics, err := statistics(&ctx, 1)
	if err != nil {
		t.Error(err)
	}
	if numStatistics.ApiExecNum != 3 && numStatistics.ApiSuccessNum != 2 {
		t.Error("fail")
	}
}
