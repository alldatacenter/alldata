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

package event

import (
	"net/url"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

const (
	createPath = "/api/dice/eventbox/webhooks"
)

// WebhookServer webhook server
type WebhookServer struct {
	addr   string
	client *httpclient.HTTPClient
}

// NewWebhook new webhook
func NewWebhook(addr string) (*WebhookServer, error) {
	if addr == "" {
		return nil, errors.Errorf("eventbox addr is null")
	}

	return &WebhookServer{
		addr:   addr,
		client: httpclient.New(),
	}, nil
}

// Create 创建 hook
func (w *WebhookServer) Create(spec apistructs.WebhookCreateRequest) error {
	var body StandardResponse
	var id string
	var ok bool

	resp, err := w.client.Post(w.addr).Path(createPath).JSONBody(spec).Do().JSON(&body)
	if err != nil {
		return err
	}

	if resp == nil {
		return errors.Errorf("response is null")
	}

	if !resp.IsOK() {
		return errors.Errorf("status code: %d", resp.StatusCode())
	}

	if !body.Success {
		return errors.Errorf("response code: %s, error: %s", body.Error.Code, body.Error.Msg)
	}

	if id, ok = body.Data.(string); !ok {
		return errors.Errorf("the returnd data is not a string")
	}

	logrus.Infof("Successfully to create webhook, id: %s", id)
	return nil
}

// List 获取 hooks 列表
func (w *WebhookServer) List() (apistructs.WebhookListResponseData, error) {
	var body apistructs.WebhookListResponse

	query := url.Values{"orgID": []string{"-1"}, "projectID": []string{"-1"}}

	resp, err := w.client.Get(w.addr).Params(query).Path(createPath).Do().JSON(&body)
	if err != nil {
		return nil, err
	}

	if resp == nil {
		return nil, errors.Errorf("response is null")
	}

	if resp.IsNotfound() {
		logrus.Infof("list webhooks results is empty")
		return nil, nil
	}

	if !resp.IsOK() {
		return nil, errors.Errorf("status code: %d", resp.StatusCode())
	}

	if !body.Success {
		return nil, errors.Errorf("response code: %s, error: %s", body.Error.Code, body.Error.Msg)
	}

	logrus.Infof("Successfully to list webhooks: %v", body.Data)
	return body.Data, nil
}
