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

package v1alpha2

import (
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/names"
)

// getDefaultPlugins returns the default set of plugins.
func getDefaultPlugins() *Plugins {
	plugins := &Plugins{
		Deschedule: PluginSet{
			Enabled: []Plugin{
				// NOTE: add default deschedule plugins here.
			},
		},
		Evictor: PluginSet{
			Enabled: []Plugin{
				{Name: names.MigrationController},
			},
		},
	}
	return plugins
}

// mergePlugins merges the custom set into the given default one, handling disabled sets.
func mergePlugins(defaultPlugins, customPlugins *Plugins) *Plugins {
	if customPlugins == nil {
		return defaultPlugins
	}

	defaultPlugins.Deschedule = mergePluginSet(defaultPlugins.Deschedule, customPlugins.Deschedule)
	defaultPlugins.Balance = mergePluginSet(defaultPlugins.Balance, customPlugins.Balance)
	defaultPlugins.Evictor = mergePluginSet(defaultPlugins.Evictor, customPlugins.Evictor)
	return defaultPlugins
}

type pluginIndex struct {
	index  int
	plugin Plugin
}

func mergePluginSet(defaultPluginSet, customPluginSet PluginSet) PluginSet {
	disabledPlugins := sets.NewString()
	enabledCustomPlugins := make(map[string]pluginIndex)
	// replacedPluginIndex is a set of index of plugins, which have replaced the default plugins.
	replacedPluginIndex := sets.NewInt()
	for _, disabledPlugin := range customPluginSet.Disabled {
		disabledPlugins.Insert(disabledPlugin.Name)
	}
	for index, enabledPlugin := range customPluginSet.Enabled {
		enabledCustomPlugins[enabledPlugin.Name] = pluginIndex{index, enabledPlugin}
	}
	var enabledPlugins []Plugin
	if !disabledPlugins.Has("*") {
		for _, defaultEnabledPlugin := range defaultPluginSet.Enabled {
			if disabledPlugins.Has(defaultEnabledPlugin.Name) {
				continue
			}
			// The default plugin is explicitly re-configured, update the default plugin accordingly.
			if customPlugin, ok := enabledCustomPlugins[defaultEnabledPlugin.Name]; ok {
				klog.InfoS("Default plugin is explicitly re-configured; overriding", "plugin", defaultEnabledPlugin.Name)
				// Update the default plugin in place to preserve order.
				defaultEnabledPlugin = customPlugin.plugin
				replacedPluginIndex.Insert(customPlugin.index)
			}
			enabledPlugins = append(enabledPlugins, defaultEnabledPlugin)
		}
	}

	// Append all the custom plugins which haven't replaced any default plugins.
	// Note: duplicated custom plugins will still be appended here.
	// If so, the instantiation of descheduler framework will detect it and abort.
	for index, plugin := range customPluginSet.Enabled {
		if !replacedPluginIndex.Has(index) {
			enabledPlugins = append(enabledPlugins, plugin)
		}
	}
	return PluginSet{Enabled: enabledPlugins}
}
