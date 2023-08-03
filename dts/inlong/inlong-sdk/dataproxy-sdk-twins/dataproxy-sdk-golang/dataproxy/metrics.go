//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dataproxy

import (
	"errors"
	"sync"

	"github.com/prometheus/client_golang/prometheus"
)

type metrics struct {
	errorCounter            *prometheus.CounterVec
	errorCounters           map[string]prometheus.Counter
	errorCounterLock        sync.RWMutex
	retryCounter            *prometheus.CounterVec
	retryCounters           map[string]prometheus.Counter
	retryCounterLock        sync.RWMutex
	timeoutCounter          *prometheus.CounterVec
	timeoutCounters         map[string]prometheus.Counter
	timeoutCounterLock      sync.RWMutex
	messageCounter          *prometheus.CounterVec
	messageCounters         map[string]prometheus.Counter
	messageCounterLock      sync.RWMutex
	updateConnCounter       *prometheus.CounterVec
	updateConnCounters      map[string]prometheus.Counter
	updateConnCounterLock   sync.RWMutex
	pendingMessageGauge     *prometheus.GaugeVec
	pendingMessageGauges    map[string]prometheus.Gauge
	pendingMessageGaugeLock sync.RWMutex
	batchSizeHistogram      *prometheus.HistogramVec
	batchSizeHistograms     map[string]prometheus.Observer
	batchSizeHistogramLock  sync.RWMutex
	batchTimeHistogram      *prometheus.HistogramVec
	batchTimeHistograms     map[string]prometheus.Observer
	batchTimeHistogramLock  sync.RWMutex
	name                    string
	registry                prometheus.Registerer
}

func newMetrics(name string, reg prometheus.Registerer) (*metrics, error) {
	if name == "" {
		return nil, errors.New("metrics name is not given")
	}

	registry := prometheus.DefaultRegisterer
	if reg != nil {
		registry = reg
	}

	m := &metrics{
		errorCounters:        make(map[string]prometheus.Counter),
		retryCounters:        make(map[string]prometheus.Counter),
		timeoutCounters:      make(map[string]prometheus.Counter),
		messageCounters:      make(map[string]prometheus.Counter),
		updateConnCounters:   make(map[string]prometheus.Counter),
		pendingMessageGauges: make(map[string]prometheus.Gauge),
		batchSizeHistograms:  make(map[string]prometheus.Observer),
		batchTimeHistograms:  make(map[string]prometheus.Observer),
		name:                 name,
		registry:             registry,
	}

	err := m.init()
	if err != nil {
		return nil, err
	}

	return m, nil
}

func (m *metrics) init() error {
	m.errorCounter = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "data_proxy_error_count",
		Help: "Counter of error events",
	}, []string{"name", "code"})
	err := m.registry.Register(m.errorCounter)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.errorCounter = are.ExistingCollector.(*prometheus.CounterVec)
	}

	m.retryCounter = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "data_proxy_retry_count",
		Help: "Counter of retry batches",
	}, []string{"name", "worker"})
	err = m.registry.Register(m.retryCounter)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.retryCounter = are.ExistingCollector.(*prometheus.CounterVec)
	}

	m.timeoutCounter = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "data_proxy_timeout_count",
		Help: "Counter of timeout batches",
	}, []string{"name", "worker"})
	err = m.registry.Register(m.timeoutCounter)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.timeoutCounter = are.ExistingCollector.(*prometheus.CounterVec)
	}

	m.messageCounter = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "data_proxy_msg_count",
		Help: "Counter of message",
	}, []string{"name", "code"})
	err = m.registry.Register(m.messageCounter)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.messageCounter = are.ExistingCollector.(*prometheus.CounterVec)
	}

	m.updateConnCounter = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "data_proxy_update_conn_count",
		Help: "Counter of update connection events",
	}, []string{"name", "code"})
	err = m.registry.Register(m.updateConnCounter)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.updateConnCounter = are.ExistingCollector.(*prometheus.CounterVec)
	}

	m.pendingMessageGauge = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "data_proxy_pending_msg_gauge",
		Help: "Gauge of pending message",
	}, []string{"name", "worker"})
	err = m.registry.Register(m.pendingMessageGauge)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.pendingMessageGauge = are.ExistingCollector.(*prometheus.GaugeVec)
	}

	m.batchSizeHistogram = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "data_proxy_batch_size",
		Help:    "Histogram of batch size",
		Buckets: []float64{1024, 2 * 1024, 4 * 1024, 8 * 1024, 16 * 1024, 32 * 1024, 64 * 1024, 128 * 1024, 256 * 1024},
	}, []string{"name", "code"})
	err = m.registry.Register(m.batchSizeHistogram)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.batchSizeHistogram = are.ExistingCollector.(*prometheus.HistogramVec)
	}

	m.batchTimeHistogram = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "data_proxy_batch_time",
		Help:    "Histogram of batch time in milliseconds",
		Buckets: []float64{5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000},
	}, []string{"name", "code"})
	err = m.registry.Register(m.batchTimeHistogram)
	if err != nil {
		are, ok := err.(prometheus.AlreadyRegisteredError)
		if !ok {
			return err
		}
		m.batchTimeHistogram = are.ExistingCollector.(*prometheus.HistogramVec)
	}

	return nil
}

func (m *metrics) incError(code string) {
	m.errorCounterLock.RLock()
	c, ok := m.errorCounters[code]
	m.errorCounterLock.RUnlock()
	if ok {
		c.Add(float64(1))
		return
	}

	m.errorCounterLock.Lock()
	defer m.errorCounterLock.Unlock()
	c, ok = m.errorCounters[code]
	if ok {
		c.Add(float64(1))
		return
	}

	c, err := m.errorCounter.GetMetricWithLabelValues(m.name, code)
	if err != nil {
		return
	}

	c.Add(float64(1))
	m.errorCounters[code] = c
}

