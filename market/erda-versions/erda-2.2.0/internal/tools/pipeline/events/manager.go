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
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/websocket"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
)

type EventManager struct {
	ch chan Event
}

var mgr EventManager

var defaultEvent DefaultEvent

func Initialize(bdl *bundle.Bundle, wsClient *websocket.Publisher, dbClient *dbclient.Client, edgeRegister edgepipeline_register.Interface) {
	mgr = EventManager{
		ch: make(chan Event, 100),
	}

	defaultEvent = DefaultEvent{
		bdl:      bdl,
		wsClient: wsClient,
		dbClient: dbClient,

		edgeRegister: edgeRegister,
	}

	go func() {
		for {
			e := <-mgr.ch
			go func(ev Event) {
				//logrus.Debugf("received an %s Event: %s (kind: %s, header: %+v, sender: %s, content: %+v)",
				//	e.Kind(), e, e.Kind(), e.Header(), e.Sender(), e.Content())

				go handle(ev, HookTypeDB, ev.HandleDB)

				if !edgeRegister.IsEdge() {
					go handle(ev, HookTypeWebHook, ev.HandleWebhook)
					go handle(ev, HookTypeWebSocket, ev.HandleWebSocket)
					go handle(ev, HookTypeDINGDING, ev.HandleDingDing)
					go handle(ev, HookTypeHTTP, ev.HandleHTTP)
				}
			}(e)
		}
	}()
}

func handle(e Event, hook HookType, handleFunc func() error) {
	//logrus.Debugf("begin handle event, hook: %s, event: %s", hook, e)
	err := handleFunc()
	if err != nil {
		logrus.Errorf("failed to handle event, hook: %s, event: %s, err: %v", hook, e, err)
		return
	}
	//logrus.Debugf("success handle event, hook: %s, event: %s", hook, e)
}
