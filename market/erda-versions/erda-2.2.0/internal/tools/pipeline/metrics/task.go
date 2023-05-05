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

import (
	"strconv"
	"time"

	"github.com/prometheus/client_golang/prometheus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

const (
	fieldTaskTotal      = "task_total"
	fieldTaskProcessing = "task_processing"

	labelTaskID     = "task_id"
	labelActionType = "action_type"

	labelTaskStatus = "task_status"
)

var (
	taskCounterLabels    = []string{"task_status", "execute_cluster", "action_type"}
	taskProcessingLabels = []string{"execute_cluster", "action_type"}
)

// TaskCounterTotalAdd 某时间段内累计执行次数、成功次数、失败次数
var TaskCounterTotalAdd = func(task spec.PipelineTask, value float64) {
	if disableMetrics {
		return
	}
	defer func() {
		if r := recover(); r != nil {
			taskErrorLog(task, "[alert] failed to do metric TaskCounterTotalAdd, err: %v", r)
		}
	}()
	labelValues := []string{task.Status.String(), task.Extra.ClusterName, task.Type}
	taskCounterTotal.WithLabelValues(labelValues...).Add(value)
	taskDebugLog(task, "metric: TaskCounterTotalAdd, value: %v, labelValues: %v", value, labelValues)
}

var taskCounterTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
	Name: fieldTaskTotal,
}, taskCounterLabels)

// TaskGaugeProcessingAdd 正在处理中的个数
var TaskGaugeProcessingAdd = func(task spec.PipelineTask, value float64) {
	if disableMetrics {
		return
	}
	defer func() {
		if r := recover(); r != nil {
			taskErrorLog(task, "[alert] failed to do metric TaskGaugeProcessingAdd, err: %v", r)
		}
	}()
	labelValues := []string{task.Extra.ClusterName, task.Type}
	taskGaugeProcessing.WithLabelValues(labelValues...).Add(value)
	taskDebugLog(task, "metric: TaskGaugeProcessingAdd, value: %v, labelValues: %v", value, labelValues)
}

var taskGaugeProcessing = prometheus.NewGaugeVec(prometheus.GaugeOpts{
	//Subsystem: "processing task",
	Name: fieldTaskProcessing,
}, taskProcessingLabels)

// TaskEndEvent 终态时推送事件
var TaskEndEvent = func(task spec.PipelineTask, p *spec.Pipeline) {
	if disableMetrics {
		return
	}
	defer func() {
		if r := recover(); r != nil {
			taskErrorLog(task, "[alert] failed to do event TaskEndEvent, err: %v", r)
		}
	}()
	if err := reportClient.Send([]*report.Metric{{
		Name:      "dice_pipeline_task",
		Timestamp: time.Now().UnixNano(),
		Tags:      generateActionEventTags(task, p),
		Fields:    generateActionEventFields(task),
	}}); err != nil {
		taskErrorLog(task, "[alert] failed to push task bulk event, err: %v", err)
	}
	taskDebugLog(task, "send task end event success")
}

func generateActionEventTags(task spec.PipelineTask, p *spec.Pipeline) map[string]string {
	tags := map[string]string{
		labelMeta:               "true",
		labelMetricScope:        "org",
		labelMetricScopeID:      p.GetOrgName(),
		labelOrgName:            p.GetOrgName(),
		labelClusterName:        p.ClusterName,
		labelPipelineID:         strconv.FormatUint(task.PipelineID, 10),
		labelTaskID:             strconv.FormatUint(task.ID, 10),
		labelActionType:         task.Type,
		labelExecuteCluster:     task.Extra.ClusterName,
		labelTaskStatus:         task.Status.String(),
		apistructs.LabelOrgName: p.GetOrgName(),

		labelPipelineSource:  labelPipelineSource,
		labelPipelineYmlName: p.PipelineYmlName,
	}
	return tags
}

func generateActionEventFields(task spec.PipelineTask) map[string]interface{} {
	return map[string]interface{}{
		num: 1,
	}
}
