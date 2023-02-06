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

package executor

import (
	"context"
	"sync"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/pkg/goroutinepool"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
)

const defaultGoPool = "defaultpool"

var (
	mgr      Manager
	initonce sync.Once
)

func GetManager() *Manager {
	err := mgr.initialize()
	if err != nil {
		// init failed, just panic here
		panic(err)
	}
	return &mgr
}

// Manager is a executor manager, it holds the all executor instances.
type Manager struct {
	// update register list lock
	uLock sync.Mutex

	factory   map[executortypes.Kind]executortypes.CreateFn
	executors map[executortypes.Name]executortypes.Executor
	pools     map[executortypes.Name]*goroutinepool.GoroutinePool
	evChanMap map[executortypes.Name]executortypes.GetEventChanFn
	evCbMap   map[executortypes.Name]executortypes.EventCbFn

	executorConfigs map[executortypes.Name]*executorconfig.ExecutorWholeConfigs
	executorStopCh  map[executortypes.Name]executortypes.StopEventsChans
}

func (m *Manager) initialize() error {
	var r error
	initonce.Do(func() {
		m.pools = make(map[executortypes.Name]*goroutinepool.GoroutinePool)
		m.pools[defaultGoPool] = goroutinepool.New(conf.PoolSize())
		m.pools[defaultGoPool].Start()

		m.executors = make(map[executortypes.Name]executortypes.Executor)
		m.factory = executortypes.Factory
		m.evChanMap = executortypes.EvFuncMap
		m.evCbMap = executortypes.EvCbMap
		m.executorConfigs = make(map[executortypes.Name]*executorconfig.ExecutorWholeConfigs)
		m.executorStopCh = make(map[executortypes.Name]executortypes.StopEventsChans)

		f := func(k string, v interface{}, t storetypes.ChangeType) {
			config, ok := v.(*executorconfig.ExecutorConfig)
			if !ok {
				logrus.Errorf("not executorConfig type, key: %s, value: %+v", k, v)
				return
			}
			switch t {
			case storetypes.Del:
				logrus.Infof("watched executor(%s) deleted, key: %s", config.Name, k)
				deleteOneExecutor(m, config)
			case storetypes.Update:
				logrus.Infof("watched executor(%s) updated, key: %s", config.Name, k)
				conf.GetConfStore().ExecutorStore.Store(config.Name, config)
				m.pools[defaultGoPool].MustGo(func() {
					if err := updateOneExecutor(m, config); err != nil {
						logrus.Errorf("updating executor(name: %s, key: %s) error: %v", config.Name, k, err)
					}
				})
			case storetypes.Add:
				logrus.Infof("watched executor(%s) created, key: %s", config.Name, k)
				conf.GetConfStore().ExecutorStore.Store(config.Name, config)
				// Use default goroutine pool to execute creating job.
				m.pools[defaultGoPool].MustGo(func() {
					if err := createOneExecutor(m, config); err != nil {
						logrus.Errorf("failed to create executor(name: %s, key: %s) error: %v", config.Name, k, err)
					}
				})
			}
		}
		option := jsonstore.UseMemEtcdStore(context.Background(), conf.CLUSTERS_CONFIG_PATH, f, executorconfig.ExecutorConfig{})
		if _, err := jsonstore.New(option); err != nil {
			r = err
		}
		logrus.Infof("executor factories:%+v", m.factory)
	})
	return r
}

func createOneExecutor(m *Manager, eConfig *executorconfig.ExecutorConfig) error {
	logrus.Infof("[createOneExecutor] config: %+v", eConfig)
	create, ok := m.factory[executortypes.Kind(eConfig.Kind)]
	if !ok {
		return errors.Errorf("executor kind (%s) not found, factory: %v", eConfig.Kind, m.factory)
	}

	name := executortypes.Name(eConfig.Name)

	if _, ok := m.executors[name]; ok {
		logrus.Warnf("executor %s already registered, skip", name)
		return nil
	}

	executor, err := create(name, eConfig.ClusterName, eConfig.Options, eConfig.OptionsPlus)
	if err != nil {
		return err
	}

	m.uLock.Lock()
	defer m.uLock.Unlock()

	m.executors[name] = executor
	m.pools[name] = goroutinepool.New(conf.PoolSize())
	m.pools[name].Start()

	// TODO: dont create manually
	wholeConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}
	m.executorConfigs[name] = wholeConfigs
	conf.GetConfStore().ExecutorStore.Store(eConfig.Name, eConfig)

	logrus.Infof("created executor: %s", name)

	if getEvChanFn, ok := m.evChanMap[name]; ok {
		eventCh, stopEventWatchCh, lstore, err := getEvChanFn(name)
		if err != nil {
			logrus.Errorf("getEvChanFn for executor(%s) err: %v", name, err)
		}

		logrus.Infof("executor(%s) found event channel, lstore addr: %p", name, lstore)
		cb, ok := m.evCbMap[name]
		if !ok {
			logrus.Errorf("executor(%s) cannot find event cb", name)
		}
		stopEventHandleCh := make(chan struct{}, 1)
		m.executorStopCh[name] = executortypes.StopEventsChans{StopWatchEventCh: stopEventWatchCh, StopHandleEventCh: stopEventHandleCh}
		go events.HandleOneExecutorEvent(string(name), eventCh, lstore, cb, stopEventHandleCh)
	}
	return nil
}

