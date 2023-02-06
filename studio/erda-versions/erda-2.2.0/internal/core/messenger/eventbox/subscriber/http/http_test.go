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

package http

import (
	"testing"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/constant"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

func Test_getUserID(t *testing.T) {
	type args struct {
		msg *types.Message
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test with nil msg",
			args: args{},
			want: "",
		},
		{
			name: "test with nil labels",
			args: args{
				msg: &types.Message{
					Labels: nil,
				},
			},
			want: "",
		},
		{
			name: "test with no WebhookLabelKey label",
			args: args{
				msg: &types.Message{
					Labels: map[types.LabelKey]interface{}{
						"foo": "bar",
					},
				},
			},
			want: "",
		},
		{
			name: "test with WebhookLabelKey label and string value",
			args: args{
				msg: &types.Message{
					Labels: map[types.LabelKey]interface{}{
						types.LabelKey(constant.WebhookLabelKey): "10001",
					},
				},
			},
			want: "",
		},
		{
			name: "test with WebhookLabelKey label and no userID",
			args: args{
				msg: &types.Message{
					Labels: map[types.LabelKey]interface{}{
						types.LabelKey(constant.WebhookLabelKey): map[string]interface{}{
							"foo": "bar",
						},
					},
				},
			},
			want: "",
		},
		{
			name: "test with not string userID value",
			args: args{
				msg: &types.Message{
					Labels: map[types.LabelKey]interface{}{
						types.LabelKey(constant.WebhookLabelKey): map[string]interface{}{
							"userID": 10001,
						},
					},
				},
			},
			want: "",
		},
		{
			name: "test with correct",
			args: args{
				msg: &types.Message{
					Labels: map[types.LabelKey]interface{}{
						types.LabelKey(constant.WebhookLabelKey): map[string]interface{}{
							"userID": "10001",
						},
					},
				},
			},
			want: "10001",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getUserIDFromMessage(tt.args.msg); got != tt.want {
				t.Errorf("getUserIDFromMessage() = %v, want %v", got, tt.want)
			}
		})
	}
}
