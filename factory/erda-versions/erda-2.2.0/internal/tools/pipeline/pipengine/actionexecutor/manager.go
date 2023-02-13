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

package actionexecutor

import (
	"context"
	"strings"
	"sync"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/goroutinepool"
)

// Manager is an executor manager, it holds the all executor instances.
type Manager struct {
	sync.RWMutex
	factory         map[types.Kind]types.CreateFn
	executorsByName map[types.Name]types.ActionExecutor
	kindsByName     map[types.Name]types.Kind
	pools           *goroutinepool.GoroutinePool
	clusterInfo     clusterinfo.Interface
}

var mgr Manager

func GetManager() *Manager {
	return &mgr
}

func (m *Manager) Initialize(ctx context.Context, cfgs chan spec.ActionExecutorConfig, clusterInfo clusterinfo.Interface) error {
	m.factory = types.Factory
	m.executorsByName = make(map[types.Name]types.ActionExecutor)
	m.kindsByName = make(map[types.Name]types.Kind)
	m.pools = goroutinepool.New(conf.K8SExecutorPoolSize())
	m.clusterInfo = clusterInfo

	logrus.Info("pipengine action executor manager Initialize ...")

	for len(cfgs) > 0 {
		c := <-cfgs

		create, err := m.GetCreateFnByKind(types.Kind(c.Kind))
		if err != nil {
			return err
		}
		kind := types.Kind(c.Kind)
		if kind.IsK8sKind() {
			// because clusters are too many, k8s type executor will be created later
			continue
		}

		name := types.Name(c.Name)
		for k, v := range c.Options {
			logrus.Infof("=> kind [%s] option: %s=%s", c.Kind, k, v)
		}

		actionExecutor, err := create(name, c.Options)
		if err != nil {
			logrus.Infof("=> kind [%s] created failed, err: %v", c.Kind, err)
			return err
		}
		m.Lock()
		m.kindsByName[name] = kind
		m.executorsByName[actionExecutor.Name()] = actionExecutor
		m.Unlock()

		logrus.Infof("=> kind [%s] created", c.Kind)
	}
	go m.ListenAndPatchK8sExecutor(ctx)

	logrus.Info("pipengine action executor manager Initialize Done .")

	return nil
}

// Get returns the executor with name.
func (m *Manager) Get(name types.Name) (types.ActionExecutor, error) {
	if len(name) == 0 {
		return nil, errors.Errorf("executor name is empty")
	}
	m.RLock()
	e, ok := m.executorsByName[name]
	m.RUnlock()
	if ok {
		return e, nil
	}
	kind, ok := m.GetKindByExecutorName(name)
	if !ok {
		return nil, errors.Errorf("executor name [%s] is not found", name)
	}
	m.Lock()
	m.kindsByName[name] = kind
	createFn, ok := m.factory[kind]
	m.Unlock()
	if !ok {
		return nil, errors.Errorf("executor kind [%s] not found", kind)
	}
	actionExecutor, err := createFn(name, nil)
	if err != nil {
		return nil, errors.Errorf("executor [%s] created failed, err: %v", name, err)
	}
	m.Lock()
	m.executorsByName[actionExecutor.Name()] = actionExecutor
	m.Unlock()
	return actionExecutor, nil
}

func (m *Manager) GetCreateFnByKind(kind types.Kind) (types.CreateFn, error) {
	if len(kind) == 0 {
		return nil, errors.Errorf("executor kind is empty")
	}
	m.Lock()
	f, ok := m.factory[kind]
	m.Unlock()
	if !ok {
		return nil, errors.Errorf("unregistered action executor kind: [%s]", kind)
	}
	return f, nil
}

func (m *Manager) GetKindByExecutorName(name types.Name) (types.Kind, bool) {
	m.RLock()
	defer m.RUnlock()
	// could find the normal or existed kind in the kind cache, return it
	if k, ok := m.kindsByName[name]; ok {
		return k, true
	}
	// could not find the normal kind in the kind cache, try to make the k8s kind
	for k := range m.factory {
		if k.IsK8sKind() && strings.HasPrefix(name.String(), k.MakeK8sKindExecutorName("").String()) {
			return k, true
		}
	}
	return "", false
}

// GetByKind returns the executor instances with specify kind.
func (m *Manager) GetByKind(kind types.Kind) []types.ActionExecutor {
	executors := make([]types.ActionExecutor, 0, len(m.executorsByName))
	for _, e := range m.executorsByName {
		if e.Kind() == kind {
			executors = append(executors, e)
		}
	}
	return executors
}

// ListExecutors returns the all executor instances.
func (m *Manager) ListExecutors() []types.ActionExecutor {
	executors := make([]types.ActionExecutor, 0, len(m.executorsByName))
	for _, e := range m.executorsByName {
		executors = append(executors, e)
	}
	return executors
}

// GetActionExecutorKindByName return executor Kind, e.g. MEMORY
func (m *Manager) GetActionExecutorKindByName(name string) (string, error) {
	e, err := m.Get(types.Name(name))
	if err != nil {
		return "", errors.Wrapf(err, "failed to get action executor by name [%s]", name)
	}
	return string(e.Kind()), nil
}

func GetExecutorInfo() interface{} {
	executors := map[types.Kind][]types.Name{}
	mgr.RLock()
	defer mgr.RUnlock()
	for name, exe := range mgr.executorsByName {
		executors[exe.Kind()] = append(executors[exe.Kind()], name)
	}
	return []interface{}{executors}
}
