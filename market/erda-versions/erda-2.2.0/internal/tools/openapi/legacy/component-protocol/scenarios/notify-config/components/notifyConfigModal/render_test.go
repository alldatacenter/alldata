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

package notifyConfigModal

import (
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"gotest.tools/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

func TestGetFieldData(t *testing.T) {
	bundleOpts := []bundle.Option{}
	bdl := bundle.New(bundleOpts...)
	i := ComponentModel{}
	b := protocol.ContextBundle{
		Bdl: bdl,
	}
	i.CtxBdl = b
	assertDetail := apistructs.DetailResponse{
		Id:         42,
		NotifyID:   `["pipeline_failed","pipeline_success","git_close_mr","git_comment_mr"]`,
		NotifyName: "pipline_test",
		Target:     `{"group_id":33,"channels":["dingding"],"dingdingUrl":"https://oapi.dingtalk.com/robot/send?access_token=xxx"}`,
		GroupType:  "dingding",
	}
	notifyDetail := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetNotifyDetail", func(_ *bundle.Bundle, id uint64) (*apistructs.DetailResponse, error) {
		return &apistructs.DetailResponse{
			Id:         42,
			NotifyID:   "[\"pipeline_failed\",\"pipeline_success\",\"git_close_mr\",\"git_comment_mr\"]",
			NotifyName: "pipline_test",
			Target:     "{\"group_id\":33,\"channels\":[\"dingding\"],\"dingdingUrl\":\"https://oapi.dingtalk.com/robot/send?access_token=xxx\"}",
			GroupType:  "dingding",
		}, nil
	})
	defer notifyDetail.Unpatch()
	allTemplate := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetAllTemplates", func(_ *bundle.Bundle, scope, scopeId, userId string) (map[string]string, error) {
		return map[string]string{
			"git_close_mr":      "合并请求-关闭",
			"git_delete_branch": "删除分支",
			"git_merge_mr":      "合并请求-合并",
			"git_push":          "代码推送",
			"pipeline_success":  "流水线运行成功",
			"pipeline_failed":   "流水线运行失败",
			"git_comment_mr":    "合并请求-评论",
			"git_create_mr":     "合并请求-创建",
			"git_delete_tag":    "删除标签",
			"pipeline_running":  "流水线开始运行",
		}, nil
	})
	defer allTemplate.Unpatch()
	allGroups := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetAllGroups", func(_ *bundle.Bundle, scope, scopeId, orgId, userId string) ([]apistructs.AllGroups, error) {
		groups := []apistructs.AllGroups{
			{
				Name:  "notify_test",
				Value: 33,
				Type:  "dingding",
			},
		}
		return groups, nil
	})
	defer allGroups.Unpatch()
	ms := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetNotifyConfigMS", func(_ *bundle.Bundle, userId, orgId string) (bool, error) {
		return false, nil
	})
	defer ms.Unpatch()
	id := 42
	scope := "app"
	scopeId := "18"
	userId := "2"
	orgId := "1"
	state := State{
		Operation: "edit",
		EditId:    uint64(id),
	}
	i.CtxBdl.Identity.OrgID = orgId
	i.CtxBdl.Identity.UserID = userId
	i.CtxBdl.InParams = map[string]interface{}{
		"scopeType": scope,
		"scopeId":   scopeId,
	}
	detail, list, err := i.getDetailAndField(state)
	fmt.Printf("the detail is %+v\n", *detail)
	fmt.Printf("the assert is %+v\n", assertDetail)
	fmt.Println()
	fmt.Println()
	fmt.Printf("the list is %+v\n", list)
	if err != nil {
		panic(err)
	}
	assert.Equal(t, assertDetail, *detail)
}
