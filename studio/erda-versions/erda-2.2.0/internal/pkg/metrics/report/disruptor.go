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

package report

import (
	"time"

	parallel "github.com/erda-project/erda-infra/pkg/parallel-writer"
)

type Disruptor interface {
	In(metrics ...*Metric) error
}

type disruptor struct {
	metrics  chan *Metric
	labels   GlobalLabel
	cfg      *config
	reporter Reporter
}

func (d *disruptor) In(metrics ...*Metric) error {
	if len(metrics) > 0 {
		for _, metric := range metrics {
			for k, v := range d.labels {
				if _, ok := metric.Tags[k]; !ok {
					metric.Tags[k] = v
				}
			}
			d.metrics <- metric
		}
	}
	return nil
}

func (d *disruptor) dataToMetric(data []interface{}) []*Metric {
	resultArr := make([]*Metric, 0)
	for _, v := range data {
		m, ok := v.(*Metric)
		if ok {
			resultArr = append(resultArr, m)
		}
	}
	return resultArr
}

func (d *disruptor) push() {
	go func(queue chan *Metric, reporter Reporter, queueSize int) {
		reportWrite := NewReportWrite(queueSize)
		buf := parallel.NewBuffer(reportWrite, queueSize)
		ticker := time.NewTicker(time.Second * time.Duration(5))
		for {
			select {
			case metric, ok := <-queue:
				if !ok {
					if res, err := buf.WriteN(metric); res != 0 && err == nil {
						data := buf.Data()
						resultArr := d.dataToMetric(data[:res])
						if resultArr != nil {
							_ = reporter.Send(resultArr)
						}
					}
					break
				}
				buf.Write(metric)
			case <-ticker.C:
				if data := buf.Data(); len(data) > 0 {
					buf.Flush()
					resultArr := d.dataToMetric(data[:])
					if resultArr != nil {
						_ = reporter.Send(resultArr)
					}
				}
			}
		}
	}(d.metrics, d.reporter, d.cfg.ReportConfig.BufferSize)
}
