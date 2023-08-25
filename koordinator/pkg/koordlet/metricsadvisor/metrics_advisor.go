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

package metricsadvisor

import (
	"time"

	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/collectors/beresource"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/collectors/nodeinfo"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/collectors/noderesource"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/collectors/performance"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/collectors/podresource"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/collectors/podthrottled"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/devices/gpu"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
)

type MetricAdvisor interface {
	Run(stopCh <-chan struct{}) error
	HasSynced() bool
}

var (
	devicePlugins = map[string]framework.DeviceFactory{
		gpu.DeviceCollectorName: gpu.New,
	}

	collectorPlugins = map[string]framework.CollectorFactory{
		noderesource.CollectorName: noderesource.New,
		beresource.CollectorName:   beresource.New,
		nodeinfo.CollectorName:     nodeinfo.New,
		podresource.CollectorName:  podresource.New,
		podthrottled.CollectorName: podthrottled.New,
		performance.CollectorName:  performance.New,
	}
)

type metricAdvisor struct {
	options *framework.Options
	context *framework.Context
}

func NewMetricAdvisor(cfg *framework.Config, statesInformer statesinformer.StatesInformer, metricCache metriccache.MetricCache) MetricAdvisor {
	opt := &framework.Options{
		Config:         cfg,
		StatesInformer: statesInformer,
		MetricCache:    metricCache,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	}
	ctx := &framework.Context{
		DeviceCollectors: make(map[string]framework.DeviceCollector, len(devicePlugins)),
		Collectors:       make(map[string]framework.Collector, len(collectorPlugins)),
	}
	for name, device := range devicePlugins {
		ctx.DeviceCollectors[name] = device(opt)
	}
	for name, collector := range collectorPlugins {
		ctx.Collectors[name] = collector(opt)
	}

	c := &metricAdvisor{
		options: opt,
		context: ctx,
	}
	return c
}

func (m *metricAdvisor) HasSynced() bool {
	return framework.CollectorsHasStarted(m.context.Collectors)
}

func (m *metricAdvisor) Run(stopCh <-chan struct{}) error {
	defer utilruntime.HandleCrash()
	if m.options.Config.CollectResUsedInterval < time.Second {
		klog.Infof("CollectResUsedInterval is %v, metric collector is disabled",
			m.options.Config.CollectResUsedInterval)
		return nil
	}

	defer m.shutdown()
	m.setup()

	defer klog.Info("shutting down metric advisor")
	klog.Info("Starting collector for NodeMetric")

	for name, dc := range m.context.DeviceCollectors {
		klog.V(4).Infof("ready to start device collector %v", name)
		if !dc.Enabled() {
			klog.V(4).Infof("device collector %v is not enabled, skip running", name)
			continue
		}
		go dc.Run(stopCh)
		klog.V(4).Infof("device collector %v start", name)
	}

	for name, collector := range m.context.Collectors {
		klog.V(4).Infof("ready to start collector %v", name)
		if !collector.Enabled() {
			klog.V(4).Infof("collector %v is not enabled, skip running", name)
			continue
		}
		go collector.Run(stopCh)
		klog.V(4).Infof("collector %v start", name)
	}

	klog.Info("Starting successfully")
	<-stopCh
	return nil
}

func (m *metricAdvisor) setup() {
	for _, device := range m.context.DeviceCollectors {
		device.Setup(m.context)
	}
	for _, collector := range m.context.Collectors {
		collector.Setup(m.context)
	}
}

func (m *metricAdvisor) shutdown() {
	for _, dc := range m.context.DeviceCollectors {
		dc.Shutdown()
	}
}
