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

package main

import (
	"context"
	"os"

	"github.com/recallsong/go-utils/logs"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
)

type define struct{}

func (d *define) Services() []string {
	return []string{"hello metric_report_client"}
}

func (d *define) Dependencies() []string {
	return []string{"metric-report-client"}
}

func (d *define) Description() string {
	return "hello for metric_report_client example"
}

type provider struct {
	Log logs.Logger
	//SendClient report.MetricReport
	SendClient report.MetricReport
}

func (d *define) Creator() servicehub.Creator {
	return func() servicehub.Provider {
		return func() servicehub.Provider {
			return &provider{}
		}
	}
}

func (p *provider) Init(ctx context.Context) error {
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	metric := []*report.Metric{
		{
			Name:      "_metric_meta",
			Timestamp: 1614583470000,
			Tags: map[string]string{
				"cluster_name": "terminus-dev",
				"meta":         "true",
				"metric_name":  "application_db",
			},
			Fields: map[string]interface{}{
				"fields": []string{"value:number"},
				"tags":   []string{"is_edge", "org_id"},
			},
		},
	}
	client := p.SendClient
	err := client.Send(metric)
	return err
}

func init() {
	servicehub.RegisterProvider("example", &define{})
}

func main() {
	hub := servicehub.New()
	hub.Run("examples", "", os.Args...)
}
