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

package model

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// Comment 工单评论模型
type Comment struct {
	dbengine.BaseModel

	TicketID    int64
	CommentType apistructs.TCType
	Content     string               `gorm:"type:text"`
	IRComment   apistructs.IRComment `json:"irComment" gorm:"column:ir_comment;type:text"`
	UserID      string
}

// TableName 设置模型对应数据库表名称
func (Comment) TableName() string {
	return "ps_comments"
}
