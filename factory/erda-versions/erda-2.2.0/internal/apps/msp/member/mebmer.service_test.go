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

package member

import (
	"context"
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/msp/member/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	db2 "github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/tenant/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

////go:generate mockgen -destination=./member_register_test.go -package member github.com/erda-project/erda-infra/pkg/transport Register
////go:generate mockgen -destination=./projectServer_test.go -package member github.com/erda-project/erda-proto-go/msp/tenant/project/pb ProjectServiceServer
func Test_memberService_ListMember(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	register := NewMockRegister(ctrl)
	pro := &provider{
		Cfg:           &config{},
		Register:      register,
		memberService: &memberService{},
		bdl:           &bundle.Bundle{},
	}
	var bdl *bundle.Bundle
	listMembersAndTotalData := &apistructs.MemberList{
		List: []apistructs.Member{
			{
				UserID: "sss",
				Email:  "ssdddd",
				Mobile: "www",
				Name:   "gg",
				Nick:   "33",
				Avatar: "s",
				Status: "f",
				Scope: apistructs.Scope{
					Type: "project",
					ID:   "dddd",
				},
				Roles:   nil,
				Labels:  nil,
				Removed: false,
				Deleted: false,
			},
		},
		Total: 1,
	}
	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListMembersAndTotal",
		func(_ *bundle.Bundle, _ apistructs.MemberListRequest) (*apistructs.MemberList, error) {
			return listMembersAndTotalData, nil
		})
	monkey.Patch(memberService.GetProjectIdByScopeId, func(_ memberService, _ string) (string, error) {
		return "4", nil
	})
	pro.memberService.p = pro
	_, err := pro.memberService.ListMember(context.Background(), &pb.ListMemberRequest{
		ScopeType: "project",
		ScopeId:   "c393550824b3d50aa758fee4593d6e31",
		PageNo:    1,
		PageSize:  1,
	})
	if err != nil {
		fmt.Println(err)
		fmt.Println("should not error")
	}
}

