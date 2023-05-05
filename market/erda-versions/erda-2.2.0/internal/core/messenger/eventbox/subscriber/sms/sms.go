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

package sms

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk"
	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

type MobileSubscriber struct {
	accessKeyId         string
	accessSecret        string
	signName            string
	monitorTemplateCode string
	bundle              *bundle.Bundle
	messenger           pb.NotifyServiceServer
}

type MobileData struct {
	Template string            `json:"template"`
	Params   map[string]string `json:"params"`
	OrgID    int64             `json:"orgID"`
}

type Option func(*MobileSubscriber)

func New(accessKeyId, accessKeySecret, signName, monitorTemplateCode string, bundle *bundle.Bundle, messenger pb.NotifyServiceServer) subscriber.Subscriber {
	subscriber := &MobileSubscriber{
		accessKeyId:         accessKeyId,
		accessSecret:        accessKeySecret,
		signName:            signName,
		bundle:              bundle,
		messenger:           messenger,
		monitorTemplateCode: monitorTemplateCode,
	}
	return subscriber
}

func (d *MobileSubscriber) Publish(dest string, content string, time int64, msg *types.Message) []error {
	errs := []error{}
	var mobiles []string
	err := json.Unmarshal([]byte(dest), &mobiles)
	if err != nil {
		return []error{err}
	}
	var mobileData MobileData
	err = json.Unmarshal([]byte(content), &mobileData)
	if err != nil {
		return []error{err}
	}
	paramStr, err := json.Marshal(mobileData.Params)
	if err != nil {
		return []error{err}
	}

	org, err := d.bundle.GetOrg(mobileData.OrgID)
	if err != nil {
		logrus.Errorf("failed to get org info err:%s", err)
	}

	notifyChannel, err := d.bundle.GetEnabledNotifyChannelByType(mobileData.OrgID, apistructs.NOTIFY_CHANNEL_TYPE_SMS)
	if err != nil {
		logrus.Errorf("failed to get notifychannel, err: %s", err)
	}

	if notifyChannel.ChannelProviderType.Name != "" && notifyChannel.ChannelProviderType.Name != apistructs.NOTIFY_CHANNEL_PROVIDER_TYPE_ALIYUN {
		logrus.Errorf("do not support channel provider: %s", notifyChannel.ChannelProviderType.Name)
	}

	accessKeyID, accessSecret, signName := d.accessKeyId, d.accessSecret, d.signName
	if err == nil && notifyChannel.Config != nil && notifyChannel.Config.AccessKeyId != "" && notifyChannel.Config.AccessKeySecret != "" {
		accessKeyID = notifyChannel.Config.AccessKeyId
		accessSecret = notifyChannel.Config.AccessKeySecret
		signName = notifyChannel.Config.SignName
	}

	sdkClient, err := sdk.NewClientWithAccessKey("cn-hangzhou", accessKeyID, accessSecret)
	if err != nil {
		return []error{err}
	}

	// 通知组的短信模版存在notifyitem里
	var templateCode string
	templateCode = d.monitorTemplateCode
	if err == nil && org.Config != nil && org.Config.SMSMonitorTemplateCode != "" {
		templateCode = org.Config.SMSMonitorTemplateCode
	}
	if err == nil && notifyChannel.Config != nil && notifyChannel.Config.TemplateCode != "" {
		templateCode = notifyChannel.Config.TemplateCode
	}
	if mobileData.Template != "" {
		templateCode = mobileData.Template
	}

	if templateCode == "" {
		return []error{fmt.Errorf("empty template_code")}
	}

	request := requests.NewCommonRequest()
	request.Method = "POST"
	request.Scheme = "https" // https | http
	request.Domain = "dysmsapi.aliyuncs.com"
	request.Version = "2017-05-25"
	request.ApiName = "SendSms"
	request.QueryParams["RegionId"] = "cn-hangzhou"
	request.QueryParams["PhoneNumbers"] = strings.Join(mobiles, ",")
	request.QueryParams["SignName"] = signName
	request.QueryParams["TemplateCode"] = templateCode
	request.QueryParams["TemplateParam"] = string(paramStr)
	response, err := sdkClient.ProcessCommonRequest(request)
	if err != nil {
		logrus.Errorf("failed to send sms  %s", err)
		errs = append(errs, err)
	}
	if !response.IsSuccess() {
		logrus.Errorf("failed to send sms  %s", response.GetHttpContentString())
		errs = append(errs, fmt.Errorf("failed to send sms %s", response.GetHttpContentString()))
	}
	if len(errs) > 0 {
		if msg != nil && msg.CreateHistory != nil {
			msg.CreateHistory.Status = "failed"
		}
	}
	if msg.CreateHistory != nil {
		subscriber.SaveNotifyHistories(msg.CreateHistory, d.messenger)
	}
	logrus.Infof("sms send success %s", response.GetHttpContentString())
	return errs
}

func (d *MobileSubscriber) Status() interface{} {
	return nil
}

func (d *MobileSubscriber) Name() string {
	return "SMS"
}
