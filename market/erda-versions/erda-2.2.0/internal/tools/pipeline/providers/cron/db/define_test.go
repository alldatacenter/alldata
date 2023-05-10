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

package db

import (
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"google.golang.org/protobuf/types/known/timestamppb"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
)

func TestGenCompensateCreatePipelineReqNormalLabels(t *testing.T) {
	pc := PipelineCron{ID: 1, Extra: PipelineCronExtra{
		NormalLabels: map[string]string{
			"org":                               "erda",
			apistructs.LabelPipelineTriggerMode: "dice",
		},
	}}
	now := time.Now()
	normalLabels := pc.GenCompensateCreatePipelineReqNormalLabels(now)
	assert.Equal(t, "erda", normalLabels["org"])
	assert.Equal(t, apistructs.PipelineTriggerModeCron.String(), normalLabels[apistructs.LabelPipelineTriggerMode])
	assert.Equal(t, strconv.FormatInt(now.UnixNano(), 10), normalLabels[apistructs.LabelPipelineCronTriggerTime])
}

func TestGenCompensateCreatePipelineReqFilterLabels(t *testing.T) {
	pc := PipelineCron{ID: 1, Extra: PipelineCronExtra{
		FilterLabels: map[string]string{
			"org":                               "erda",
			apistructs.LabelPipelineTriggerMode: "dice",
		},
	}}
	filterLabels := pc.GenCompensateCreatePipelineReqFilterLabels()
	assert.Equal(t, "true", filterLabels[apistructs.LabelPipelineCronCompensated])
	assert.Equal(t, apistructs.PipelineTriggerModeCron.String(), filterLabels[apistructs.LabelPipelineTriggerMode])
}

func TestPipelineCron_Convert2DTO(t *testing.T) {

	stringTime := "2017-08-30 16:40:41"
	loc, _ := time.LoadLocation("UTC")
	parseTime, _ := time.ParseInLocation("2006-01-02 15:04:05", stringTime, loc)

	tests := []struct {
		name   string
		dbCron PipelineCron
		want   *pb.Cron
	}{
		{
			name: "test cover",
			dbCron: PipelineCron{
				ID:              1,
				TimeCreated:     parseTime,
				TimeUpdated:     parseTime,
				PipelineSource:  apistructs.PipelineSource("test"),
				PipelineYmlName: "test",
				CronExpr:        "test",
				Enable:          &[]bool{true}[0],
				Extra: PipelineCronExtra{
					PipelineYml: "test",
					ClusterName: "test",
					FilterLabels: map[string]string{
						"test":                 "test",
						apistructs.LabelUserID: "test",
						apistructs.LabelOrgID:  "1",
					},
					NormalLabels: map[string]string{
						"test": "test",
					},
					Envs: map[string]string{
						"test": "test",
					},
					ConfigManageNamespaces: []string{"test"},
					IncomingSecrets: map[string]string{
						"test": "test",
					},
					CronStartFrom: &parseTime,
					Version:       "v2",
					Compensator: &apistructs.CronCompensator{
						Enable:               true,
						LatestFirst:          true,
						StopIfLatterExecuted: true,
					},
					LastCompensateAt: &parseTime,
				},
				ApplicationID:        1,
				Branch:               "test",
				BasePipelineID:       1,
				PipelineDefinitionID: "test",
				IsEdge:               &[]bool{true}[0],
			},
			want: &pb.Cron{
				ID:                     1,
				TimeCreated:            timestamppb.New(parseTime),
				TimeUpdated:            timestamppb.New(parseTime),
				ApplicationID:          1,
				Branch:                 "test",
				CronExpr:               "test",
				CronStartTime:          timestamppb.New(parseTime),
				PipelineYmlName:        "test",
				BasePipelineID:         1,
				Enable:                 wrapperspb.Bool(true),
				PipelineYml:            "test",
				ConfigManageNamespaces: []string{"test"},
				PipelineDefinitionID:   "test",
				PipelineSource:         "test",
				UserID:                 "test",
				OrgID:                  1,
				Secrets: map[string]string{
					"test": "test",
				},
				IsEdge: wrapperspb.Bool(true),
				Extra: &pb.CronExtra{
					PipelineYml: "test",
					ClusterName: "test",
					Labels: map[string]string{
						"test":                 "test",
						apistructs.LabelUserID: "test",
						apistructs.LabelOrgID:  "1",
					},
					NormalLabels: map[string]string{
						"test": "test",
					},
					Envs: map[string]string{
						"test": "test",
					},
					ConfigManageNamespaces: []string{"test"},
					IncomingSecrets:        map[string]string{"test": "test"},
					CronStartFrom:          timestamppb.New(parseTime),
					Version:                "v2",
					Compensator: &pb.CronCompensator{
						Enable:               wrapperspb.Bool(true),
						LatestFirst:          wrapperspb.Bool(true),
						StopIfLatterExecuted: wrapperspb.Bool(true),
					},
					LastCompensateAt: timestamppb.New(parseTime),
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.dbCron.Convert2DTO()
			assert.EqualValues(t, got, tt.want)
		})
	}
}
