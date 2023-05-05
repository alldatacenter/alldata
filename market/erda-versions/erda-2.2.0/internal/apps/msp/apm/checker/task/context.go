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

package task

import (
	"context"
	"encoding/json"
	"io"
	"os"
	"time"

	"github.com/erda-project/erda/internal/apps/msp/apm/checker/plugins"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
)

type reportContext struct {
	context.Context
	report report.MetricReport
}

func newTaskContext(ctx context.Context, r report.MetricReport) plugins.Context {
	if r == nil {
		return &writerContext{Context: ctx, w: os.Stdout}
	}
	return &reportContext{
		Context: ctx,
		report:  r,
	}
}

func (c *reportContext) Report(metrics ...*plugins.Metric) error {
	if len(metrics) > 0 {
		list := make(report.Metrics, len(metrics))
		now := time.Now().UnixNano()
		for i, m := range metrics {
			if m.Timestamp <= 0 {
				m.Timestamp = now
			}
			list[i] = &report.Metric{
				Name:      m.Name,
				Timestamp: m.Timestamp,
				Tags:      m.Tags,
				Fields:    m.Fields,
			}
		}
		return c.report.Send(list)
	}
	return nil
}

type writerContext struct {
	context.Context
	w io.Writer
}

func (c *writerContext) Report(metrics ...*plugins.Metric) error {
	now := time.Now().UnixNano()
	for _, m := range metrics {
		if m.Timestamp <= 0 {
			m.Timestamp = now
		}
		err := json.NewEncoder(c.w).Encode(m)
		if err != nil {
			return err
		}
	}
	return nil
}
