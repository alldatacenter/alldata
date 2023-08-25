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

package runtimehooks

import (
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/proxyserver"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/reconciler"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/rule"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
)

type HookPlugin interface {
	Register(op hooks.Options)
}

type RuntimeHook interface {
	Run(stopCh <-chan struct{}) error
}

type runtimeHook struct {
	statesInformer statesinformer.StatesInformer
	server         proxyserver.Server
	reconciler     reconciler.Reconciler
	executor       resourceexecutor.ResourceUpdateExecutor
}

func (r *runtimeHook) Run(stopCh <-chan struct{}) error {
	klog.V(5).Infof("runtime hook server start running")
	go r.executor.Run(stopCh)
	if err := r.server.Start(); err != nil {
		return err
	}
	if err := r.reconciler.Run(stopCh); err != nil {
		return err
	}
	if err := r.server.Register(); err != nil {
		return err
	}
	klog.V(5).Infof("runtime hook server has started")
	<-stopCh
	klog.Infof("runtime hook is stopped")
	return nil
}

func NewRuntimeHook(si statesinformer.StatesInformer, cfg *Config) (RuntimeHook, error) {
	failurePolicy, err := config.GetFailurePolicyType(cfg.RuntimeHooksFailurePolicy)
	if err != nil {
		return nil, err
	}
	pluginFailurePolicy, err := config.GetFailurePolicyType(cfg.RuntimeHooksPluginFailurePolicy)
	if err != nil {
		return nil, err
	}
	e := resourceexecutor.NewResourceUpdateExecutor()
	newServerOptions := proxyserver.Options{
		Network:             cfg.RuntimeHooksNetwork,
		Address:             cfg.RuntimeHooksAddr,
		HostEndpoint:        cfg.RuntimeHookHostEndpoint,
		FailurePolicy:       failurePolicy,
		PluginFailurePolicy: pluginFailurePolicy,
		ConfigFilePath:      cfg.RuntimeHookConfigFilePath,
		DisableStages:       getDisableStagesMap(cfg.RuntimeHookDisableStages),
		Executor:            e,
	}
	s, err := proxyserver.NewServer(newServerOptions)
	newReconcilerOptions := reconciler.Options{
		StatesInformer: si,
		Executor:       e,
	}

	newPluginOptions := hooks.Options{
		Executor: e,
	}

	if err != nil {
		return nil, err
	}
	r := &runtimeHook{
		statesInformer: si,
		server:         s,
		reconciler:     reconciler.NewReconciler(newReconcilerOptions),
		executor:       e,
	}
	registerPlugins(newPluginOptions)
	si.RegisterCallbacks(statesinformer.RegisterTypeNodeSLOSpec, "runtime-hooks-rule-node-slo",
		"Update hooks rule can run callbacks if NodeSLO spec update",
		rule.UpdateRules)
	si.RegisterCallbacks(statesinformer.RegisterTypeNodeTopology, "runtime-hooks-rule-node-topo",
		"Update hooks rule if NodeTopology infor update",
		rule.UpdateRules)
	if err := s.Setup(); err != nil {
		klog.Fatal("failed to setup runtime hook server, error %v", err)
		return nil, err
	}
	return r, nil
}

func registerPlugins(op hooks.Options) {
	klog.V(5).Infof("start register plugins for runtime hook")
	for hookFeature, hookPlugin := range runtimeHookPlugins {
		enabled := features.DefaultKoordletFeatureGate.Enabled(hookFeature)
		if enabled {
			hookPlugin.Register(op)
		}
		klog.Infof("runtime hook plugin %s enable %v", hookFeature, enabled)
	}
}

func getDisableStagesMap(stagesSlice []string) map[string]struct{} {
	stagesMap := map[string]struct{}{}
	for _, item := range stagesSlice {
		if _, ok := stagesMap[item]; !ok {
			stagesMap[item] = struct{}{}
		}
	}
	return stagesMap
}
