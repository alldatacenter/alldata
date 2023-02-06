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

package webhook

import (
	"context"
	"fmt"
	"net/url"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/constant"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/jsonstore"
)

var BadRequestErr = errors.New("bad request input")
var InternalServerErr = errors.New("internal server error")

type WebHookImpl struct {
	js jsonstore.JsonStore
}

func NewWebHookImpl() (*WebHookImpl, error) {
	js, err := jsonstore.New(jsonstore.UseMemEtcdStore(context.Background(), constant.WebhookDir, nil, nil))
	if err != nil {
		return nil, err
	}
	return &WebHookImpl{js: js}, nil
}

type Hook = apistructs.Hook
type HookLocation = apistructs.HookLocation
type ListHooksResponse = apistructs.WebhookListResponseData
type InspectHookResponse = apistructs.WebhookInspectResponseData
type CreateHookRequest = apistructs.CreateHookRequest
type CreateHookResponse = apistructs.WebhookCreateResponseData

type EditHookRequest = apistructs.WebhookUpdateRequestBody

type EditHookResponse = apistructs.WebhookUpdateResponseData

func hookCheckOrg(h Hook, orgID string) bool {
	return h.CreateHookRequest.Org == orgID
}

// ListHooks 列出符合 location 的 hook 列表。
func (w *WebHookImpl) ListHooks(location HookLocation) (ListHooksResponse, error) {
	dir := mkLocationDir(location)

	ids := []string{}
	keys, err := w.js.ListKeys(context.Background(), dir)
	if err != nil {
		return ListHooksResponse{}, errors.Wrap(InternalServerErr, fmt.Sprintf("list hooks fail: %v", err))
	}
	for _, k := range keys {
		parts := strings.Split(k, "/")
		if len(parts) == 0 {
			return ListHooksResponse{},
				errors.Wrap(InternalServerErr, fmt.Sprintf("list hooks fail: bad hook index format: %v", k))
		}
		ids = append(ids, parts[len(parts)-1])
	}
	r := ListHooksResponse{}
	for _, id := range ids {
		h := Hook{}
		if err := w.js.Get(context.Background(), mkHookEtcdName(id), &h); err != nil {
			if err == jsonstore.NotFoundErr {
				// 如果有索引但没找到数据，则忽略这个webhook
				continue
			}
			return ListHooksResponse{},
				errors.Wrap(InternalServerErr, fmt.Sprintf("list hooks fail: get hook: %v", mkHookEtcdName(id)))
		}
		if envSatisfy(location.Env, h.Env) {
			r = append(r, h)
		}
	}
	return r, nil
}
func (w *WebHookImpl) InspectHook(realOrg, id string) (InspectHookResponse, error) {
	h := Hook{}
	if err := w.js.Get(context.Background(), mkHookEtcdName(id), &h); err != nil {
		return InspectHookResponse{}, errors.Wrap(InternalServerErr, err.Error())
	}
	if realOrg != "" && !hookCheckOrg(h, realOrg) { // illegal org, return not found
		return InspectHookResponse{}, fmt.Errorf("not found")
	}
	return InspectHookResponse(h), nil
}

