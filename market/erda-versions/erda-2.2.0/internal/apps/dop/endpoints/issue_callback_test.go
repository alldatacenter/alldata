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

package endpoints

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
)

func Test_sendIssueEventToSpecificRecipient(t *testing.T) {
	var bdl *bundle.Bundle
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetOrg",
		func(*bundle.Bundle, interface{}) (*apistructs.OrgDTO, error) {
			return &apistructs.OrgDTO{ID: 1, Config: &apistructs.OrgConfig{}}, nil
		})
	defer pm1.Unpatch()

	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListUsers",
		func(b *bundle.Bundle, req apistructs.UserListRequest) (*apistructs.UserListResponseData, error) {
			return &apistructs.UserListResponseData{Users: []apistructs.UserInfo{
				{ID: "1", Email: "test@erda.io", Phone: "13000000000"},
			}}, nil
		})
	defer pm2.Unpatch()

	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateMboxNotify",
		func(b *bundle.Bundle, templatename string, params map[string]string, locale string, orgid uint64, users []string) error {
			return nil
		})
	defer pm3.Unpatch()

	pm4 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateDingTalkWorkNotify",
		func(b *bundle.Bundle, templatename string, params map[string]string, locale string, orgid uint64, mobiles []string) error {
			return nil
		})
	defer pm4.Unpatch()

	pm5 := monkey.Patch(conf.UIPublicURL, func() string {
		return "erda.cloud"
	})
	defer pm5.Unpatch()

	req := apistructs.IssueEvent{
		EventHeader: apistructs.EventHeader{
			Action:    "create",
			OrgID:     "1",
			ProjectID: "1",
		},
		Content: apistructs.IssueEventData{
			Content:    "issue event",
			Receivers:  []string{"erda"},
			Params:     map[string]string{},
			IssueType:  apistructs.IssueTypeTask,
			StreamType: apistructs.ISTCreate,
		},
	}

	ep := Endpoints{bdl: bdl}
	err := ep.sendIssueEventToSpecificRecipient(req)
	assert.NoError(t, err)
}

func Test_processIssueEvent(t *testing.T) {
	var bdl *bundle.Bundle
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetOrg",
		func(*bundle.Bundle, interface{}) (*apistructs.OrgDTO, error) {
			return &apistructs.OrgDTO{ID: 1, Config: &apistructs.OrgConfig{}}, nil
		})
	defer pm1.Unpatch()

	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListUsers",
		func(b *bundle.Bundle, req apistructs.UserListRequest) (*apistructs.UserListResponseData, error) {
			return &apistructs.UserListResponseData{Users: []apistructs.UserInfo{
				{ID: "1", Email: "test@erda.io", Phone: "13000000000"},
			}}, nil
		})
	defer pm2.Unpatch()

	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateMboxNotify",
		func(b *bundle.Bundle, templatename string, params map[string]string, locale string, orgid uint64, users []string) error {
			return nil
		})
	defer pm3.Unpatch()

	pm4 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateDingTalkWorkNotify",
		func(b *bundle.Bundle, templatename string, params map[string]string, locale string, orgid uint64, mobiles []string) error {
			return nil
		})
	defer pm4.Unpatch()

	pm5 := monkey.Patch(conf.UIPublicURL, func() string {
		return "erda.cloud"
	})
	defer pm5.Unpatch()

	pm6 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryNotifiesBySource", func(b *bundle.Bundle, orgID string, sourceType string, sourceID string, itemName string, label string, clusterNames ...string) ([]*apistructs.NotifyDetail, error) {
		return []*apistructs.NotifyDetail{
			{
				NotifyGroup: &apistructs.NotifyGroup{},
				NotifyItems: []*apistructs.NotifyItem{
					{
						ID: 1,
					},
				},
			},
		}, nil
	})
	defer pm6.Unpatch()

	pm7 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateGroupNotifyEvent", func(b *bundle.Bundle, groupNotifyRequest apistructs.EventBoxGroupNotifyRequest) error {
		return nil
	})
	defer pm7.Unpatch()

	req := apistructs.IssueEvent{
		EventHeader: apistructs.EventHeader{
			Action:    "create",
			OrgID:     "1",
			ProjectID: "1",
		},
		Content: apistructs.IssueEventData{
			Content:    "issue event",
			Receivers:  []string{"erda"},
			Params:     map[string]string{},
			IssueType:  apistructs.IssueTypeTask,
			StreamType: apistructs.ISTCreate,
		},
	}

	ep := Endpoints{bdl: bdl}
	err := ep.processIssueEvent(req)
	assert.NoError(t, err)
}
