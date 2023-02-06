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
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"sync"

	"github.com/gorilla/websocket"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

const (
	maxMessageSize = 65535
)

type Websocket struct {
	conn           *websocket.Conn
	handlers       map[string]Handler
	l              *sync.Mutex
	inC            chan interface{}
	afterConnected func(w ResponseWriter)
	beforeClose    func(w ResponseWriter, err error)
}

func New() *Websocket {
	return &Websocket{
		conn:     nil,
		handlers: make(map[string]Handler),
		inC:      make(chan interface{}, 1),
		l:        &sync.Mutex{},
	}
}

func (ws *Websocket) Register(type_ string, handler Handler) {
	ws.handlers[type_] = handler
}

func (ws *Websocket) AfterConnected(handler func(w ResponseWriter)) {
	ws.afterConnected = handler

}

func (ws *Websocket) BeforeClose(handler func(w ResponseWriter, err error)) {
	ws.beforeClose = handler
}

func (ws *Websocket) Upgrade(w http.ResponseWriter, r *http.Request) error {
	var up = websocket.Upgrader{
		HandshakeTimeout: 0,
		ReadBufferSize:   maxMessageSize,
		WriteBufferSize:  maxMessageSize,
		WriteBufferPool:  nil,
		Subprotocols:     nil,
		Error: func(w http.ResponseWriter, r *http.Request, status int, reason error) {
			logrus.Errorf("failed to hand shake, headers: %v", r.Header)
			httpserver.WriteErr(w, strconv.FormatInt(int64(status), 10), reason.Error())
		},
		CheckOrigin: func(_ *http.Request) bool {
			return true
		},
		EnableCompression: true,
	}
	conn, err := up.Upgrade(w, r, nil)
	if err != nil {
		return err
	}
	ws.conn = conn
	return nil
}

func (ws *Websocket) Write(p []byte) (n int, err error) {
	ws.l.Lock()
	defer ws.l.Unlock()

	if err = ws.conn.WriteMessage(websocket.TextMessage, p); err != nil {
		return 0, err
	}
	return len(p), nil
}

func (ws *Websocket) Close() error {
	return ws.conn.Close()
}

func (ws *Websocket) Run() {
	go ws.run()
}

func (ws *Websocket) run() {
	ws.afterConnected(ws)

	var (
		msgType int
		pkg     []byte
		err     error
	)

	for {
		msgType, pkg, err = ws.conn.ReadMessage()
		if err != nil {
			logrus.Errorf("ReadMessage err: %v", err)
			break
		}
		if msgType == websocket.CloseMessage {
			break
		}
		if msgType != websocket.TextMessage {
			continue
		}

		var pump apistructs.WebsocketRequest
		if err = json.Unmarshal(pkg, &pump); err != nil {
			_, _ = ws.Write(append([]byte("message struct error: "), pkg...))
			logrus.Warnf("message struct error, err: %v, message: %s", err, string(pkg))
			continue
		}
		handler, ok := ws.handlers[pump.Type]
		if !ok {
			logrus.Warnf("unregistered message type, message: %s", string(pkg))
			_, _ = ws.Write([]byte(fmt.Sprintf("unregistered package type: %s", pump.Type)))
			continue
		}
		if err = handler(ws, &pump); err != nil {
			break
		}
	}

	if ws.beforeClose != nil {
		switch err.(type) {
		case ExitWithDoingNothing, *ExitWithDoingNothing:
		default:
			ws.beforeClose(ws, err)
		}
	}

	_ = ws.Close()
}
