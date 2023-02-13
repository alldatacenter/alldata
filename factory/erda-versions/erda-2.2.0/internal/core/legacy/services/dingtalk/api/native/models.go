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

type DingtalkMessageType string

const (
	DINGTALKMESSAGETYPE_MARKDOWN   = DingtalkMessageType("markdown")
	DINGTALKMESSAGETYPE_TEXT       = DingtalkMessageType("text")
	DINGTALKMESSAGETYPE_IMAGE      = DingtalkMessageType("image")
	DINGTALKMESSAGETYPE_LINK       = DingtalkMessageType("link")
	DINGTALKMESSAGETYPE_FILE       = DingtalkMessageType("file")
	DINGTALKMESSAGETYPE_VOICE      = DingtalkMessageType("voice")
	DINGTALKMESSAGETYPE_OA         = DingtalkMessageType("oa")
	DINGTALKMESSAGETYPE_ACTIONCARD = DingtalkMessageType("action_card")
)

type GetUserIdByMobileRequest struct {
	Mobile                        string `json:"mobile"`
	SupportExclusiveAccountSearch bool   `json:"support_exclusive_account_search"`
}

type DingtalkBaseResponse struct {
	ErrCode int    `json:"errcode"`
	ErrMsg  string `json:"errmsg"`
}

type GetUserIdByMobileResponse struct {
	DingtalkBaseResponse
	Result struct {
		UserId string `json:"userid"`
	} `json:"result"`
}

type SendCorpConversationMarkdownMessageRequest struct {
	AgentId    int64              `json:"agent_id"`
	UseridList string             `json:"userid_list"`
	ToAllUser  bool               `json:"to_all_user"`
	Msg        DingtalkMessageObj `json:"msg"`
}

type DingtalkMessageObj struct {
	MsgType  DingtalkMessageType `json:"msgtype"`
	Markdown MarkdownMessage     `json:"markdown"`
}

type MarkdownMessage struct {
	Text  string `json:"text"`
	Title string `json:"title"`
}

type SendCorpConversationMarkdownMessageResponse struct {
	DingtalkBaseResponse
	TaskId int64 `json:"task_id"`
}
