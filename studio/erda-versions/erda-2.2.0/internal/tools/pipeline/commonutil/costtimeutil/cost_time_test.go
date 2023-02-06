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

package costtimeutil

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func TestCalculateTaskCostTimeSec(t *testing.T) {
	require.True(t, CalculateTaskCostTimeSec(&spec.PipelineTask{CostTimeSec: 100}) == 100)
	require.True(t, CalculateTaskCostTimeSec(&spec.PipelineTask{CostTimeSec: -1, TimeBegin: time.Time{}}) == -1)
	begin := time.Now()
	time.Sleep(time.Second * 1)
	require.True(t, CalculateTaskCostTimeSec(&spec.PipelineTask{CostTimeSec: -1, TimeBegin: begin}) > 0)
	require.True(t, CalculateTaskCostTimeSec(&spec.PipelineTask{CostTimeSec: -1, TimeBegin: begin, TimeEnd: begin.Add(time.Second * 2)}) == 2)
}

func TestCalculatePipelineCostTimeSec(t *testing.T) {
	require.True(t, CalculatePipelineCostTimeSec(&spec.Pipeline{PipelineBase: spec.PipelineBase{CostTimeSec: 100}}) == 100)
	require.True(t, CalculatePipelineCostTimeSec(&spec.Pipeline{PipelineBase: spec.PipelineBase{CostTimeSec: -1, TimeBegin: &time.Time{}}}) == -1)
	begin := time.Now()
	time.Sleep(time.Second * 1)
	require.True(t, CalculatePipelineCostTimeSec(&spec.Pipeline{PipelineBase: spec.PipelineBase{CostTimeSec: -1, TimeBegin: &begin}}) > 0)
	end := begin.Add(time.Minute * 2)
	require.Equal(t, int64(time.Minute*2/time.Second), CalculatePipelineCostTimeSec(&spec.Pipeline{PipelineBase: spec.PipelineBase{CostTimeSec: -1, TimeBegin: &begin, TimeEnd: &end}}))
}
