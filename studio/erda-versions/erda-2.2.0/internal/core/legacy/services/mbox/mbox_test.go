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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

func TestSetDuplicateMboxStatus(t *testing.T) {
	tm := time.Now()
	mbox := &model.MBox{
		Title:         "fakeTitle",
		Content:       "fakeContent",
		UserID:        "2",
		Status:        apistructs.MBoxUnReadStatus,
		OrgID:         1,
		ReadAt:        &tm,
		DeduplicateID: "issue-1",
		UnreadCount:   10,
	}
	createReq := &apistructs.CreateMBoxRequest{
		Title:   "fakeTitle2",
		Content: "fakeContent2",
	}
	setDuplicateMbox(mbox, createReq)
	assert.Equal(t, int64(11), mbox.UnreadCount)
	assert.Equal(t, apistructs.MBoxUnReadStatus, mbox.Status)
	assert.Equal(t, "fakeTitle2", mbox.Title)
	assert.Equal(t, "fakeContent2", mbox.Content)
	assert.Equal(t, tm.Format("2006-01-02 15:04:05"), mbox.CreatedAt.Format("2006-01-02 15:04:05"))

	mbox.Status = apistructs.MBoxReadStatus
	setDuplicateMbox(mbox, createReq)
	assert.Equal(t, int64(1), mbox.UnreadCount)
	assert.Equal(t, apistructs.MBoxUnReadStatus, mbox.Status)
	assert.Equal(t, "fakeTitle2", mbox.Title)
	assert.Equal(t, "fakeContent2", mbox.Content)
	assert.Equal(t, tm.Format("2006-01-02 15:04:05"), mbox.CreatedAt.Format("2006-01-02 15:04:05"))
}
