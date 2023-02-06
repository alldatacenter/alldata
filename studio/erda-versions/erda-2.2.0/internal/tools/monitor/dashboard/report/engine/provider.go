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

package engine

import (
	"context"
	"fmt"

	"github.com/go-playground/validator"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
)

type define struct{}

func (d *define) Services() []string     { return []string{"report-engine"} }
func (d *define) Dependencies() []string { return []string{} }
func (d *define) Summary() string        { return "report engine" }
func (d *define) Description() string    { return d.Summary() }
func (d *define) Config() interface{} {
	return &config{}
}
func (d *define) Creator() servicehub.Creator {
	return func() servicehub.Provider {
		return &provider{}
	}
}

type config struct {
	MonitorAddr      string `file:"monitor_addr" env:"ACTION_MONITOR_ADDR" validate:"required"`
	DomainAddr       string `file:"domain_addr" env:"ACTION_DOMAIN_ADDR" validate:"required"`
	ReportID         string `file:"report_id" env:"ACTION_REPORT_ID" validate:"required"`
	OrgName          string `file:"org_name" env:"ACTION_ORG_NAME" validate:"required"`
	CoreServicesAddr string `file:"core_services_addr" env:"ACTION_CORE_SERVICES_ADDR" validate:"required"`
}

type provider struct {
	Cfg    *config
	Log    logs.Logger
	report *Report
}

func (p *provider) Init(ctx servicehub.Context) error {
	valid := validator.New()
	err := valid.Struct(p.Cfg)
	if err != nil {
		return fmt.Errorf("invalid config: %s", err)
	}
	p.report = New(p.Cfg)
	return nil
}

// Start .
func (p *provider) Start() error {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	p.Log.Info("reportengine start running ...")
	if err := p.report.Run(ctx); err != nil {
		return fmt.Errorf("reportengine failed: %s", err)
	}
	p.Log.Info("reportengine done successfully ...")
	return nil
}

func (p *provider) Close() error {
	p.Log.Debug("not support close report engine")
	return nil
}

func init() {
	servicehub.RegisterProvider("report-engine", &define{})
}
