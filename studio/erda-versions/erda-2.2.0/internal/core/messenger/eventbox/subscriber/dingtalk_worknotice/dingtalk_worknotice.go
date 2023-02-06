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

package dingtalk_worknotice

import (
	"encoding/json"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/interfaces"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber/dingding"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/pkg/template"
)

type DingWorkNoticeSubscriber struct {
	AgentId     int64
	AppKey      string
	AppSecret   string
	bundle      *bundle.Bundle
	dingTalkApi interfaces.DingTalkApiClientFactory
	messenger   pb.NotifyServiceServer
}

type WorkNoticeData struct {
	Template string            `json:"template"`
	Params   map[string]string `json:"params"`
	OrgID    int64             `json:"orgID"`
}

func New(bundle *bundle.Bundle, dingtalk interfaces.DingTalkApiClientFactory, messenger pb.NotifyServiceServer) subscriber.Subscriber {
	subscriber := &DingWorkNoticeSubscriber{
		bundle:      bundle,
		dingTalkApi: dingtalk,
		messenger:   messenger,
	}
	return subscriber
}

func (d DingWorkNoticeSubscriber) Publish(dest string, content string, time int64, m *types.Message) []error {
	var mobiles []string
	errs := []error{}
	err := json.Unmarshal([]byte(dest), &mobiles)
	if err != nil {
		return []error{err}
	}
	var workNotifyData WorkNoticeData
	err = json.Unmarshal([]byte(content), &workNotifyData)
	if err != nil {
		return []error{err}
	}
	paramStr, err := json.Marshal(workNotifyData.Params)
	paramMap := map[string]string{}
	err = json.Unmarshal(paramStr, &paramMap)
	if err != nil {
		return []error{err}
	}
	if err != nil {
		return []error{err}
	}
	// render template params
	workNotifyData.Template = template.Render(workNotifyData.Template, workNotifyData.Params)
	notifyChannel, err := d.bundle.GetEnabledNotifyChannelByType(workNotifyData.OrgID, apistructs.NOTIFY_CHANNEL_TYPE_DINGTALK_WORK_NOTICE)
	if err != nil {
		return []error{errors.Errorf("non-enaled dingtalk_work_notify channel, orgID: %d, err: %v", workNotifyData.OrgID, err)}
	}
	if notifyChannel.ID == "" {
		return []error{errors.Errorf("non-enaled dingtalk_work_notify channel, orgID: %d", workNotifyData.OrgID)}
	}
	agentId, appKey, appSecret := d.AgentId, d.AppKey, d.AppSecret
	if notifyChannel.Config != nil && notifyChannel.Config.AgentId != 0 && notifyChannel.Config.AppKey != "" && notifyChannel.Config.AppSecret != "" {
		agentId = notifyChannel.Config.AgentId
		appKey = notifyChannel.Config.AppKey
		appSecret = notifyChannel.Config.AppSecret
	}
	dingClient := d.dingTalkApi.GetClient(appKey, appSecret, agentId)
	err = dingClient.SendWorkNotice(mobiles, paramMap["title"], dingding.DingPrint(workNotifyData.Template))
	if err != nil {
		errs = append(errs, err)
	}
	if len(errs) > 0 {
		if m != nil && m.CreateHistory != nil {
			m.CreateHistory.Status = "failed"
		}
	}
	if m.CreateHistory != nil {
		subscriber.SaveNotifyHistories(m.CreateHistory, d.messenger)
	}
	return errs
}

func (d DingWorkNoticeSubscriber) Status() interface{} {
	return nil
}

func (d DingWorkNoticeSubscriber) Name() string {
	return "DINGTALK_WORK_NOTICE"
}
