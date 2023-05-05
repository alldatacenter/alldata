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

package action

import (
	"testing"

	"github.com/bmizerany/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/i18n"
)

func Test_fillMysqlCliFields(t *testing.T) {
	type args struct {
		field          []apistructs.FormPropItem
		dataSourceList []map[string]interface{}
	}
	tests := []struct {
		name string
		args args
		want []apistructs.FormPropItem
	}{
		{
			name: "test_execution_conditions",
			args: args{
				field: []apistructs.FormPropItem{
					{
						Key: "if",
					},
				},
				dataSourceList: nil,
			},
			want: []apistructs.FormPropItem{
				{
					Key: "if",
				},
				{
					Component: "formGroup",
					ComponentProps: map[string]interface{}{
						"title": "任务参数",
					},
					Group: "params",
					Key:   "params",
				},
				{
					Label:     "datasource",
					Component: "select",
					Required:  true,
					ComponentProps: map[string]interface{}{
						"options": nil,
					},
					Group:    "params",
					Key:      "params.datasource",
					LabelTip: "数据源",
				},
				{
					Label:     "database",
					Component: "input",
					Required:  true,
					Key:       "params.database",
					ComponentProps: map[string]interface{}{
						"placeholder": "请输入数据",
					},
					Group:    "params",
					LabelTip: "数据库名称",
				},
				{
					Label:     "sql",
					Component: "textarea",
					Required:  true,
					Key:       "params.sql",
					ComponentProps: map[string]interface{}{
						"autoSize": map[string]interface{}{
							"minRows": 2,
							"maxRows": 12,
						},
						"placeholder": "请输入数据",
					},
					Group:    "params",
					LabelTip: "sql语句",
				},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := fillMysqlCliFields(&i18n.LocaleResource{}, tt.args.field, tt.args.dataSourceList)
			assert.Equal(t, len(got), len(tt.want))
			for index, v := range got {
				assert.Equal(t, v.Key, tt.want[index].Key)
				assert.Equal(t, v.Label, tt.want[index].Label)
				assert.Equal(t, v.Group, tt.want[index].Group)
				assert.Equal(t, v.Component, tt.want[index].Component)
			}
		})
	}
}
