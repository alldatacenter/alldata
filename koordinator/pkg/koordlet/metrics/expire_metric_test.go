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

import (
	"strconv"
	"testing"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/util/rand"
)

type metric struct {
	labels     prometheus.Labels
	value      float64
	updateTime int64
}

func Test_GCGaugeVec_WithSet(t *testing.T) {

	testGaugeVec := NewGCGaugeVec("test", prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "test_gauge",
	}, []string{NodeKey, PodName, PodNamespace}))
	vec := testGaugeVec.GetGaugeVec()
	//add metric1
	pod1Labels := prometheus.Labels{NodeKey: "node1", PodName: "pod1", PodNamespace: "ns1"}
	testGaugeVec.WithSet(pod1Labels, 1)
	metrics := collectMetrics(vec)
	assert.Equal(t, 1, len(metrics), "checkMetricsNum")

	//add metric2
	pod2Labels := prometheus.Labels{NodeKey: "node2", PodName: "pod2", PodNamespace: "ns2"}
	testGaugeVec.WithSet(pod2Labels, 2)
	metrics = collectMetrics(vec)
	assert.Equal(t, 2, len(metrics), "checkMetricsNum")

	//update metric1
	testGaugeVec.WithSet(pod1Labels, 3)
	metrics = collectMetrics(vec)
	assert.Equal(t, 2, len(metrics), "checkMetricsNum")

	testGaugeVec.expireStatus.Stop()

}

func Test_GCCounterVec_WithInc(t *testing.T) {

	testCounterVec := NewGCCounterVec("test", prometheus.NewCounterVec(prometheus.CounterOpts{
		Subsystem: KoordletSubsystem,
		Name:      "test_counter",
	}, []string{NodeKey, PodName, PodNamespace}))
	vec := testCounterVec.GetCounterVec()
	//add metric1
	pod1Labels := prometheus.Labels{NodeKey: "node1", PodName: "pod1", PodNamespace: "ns1"}
	testCounterVec.WithInc(pod1Labels)
	metrics := collectMetrics(vec)
	assert.Equal(t, 1, len(metrics), "checkMetricsNum")

	//add metric2
	pod2Labels := prometheus.Labels{NodeKey: "node2", PodName: "pod2", PodNamespace: "ns2"}
	testCounterVec.WithInc(pod2Labels)
	metrics = collectMetrics(vec)
	assert.Equal(t, 2, len(metrics), "checkMetricsNum")

	//update metric1
	testCounterVec.WithInc(pod1Labels)
	metrics = collectMetrics(vec)
	assert.Equal(t, 2, len(metrics), "checkMetricsNum")

	testCounterVec.expireStatus.Stop()

}

func Test_MetricGC_GC(t *testing.T) {

	gaugeVec := prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "test_gauge",
	}, []string{NodeKey, PodName, PodNamespace})
	expireMetric := NewMetricGC("test_gauge", gaugeVec.MetricVec, DefaultExpireTime).(*metricGC)
	expireMetric.interval = 1 * time.Microsecond
	//test gc start
	expireMetric.Run()

	gcGaugeVec := GCGaugeVec{vec: gaugeVec, expireStatus: expireMetric}

	// metric should not expire
	metrics := generatePodMetrics(10, time.Now().Unix())
	for _, m := range metrics {
		gcGaugeVec.WithSet(m.labels, m.value)
	}
	gotMetrics := collectMetrics(gaugeVec)
	assert.Equal(t, len(metrics), len(gotMetrics), "checkMetricsNum")
	assert.Equal(t, len(metrics), expireMetric.statusLen(), "checkStatusNum")

	// metric should expire
	metricsUpdate := generatePodMetrics(5, time.Now().Unix()-int64(DefaultExpireTime/time.Second))
	for _, m := range metricsUpdate {
		gcGaugeVec.WithSet(m.labels, m.value)
		expireMetric.upateStatus(m.labels, m.updateTime)
	}
	time.Sleep(10 * time.Millisecond)
	gotMetrics = collectMetrics(gaugeVec)
	assert.Equal(t, len(metricsUpdate), len(gotMetrics), "checkMetricsNum")
	assert.Equal(t, len(metricsUpdate), expireMetric.statusLen(), "checkStatusNum")

	expireMetric.Stop()
}

func generatePodMetrics(num int, baseUpdateTime int64) []metric {
	var metrics []metric
	for i := 0; i < num; i++ {
		iStr := strconv.Itoa(i)
		metrics = append(metrics, metric{labels: prometheus.Labels{NodeKey: "node" + iStr, PodName: "pod" + iStr, PodNamespace: "ns" + iStr},
			value:      1,
			updateTime: baseUpdateTime - rand.Int63nRange(1, 100)})
	}
	return metrics
}

func collectMetrics(vec prometheus.Collector) []prometheus.Metric {
	metricsCh := make(chan prometheus.Metric, 10)
	go func() {
		vec.Collect(metricsCh)
		close(metricsCh)
	}()
	var metrics []prometheus.Metric
	for {
		select {
		case metric, ok := <-metricsCh:
			if !ok {
				return metrics
			}
			metrics = append(metrics, metric)
		}
	}
}
