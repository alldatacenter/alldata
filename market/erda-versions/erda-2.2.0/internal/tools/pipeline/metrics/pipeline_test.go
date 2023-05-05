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

package metrics

// import (
// 	"bytes"
// 	"encoding/json"
// 	"fmt"
// 	"os"
// 	"testing"
// 	"time"

// 	"github.com/stretchr/testify/assert"

// 	"github.com/erda-project/erda/internal/tools/pipeline/spec"
// 	"terminus.io/dice/telemetry/metrics"
// )

// func init() {
// 	os.Setenv("MONITOR_ADDR", "monitor.default.svc.cluster.local:7096")
// }

// func TestTriggerPipelineTotalCounter(t *testing.T) {
// 	PipelineCounterTotalAdd(spec.Pipeline{
// 		PipelineBase: spec.PipelineBase{
// 			Status:      "success",
// 			ClusterName: "terminus-dev",
// 		},
// 	}, 1)
// }

// func TestPipelineTotalCounter(t *testing.T) {
// 	cfg := metrics.NewQueryConfig()
// 	queryAction, _ := metrics.NewClient().NewQuery(cfg)

// 	req := metrics.CreateQueryMetricRequest("dice_pipeline")
// 	now := time.Now()
// 	start, end := now.AddDate(0, 0, -7), now
// 	req = req.StartFrom(start).EndWith(end).Filter("field", "pipeline_total").Filter("type", "success")
// 	resp, err := queryAction.QueryMetric(req)
// 	if err != nil {
// 		return
// 	}
// 	var v interface{}
// 	assert.NoError(t, json.NewDecoder(bytes.NewBuffer(resp.Body)).Decode(&v))
// 	b, err := json.MarshalIndent(v, "", "  ")
// 	assert.NoError(t, err)
// 	fmt.Println(string(b))
// }

// func TestPipelineGauge(t *testing.T) {
// 	for {
// 		PipelineGaugeProcessingAdd(spec.Pipeline{}, 1)
// 		time.Sleep(time.Second * 5)
// 	}
// }
