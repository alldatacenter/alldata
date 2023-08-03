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

package framework

import (
	"fmt"
	"sync"

	"k8s.io/klog/v2"
)

type PluginConfig struct {
	Plugin Plugin
}

type PluginRegistry struct {
	sync.RWMutex
	name      string
	plugins   []Plugin
	pluginSet map[string]struct{}
}

func NewRegistry(registryName string, plugins ...Plugin) *PluginRegistry {
	registry := &PluginRegistry{
		name:      registryName,
		pluginSet: map[string]struct{}{},
	}

	registry.MustRegister(plugins...)
	return registry
}

func (r *PluginRegistry) Size() int {
	r.RLock()
	defer r.RUnlock()
	return len(r.plugins)
}

func (r *PluginRegistry) MustRegister(plugins ...Plugin) {
	r.Lock()
	defer r.Unlock()
	for _, p := range plugins {
		if _, exist := r.pluginSet[p.Name()]; exist {
			klog.ErrorS(fmt.Errorf("plugin already exist"), "failed to register existing plugin",
				"registry", r.name, "plugin", p.Name())
			continue
		}
		r.plugins = append(r.plugins, p)
		r.pluginSet[p.Name()] = struct{}{}
	}
}

func (r *PluginRegistry) Unregister(name string) {
	r.Lock()
	defer r.Unlock()
	_, exist := r.pluginSet[name]
	if !exist {
		return
	}
	delete(r.pluginSet, name)
	for i := range r.plugins {
		if r.plugins[i].Name() == name {
			r.plugins = append(r.plugins[:i], r.plugins[i+1:]...)
			return
		}
	}
}

func (r *PluginRegistry) GetAll() []Plugin {
	r.RLock()
	defer r.RUnlock()
	plugins := make([]Plugin, len(r.plugins))
	copy(plugins, r.plugins)
	return plugins
}
