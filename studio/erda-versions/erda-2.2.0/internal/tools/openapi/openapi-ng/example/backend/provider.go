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

package backend

import (
	"embed"
	"fmt"
	"net/http"

	"golang.org/x/net/websocket"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
)

//go:embed static
var webfs embed.FS

type provider struct {
	Log    logs.Logger
	Router httpserver.Router `autowired:"http-router@example"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	routes := p.Router

	routes.GET("/api/hello", func(resp http.ResponseWriter, req *http.Request) {
		resp.Write([]byte("GET hello"))
	})
	routes.POST("/api/hello", func(resp http.ResponseWriter, req *http.Request) {
		resp.Write([]byte("POST hello"))
	})
	routes.GET("/api/hello/:name", func(resp http.ResponseWriter, params struct {
		Name string `param:"name"`
	}) {
		resp.Write([]byte("hello " + params.Name))
	})
	routes.GET("/api/user-info", func(resp http.ResponseWriter, req *http.Request) interface{} {
		var ids []string
		userID := req.Header.Get("User-ID")
		if len(userID) > 0 {
			ids = append(ids, userID)
		}
		resp.Header().Set("Content-Type", "application/json")
		return map[string]interface{}{
			"userIDs": ids,
		}
	})

	routes.GET("/api/websocket", p.handleWebsocket(ctx))
	routes.Static("/static", "/static", httpserver.WithFileSystem(http.FS(webfs)))
	return nil
}

func (p *provider) handleWebsocket(ctx servicehub.Context) func(resp http.ResponseWriter, req *http.Request) {
	return websocket.Handler(func(ws *websocket.Conn) {
		defer ws.Close()
		for {
			// Write
			err := websocket.Message.Send(ws, "Hello, Client!")
			if err != nil {
				p.Log.Error(err)
				return
			}

			// Read
			var msg string
			err = websocket.Message.Receive(ws, &msg)
			if err != nil {
				p.Log.Error(err)
				return
			}
			fmt.Printf("%s\n", msg)
		}
	}).ServeHTTP
}

func init() {
	servicehub.Register("openapi-example-backend", &servicehub.Spec{
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
