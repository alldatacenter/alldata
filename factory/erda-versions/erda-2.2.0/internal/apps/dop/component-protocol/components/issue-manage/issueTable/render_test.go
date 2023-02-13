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

package issueTable

import (
	"context"
	"testing"

	gomock "github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	idpb "github.com/erda-project/erda-infra/providers/component-protocol/protobuf/proto-go/cp/pb"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
)

func TestGetTotalPage(t *testing.T) {
	data := []struct {
		total    uint64
		pageSize uint64
		page     uint64
	}{
		{10, 0, 0},
		{0, 10, 0},
		{20, 10, 2},
		{21, 10, 3},
	}
	for _, v := range data {
		assert.Equal(t, getTotalPage(v.total, v.pageSize), v.page)
	}
}

func Test_resetPageInfo(t *testing.T) {
	type args struct {
		req   *pb.PagingIssueRequest
		state map[string]interface{}
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "reset",
			args: args{
				req: &pb.PagingIssueRequest{},
				state: map[string]interface{}{
					"pageNo":   float64(1),
					"pageSize": float64(2),
				},
			},
		},
	}

	expected := []*pb.PagingIssueRequest{
		{
			PageNo:   1,
			PageSize: 2,
		},
	}
	for i, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			resetPageInfo(tt.args.req, tt.args.state)
			assert.Equal(t, expected[i], tt.args.req)
		})
	}
}

func Test_getPrefixIcon(t *testing.T) {
	assert.Equal(t, "ISSUE_ICON.issue.abc", getPrefixIcon("abc"))
	assert.Equal(t, "ISSUE_ICON.issue.123", getPrefixIcon("123"))
	assert.Equal(t, "ISSUE_ICON.issue.", getPrefixIcon(""))
}

func Test_resetPageNoByFilterCondition(t *testing.T) {
	assert.False(t, resetPageNoByFilterCondition("a", struct {
		a string
	}{}, map[string]interface{}{"a": "111"}))
	assert.True(t, resetPageNoByFilterCondition("b", struct {
		a string
	}{}, map[string]interface{}{"a": "111"}))
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return ""
}

func (m *MockTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return ""
}

func Test_buildTableItem(t *testing.T) {
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{Tran: &MockTran{}})
	ca := ComponentAction{}
	i := ca.buildTableItem(ctx, &pb.Issue{}, nil)
	assert.NotNil(t, i)
}

func Test_eventHandler(t *testing.T) {
	type args struct {
		ctx   context.Context
		event cptype.ComponentEvent
	}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{Identity: &idpb.IdentityInfo{UserID: "2"}})
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	issueService := NewMockInterface(ctrl)
	issueService.EXPECT().UpdateIssue(gomock.Any()).AnyTimes().Return(nil)
	ctx = context.WithValue(ctx, "issue", issueService)
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			args: args{ctx, cptype.ComponentEvent{
				Operation: "changePriorityToaURGENT", OperationData: map[string]interface{}{
					"meta": map[string]interface{}{
						"id": "1",
					},
				}}},
		},
		{
			args: args{ctx, cptype.ComponentEvent{
				Operation: "changeStateToWorking", OperationData: map[string]interface{}{
					"meta": map[string]interface{}{
						"id":    "1",
						"state": "2",
					},
				}}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := eventHandler(tt.args.ctx, tt.args.event); (err != nil) != tt.wantErr {
				t.Errorf("eventHandler() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
