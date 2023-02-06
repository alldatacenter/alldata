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

package edgepipeline_register

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"

	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher"
	httpsubscriber "github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber/http"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (p *provider) listWebHook(rw http.ResponseWriter, r *http.Request) {
	var req pb.ListHooksRequest
	if err := p.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	vars := mux.Vars(r)
	hooks, err := p.webHookHTTP.ListHooks(context.Background(), &req, vars)
	if err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	httpserver.WriteJSON(rw, hooks)
}

func (p *provider) getWebHook(rw http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	hookID := vars["id"]
	var req pb.InspectHookRequest
	if err := p.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	req.Id = hookID
	res, err := p.webHookHTTP.InspectHook(context.Background(), &req, vars)
	if err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	httpserver.WriteJSON(rw, res)
}

func (p *provider) createWebHook(rw http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	var req pb.CreateHookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	res, err := p.webHookHTTP.CreateHook(context.Background(), &req, vars)
	if err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	httpserver.WriteJSON(rw, res)
}

func (p *provider) editWebHook(rw http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	var req pb.EditHookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	req.Id = vars["id"]
	res, err := p.webHookHTTP.EditHook(context.Background(), &req, vars)
	if err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	httpserver.WriteJSON(rw, res)
}

func (p *provider) pingWebHook(rw http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	var req pb.PingHookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	req.Id = vars["id"]
	res, err := p.webHookHTTP.PingHook(context.Background(), &req, vars)
	if err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	httpserver.WriteJSON(rw, res)
}

func (p *provider) deleteWebHook(rw http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	var req pb.DeleteHookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	req.Id = vars["id"]
	res, err := p.webHookHTTP.DeleteHook(context.Background(), &req, vars)
	if err != nil {
		p.wrapBadRequest(rw, err)
		return
	}
	httpserver.WriteJSON(rw, res)
}

func (p *provider) initWebHookEndpoints(ctx context.Context) {
	p.Register.Add(http.MethodGet, "/api/dice/eventbox/webhooks", p.listWebHook)

	p.Register.Add(http.MethodGet, "/api/dice/eventbox/webhooks/{id}", p.getWebHook)

	p.Register.Add(http.MethodPost, "/api/dice/eventbox/webhooks", p.createWebHook)

	p.Register.Add(http.MethodPut, "/api/dice/eventbox/webhooks/{id}", p.editWebHook)

	p.Register.Add(http.MethodPost, "/api/dice/eventbox/webhooks/{id}/actions/ping", p.pingWebHook)

	p.Register.Add(http.MethodDelete, "/api/dice/eventbox/webhooks/{id}", p.deleteWebHook)
}

func (p *provider) wrapBadRequest(rw http.ResponseWriter, err error) {
	httpserver.WriteErr(rw, strconv.FormatInt(int64(http.StatusBadRequest), 10), err.Error())
}

func (p *provider) newEventDispatcher() (dispatcher.Dispatcher, error) {
	eventDispatcher, err := dispatcher.NewImpl()
	if err != nil {
		return nil, err
	}

	eventDispatcher.RegisterInput(p.httpI)

	httpS := httpsubscriber.New()
	eventDispatcher.RegisterSubscriber(httpS)

	router, err := dispatcher.NewRouter(eventDispatcher)
	if err != nil {
		return nil, err
	}
	eventDispatcher.SetRouter(router)

	return eventDispatcher, nil
}

func (p *provider) startEventDispatcher(ctx context.Context) {
	go func() {
		for {
			select {
			case <-ctx.Done():
				p.eventDispatcher.Stop()
			}
		}
	}()
	p.eventDispatcher.Start()
}

func (p *provider) CreateMessageEvent(event *apistructs.EventCreateRequest) error {
	eventPB, err := event.ConvertToPB()
	if err != nil {
		return err
	}
	err = p.httpI.CreateMessage(context.Background(), eventPB, nil)
	if err != nil {
		return err
	}
	return nil
}