func Test_memberService_DeleteMember(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	register := NewMockRegister(ctrl)
	pro := &provider{
		Cfg:           &config{},
		Register:      register,
		memberService: &memberService{},
		bdl:           &bundle.Bundle{},
	}
	var bdl *bundle.Bundle
	defer monkey.UnpatchAll()
	monkey.Patch(memberService.GetProjectIdByScopeId, func(_ memberService, _ string) (string, error) {
		return "4", nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DeleteMember",
		func(_ *bundle.Bundle, _ apistructs.MemberRemoveRequest) error {
			return nil
		})
	pro.memberService.p = pro
	_, err := pro.memberService.DeleteMember(context.Background(), &pb.DeleteMemberRequest{
		Scope: &pb.Scope{
			Id:   "12",
			Type: "project",
		},
		UserIds:        []string{"11"},
		UserID:         "ss",
		InternalClient: "ss",
	})
	if err != nil {
		fmt.Println(err)
		fmt.Println("should not err")
	}
}

func Test_memberService_ListMemberRoles(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	register := NewMockRegister(ctrl)
	pService := NewMockProjectServiceServer(ctrl)
	defer monkey.UnpatchAll()
	pro := &provider{
		Cfg:           &config{},
		Register:      register,
		memberService: &memberService{},
		bdl:           &bundle.Bundle{},
		ProjectServer: pService,
	}

	pService.EXPECT().GetProject(gomock.Any(), gomock.Any()).AnyTimes().Return(&projectpb.GetProjectResponse{
		Data: &projectpb.Project{
			Id:           "10",
			Name:         "sqw-xm",
			Type:         "DOP",
			Relationship: nil,
			CreateTime:   1632461946000000000,
			UpdateTime:   1632461946000000000,
			IsDeleted:    false,
			DisplayName:  "sqw-xm",
			DisplayType:  "DOP",
		},
	}, nil)
	monkey.Patch(apis.GetOrgID, func(_ context.Context) string {
		return "1"
	})
	monkey.Patch(apis.Language, func(_ context.Context) i18n.LanguageCodes {
		return i18n.LanguageCodes{
			{
				Code:    "zh",
				Quality: 0,
			},
		}
	})
	monkey.Patch((*db.MSPTenantDB).QueryTenant, func(_ *db.MSPTenantDB, _ string) (*db.MSPTenant, error) {
		return nil, nil
	})
	monkey.Patch((*db2.InstanceTenantDB).GetInstanceByTenantGroup, func(_ *db2.InstanceTenantDB, _ string) (*db2.InstanceTenant, error) {
		return &db2.InstanceTenant{
			ID:          "1",
			InstanceID:  "",
			Config:      "",
			Options:     `{"projectId":"3"}`,
			TenantGroup: "",
			Engine:      "",
			Az:          "",
			CreateTime:  time.Time{},
			UpdateTime:  time.Time{},
			IsDeleted:   "",
		}, nil
	})
	monkey.Patch((*bundle.Bundle).ListMemberRoles, func(_ *bundle.Bundle, _ apistructs.ListScopeManagersByScopeIDRequest, _ int64, _ string) (*apistructs.RoleList, error) {
		return &apistructs.RoleList{
			List: []apistructs.RoleInfo{
				{
					Role: "Owner",
					Name: "项目所有者",
				},
				{
					Role: "",
					Name: "",
				},
				{
					Role: "Lead",
					Name: "研发主管",
				},
			},
			Total: 8,
		}, nil
	})
	monkey.Patch(memberService.GetProjectIdByScopeId, func(_ memberService, _ string) (string, error) {
		return "4", nil
	})
	pro.memberService.p = pro
	_, err := pro.memberService.ListMemberRoles(context.Background(), &pb.ListMemberRolesRequest{
		ScopeType: "project",
		ScopeId:   "fc1f8c074e46a9df505a15c1a94d62cc",
	})
	if err != nil {
		fmt.Println(err)
		fmt.Println("should not err")
	}
}

func Test_memberService_CreateOrUpdateMember(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	register := NewMockRegister(ctrl)
	pro := &provider{
		Cfg:           &config{},
		Register:      register,
		memberService: &memberService{},
		bdl:           &bundle.Bundle{},
	}
	var bdl *bundle.Bundle
	defer monkey.UnpatchAll()
	monkey.Patch(apis.GetUserID, func(ctx context.Context) string {
		return "1"
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "AddMember",
		func(_ *bundle.Bundle, _ apistructs.MemberAddRequest, _ string) error {
			return nil
		})
	monkey.Patch(memberService.GetProjectIdByScopeId, func(_ memberService, _ string) (string, error) {
		return "4", nil
	})
	pro.memberService.p = pro
	_, err := pro.memberService.CreateOrUpdateMember(context.Background(), &pb.CreateOrUpdateMemberRequest{
		Scope: &pb.Scope{
			Id:   "sss",
			Type: "projuect",
		},
		TargetScopeType: "sss",
		TargetScopeIds:  []int64{12},
		Roles:           []string{"Dev"},
		UserIds:         []string{"12"},
		Options: &pb.MemberAddOptions{
			Rewrite: true,
		},
		Labels:     []string{"xxx"},
		VerifyCode: "sss",
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_memberService_ScopeRoleAccess(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	register := NewMockRegister(ctrl)
	pro := &provider{
		Cfg:           &config{},
		Register:      register,
		memberService: &memberService{},
		bdl:           &bundle.Bundle{},
	}
	var bdl *bundle.Bundle
	defer monkey.UnpatchAll()
	monkey.Patch(apis.GetUserID, func(ctx context.Context) string {
		return "1"
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ScopeRoleAccessList",
		func(_ *bundle.Bundle, _ string, _ *apistructs.ScopeRoleAccessRequest) (*apistructs.PermissionList, error) {
			return &apistructs.PermissionList{
				Access: false,
				Roles:  []string{"Dev"},
				PermissionList: []apistructs.ScopeResource{
					{
						Resource:     "xxx",
						Action:       "xxx",
						ResourceRole: "xxx",
					},
				},
				ResourceRoleList: []apistructs.ScopeResource{
					{
						Resource:     "sss",
						Action:       "sss",
						ResourceRole: "sss",
					},
				},
				Exist: false,
				ContactsWhenNoPermission: []string{
					"sss",
				},
			}, nil
		})
	pro.memberService.p = pro
	_, err := pro.memberService.ScopeRoleAccess(context.Background(), &pb.ScopeRoleAccessRequest{
		Scope: &pb.Scope{
			Id:   "sss",
			Type: "sss",
		},
	})
	if err != nil {
		fmt.Println("should not err")
	}
}
