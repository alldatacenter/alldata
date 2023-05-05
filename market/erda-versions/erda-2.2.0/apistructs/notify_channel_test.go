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

package apistructs

import (
	"encoding/json"
	"testing"
)

func Test_NotifyChannelDTO_Deserialization(t *testing.T) {
	var jsonStr = `{
        "id": "85fe1f9e-7cc4-4d65-baab-be0346d53849",
        "name": "test_channel18",
        "type": {
            "name": "sms",
            "displayName": "短信"
        },
        "config": {
            "accessKeyId": "xx",
            "accessKeySecret": "xx",
            "signName": "xx",
            "templateCode": "xx"
        },
        "scopeId": "1",
        "scopeType": "org",
        "creatorName": "admin",
        "createAt": "2021-10-20 21:10:14",
        "updateAt": "2021-10-20 21:30:02",
        "channelProviderType": {
            "name": "aliyun_sms",
            "displayName": "阿里云短信服务"
        },
        "enable": true
    }`
	var want = NotifyChannelDTO{
		ID:   "85fe1f9e-7cc4-4d65-baab-be0346d53849",
		Name: "test_channel18",
		Type: struct {
			Name        NotifyChannelType `json:"name"`
			DisplayName string            `json:"displayName"`
		}{Name: NOTIFY_CHANNEL_TYPE_SMS, DisplayName: "短信"},
		Config: &NotifyChannelConfig{
			AccessKeyId:     "xx",
			AccessKeySecret: "xx",
			SignName:        "xx",
			TemplateCode:    "xx",
		},
		ScopeId:   "1",
		ScopeType: "org",
		ChannelProviderType: struct {
			Name        NotifyChannelProviderType `json:"name"`
			DisplayName string                    `json:"displayName"`
		}{Name: NOTIFY_CHANNEL_PROVIDER_TYPE_ALIYUN, DisplayName: "阿里云短信服务"},
		Enable: true,
	}

	var data NotifyChannelDTO

	err := json.Unmarshal([]byte(jsonStr), &data)
	if err != nil {
		t.Errorf("should not error: %s", err)
	}

	if data.ID != want.ID || data.Enable != want.Enable || data.ScopeId != want.ScopeId {
		t.Errorf("assert id,enable,scopeId property failed")
	}
	if data.Config == nil {
		t.Errorf("config should not nil")
	}

	if data.Type.Name != want.Type.Name {
		t.Errorf("channel type assert failed, want: %s, actual: %s", want.Type.Name, data.Type.Name)
	}

	if data.ChannelProviderType.Name != want.ChannelProviderType.Name {
		t.Errorf("channel provier type assert failed, want: %s, actual: %s", want.ChannelProviderType.Name, data.ChannelProviderType.Name)
	}
}
