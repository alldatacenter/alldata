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

package nodeinfo

import (
	"time"

	"go.uber.org/atomic"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
)

const (
	CollectorName = "NodeInfoCollector"
)

// TODO more ut is needed for this plugin
type nodeInfoCollector struct {
	collectInterval time.Duration
	metricDB        metriccache.MetricCache
	started         *atomic.Bool
}

func New(opt *framework.Options) framework.Collector {
	return &nodeInfoCollector{
		collectInterval: opt.Config.CollectNodeCPUInfoInterval,
		metricDB:        opt.MetricCache,
		started:         atomic.NewBool(false),
	}
}

func (n *nodeInfoCollector) Enabled() bool {
	return true
}

func (n *nodeInfoCollector) Setup(s *framework.Context) {}

func (n *nodeInfoCollector) Run(stopCh <-chan struct{}) {
	go wait.Until(n.collectNodeCPUInfo, n.collectInterval, stopCh)
}

func (n *nodeInfoCollector) Started() bool {
	return n.started.Load()
}

func (n *nodeInfoCollector) collectNodeCPUInfo() {
	klog.V(6).Info("start nodeInfoCollector")

	localCPUInfo, err := koordletutil.GetLocalCPUInfo()
	if err != nil {
		klog.Warningf("failed to collect node cpu info, err: %s", err)
		metrics.RecordCollectNodeCPUInfoStatus(err)
		return
	}

	nodeCPUInfo := &metriccache.NodeCPUInfo{
		BasicInfo:      localCPUInfo.BasicInfo,
		ProcessorInfos: localCPUInfo.ProcessorInfos,
		TotalInfo:      localCPUInfo.TotalInfo,
	}
	klog.V(6).Infof("collect cpu info finished, nodeCPUInfo %v", nodeCPUInfo)
	if err = n.metricDB.InsertNodeCPUInfo(nodeCPUInfo); err != nil {
		klog.Errorf("insert node cpu info error: %v", err)
	}

	n.started.Store(true)
	klog.Infof("collectNodeCPUInfo finished, cpu info: processors %v", len(nodeCPUInfo.ProcessorInfos))
	metrics.RecordCollectNodeCPUInfoStatus(nil)
}
