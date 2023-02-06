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

package expression

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_expression(t *testing.T) {

	var testData = []struct {
		express string
		result  string
	}{
		{
			"${{ '${{' == '}}' }}", " '${{' == '}}' ",
		},
		{
			"${{ '}}' == '}}' }}", " '}}' == '}}' ",
		},
		{
			"${{ '1' == '}}' }}", " '1' == '}}' ",
		},
		{
			"${{ '${{' == '2' }}", " '${{' == '2' ",
		},
		{
			"${{ '1' == '2' }}", " '1' == '2' ",
		},
		{
			"${{ '2' == '2' }}", " '2' == '2' ",
		},
		{
			"${{ '' == '2' }}", " '' == '2' ",
		},
		{
			"${{${{ == '2' }}}}", "${{ == '2' }}",
		},
	}

	for _, condition := range testData {
		if ReplacePlaceholder(condition.express) != condition.result {
			fmt.Println(" error express ", condition.express)
			t.Fail()
		}
	}
}

func TestReconcile(t *testing.T) {
	type args struct {
		condition string
	}
	tests := []struct {
		name     string
		args     args
		wantSign SignType
	}{
		{
			name:     "invalid aa",
			args:     args{condition: "aa"},
			wantSign: TaskJumpOver,
		},
		{
			name:     "1 != 2",
			args:     args{condition: "1 != 2"},
			wantSign: TaskNotJumpOver,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if gotSign := Reconcile(tt.args.condition); gotSign.Sign != tt.wantSign {
				t.Errorf("Reconcile() = %v, want %v", gotSign, tt.wantSign)
			}
		})
	}
}

func TestQuote(t *testing.T) {
	s := "version: \"1.1\"\nstages:\n  - stage:\n      - api-test:\n          alias: \"8231\"\n          version: \"2.0\"\n          params:\n            asserts:\n              - arg: aa\n                operator: not_empty\n                value: \"\"\n            body:\n              content: null\n              type: \"\"\n            headers: []\n            id: \"\"\n            method: GET\n            name: 获取yml文件\n            out_params:\n              - expression: data.meta.pipelineYml\n                key: aa\n                source: body:json\n            params:\n              - desc: \"\"\n                key: scope\n                value: project-app\n              - desc: \"\"\n                key: scopeID\n                value: \"2\"\n            url: /api/project-pipeline/filetree/Mi8yL3RyZWUvbWFzdGVyL3BpcGVsaW5lLnltbA%3D%3D?scopeID=2&scope=project-app\n          labels:\n            AUTOTESTTYPE: STEP\n            STEP: eyJpZCI6ODIzMSwidHlwZSI6IkFQSSIsIm1ldGhvZCI6IiIsInZhbHVlIjoie1wiYXBpU3BlY1wiOntcImFzc2VydHNcIjpbe1wiYXJnXCI6XCJhYVwiLFwib3BlcmF0b3JcIjpcIm5vdF9lbXB0eVwiLFwidmFsdWVcIjpcIlwifV0sXCJib2R5XCI6e1wiY29udGVudFwiOm51bGwsXCJ0eXBlXCI6XCJcIn0sXCJoZWFkZXJzXCI6W10sXCJpZFwiOlwiXCIsXCJtZXRob2RcIjpcIkdFVFwiLFwibmFtZVwiOlwi6I635Y+WeW1s5paH5Lu2XCIsXCJvdXRfcGFyYW1zXCI6W3tcImV4cHJlc3Npb25cIjpcImRhdGEubWV0YS5waXBlbGluZVltbFwiLFwia2V5XCI6XCJhYVwiLFwic291cmNlXCI6XCJib2R5Ompzb25cIn1dLFwicGFyYW1zXCI6W3tcImRlc2NcIjpcIlwiLFwia2V5XCI6XCJzY29wZVwiLFwidmFsdWVcIjpcInByb2plY3QtYXBwXCJ9LHtcImRlc2NcIjpcIlwiLFwia2V5XCI6XCJzY29wZUlEXCIsXCJ2YWx1ZVwiOlwiMlwifV0sXCJ1cmxcIjpcIi9hcGkvcHJvamVjdC1waXBlbGluZS9maWxldHJlZS9NaTh5TDNSeVpXVXZiV0Z6ZEdWeUwzQnBjR1ZzYVc1bExubHRiQSUzRCUzRD9zY29wZUlEPTJcXHUwMDI2c2NvcGU9cHJvamVjdC1hcHBcIn0sXCJsb29wXCI6bnVsbH0iLCJuYW1lIjoi6I635Y+WeW1s5paH5Lu2IiwicHJlSUQiOjAsInByZVR5cGUiOiJTZXJpYWwiLCJzY2VuZUlEIjo5NTEsInNwYWNlSUQiOjcsImNyZWF0b3JJRCI6IiIsInVwZGF0ZXJJRCI6IiIsIkNoaWxkcmVuIjpudWxsLCJhcGlTcGVjSUQiOjB9\n          if: ${{ 1 == 1 }}"
	qs := Quote(s)
	assert.Equal(t, false, s == qs)
}

func TestDecodeOutputKey(t *testing.T) {
	type args struct {
		express string
	}
	tests := []struct {
		name  string
		args  args
		want  string
		want1 bool
	}{
		{
			name: "test_nil",
			args: args{
				express: "",
			},
			want:  "",
			want1: false,
		},
		{
			name: "test_other_express",
			args: args{
				express: "${{ configs.aaa }}",
			},
			want:  "${{ configs.aaa }}",
			want1: false,
		},
		{
			name: "test_true_express",
			args: args{
				express: "${{ outputs.aaa.bbb }}",
			},
			want:  "bbb",
			want1: true,
		},
		{
			name: "test_error_express",
			args: args{
				express: "${{ outputs.bbb }}",
			},
			want:  "${{ outputs.bbb }}",
			want1: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1 := DecodeOutputKey(tt.args.express)
			if got != tt.want {
				t.Errorf("DecodeOutputKey() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("DecodeOutputKey() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}
