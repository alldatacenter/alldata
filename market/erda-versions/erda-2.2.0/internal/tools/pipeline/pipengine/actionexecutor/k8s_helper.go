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

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
)

func (m *Manager) deleteK8sExecutor(cluster apistructs.ClusterInfo) {
	m.Lock()
	defer m.Unlock()
	for kind := range m.factory {
		if kind.IsK8sKind() {
			name := kind.MakeK8sKindExecutorName(cluster.Name)
			delete(m.kindsByName, name)
			delete(m.executorsByName, name)
		}
	}
}

func (m *Manager) updateK8sExecutor(cluster apistructs.ClusterInfo) error {
	// create a duplication of k8s kind create fn
	k8sFactory := make(map[types.Kind]types.CreateFn)
	m.Lock()
	for kind, createFn := range m.factory {
		if kind.IsK8sKind() {
			k8sFactory[kind] = createFn
		}
	}
	m.Unlock()
	for kind, createFn := range k8sFactory {
		name := kind.MakeK8sKindExecutorName(cluster.Name)
		actionExecutor, err := createFn(name, nil)
		if err != nil {
			return errors.Errorf("executor [%s] created failed, err: %v", name, err)
		}
		m.Lock()
		m.kindsByName[name] = kind
		m.executorsByName[actionExecutor.Name()] = actionExecutor
		m.Unlock()
	}
	return nil
}

func (m *Manager) batchUpdateK8sExecutor() error {
	clusters, err := m.clusterInfo.ListAllClusterInfos()
	if err != nil {
		return err
	}
	m.pools.Start()
	for i := range clusters {
		if clusters[i].Type != apistructs.K8S && clusters[i].Type != apistructs.EDAS {
			continue
		}
		cluster := clusters[i]
		m.pools.MustGo(func() {
			m.updateK8sExecutor(cluster)
		})
	}
	m.pools.Stop()
	return nil
}

func (m *Manager) ListenAndPatchK8sExecutor(ctx context.Context) {
	triggerChan := m.clusterInfo.RegisterRefreshEvent()
	eventChan := m.clusterInfo.RegisterClusterEvent()
	for {
		select {
		case <-ctx.Done():
			return
		case <-triggerChan:
			if err := m.batchUpdateK8sExecutor(); err != nil {
				logrus.Errorf("failed to batch update k8s executor, err: %v", err)
			}
		case event := <-eventChan:
			switch event.Action {
			case apistructs.ClusterActionCreate:
				if err := m.updateK8sExecutor(event.Content); err != nil {
					logrus.Errorf("failed to add k8s executor, err: %v", err)
				}
			case apistructs.ClusterActionUpdate:
				if err := m.updateK8sExecutor(event.Content); err != nil {
					logrus.Errorf("failed to update k8s executor, err: %v", err)
				}
			case apistructs.ClusterActionDelete:
				m.deleteK8sExecutor(event.Content)
			}
		}
	}
}
