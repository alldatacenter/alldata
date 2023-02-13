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

// MessageLabel alias as string
type MessageLabel = string

const (
	// DingdingLabel "DINGDING": ["<url-1>", "<url-2>"]
	DingdingLabel MessageLabel = "DINGDING"

	// DingdingMarkdownLabel "MARKDOWN": {"title": "title"}
	DingdingMarkdownLabel MessageLabel = "MARKDOWN"

	// HTTPLabel "HTTP": ["<url-1>", "<url-2>"]
	HTTPLabel MessageLabel = "HTTP"

	// HTTPHeaderLabel "HTTP-HEADERS": {"k1": "v1", "k2": "v2"}
	HTTPHeaderLabel MessageLabel = "HTTP-HEADERS"

	// DingdingATLabel
	// "AT":
	// {
	//  "atMobiles": [
	//     "1825718XXXX"
	//   ],
	//   "isAtAll": false
	// }
	DingdingATLabel MessageLabel = "AT"

	// DingdingWorkNoticeLabel see also 'https://open-doc.dingtalk.com/microapp/serverapi2/pgoxpy'
	// "DINGDING-WORKNOTICE":
	// [{
	//   "url": "<worknotice-url>",
	//   "agent_id": "<agentid>",
	//   "userid_list": ["<id1>", "<id2>"]
	// }, ...]
	DingdingWorkNoticeLabel MessageLabel = "DINGDING-WORKNOTICE"

	// MySQLLabel "MYSQL": "<table-name>"
	MySQLLabel MessageLabel = "MYSQL"
)

// MessageCreateRequest see also `bundle/messages.go'
type MessageCreateRequest struct {
	Sender  string                       `json:"sender"`
	Content interface{}                  `json:"content"`
	Labels  map[MessageLabel]interface{} `json:"labels"`
}
