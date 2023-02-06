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

package websocket

import (
	"github.com/sirupsen/logrus"
	"gopkg.in/igm/sockjs-go.v2/sockjs"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/input"
	"github.com/erda-project/erda/internal/pkg/websocket"
)

type WebsocketHTTP struct {
	m *websocket.Manager
	s chan struct{}
}

func New() (*WebsocketHTTP, error) {
	m, err := websocket.New()
	if err != nil {
		return nil, err
	}

	return &WebsocketHTTP{
		m: m,
		s: make(chan struct{}),
	}, nil
}

func (w *WebsocketHTTP) Start(handler input.Handler) error {
	w.m.Start()
	logrus.Info("Websocket start() done")
	// blocking
	<-w.s
	return nil
}

func (w *WebsocketHTTP) Stop() error {
	w.m.Stop()
	w.s <- struct{}{}
	logrus.Info("Websocket stop() done")
	return nil
}

func (w *WebsocketHTTP) Name() string {
	return "Websocket"
}

func (w *WebsocketHTTP) HTTPHandle(s sockjs.Session) {
	w.m.Handle(s)
}
