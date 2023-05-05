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

package taskop

import (
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/bmizerany/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/costtimeutil"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_queue_WhenDone(t *testing.T) {
	tests := []struct {
		name    string
		q       queue
		wantErr bool
	}{
		{
			name: "test_reset_time_begin",
			q: queue{
				P: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID: 1,
					},
				},
				Task: &spec.PipelineTask{
					Extra: spec.PipelineTaskExtra{
						LoopOptions: &apistructs.PipelineTaskLoopOptions{},
					},
					TimeBegin: time.Date(2009, 11, 17, 20, 34, 58, 651387237, time.UTC),
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			patch := monkey.Patch(costtimeutil.CalculateTaskQueueTimeSec, func(task *spec.PipelineTask) (cost int64) {
				return 0
			})
			defer patch.Unpatch()
			if err := tt.q.WhenDone(nil); (err != nil) != tt.wantErr {
				t.Errorf("WhenDone() error = %v, wantErr %v", err, tt.wantErr)
			}
			assert.Equal(t, tt.q.Task.TimeBegin.Unix(), time.Date(2009, 11, 17, 20, 34, 58, 651387237, time.UTC).Unix())
		})
	}
}
