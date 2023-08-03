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

package plugins

import (
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/qosmanager/k8s"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/qosmanager/metricsquery"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
)

type PluginContext struct {
	K8sClient      k8s.K8sClient
	StatesInformer statesinformer.StatesInformer
	MetricCache    metriccache.MetricCache
	MetricsQuery   metricsquery.MetricsQuery
	// Extra custom configuration for plugin.
	ExtraConfig *string
}