func (w *WebHookImpl) CreateHook(realOrg string, h CreateHookRequest) (CreateHookResponse, error) {
	hook := Hook{}
	hook.CreateHookRequest = h
	if hook.Name == "" {
		return CreateHookResponse(""), errors.Wrap(BadRequestErr, "not provide hook's name")
	}
	if hook.URL == "" {
		return CreateHookResponse(""), errors.Wrap(BadRequestErr, "not provide hook's URL")
	}
	if hook.Org == "" {
		return CreateHookResponse(""), errors.Wrap(BadRequestErr, "not provide hook's org")
	}
	if hook.Project == "" {
		return CreateHookResponse(""), errors.Wrap(BadRequestErr, "not provide hook's project")
	}
	if hook.Application == "" {
		return CreateHookResponse(""), errors.Wrap(BadRequestErr, "not provide hook's application")
	}
	if len(hook.Env) != 0 {
		for _, env := range hook.Env {
			normalizedEnv := strings.ToLower(strings.TrimSpace(env))
			if normalizedEnv == "" {
				continue
			}
			if normalizedEnv != "dev" &&
				normalizedEnv != "test" &&
				normalizedEnv != "staging" &&
				normalizedEnv != "prod" {
				return CreateHookResponse(""), errors.Wrap(BadRequestErr, "bad hook's env params, only support [dev, test, staging, prod]")
			}
		}
	}

	if hook.URL != "" {
		if _, err := url.Parse(hook.URL); err != nil {
			return CreateHookResponse(""), errors.Wrap(BadRequestErr, "bad hook url")
		}
	}
	if realOrg != "" && !hookCheckOrg(Hook{CreateHookRequest: h}, realOrg) {
		return CreateHookResponse(""), errors.Wrap(BadRequestErr, fmt.Sprintf("cannot operate on org: %v", hook.Org))
	}

	hook.CreatedAt = nowTimestamp()
	hook.UpdatedAt = nowTimestamp()
	hook.ID = genID()
	var err error
	defer func() {
		if err != nil {
			var unused interface{}
			w.js.Remove(context.Background(), mkHookEtcdName(hook.ID), &unused) // ignore err
			w.js.Remove(context.Background(), mkHookIndex(hook.Org, hook.Project, hook.Application, hook.ID), &unused)
		}
	}()

	if err = w.js.Put(context.Background(), mkHookEtcdName(hook.ID), hook); err != nil {
		return CreateHookResponse(""), errors.Wrap(InternalServerErr, fmt.Sprintf("jsonstore put webhook fail: %v", err))
	}
	if err = w.js.Put(context.Background(), mkHookIndex(hook.Org, hook.Project, hook.Application, hook.ID), ""); err != nil {
		return CreateHookResponse(""), errors.Wrap(InternalServerErr, fmt.Sprintf("jsonstore put webhook index fail: %v", err))
	}

	return CreateHookResponse(hook.ID), nil
}

func (w *WebHookImpl) EditHook(realOrg, id string, e EditHookRequest) (EditHookResponse, error) {
	h := Hook{}
	if err := w.js.Get(context.Background(), mkHookEtcdName(id), &h); err != nil {
		return EditHookResponse(""), errors.Wrap(InternalServerErr, err.Error())
	}
	if realOrg != "" && !hookCheckOrg(h, realOrg) {
		return EditHookResponse(""), fmt.Errorf("not found")
	}

	if len(e.Events) != 0 {
		h.Events = e.Events
	}
	if len(e.RemoveEvents) != 0 {
		h.Events = removeEvents(h.Events, e.RemoveEvents)
	}
	if len(e.AddEvents) != 0 {
		h.Events = addEvents(h.Events, e.AddEvents)
	}

	if e.URL != "" {
		if _, err := url.Parse(e.URL); err != nil {
			return EditHookResponse(""), errors.Wrap(BadRequestErr, "bad hook url")
		}
		h.URL = e.URL
	}

	h.Active = e.Active
	h.UpdatedAt = nowTimestamp()

	if err := w.js.Put(context.Background(), mkHookEtcdName(id), h); err != nil {
		return EditHookResponse(""),
			errors.Wrap(InternalServerErr, fmt.Sprintf("jsonstore put webhook fail: %v", err))
	}
	return EditHookResponse(h.ID), nil
}

func (w *WebHookImpl) PingHook(realOrg, id string) error {
	h := Hook{}
	if err := w.js.Get(context.Background(), mkHookEtcdName(id), &h); err != nil {
		return err
	}
	if realOrg != "" && !hookCheckOrg(h, realOrg) {
		return fmt.Errorf("not found")
	}
	u, err := url.Parse(h.URL)
	if err != nil {
		return errors.Wrap(BadRequestErr, "bad hook url")
	}
	pingEvent, err := PingEvent(h.Org, h.Project, h.Application, h)
	if err != nil {
		return errors.Wrap(InternalServerErr, err.Error())
	}
	opt := []httpclient.OpOption{}
	if u.Scheme == "https" {
		opt = []httpclient.OpOption{httpclient.WithHTTPS()}
	}
	r, err := httpclient.New(opt...).Post(u.Host).Path(u.Path).JSONBody(pingEvent).Do().DiscardBody()
	if err != nil {
		return errors.Wrap(InternalServerErr, err.Error())
	}
	if !r.IsOK() {
		return errors.Wrap(InternalServerErr, fmt.Sprintf("ping hook: %v, statuscode: %d", mkHookEtcdName(id), r.StatusCode()))
	}
	return nil
}

