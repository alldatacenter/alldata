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

package core

import (
	"context"
	"fmt"
	"reflect"
	"runtime"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/config"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/pipeline"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/common/unmarshalwork"
)

func init() {
	runtime.SetBlockProfileRate(1000)
}

// +provider
type provider struct {
	Cfg        *config.Config
	Log        logs.Logger
	servicectx servicehub.Context

	metricPipelines, spanPipelines, logPipeline, rawPipeline []*pipeline.Pipeline
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	p.servicectx = ctx
	p.metricPipelines = make([]*pipeline.Pipeline, 0)
	return nil
}

// Run this is optional
func (p *provider) Run(ctx context.Context) error {
	err := p.initComponents()
	if err != nil {
		return fmt.Errorf("initComponents err: %w", err)
	}

	p.start(ctx)
	return nil
}

func (p *provider) Start() error {
	unmarshalwork.Start()
	return nil
}

func (p *provider) Close() error {
	unmarshalwork.Stop()
	return nil
}

func (p *provider) initComponents() error {
	var err error
	p.metricPipelines, err = p.createPipelines(p.Cfg.Pipelines.Metrics, odata.MetricType)
	if err != nil {
		return err
	}
	p.logPipeline, err = p.createPipelines(p.Cfg.Pipelines.Logs, odata.LogType)
	if err != nil {
		return err
	}
	p.spanPipelines, err = p.createPipelines(p.Cfg.Pipelines.Spans, odata.SpanType)
	if err != nil {
		return err
	}
	p.rawPipeline, err = p.createPipelines(p.Cfg.Pipelines.Raws, odata.RawType)
	if err != nil {
		return err
	}
	return nil
}

var (
	defaultEnable      = true
	defaultPipelineCfg = config.Pipeline{
		BatchSize:     10,
		FlushInterval: time.Second,
		FlushJitter:   time.Second,
		Enable:        &defaultEnable,
	}
)

func (p *provider) createPipelines(cfgs []config.Pipeline, dtype odata.DataType) ([]*pipeline.Pipeline, error) {
	res := []*pipeline.Pipeline{}
	for idx, item := range cfgs {
		if item.Enable == nil {
			item.Enable = &defaultEnable
		}
		if item.BatchSize == 0 {
			item.BatchSize = defaultPipelineCfg.BatchSize
		}
		if item.FlushInterval == 0 {
			item.FlushInterval = defaultPipelineCfg.FlushInterval
		}
		if item.FlushJitter == 0 {
			item.FlushJitter = defaultPipelineCfg.FlushJitter
		}

		if !(*item.Enable) {
			continue
		}

		rs, err := findComponents(p.servicectx, item.Receivers)
		if err != nil {
			return nil, err
		}
		ps, err := findComponents(p.servicectx, item.Processors)
		if err != nil {
			return nil, err
		}
		es, err := findComponents(p.servicectx, item.Exporters)
		if err != nil {
			return nil, err
		}

		name := fmt.Sprintf("core-pipeline-%s-%d", dtype, idx)
		pipe := pipeline.NewPipeline(p.Log.Sub(name), item, dtype)
		err = pipe.InitComponents(rs, ps, es)
		if err != nil {
			return nil, fmt.Errorf("init components err: %w", err)
		}
		p.Log.Infof("%s has been created successfully!", name)
		res = append(res, pipe)
	}
	return res, nil
}

func (p *provider) start(ctx context.Context) {
	startPipeline(ctx, p.metricPipelines)
	startPipeline(ctx, p.logPipeline)
	startPipeline(ctx, p.spanPipelines)
	startPipeline(ctx, p.rawPipeline)
}

func startPipeline(ctx context.Context, pipes []*pipeline.Pipeline) {
	for _, pipe := range pipes {
		go func(pi *pipeline.Pipeline) {
			pi.StartStream(ctx)
		}(pipe)
	}
}

func findComponents(ctx servicehub.Context, components []string) ([]model.ComponentUnit, error) {
	res := make([]model.ComponentUnit, 0)
	for _, item := range components {
		obj := ctx.Service(item)
		if obj == nil {
			return nil, fmt.Errorf("component %s not found", item)
		}
		com, ok := obj.(model.Component)
		if !ok {
			return nil, fmt.Errorf("%s is not a Component", item)
		}
		f, err := model.NewDataFilter(extractFilterConfig(com.ComponentConfig()))
		if err != nil {
			return nil, fmt.Errorf("create data filter: %w", err)
		}
		res = append(res, model.ComponentUnit{
			Component: com,
			Name:      item,
			Filter:    f,
		})
	}
	return res, nil
}

func extractFilterConfig(cfg interface{}) model.FilterConfig {
	t, v := reflect.TypeOf(cfg), reflect.ValueOf(cfg)
	if t.Kind() == reflect.Ptr || t.Kind() == reflect.Interface {
		t = t.Elem()
		v = v.Elem()
	}
	switch t.Kind() {
	case reflect.Struct:
		return extractFromStruct(t, v)
	case reflect.Map:
		// TODO
		return model.FilterConfig{}
	default:
		return model.FilterConfig{}
	}
}

func extractFromStruct(t reflect.Type, v reflect.Value) model.FilterConfig {
	target := model.FilterConfig{}
	typeTarget, valueTarget := reflect.TypeOf(&target).Elem(), reflect.ValueOf(&target).Elem()
	fieldmap := make(map[string]reflect.Value)
	for i := 0; i < typeTarget.NumField(); i++ {
		tf := typeTarget.Field(i).Tag.Get("file")
		if tf != "" {
			fieldmap[tf] = valueTarget.Field(i)
		}
	}

	for i := 0; i < t.NumField(); i++ {
		tf := t.Field(i).Tag.Get("file")
		if vt, ok := fieldmap[tf]; ok {
			vt.Set(v.Field(i))
		}
	}
	return target
}

func init() {
	servicehub.Register("erda.oap.collector.core", &servicehub.Spec{
		Services:    []string{},
		Description: "core logic for schedule",
		ConfigFunc: func() interface{} {
			return &config.Config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
