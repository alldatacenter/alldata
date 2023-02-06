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

package mbox

import (
	"encoding/json"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/pkg/template"
)

type MBoxSubscriber struct {
	bundle    *bundle.Bundle
	messenger pb.NotifyServiceServer
}

type MBoxData struct {
	Template      string            `json:"template"`
	Params        map[string]string `json:"params"`
	OrgID         int64             `json:"orgID"`
	Label         string            `json:"label"`
	DeduplicateID string            `json:"deduplicateId"`
}

type Option func(*MBoxSubscriber)

func New(bundle *bundle.Bundle, messenger pb.NotifyServiceServer) subscriber.Subscriber {
	return &MBoxSubscriber{
		bundle:    bundle,
		messenger: messenger,
	}
}

func (d *MBoxSubscriber) Publish(dest string, content string, time int64, msg *types.Message) []error {
	errs := []error{}
	var userIDs []string
	err := json.Unmarshal([]byte(dest), &userIDs)
	if err != nil {
		return []error{err}
	}
	var mboxData MBoxData
	err = json.Unmarshal([]byte(content), &mboxData)
	if err != nil {
		return []error{err}
	}
	title, ok := mboxData.Params["title"]
	if !ok {
		title = "站内信通知"
	}
	err = d.bundle.CreateMBox(&apistructs.CreateMBoxRequest{
		Title:         template.Render(title, mboxData.Params),
		Content:       template.Render(mboxData.Template, mboxData.Params),
		OrgID:         mboxData.OrgID,
		UserIDs:       userIDs,
		Label:         mboxData.Label,
		DeduplicateID: mboxData.DeduplicateID,
	})
	if err != nil {
		errs = append(errs, err)
	}
	if len(errs) > 0 {
		if msg != nil && msg.CreateHistory != nil {
			msg.CreateHistory.Status = "failed"
		}
	}
	if msg.CreateHistory != nil {
		subscriber.SaveNotifyHistories(msg.CreateHistory, d.messenger)
	}
	return errs
}

func (d *MBoxSubscriber) Status() interface{} {
	return nil
}

func (d *MBoxSubscriber) Name() string {
	return "MBOX"
}