func deleteOneExecutor(m *Manager, config *executorconfig.ExecutorConfig) {
	logrus.Infof("[deleteOneExecutor] config: %+v", config)
	name := executortypes.Name(config.Name)
	if chs, ok := m.executorStopCh[name]; ok {
		logrus.Infof("close stop watch event ch on %s", config.Name)
		close(chs.StopWatchEventCh)
		logrus.Infof("close stop handle event ch on %s", config.Name)
		close(chs.StopHandleEventCh)
	}

	if p, ok := m.pools[name]; ok {
		p.Stop()
	}

	executortypes.UnRegisterEvChan(executortypes.Name(config.Name))
	events.GetEventManager().UnRegisterEventCallback(config.Name)

	if executor, ok := m.executors[executortypes.Name(config.Name)]; ok {
		executor.CleanUpBeforeDelete()
	}

	// Delete the creation function corresponding to the executor
	delete(m.executors, executortypes.Name(config.Name))
	// Delete the configuration of the executor
	delete(m.executorConfigs, executortypes.Name(config.Name))
	//
	//delete(m.executorStopCh, executortypes.Name(config.Name))
	// Delete the map corresponding to the relationship between executor name and cluster name in conf
	conf.GetConfStore().ExecutorStore.Delete(config.Name)
}

func updateOneExecutor(m *Manager, config *executorconfig.ExecutorConfig) error {
	name := executortypes.Name(config.Name)
	_, ok := m.executors[name]
	logrus.Infof("updating executor: %s", config.Name)
	if ok {
		deleteOneExecutor(m, config)
	}
	return createOneExecutor(m, config)
}

func (m *Manager) Pool(name executortypes.Name) *goroutinepool.GoroutinePool {
	p, ok := m.pools[name]
	if !ok {
		return m.pools[defaultGoPool]
	}
	return p
}

// Get returns the executor with name.
func (m *Manager) Get(name executortypes.Name) (executortypes.Executor, error) {
	c, ok := m.executors[name]
	if !ok {
		k, e, err := clusterutil.GetExecuteConfigByExecutorName(name)
		if err != nil {
			logrus.Errorf("faield to recreate executor, name: %v, err: %v", name, err)
			return nil, err
		}
		// TODO: execute once when concurrent request
		m.pools[defaultGoPool].MustGo(func() {
			if err := createOneExecutor(m, e); err != nil {
				logrus.Errorf("failed to create executor(name: %s, key: %s) error: %v", e.Name, k, err)
			}
		})
		return nil, errors.Errorf("try to create executor %s, please try again later", name)
	}
	return c, nil
}

// GetByKind returns the executor instances with specify kind.
func (m *Manager) GetByKind(kind executortypes.Kind) []executortypes.Executor {
	executors := make([]executortypes.Executor, 0, len(m.executors))
	for _, c := range m.executors {
		if c.Kind() == kind {
			executors = append(executors, c)
		}
	}
	return executors
}

// ListExecutors returns the all executor instances.
func (m *Manager) ListExecutors() []executortypes.Executor {
	executors := make([]executortypes.Executor, 0, len(m.executors))
	for _, c := range m.executors {
		executors = append(executors, c)
	}
	return executors
}

func (m *Manager) PrintPoolUsage() {
	for exc, pool := range m.pools {
		stat := pool.Statistics()
		total := stat[1]
		inuse := total - stat[0]
		logrus.Infof("[%s] pool: total worker num: %d, inuse worker num: %d", exc, total, inuse)
	}
}

// GetJobExecutorKindByName return executor Kind, e.g. METRONOME, according to user-defined job specification.
func GetJobExecutorKindByName(name string) string {
	e, err := GetManager().Get(executortypes.Name(name))
	if err != nil {
		logrus.Errorf("[alert] failed to get executor by name %s", name)
		return ""
	}
	return string(e.Kind())
}

func (m *Manager) GetExecutorConfigs(name executortypes.Name) (*executorconfig.ExecutorWholeConfigs, error) {
	config, ok := m.executorConfigs[name]
	if !ok {
		return nil, errors.Errorf("[GetExecutorConfigs] not found executor: %s", name)
	}
	if config == nil {
		return nil, errors.Errorf("get executor(%s) configs null", name)
	}
	return config, nil
}