func (w *WebHookImpl) DeleteHook(realOrg, id string) error {
	h := Hook{}
	if err := w.js.Get(context.Background(), mkHookEtcdName(id), &h); err != nil {
		return errors.Wrap(InternalServerErr, err.Error())
	}
	if realOrg != "" && !hookCheckOrg(h, realOrg) {
		return fmt.Errorf("not found")
	}

	var unused interface{}
	if err := w.js.Remove(context.Background(), mkHookIndex(h.Org, h.Project, h.Application, id), &unused); err != nil {
		return errors.Wrap(InternalServerErr, fmt.Sprintf("delete hook index: %v", err))
	}
	if err := w.js.Remove(context.Background(), mkHookEtcdName(id), &unused); err != nil {
		return errors.Wrap(InternalServerErr, fmt.Sprintf("delete hook: %v", err))
	}
	return nil
}

/* search hooks which include 'event' and is 'active' */
func (w *WebHookImpl) SearchHooks(location HookLocation, event string) []Hook {
	hs, err := w.ListHooks(location)
	if err != nil {
		return nil
	}
	r := []Hook{}
	for _, h := range hs {
		for _, e := range h.Events {
			if e == event && h.Active {
				r = append(r, h)
				break
			}
		}
	}
	return r
}

// webhook dir structure
// /<webhookdir>/<org>/<project>/<ID> -> ""
// /<webhookdir>/<ID> -> <hook>

// <webhookdir>/<ID>
func mkHookEtcdName(id string) string {
	return strings.Join([]string{constant.WebhookDir, id}, "/")
}

func mkHookIndex(org, project, app, id string) string {
	return strings.Join([]string{constant.WebhookDir, org, project, app, id}, "/")
}

func mkLocationDir(location HookLocation) string {
	if location.Org == "" {
		logrus.Errorf("[Bug] empty location.Org")
	}
	if location.Project == "" {
		return strings.Join([]string{constant.WebhookDir, location.Org}, "/") + "/"
	}
	if location.Application == "" {
		return strings.Join([]string{constant.WebhookDir, location.Org, location.Project}, "/") + "/"
	}
	return strings.Join([]string{constant.WebhookDir, location.Org, location.Project, location.Application}, "/") + "/"
}

func genID() string {
	return uuid.UUID()[0:12]
}

func nowTimestamp() string {
	return time.Now().In(time.FixedZone("CST", 8*3600)).Format("2006-01-02 15:04:05")
}

func removeEvents(origin, remove []string) []string {
	new := []string{}
	for _, o := range origin {
		del := false
		for _, e := range remove {
			if e == o {
				del = true
				break
			}
		}
		if !del {
			new = append(new, o)
		}
	}
	return new
}

func addEvents(origin, add []string) []string {
	new := origin
	// filter same events
	addm := map[string]struct{}{}
	for _, e := range add {
		addm[e] = struct{}{}
	}

	for e := range addm {
		add := true

		for _, o := range origin {
			if e == o {
				add = false
				break
			}
		}
		if add {
			new = append(new, e)
		}
	}

	return new
}

// hookenv 中 是否有 envlist 的中的所列的元素
// len(envlist) == 0 : 所有 hook 都满足
// len(hookenv) == 0 : 满足所有 envlist
func envSatisfy(envlist, hookenv []string) bool {
	if len(envlist) == 0 {
		return true
	}
	if len(hookenv) == 0 {
		return true
	}
	for _, env := range envlist {
		for _, henv := range hookenv {
			if henv == env {
				return true
			}
		}
	}
	return false
}
