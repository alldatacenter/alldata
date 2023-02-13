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

package tagger

import (
	"context"
	"time"

	apiv1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/watch"

	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"
	pod2 "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/k8s-tagger/metadata/pod"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/kubernetes"
)

var providerName = plugins.WithPrefixProcessor("k8s-tagger")

type config struct {
	Pod     pod2.Config         `file:"pod"`
	Keypass map[string][]string `file:"keypass"`
}

// +provider
type provider struct {
	Cfg        *config
	Log        logs.Logger
	Kubernetes kubernetes.Interface `autowired:"kubernetes"`

	// cache
	podCache *pod2.Cache
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) ProcessMetric(item *metric.Metric) (*metric.Metric, error) {
	podMeta := p.getPodMetadata(item.Tags)
	for k, v := range podMeta.Tags {
		item.Tags[k] = v
	}
	for k, v := range podMeta.Fields {
		item.Fields[k] = v
	}
	return item, nil
}

func (p *provider) ProcessLog(item *log.Log) (*log.Log, error) {
	podMeta := p.getPodMetadata(item.Tags)
	for k, v := range podMeta.Tags {
		item.Tags[k] = v
	}
	return item, nil
}

func (p *provider) ProcessSpan(item *trace.Span) (*trace.Span, error) {
	podMeta := p.getPodMetadata(item.Tags)
	for k, v := range podMeta.Tags {
		item.Tags[k] = v
	}
	return item, nil
}

func (p *provider) ProcessRaw(item *odata.Raw) (*odata.Raw, error) { return item, nil }

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	to, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()

	return p.initCache(to)
}

// Run this is optional
func (p *provider) Run(ctx context.Context) error {
begin:
	w, err := p.Kubernetes.Client().CoreV1().Pods(p.Cfg.Pod.WatchSelector.Namespace).Watch(ctx, metav1.ListOptions{
		LabelSelector: p.Cfg.Pod.WatchSelector.LabelSelector,
		FieldSelector: p.Cfg.Pod.WatchSelector.FieldSelector,
	})
	defer func() {
		if w != nil {
			w.Stop()
		}
	}()

	if err != nil {
		return err
	}
	p.Log.Infof("watch loop start...")
	for {
		select {
		case <-ctx.Done():
			return nil
		case event, ok := <-w.ResultChan():
			if !ok {
				p.Log.Errorf("closed ResultChan received. Watch retry...")
				goto begin
			}
			switch event.Type {
			case watch.Added, watch.Modified:
				switch event.Object.(type) {
				case *apiv1.Pod:
					p.podCache.AddOrUpdate(event.Object.(*apiv1.Pod))
				}
			case watch.Deleted:
				// TODO may need delay
				switch event.Object.(type) {
				case *apiv1.Pod:
					p.podCache.Delete(event.Object.(*apiv1.Pod))
				}
			default:
			}
		}
	}
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services: []string{
			providerName,
		},
		Description: "here is description of erda.oap.collector.processor.k8s-tagger",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
