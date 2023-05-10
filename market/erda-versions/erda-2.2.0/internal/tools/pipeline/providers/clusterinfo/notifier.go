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

package clusterinfo

import "github.com/erda-project/erda/apistructs"

type Notifier interface {
	RegisterClusterEvent() <-chan apistructs.ClusterEvent
	RegisterRefreshEvent() <-chan struct{}

	NotifyClusterEvent(event apistructs.ClusterEvent)
	NotifyRefreshEvent()
}

type ClusterInfoNotifier struct {
	clusterEventChans []chan apistructs.ClusterEvent
	refreshEventChans []chan struct{}
}

func NewClusterInfoNotifier() *ClusterInfoNotifier {
	return &ClusterInfoNotifier{
		clusterEventChans: make([]chan apistructs.ClusterEvent, 0),
		refreshEventChans: make([]chan struct{}, 0),
	}
}

func (c *ClusterInfoNotifier) RegisterClusterEvent() <-chan apistructs.ClusterEvent {
	ch := make(chan apistructs.ClusterEvent, 1)
	c.clusterEventChans = append(c.clusterEventChans, ch)
	return ch
}

func (c *ClusterInfoNotifier) RegisterRefreshEvent() <-chan struct{} {
	ch := make(chan struct{}, 1)
	c.refreshEventChans = append(c.refreshEventChans, ch)
	return ch
}

func (c *ClusterInfoNotifier) NotifyClusterEvent(event apistructs.ClusterEvent) {
	for _, ch := range c.clusterEventChans {
		ch <- event
	}
}

func (c *ClusterInfoNotifier) NotifyRefreshEvent() {
	for _, ch := range c.refreshEventChans {
		ch <- struct{}{}
	}
}
