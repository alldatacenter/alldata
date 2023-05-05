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

package events

import (
	"runtime/debug"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/queue"
)

type EventManager struct {
	c         chan *RuntimeEvent
	listeners []*EventListener
	db        *dbclient.DBClient
	bdl       *bundle.Bundle
}

func NewEventManager(cap int, queue *queue.PusherQueue, db *dbclient.DBClient, bdl *bundle.Bundle) *EventManager {
	m := &EventManager{
		c:   make(chan *RuntimeEvent, cap),
		db:  db,
		bdl: bdl,
	}
	m.listeners = []*EventListener{
		NewActivityPublisher(m),
		NewEventboxPublisher(m),
		NewWsPublisher(m),
		NewDeployPusher(m, queue),
		NewDeployTimeCollector(m, db),
	}
	return m
}

type EventListener interface {
	OnEvent(e *RuntimeEvent)
}

func (m *EventManager) EmitEvent(e *RuntimeEvent) {
	go func(c chan *RuntimeEvent) {
		c <- e
	}(m.c)
}

func (m *EventManager) Start() {
	go func() {
		for {
			e := <-m.c
			logrus.Debugf("received RuntimeEvent %v", e)
			for i := range m.listeners {
				go onEvent(m.listeners[i], e)
			}
		}
	}()
}

func onEvent(l *EventListener, e *RuntimeEvent) {
	defer func() {
		if err := recover(); err != nil {
			debug.PrintStack()
			logrus.Errorf("[alert] failed to emit event to listener: %v", err)
		}
	}()
	// do send event to listener's OnEvent
	(*l).OnEvent(e)
}
