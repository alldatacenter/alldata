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

package kms

import (
	"context"
	"fmt"
	"sync"

	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

var (
	mgr      Manager
	initOnce sync.Once
)

type Manager struct {
	pluginFactory map[kmstypes.PluginKind]kmstypes.PluginCreateFn
	plugins       map[kmstypes.PluginKind]kmstypes.Plugin

	storeFactory map[kmstypes.StoreKind]kmstypes.StoreCreateFn
	stores       map[kmstypes.StoreKind]kmstypes.Store

	pluginCtx context.Context
	storeCtx  context.Context
}

func GetManager(ops ...Option) (*Manager, error) {
	err := mgr.initialize(ops...)
	if err != nil {
		return nil, err
	}
	return &mgr, nil
}

type Option func(*Manager)

func WithPluginConfigs(configs map[string]string) Option {
	return func(mgr *Manager) {
		mgr.pluginCtx = context.WithValue(mgr.pluginCtx, kmstypes.CtxKeyConfigMap, configs)
	}
}

func WithStoreConfigs(configs map[string]string) Option {
	return func(mgr *Manager) {
		mgr.storeCtx = context.WithValue(mgr.storeCtx, kmstypes.CtxKeyConfigMap, configs)
	}
}

func (m *Manager) initialize(ops ...Option) error {
	initOnce.Do(func() {
		m.pluginCtx = context.Background()
		m.storeCtx = context.Background()

		// apply options
		for _, op := range ops {
			op(m)
		}

		// plugin
		m.pluginFactory = kmstypes.PluginFactory
		m.plugins = make(map[kmstypes.PluginKind]kmstypes.Plugin)
		for kind, createFn := range m.pluginFactory {
			m.plugins[kind] = createFn(m.pluginCtx)
		}

		// store
		m.storeFactory = kmstypes.StoreFactory
		m.stores = make(map[kmstypes.StoreKind]kmstypes.Store)
		for kind, createFn := range m.storeFactory {
			m.stores[kind] = createFn(m.storeCtx)
		}
	})
	return nil
}

func (m *Manager) GetPlugin(pluginKind kmstypes.PluginKind, storeKind kmstypes.StoreKind) (kmstypes.Plugin, error) {
	// store
	store, err := m.GetStore(storeKind)
	if err != nil {
		return nil, err
	}

	// plugin
	plugin, ok := m.plugins[pluginKind]
	if !ok || plugin == nil {
		return nil, fmt.Errorf("not found plugin kind: %s", pluginKind)
	}
	plugin.SetStore(store)

	return plugin, nil
}

func (m *Manager) GetStore(storeKind kmstypes.StoreKind) (kmstypes.Store, error) {
	store, ok := m.stores[storeKind]
	if !ok || store == nil {
		return nil, fmt.Errorf("not found store kind: %s", storeKind)
	}
	return store, nil
}
