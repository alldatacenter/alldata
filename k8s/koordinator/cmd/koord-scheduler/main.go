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

package main

import (
	"math/rand"
	"os"
	"time"

	"k8s.io/component-base/logs"
	frameworkruntime "k8s.io/kubernetes/pkg/scheduler/framework/runtime"

	"github.com/koordinator-sh/koordinator/cmd/koord-scheduler/app"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/batchresource"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/compatibledefaultpreemption"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/deviceshare"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/elasticquota"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/loadaware"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/nodenumaresource"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/reservation"

	// Ensure metric package is initialized
	_ "k8s.io/component-base/metrics/prometheus/clientgo"
	// Ensure scheme package is initialized.
	_ "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/scheme"
)

var koordinatorPlugins = map[string]frameworkruntime.PluginFactory{
	loadaware.Name:                   loadaware.New,
	nodenumaresource.Name:            nodenumaresource.New,
	reservation.Name:                 reservation.New,
	batchresource.Name:               batchresource.New,
	coscheduling.Name:                coscheduling.New,
	deviceshare.Name:                 deviceshare.New,
	elasticquota.Name:                elasticquota.New,
	compatibledefaultpreemption.Name: compatibledefaultpreemption.New,
}

// Register custom scheduling hooks for pre-process scheduling context before call plugins.
// e.g. change the nodeInfo and make a copy before calling filter plugins
var schedulingHooks = []frameworkext.SchedulingPhaseHook{
	reservation.NewHook(),
}

func flatten(plugins map[string]frameworkruntime.PluginFactory) []app.Option {
	options := make([]app.Option, 0, len(plugins))
	for name, factoryFn := range plugins {
		options = append(options, app.WithPlugin(name, factoryFn))
	}
	return options
}

func main() {
	rand.Seed(time.Now().UnixNano())

	// Register custom plugins to the scheduler framework.
	// Later they can consist of scheduler profile(s) and hence
	// used by various kinds of workloads.
	command := app.NewSchedulerCommand(
		schedulingHooks,
		flatten(koordinatorPlugins)...,
	)

	logs.InitLogs()
	defer logs.FlushLogs()

	if err := command.Execute(); err != nil {
		os.Exit(1)
	}
}