func (m *metrics) incRetry(worker string) {
	m.retryCounterLock.RLock()
	c, ok := m.retryCounters[worker]
	m.retryCounterLock.RUnlock()
	if ok {
		c.Add(float64(1))
		return
	}

	m.retryCounterLock.Lock()
	defer m.retryCounterLock.Unlock()
	c, ok = m.retryCounters[worker]
	if ok {
		c.Add(float64(1))
		return
	}

	c, err := m.retryCounter.GetMetricWithLabelValues(m.name, worker)
	if err != nil {
		return
	}

	c.Add(float64(1))
	m.retryCounters[worker] = c
}

func (m *metrics) incTimeout(worker string) {
	m.timeoutCounterLock.RLock()
	c, ok := m.timeoutCounters[worker]
	m.timeoutCounterLock.RUnlock()
	if ok {
		c.Add(float64(1))
		return
	}

	m.timeoutCounterLock.Lock()
	defer m.timeoutCounterLock.Unlock()
	c, ok = m.timeoutCounters[worker]
	if ok {
		c.Add(float64(1))
		return
	}

	c, err := m.timeoutCounter.GetMetricWithLabelValues(m.name, worker)
	if err != nil {
		return
	}

	c.Add(float64(1))
	m.timeoutCounters[worker] = c
}

func (m *metrics) incMessage(code string) {
	m.messageCounterLock.RLock()
	c, ok := m.messageCounters[code]
	m.messageCounterLock.RUnlock()
	if ok {
		c.Add(float64(1))
		return
	}

	m.messageCounterLock.Lock()
	defer m.messageCounterLock.Unlock()
	c, ok = m.messageCounters[code]
	if ok {
		c.Add(float64(1))
		return
	}

	c, err := m.messageCounter.GetMetricWithLabelValues(m.name, code)
	if err != nil {
		return
	}

	c.Add(float64(1))
	m.messageCounters[code] = c
}

func (m *metrics) incUpdateConn(code string) {
	m.updateConnCounterLock.RLock()
	c, ok := m.updateConnCounters[code]
	m.updateConnCounterLock.RUnlock()
	if ok {
		c.Add(float64(1))
		return
	}

	m.updateConnCounterLock.Lock()
	defer m.updateConnCounterLock.Unlock()
	c, ok = m.updateConnCounters[code]
	if ok {
		c.Add(float64(1))
		return
	}

	c, err := m.updateConnCounter.GetMetricWithLabelValues(m.name, code)
	if err != nil {
		return
	}

	c.Add(float64(1))
	m.updateConnCounters[code] = c
}

func (m *metrics) incPending(worker string) {
	m.pendingMessageGaugeLock.RLock()
	c, ok := m.pendingMessageGauges[worker]
	m.pendingMessageGaugeLock.RUnlock()
	if ok {
		c.Add(float64(1))
		return
	}

	m.pendingMessageGaugeLock.Lock()
	defer m.pendingMessageGaugeLock.Unlock()
	c, ok = m.pendingMessageGauges[worker]
	if ok {
		c.Add(float64(1))
		return
	}

	c, err := m.pendingMessageGauge.GetMetricWithLabelValues(m.name, worker)
	if err != nil {
		return
	}

	c.Add(float64(1))
	m.pendingMessageGauges[worker] = c
}

func (m *metrics) decPending(worker string) {
	m.pendingMessageGaugeLock.RLock()
	c, ok := m.pendingMessageGauges[worker]
	m.pendingMessageGaugeLock.RUnlock()
	if ok {
		c.Dec()
		return
	}

	m.pendingMessageGaugeLock.Lock()
	defer m.pendingMessageGaugeLock.Unlock()
	c, ok = m.pendingMessageGauges[worker]
	if ok {
		c.Dec()
		return
	}

	c, err := m.pendingMessageGauge.GetMetricWithLabelValues(m.name, worker)
	if err != nil {
		return
	}

	c.Dec()
	m.pendingMessageGauges[worker] = c
}

func (m *metrics) observeSize(code string, value int) {
	m.batchSizeHistogramLock.RLock()
	o, ok := m.batchSizeHistograms[code]
	m.batchSizeHistogramLock.RUnlock()
	if ok {
		o.Observe(float64(value))
		return
	}

	m.batchSizeHistogramLock.Lock()
	defer m.batchSizeHistogramLock.Unlock()
	o, ok = m.batchSizeHistograms[code]
	if ok {
		o.Observe(float64(value))
		return
	}

	o, err := m.batchSizeHistogram.GetMetricWithLabelValues(m.name, code)
	if err != nil {
		return
	}

	o.Observe(float64(value))
	m.batchSizeHistograms[code] = o
}

func (m *metrics) observeTime(code string, value int64) {
	m.batchTimeHistogramLock.RLock()
	o, ok := m.batchTimeHistograms[code]
	m.batchTimeHistogramLock.RUnlock()
	if ok {
		o.Observe(float64(value))
		return
	}

	m.batchTimeHistogramLock.Lock()
	defer m.batchTimeHistogramLock.Unlock()
	o, ok = m.batchTimeHistograms[code]
	if ok {
		o.Observe(float64(value))
		return
	}

	o, err := m.batchTimeHistogram.GetMetricWithLabelValues(m.name, code)
	if err != nil {
		return
	}

	o.Observe(float64(value))
	m.batchTimeHistograms[code] = o
}
