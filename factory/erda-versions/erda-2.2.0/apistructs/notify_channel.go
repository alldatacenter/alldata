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

// NotifyChannelFetchResponse 通知渠道详情响应结构
type NotifyChannelFetchResponse struct {
	Header
	Data NotifyChannelDTO `json:"data"`
}

// NotifyChannelDTO 通知渠道结构
type NotifyChannelDTO struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	Type struct {
		Name        NotifyChannelType `json:"name"`
		DisplayName string            `json:"displayName"`
	} `json:"type"`
	Config              *NotifyChannelConfig `json:"config"`
	ScopeId             string               `json:"scopeId"`
	ScopeType           string               `json:"scopeType"`
	ChannelProviderType struct {
		Name        NotifyChannelProviderType `json:"name"`
		DisplayName string                    `json:"displayName"`
	} `json:"channelProviderType"`
	Enable bool `json:"enable"`
}

type NotifyChannelConfig struct {
	AccessKeyId     string `json:"accessKeyId"`
	AccessKeySecret string `json:"accessKeySecret"`
	SignName        string `json:"signName"`
	TemplateCode    string `json:"templateCode"`
	AgentId         int64  `json:"agentId"`
	AppKey          string `json:"appKey"`
	AppSecret       string `json:"appSecret"`
	SMTPHost        string `json:"smtpHost"`
	SMTPUser        string `json:"smtpUser"`
	SMTPPassword    string `json:"smtpPassword"`
	SMTPPort        int64  `json:"smtpPort"`
	SMTPIsSSL       bool   `json:"smtpIsSsl"`
	VMSTtsCode      string `json:"vmsTtsCode"`
}

type NotifyChannelType string
type NotifyChannelProviderType string

const NOTIFY_CHANNEL_TYPE_SMS = NotifyChannelType("sms")
const NOTIFY_CHANNEL_PROVIDER_TYPE_ALIYUN = NotifyChannelProviderType("aliyun_sms")
const NOTIFY_CHANNEL_TYPE_DINGTALK_WORK_NOTICE = NotifyChannelType("dingtalk_work_notice")
const NOTIFY_CHANNEL_TYPE_EMAIL = NotifyChannelType("email")
const NOTIFY_CHANNEL_TYPE_VMS = NotifyChannelType("vms")
