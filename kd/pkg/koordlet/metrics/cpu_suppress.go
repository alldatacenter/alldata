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

package metrics

import "github.com/prometheus/client_golang/prometheus"

var (
	BESuppressCPU = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "be_suppress_cpu_cores",
		Help:      "Number of cores suppress by koordlet",
	}, []string{NodeKey, BESuppressTypeKey})

	BESuppressLSUsedCPU = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "be_suppress_ls_used_cpu_cores",
		Help:      "Number of cpu cores used by LS. We consider non-BE pods and podMeta-missing pods as LS.",
	}, []string{NodeKey})

	CPUSuppressCollector = []prometheus.Collector{
		BESuppressCPU,
		BESuppressLSUsedCPU,
	}
)

func RecordBESuppressCores(suppressType string, value float64) {
	labels := genNodeLabels()
	if labels == nil {
		return
	}
	labels[BESuppressTypeKey] = suppressType
	BESuppressCPU.With(labels).Set(value)
}

func RecordBESuppressLSUsedCPU(value float64) {
	labels := genNodeLabels()
	if labels == nil {
		return
	}
	BESuppressLSUsedCPU.With(labels).Set(value)
}
