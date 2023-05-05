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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestGenEventParams(t *testing.T) {
	issueEventImpl := &IssueEvent{
		EventHeader: EventHeader{
			Event:         "issue",
			Action:        "update",
			OrgID:         "1",
			ProjectID:     "1",
			ApplicationID: "-1",
			TimeStamp:     time.Now().Format("2006-01-02 15:04:05"),
		},
		Content: IssueEventData{
			Title:      "fakeTitle",
			Content:    "hello\\nword\\na",
			AtUserIDs:  "2",
			IssueType:  IssueTypeRequirement,
			StreamType: ISTComment,
			StreamParams: ISTParam{
				CommentTime: "2006-01-02 15:04:05", // comment time
				UserName:    "LiLei",
			},
			Receivers: []string{"2", "3", "4"},
			Params: map[string]string{
				"orgName":     "fakeOrg",
				"projectName": "fakePrj",
				"issueID":     "1",
			},
		},
	}

	params := issueEventImpl.GenEventParams("zh-CN", "https://fake.xx.com")
	assert.Equal(t, params["issueType"], "需求")
	assert.Equal(t, params["issueTitle"], "fakeTitle")
	assert.Equal(t, params["title"], "需求-fakeTitle (fakeOrg/fakePrj 项目)")
	assert.Equal(t, params["projectMboxLink"], "/fakeOrg/dop/projects/1/issues/all")
	assert.Equal(t, params["issueMboxLink"], "/fakeOrg/dop/projects/1/issues/all?id=1&type=REQUIREMENT")
	assert.Equal(t, params["projectEmailLink"], "https://fake.xx.com/fakeOrg/dop/projects/1/issues/all")
	assert.Equal(t, params["issueEmailLink"], "https://fake.xx.com/fakeOrg/dop/projects/1/issues/all?id=1&type=REQUIREMENT")
	assert.Equal(t, params["issueMboxContent"], "LiLei 备注于 2006-01-02 15:04:05\nhello\nword\na")
	assert.Equal(t, params["issueEmailContent"], "LiLei 备注于 2006-01-02 15:04:05</br>hello</br>word</br>a")
	assert.Equal(t, params["mboxDeduplicateID"], "issue-1")

	ticketIssueEvent := issueEventImpl
	ticketIssueEvent.Content.IssueType = IssueTypeTicket
	params = ticketIssueEvent.GenEventParams("zh-CN", "https://fake.xx.com")
	assert.Equal(t, params["issueEmailLink"], "https://fake.xx.com/fakeOrg/dop/projects/1/ticket?id=1&pageNo=1")
}
