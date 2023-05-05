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

package native

import (
	"fmt"
	"net/url"
	"strings"

	openapi "github.com/alibabacloud-go/darabonba-openapi/client"
	dingtalkoauth2_1_0 "github.com/alibabacloud-go/dingtalk/oauth2_1_0"
	"github.com/alibabacloud-go/tea/tea"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

const DINGTALK_OAPI_DOMAIN = "https://oapi.dingtalk.com"

func CreateNativeClient() (client *dingtalkoauth2_1_0.Client, err error) {
	config := &openapi.Config{}
	config.Protocol = tea.String("https")
	config.RegionId = tea.String("central")
	client = &dingtalkoauth2_1_0.Client{}
	client, err = dingtalkoauth2_1_0.NewClient(config)
	return client, err
}

func GetAccessToken(appKey, appSecret string) (accessToken string, expireIn int64, err error) {
	client, err := CreateNativeClient()
	if err != nil {
		return "", 0, err
	}

	getAccessTokenRequest := &dingtalkoauth2_1_0.GetAccessTokenRequest{
		AppKey:    tea.String(appKey),
		AppSecret: tea.String(appSecret),
	}

	result, err := client.GetAccessToken(getAccessTokenRequest)
	if err != nil {
		return "", 0, err
	}

	return tea.StringValue(result.Body.AccessToken), tea.Int64Value(result.Body.ExpireIn), nil
}

func GetUserIdByMobile(accessToken string, mobile string) (userId string, err error) {
	request := GetUserIdByMobileRequest{
		mobile,
		true,
	}
	var result GetUserIdByMobileResponse

	resp, err := httpclient.New(httpclient.WithHTTPS()).
		Post(DINGTALK_OAPI_DOMAIN).
		Path("/topapi/v2/user/getbymobile").
		Params(url.Values{"access_token": []string{accessToken}}).
		Header("Content-Type", "application/json;charset=utf-8").
		JSONBody(request).
		Do().
		JSON(&result)

	if err != nil {
		return "", err
	}
	if resp.StatusCode() != 200 || result.ErrCode != 0 {
		return "", fmt.Errorf("response error: %s", string(resp.Body()))
	}
	return result.Result.UserId, nil
}

func SendCorpConversationMarkdownMessage(accessToken string, agentId int64, userIds []string, title, content string) error {
	request := SendCorpConversationMarkdownMessageRequest{
		UseridList: strings.Join(userIds, ","),
		AgentId:    agentId,
		Msg: DingtalkMessageObj{
			MsgType: DINGTALKMESSAGETYPE_MARKDOWN,
			Markdown: MarkdownMessage{
				Title: title,
				Text:  content,
			},
		},
	}
	var result SendCorpConversationMarkdownMessageResponse

	resp, err := httpclient.New(httpclient.WithHTTPS()).
		Post(DINGTALK_OAPI_DOMAIN).
		Path("/topapi/message/corpconversation/asyncsend_v2").
		Params(url.Values{"access_token": []string{accessToken}}).
		Header("Content-Type", "application/json;charset=utf-8").
		JSONBody(request).
		Do().
		JSON(&result)

	if err != nil {
		return err
	}
	if resp.StatusCode() != 200 || result.ErrCode != 0 {
		return fmt.Errorf("response error: %s", string(resp.Body()))
	}
	return nil
}
