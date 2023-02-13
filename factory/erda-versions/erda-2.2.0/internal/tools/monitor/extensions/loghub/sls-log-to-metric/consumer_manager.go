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

package sls

import (
	"context"
	"sync"
	"time"

	sls "github.com/aliyun/aliyun-log-go-sdk"
	"github.com/recallsong/go-utils/errorx"

	"github.com/erda-project/erda-infra/base/logs"
)

// ConsumerManager .
type ConsumerManager struct {
	log       logs.Logger
	account   *Account
	clients   map[string]sls.ClientInterface
	projects  map[string]map[string]*projectConsumers
	processor Processor

	reloadInterval time.Duration
	reloadCh       chan *Account
	closeCh        chan struct{}
	wg             sync.WaitGroup
}

func newConsumerManager(log logs.Logger, account *Account, reloadInterval time.Duration, processor Processor) *ConsumerManager {
	return &ConsumerManager{
		log:            log,
		account:        account,
		reloadInterval: reloadInterval,
		processor:      processor,
		closeCh:        make(chan struct{}),
	}
}

// Start .
func (cm *ConsumerManager) Start(ctx context.Context) error {
	cm.wg.Add(1)
	cm.reloadClient(nil)
	timer := time.NewTimer(0)
	defer func() {
		cm.closeAllConsumers()
		cm.wg.Done()
	}()

	for {
		select {
		case <-cm.closeCh:
			return nil
		case <-ctx.Done():
			return nil
		case <-timer.C:
		case account := <-cm.reloadCh:
			reload := cm.reloadClient(account)
			if !reload {
				continue
			}
		}

		cm.loadAllEndpoints()
		timer.Reset(cm.reloadInterval)
	}
}

// Close .
func (cm *ConsumerManager) Close() error {
	close(cm.closeCh)
	cm.wg.Wait()
	return nil
}

func (cm *ConsumerManager) closeAllConsumers() {
	for endpoint, projects := range cm.projects {
		for project, consumers := range projects {
			err := consumers.Close()
			if err != nil {
				cm.log.Errorf("failed to close project %s / %s: %s", endpoint, project, err)
			}
		}
	}
	cm.projects = make(map[string]map[string]*projectConsumers)
}

func (cm *ConsumerManager) Patch(account *Account) {
	cm.reloadCh <- account
}

func (cm *ConsumerManager) reloadClient(account *Account) bool {
	if account == nil {
		account = cm.account
	}

	if cm.account.AccessKey != account.AccessKey ||
		cm.account.AccessSecretKey != account.AccessSecretKey {
		cm.closeAllConsumers()
	}

	if cm.clients == nil {
		cm.clients = make(map[string]sls.ClientInterface)
	}

	endpoints := make(map[string]bool)
	for _, endpoint := range account.Endpoints {
		endpoints[endpoint] = true
		if _, ok := cm.clients[endpoint]; ok {
			continue
		}
		cm.clients[endpoint] = sls.CreateNormalInterface(endpoint, account.AccessKey, account.AccessSecretKey, "")
	}

	for endpoint, client := range cm.clients {
		if !endpoints[endpoint] {
			// close all projects in this endpoint
			for project, consumers := range cm.projects[endpoint] {
				err := consumers.Close()
				if err != nil {
					cm.log.Errorf("failed to close project %s / %s: %s", endpoint, project, err)
				}
			}
			delete(cm.projects, endpoint)

			err := client.Close()
			if err != nil {
				cm.log.Errorf("failed to close client(%s): %s", endpoint, err)
			}
			delete(cm.clients, endpoint)
		}
	}
	cm.account = account
	return true
}

func (cm *ConsumerManager) loadAllEndpoints() {
	for endpoint, client := range cm.clients {
		cm.loadProjects(endpoint, client)
	}
}

// loadProjects load all the projects below this region.
func (cm *ConsumerManager) loadProjects(endpoint string, client sls.ClientInterface) {
	allProjects, err := cm.listProjects(client)
	if err != nil {
		cm.log.Errorf("failed to list sls projects for endpoint(%s): %s", endpoint, err)
		return
	}

	if cm.projects == nil {
		cm.projects = make(map[string]map[string]*projectConsumers)
	}
	projects := cm.projects[endpoint]
	if projects == nil {
		projects = make(map[string]*projectConsumers)
		cm.projects[endpoint] = projects
	}
	for name := range allProjects {
		if !cm.processor.MatchProject(name) {
			continue
		}
		consumers, ok := projects[name]
		if !ok {
			consumers = newProjectConsumers(cm.log, cm.account, endpoint, name)
			projects[name] = consumers
		}
		consumers.LoadLogStores(client, cm.processor)
	}
	for name, consumers := range projects {
		if _, ok := allProjects[name]; !ok {
			err := consumers.Close()
			if err != nil {
				cm.log.Errorf("failed to close project %s: %s", name, err)
			}
			delete(projects, name)
		}
	}
}

func (cm *ConsumerManager) listProjects(client sls.ClientInterface) (map[string]*sls.LogProject, error) {
	result := make(map[string]*sls.LogProject)
	const pageSize = 1000
	offset := 0
	for {
		projects, count, total, err := client.ListProjectV2(offset, pageSize)
		if err != nil {
			return nil, err
		}
		for _, item := range projects {
			project := item // copy
			result[project.Name] = &project
		}
		if offset+count >= total {
			break
		}
		offset += count
	}
	return result, nil
}

// projectConsumers .
type projectConsumers struct {
	log       logs.Logger
	account   *Account
	endpoint  string
	project   string
	consumers map[string]*Consumer
}

func newProjectConsumers(log logs.Logger, account *Account, endpoint, project string) *projectConsumers {
	return &projectConsumers{
		log:       log,
		account:   account,
		endpoint:  endpoint,
		project:   project,
		consumers: make(map[string]*Consumer),
	}
}

func (pc *projectConsumers) LoadLogStores(client sls.ClientInterface, processor Processor) {
	list, err := client.ListLogStore(pc.project)
	if err != nil {
		pc.log.Errorf("failed to load log stores from project(%s): %s", pc.project, err)
		return
	}
	logStores := make(map[string]bool)
	for _, logStore := range list {
		logStores[logStore] = true
	}

	for logStore := range logStores {
		data := processor.MatchStore(pc.project, logStore)
		if data == nil {
			continue
		}
		c, ok := pc.consumers[logStore]
		if !ok {
			c = newConsumer(pc.log, pc.account, pc.endpoint, pc.project, logStore, processor.GetHandler(pc.project, logStore, data.Type, pc.account))
			err := c.Start()
			if err != nil {
				pc.log.Error(err)
				continue
			}
			pc.consumers[logStore] = c
		}
	}

	for logStore, c := range pc.consumers {
		if !logStores[logStore] {
			err := c.Close()
			if err != nil {
				pc.log.Errorf("failed to close consumer{log_store=%q}: %s", logStore, err)
			}
			delete(pc.consumers, logStore)
		}
	}
}

// Close .
func (pc *projectConsumers) Close() error {
	var errs errorx.Errors
	for _, c := range pc.consumers {
		err := c.Close()
		if err != nil {
			errs = append(errs, err)
		}
	}
	return errs.MaybeUnwrap()
}
