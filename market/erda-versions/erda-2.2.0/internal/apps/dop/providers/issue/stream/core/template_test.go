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

package core

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
)

func TestGetDefaultContent(t *testing.T) {
	type args struct {
		req StreamTemplateRequest
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "comment",
			args: args{
				req: StreamTemplateRequest{
					StreamType:   common.ISTComment,
					StreamParams: common.ISTParam{Comment: "hello world"},
					Locale:       "zh",
				},
			},
			want:    `hello world`,
			wantErr: false,
		},
		{
			name: "change content",
			args: args{
				req: StreamTemplateRequest{
					StreamType: common.ISTChangeContent,
					StreamParams: common.ISTParam{
						CurrentContent: "old",
						NewContent:     "new",
					},
					Locale: "zh",
				},
			},
			want:    `内容发生变更`,
			wantErr: false,
		},
		{
			name: "change content by system",
			args: args{
				req: StreamTemplateRequest{
					StreamType: common.ISTTransferState,
					StreamParams: common.ISTParam{
						CurrentState: "old",
						NewState:     "new",
						ReasonDetail: "mrCreated",
					},
					Locale: "zh",
				},
			},
			want:    `状态自 "old" 迁移至 "new" mrCreated`,
			wantErr: false,
		},
		{
			args: args{
				req: StreamTemplateRequest{
					StreamType: common.ISTTransferState,
					StreamParams: common.ISTParam{
						CurrentState: "old",
						NewState:     "new",
						ReasonDetail: "mrCreated",
					},
				},
			},
			want:    `状态自 "old" 迁移至 "new" mrCreated`,
			wantErr: false,
		},
	}

	p := &provider{commonTran: &mockTranslator{}, I18n: &mockTranslator{}}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := p.GetDefaultContent(tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("getDefaultContent() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getDefaultContent() got = %v, want %v", got, tt.want)
			}
		})
	}
}

type mockTranslator struct{}

func (m *mockTranslator) Get(lang i18n.LanguageCodes, key, def string) string { return key }
func (m *mockTranslator) Text(lang i18n.LanguageCodes, key string) string     { return key }
func (m *mockTranslator) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return key
}

func Test_getDefaultContentForMsgSending(t *testing.T) {
	type args struct {
		ist    string
		param  common.ISTParam
		tran   i18n.Translator
		locale string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "comment",
			args: args{
				ist: common.ISTComment,
				param: common.ISTParam{
					Comment: "hello world",
				},
			},
			want:    `添加了备注: hello world`,
			wantErr: false,
		},
		{
			name: "change content",
			args: args{
				ist: common.ISTChangeContent,
			},
			want:    common.IssueTemplate["zh"][common.ISTChangeContent],
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getDefaultContentForMsgSending(tt.args.ist, tt.args.param, &mockTranslator{}, "zh")
			if (err != nil) != tt.wantErr {
				t.Errorf("getDefaultContentForMsgSending() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getDefaultContentForMsgSending() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_renderTemplate(t *testing.T) {
	type args struct {
		templateContent string
		param           common.ISTParam
		tran            i18n.Translator
		lang            i18n.LanguageCodes
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "change iteration",
			args: args{
				templateContent: common.IssueTemplate["zh"][common.ISTChangeIteration],
				param:           common.ISTParam{CurrentIteration: "1.2", NewIteration: "1.3"},
			},
			want:    `迭代由 "1.2" 变更为 "1.3"`,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := renderTemplate(tt.args.templateContent, tt.args.param, &mockTranslator{}, tt.args.lang)
			if (err != nil) != tt.wantErr {
				t.Errorf("renderTemplate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("renderTemplate() got = %v, want %v", got, tt.want)
			}
		})
	}
}
