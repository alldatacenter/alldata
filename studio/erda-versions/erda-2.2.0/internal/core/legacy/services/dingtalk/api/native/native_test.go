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
	"testing"

	"bou.ke/monkey"
	dingtalkoauth2_1_0 "github.com/alibabacloud-go/dingtalk/oauth2_1_0"
	"github.com/alibabacloud-go/tea/tea"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

func Test_GetAccessToken_Should_Success(t *testing.T) {
	defer monkey.Unpatch((*httpclient.Response).StatusCode)
	monkey.Patch((*httpclient.Response).StatusCode, func(r *httpclient.Response) int {
		return 200
	})

	defer monkey.Unpatch((*dingtalkoauth2_1_0.Client).GetAccessToken)
	monkey.Patch((*dingtalkoauth2_1_0.Client).GetAccessToken, func(c *dingtalkoauth2_1_0.Client, request *dingtalkoauth2_1_0.GetAccessTokenRequest) (_result *dingtalkoauth2_1_0.GetAccessTokenResponse, _err error) {
		_result = &dingtalkoauth2_1_0.GetAccessTokenResponse{
			Body: &dingtalkoauth2_1_0.GetAccessTokenResponseBody{
				AccessToken: tea.String("mock_accesstoken"),
				ExpireIn:    tea.Int64(7200),
			},
		}
		return _result, _err
	})

	token, expireIn, err := GetAccessToken("mock_appkey", "mock_appsecret")
	if err != nil {
		t.Errorf("should not error")
	}
	if len(token) == 0 {
		t.Errorf("token should not empty")
	}
	if expireIn <= 0 {
		t.Errorf("expireIn should gt 0")
	}
}

func Test_GetUserIdByMobile_Should_Success(t *testing.T) {
	defer monkey.Unpatch((*httpclient.Response).StatusCode)
	monkey.Patch((*httpclient.Response).StatusCode, func(r *httpclient.Response) int {
		return 200
	})

	defer monkey.Unpatch(httpclient.AfterDo.JSON)
	monkey.Patch(httpclient.AfterDo.JSON, func(r httpclient.AfterDo, request interface{}) (*httpclient.Response, error) {
		data := request.(*GetUserIdByMobileResponse)
		data.Result = struct {
			UserId string `json:"userid"`
		}{UserId: "12345"}

		return &httpclient.Response{}, nil
	})

	userId, err := GetUserIdByMobile("mock_userid", "17138393333")
	if err != nil {
		t.Errorf("should not error")
	}
	if len(userId) == 0 {
		t.Errorf("userId should not empty")
	}
	fmt.Println(userId)
}

func Test_SendCorpConversationMessage_Should_Success(t *testing.T) {

	defer monkey.Unpatch((*httpclient.Response).StatusCode)
	monkey.Patch((*httpclient.Response).StatusCode, func(r *httpclient.Response) int {
		return 200
	})

	defer monkey.Unpatch(httpclient.AfterDo.JSON)
	monkey.Patch(httpclient.AfterDo.JSON, func(r httpclient.AfterDo, request interface{}) (*httpclient.Response, error) {
		data := request.(*SendCorpConversationMarkdownMessageResponse)
		data.TaskId = 123
		return &httpclient.Response{}, nil
	})

	err := SendCorpConversationMarkdownMessage("mock_accesstoken", 123, []string{"mock_userid"}, "测试消息标题", "## 测试消息标题 \n 测试消息内容，[链接](https://erda.cloud)")
	if err != nil {
		t.Errorf("should not error")
	}
}
