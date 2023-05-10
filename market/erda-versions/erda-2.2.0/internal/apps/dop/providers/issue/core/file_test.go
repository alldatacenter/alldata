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
	"io"
	"reflect"
	"strings"
	"testing"

	"bou.ke/monkey"
	"gotest.tools/assert"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/excel"
)

func TestIssueService_decodeFromExcelFile(t *testing.T) {
	tm := monkey.Patch(excel.Decode, func(r io.Reader) ([][][]string, error) {
		return [][][]string{
			{
				[]string{"ID", "标题", "内容", "状态", "创建人", "处理人", "负责人", "任务类型或缺陷引入源", "优先级", "所属迭代", "复杂度", "严重程度", "标签", "类型", "截止时间", "创建时间", "被以下事项关联", "预估时间", "关闭时间", "开始时间", "custom"},
				[]string{"1", "erda", "erda", "待处理", "erda", "erda", "erda", "缺陷", "低", "1.1", "中", "一般", "", "缺陷", "", "2021-09-26 15:19:00", "", "", "", "", "a"},
				[]string{"a", "erda", "erda", "待处理", "erda", "erda", "erda", "缺陷", "低", "1.1", "中", "一般", "", "缺陷", "", "2021-09-26 15:19:00", "", "", "", "", "b"},
				[]string{"2", "erda", "erda", "待处理", "erda", "erda", "erda", "缺陷", "低", "1.1", "中", "一般", "", "缺陷", "", "2021-09-26 15:19:00", "", "3h", "", "", "c"},
				[]string{"2", "erda", "erda", "待处理", "erda", "erda", "erda", "缺陷", "低", "1.1", "中", "一般", "", "缺陷", "", "2021-09-26 15:19:00", "", "3d", "", "2021-09-26 15:19:00", "d"},
			},
		}, nil
	})
	defer tm.Unpatch()

	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStatesByProjectID",
		func(d *dao.DBClient, projectID uint64, issueType string) ([]dao.IssueState, error) {
			return []dao.IssueState{
				{
					ProjectID: projectID,
					Name:      "待处理",
					BaseModel: dbengine.BaseModel{ID: 1},
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "FindIterations",
		func(d *dao.DBClient, projectID uint64) (iterations []dao.Iteration, err error) {
			return []dao.Iteration{
				{
					ProjectID: projectID,
					Title:     "1.1",
					BaseModel: dbengine.BaseModel{ID: 1},
				},
			}, nil
		},
	)
	defer p2.Unpatch()

	svc := &IssueService{db: db}
	_, _, _, _, _, _, err := svc.decodeFromExcelFile(&pb.ImportExcelIssueRequest{ProjectID: 1, OrgID: 1}, strings.NewReader(""), []*pb.IssuePropertyIndex{{PropertyName: "custom", Index: 1}})
	assert.Equal(t, nil, err)
}

func Test_importIssueBuilder(t *testing.T) {
	type args struct {
		req       pb.Issue
		request   *pb.ImportExcelIssueRequest
		memberMap map[string]string
	}
	tests := []struct {
		name string
		args args
		want dao.Issue
	}{
		{
			args: args{
				req: pb.Issue{
					Id:      1,
					Creator: "2",
				},
				request: &pb.ImportExcelIssueRequest{
					IdentityInfo: &commonpb.IdentityInfo{
						UserID: "3",
					},
				},
			},
			want: dao.Issue{
				Creator: "3",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := importIssueBuilder(tt.args.req, tt.args.request, tt.args.memberMap); !reflect.DeepEqual(got.Creator, tt.want.Creator) {
				t.Errorf("importIssueBuilder() = %v, want %v", got, tt.want)
			}
		})
	}
}
