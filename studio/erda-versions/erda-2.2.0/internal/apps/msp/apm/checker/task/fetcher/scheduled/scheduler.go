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

package scheduled

import (
	"context"
	"math"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage"
)

// Scheduler .
type Scheduler struct {
	log        logs.Logger
	source     storage.Interface
	storage    ScheduleStorage
	interval   time.Duration
	scheduleCh chan struct{}
}

// NewScheduler .
func NewScheduler(source storage.Interface, storage ScheduleStorage, interval time.Duration, log logs.Logger) *Scheduler {
	return &Scheduler{
		log:        log,
		source:     source,
		storage:    storage,
		interval:   interval,
		scheduleCh: make(chan struct{}, 1),
	}
}

// Run .
func (s *Scheduler) Run(ctx context.Context) {
	for {
		s.schedule()
		select {
		case <-time.After(s.interval):
		case <-s.scheduleCh:
		case <-ctx.Done():
			return
		}
	}
}

func (s *Scheduler) schedule() {
	nodes, err := s.storage.Nodes()
	if err != nil {
		s.log.Errorf("fail to get nodes: %s", err)
		return
	}
	defer func() {
		err := s.storage.NodesKeepAlive(nodes, 2*s.interval)
		if err != nil {
			s.log.Errorf("fail to nodes keep alive: %s", err)
		}
	}()
	allIDs, err := s.getAllIDs()
	if err != nil {
		s.log.Errorf("fail to list id: %s", err)
		return
	}

	scheduled := make(map[string]IDSet)
	for _, node := range nodes {
		ids, err := s.storage.Get(node.ID)
		if err != nil {
			s.log.Errorf("fail to list id by node(%s): %s", node.ID, err)
			return
		}
		scheduled[node.ID] = ids
	}
	scheduledIDs := make(IDSet)
	for node, ids := range scheduled {
		for id := range ids {
			if scheduledIDs.Contains(id) || !allIDs.Contains(id) {
				delete(ids, id)
				err := s.storage.Del(node, id)
				if err != nil {
					s.log.Errorf("fail to delete id(%d) in node(%s): %s", id, node, err)
					return
				}
			} else {
				scheduledIDs.Put(id)
			}
		}
	}

	if len(nodes) > 0 {
		for id := range allIDs {
			if !scheduledIDs.Contains(id) {
				node := s.selectNode(scheduled, nodes).ID

				ids := scheduled[node]
				if ids == nil {
					ids := make(IDSet)
					scheduled[node] = ids
				}
				ids.Put(id)

				err := s.storage.Add(node, id)
				if err != nil {
					s.log.Errorf("fail to add id(%d) into node(%s): %s", id, node, err)
					return
				}
			}
		}
	}
}

// Reschedule .
func (s *Scheduler) Reschedule() {
	s.scheduleCh <- struct{}{}
}

func (s *Scheduler) RemoveNode(nodeID string) {
	err := s.storage.RemoveNode(nodeID)
	if err != nil {
		s.log.Errorf("fail to remove node(%q)", nodeID)
	}
}

func (s *Scheduler) getAllIDs() (IDSet, error) {
	list, err := s.source.ListIDs()
	if err != nil {
		return nil, err
	}
	ids := make(IDSet)
	for _, item := range list {
		ids.Put(item)
	}
	return ids, nil
}

func (s *Scheduler) selectNode(scheduled map[string]IDSet, nodes []*Node) *Node {
	var node *Node
	min := math.MaxInt64
	for _, n := range nodes {
		num := len(scheduled[n.ID])
		if num < min {
			min = num
			node = n
		}
	}
	if node != nil {
		return node
	}
	return nodes[0]
}

func (s *Scheduler) ListIDs(nodeID string) (list []int64, err error) {
	err = s.storage.Foreach(nodeID, func(id int64) bool {
		list = append(list, id)
		return true
	})
	if err != nil {
		return nil, err
	}
	return list, err
}
