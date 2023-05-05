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
	"github.com/prometheus/client_golang/prometheus"

	"github.com/erda-project/erda/internal/pkg/metrics/report"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
)

// disableMetrics 是否禁用 metric 相关操作
var disableMetrics bool

//var reportClient *report.MetricReport
var reportClient report.MetricReport

func Initialize(client report.MetricReport) {
	disableMetrics = conf.DisableMetrics()
	reportClient = client
	// if enable metrics, need register pipeline, task counter to metrics
	if !disableMetrics {
		prometheus.MustRegister(pipelineGaugeProcessing, pipelineCounterTotal, taskGaugeProcessing, taskCounterTotal)
	}
}
