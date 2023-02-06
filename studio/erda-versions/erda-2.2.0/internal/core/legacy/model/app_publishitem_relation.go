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

import "github.com/erda-project/erda/apistructs"

// ApplicationPublishItemRelation 应用和发布项关联关系
type ApplicationPublishItemRelation struct {
	BaseModel
	AppID         int64
	PublishItemID int64
	Env           apistructs.DiceWorkspace
	Creator       string
	AK            string
	AI            string
}

// TableName 设置模型对应数据库表名称
func (ApplicationPublishItemRelation) TableName() string {
	return "dice_app_publish_item_relation"
}
