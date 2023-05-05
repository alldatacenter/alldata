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

package filters

import (
	"encoding/json"
	"fmt"
	"net/url"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/constant"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher/errors"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/webhook"
)

type WebhookFilter struct {
	impl *webhook.WebHookImpl
}

func NewWebhookFilter() (Filter, error) {
	impl, err := webhook.NewWebHookImpl()
	if err != nil {
		return nil, err
	}
	return &WebhookFilter{impl: impl}, nil
}

func (*WebhookFilter) Name() string {
	return "WebhookFilter"
}

func (w *WebhookFilter) Filter(m *types.Message) *errors.DispatchError {
	derr := errors.New()
	whLabel, ok := m.Labels[types.LabelKey(constant.WebhookLabelKey)]
	if !ok {
		return derr
	}
	eventLabel, err := decodeWebhookLabel(whLabel)
	if err != nil {
		err := fmt.Errorf("WebhookFilter: decode label: %v, origin-label:%v", err, whLabel)
		logrus.Error(err)
		derr.FilterErr = err
		return derr
	}
	internalHs := w.impl.SearchHooks(apistructs.HookLocation{
		Org: "-1",
	}, eventLabel.Event)
	hs := w.impl.SearchHooks(apistructs.HookLocation{
		Org:         eventLabel.OrgID,
		Project:     eventLabel.ProjectID,
		Application: eventLabel.ApplicationID,
		Env:         []string{eventLabel.Env},
	}, eventLabel.Event)
	if len(hs)+len(internalHs) == 0 {
		logrus.Warnf("no webhook care event: %v", eventLabel)
		return derr
	}

	urls := []string{}
	for _, h := range hs {
		urls = append(urls, h.URL)
	}
	for _, h := range internalHs {
		urls = append(urls, h.URL)
	}

	if err := replaceLabel(m, urls); err != nil {
		derr.FilterErr = err
		return derr
	}
	if err := replaceContent(m, *eventLabel); err != nil {
		derr.FilterErr = err
		return derr
	}
	return derr
}

func decodeWebhookLabel(l interface{}) (*webhook.EventLabel, error) {
	raw, err := json.Marshal(l)
	if err != nil {
		return nil, err
	}
	label := webhook.EventLabel{}
	if err := json.Unmarshal(raw, &label); err != nil {
		return nil, err
	}
	return &label, nil
}

func replaceContent(m *types.Message, eventLabel webhook.EventLabel) error {
	origin, err := json.Marshal(m.Content)
	if err != nil {
		return err
	}
	em := webhook.MkEventMessage(eventLabel, origin)
	if m.OriginContent() == nil {
		m.SetOriginContent(m.Content)
	}
	m.Content = em
	return nil
}

func replaceLabel(m *types.Message, urls []string) error {
	httpLabel := m.Labels[types.LabelKey("HTTP").NormalizeLabelKey()]
	httpraw, err := json.Marshal(httpLabel)
	if err != nil {
		return err
	}
	dingdingLabel := m.Labels[types.LabelKey("DINGDING").NormalizeLabelKey()]
	dingdingraw, err := json.Marshal(dingdingLabel)
	if err != nil {
		return err
	}

	httpdest := []string{}
	if err := json.Unmarshal(httpraw, &httpdest); err != nil {
		return err
	}
	dingdingdest := []string{}
	if err := json.Unmarshal(dingdingraw, &dingdingdest); err != nil {
		return err
	}
	for _, u := range urls {
		parsed, err := url.Parse(u)
		if err != nil {
			// 在 webhook 创建的时候应该检查过了url， 所以err!=nil一定是bug
			logrus.Errorf("[alert][BUG]replace label: bad url: %v, message: %+v, urls: %v", u, m, urls)
		}
		switch urltype(parsed) {
		case dingdingURL:
			dingdingdest = append(dingdingdest, u)
		case normalURL:
			httpdest = append(httpdest, u)
		}
	}
	m.Labels[types.LabelKey("HTTP").NormalizeLabelKey()] = httpdest
	m.Labels[types.LabelKey("DINGDING").NormalizeLabelKey()] = dingdingdest
	return nil
}

type urltp int

const (
	dingdingURL = iota
	normalURL

	dingdingHostname = "oapi.dingtalk.com"
)

func urltype(url *url.URL) urltp {
	if url.Hostname() == dingdingHostname {
		return dingdingURL
	}
	return normalURL
}
