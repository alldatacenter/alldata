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
	"fmt"
	"sort"
	"sync"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"k8s.io/klog/v2"
)

const DefaultExpireTime = 5 * time.Minute
const DefaultGCInterval = 1 * time.Minute

type GCGaugeVec struct {
	vec          *prometheus.GaugeVec
	expireStatus MetricGC
}

func NewGCGaugeVec(name string, vec *prometheus.GaugeVec) *GCGaugeVec {
	expireMetric := NewMetricGC(name, vec.MetricVec, DefaultExpireTime)
	expireMetric.Run()
	return &GCGaugeVec{vec: vec, expireStatus: expireMetric}
}

func (g *GCGaugeVec) GetGaugeVec() *prometheus.GaugeVec {
	return g.vec
}

func (g *GCGaugeVec) WithSet(labels prometheus.Labels, value float64) {
	g.vec.With(labels).Set(value)
	g.expireStatus.UpdateStatus(labels)
}

type GCCounterVec struct {
	vec          *prometheus.CounterVec
	expireStatus MetricGC
}

func NewGCCounterVec(name string, vec *prometheus.CounterVec) *GCCounterVec {
	expireMetric := NewMetricGC(name, vec.MetricVec, DefaultExpireTime)
	expireMetric.Run()
	return &GCCounterVec{vec: vec, expireStatus: expireMetric}
}

func (g *GCCounterVec) GetCounterVec() *prometheus.CounterVec {
	return g.vec
}

func (g *GCCounterVec) WithInc(labels prometheus.Labels) {
	g.vec.With(labels).Inc()
	g.expireStatus.UpdateStatus(labels)
}

type MetricGC interface {
	Run()
	Stop()
	UpdateStatus(labels prometheus.Labels)
}

// record metric last updateTime and then can expire by updateTime
type metricGC struct {
	mtx       sync.Mutex
	name      string
	metricVec *prometheus.MetricVec
	status    map[string]metricStatus
	//expire time for metrics
	expireTime time.Duration
	//gc interval
	interval time.Duration

	stopCh chan struct{}
}

type metricStatus struct {
	Labels          prometheus.Labels
	lastUpdatedUnix int64
}

func NewMetricGC(name string, metricVec *prometheus.MetricVec, expireTime time.Duration) MetricGC {
	return &metricGC{
		name:       name,
		metricVec:  metricVec,
		status:     map[string]metricStatus{},
		expireTime: expireTime,
		interval:   DefaultGCInterval,
		stopCh:     make(chan struct{}, 1),
	}
}

func (e *metricGC) Run() {
	go e.run()
}

func (e *metricGC) run() {
	timer := time.NewTimer(e.interval)
	defer timer.Stop()
	for {
		select {
		case <-timer.C:
			err := e.expire()
			if err != nil {
				klog.Errorf("%s expire metrics error! err: %v", e.name, err)
			}
			timer.Reset(e.interval)
		case <-e.stopCh:
			klog.Infof("%s metrics gc task stopped!", e.name)
			return
		}
	}
}

func (e *metricGC) Stop() {
	close(e.stopCh)
}

func (e *metricGC) UpdateStatus(labels prometheus.Labels) {
	e.upateStatus(labels, time.Now().Unix())
}

func (e *metricGC) upateStatus(labels prometheus.Labels, updateTime int64) {
	e.mtx.Lock()
	defer e.mtx.Unlock()
	statusKey := labelsToKey(labels)
	status := metricStatus{Labels: labels, lastUpdatedUnix: updateTime}
	e.status[statusKey] = status
}

func (e *metricGC) statusLen() int {
	e.mtx.Lock()
	defer e.mtx.Unlock()
	return len(e.status)
}

func (e *metricGC) expire() error {
	e.mtx.Lock()
	defer e.mtx.Unlock()
	now := time.Now().Unix()
	var deletedKeys []string
	for statusKey, status := range e.status {
		if (now - status.lastUpdatedUnix) > int64(e.expireTime/time.Second) {
			deletedKeys = append(deletedKeys, statusKey)
			e.metricVec.Delete(status.Labels)
		}
	}
	for _, delKey := range deletedKeys {
		delete(e.status, delKey)
	}
	if len(deletedKeys) > 0 {
		klog.V(4).Infof("%s expire metrics success, expire num: %d", e.name, len(deletedKeys))
	}
	return nil
}

func labelsToKey(labels prometheus.Labels) string {
	keys := make([]string, 0, len(labels))
	for key := range labels {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	var mapKey string
	for _, key := range keys {
		mapKey = fmt.Sprintf("%s,%s:%s", mapKey, key, labels[key])
	}
	return mapKey
}
