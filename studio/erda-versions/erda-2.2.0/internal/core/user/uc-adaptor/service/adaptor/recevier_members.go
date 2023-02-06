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

package adaptor

import (
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

// MemberReceiver member接收uc审计对象
type MemberReceiver struct {
	bdl *bundle.Bundle
}

// NewMemberReceiver 初始化成员接收者
func NewMemberReceiver(bdl *bundle.Bundle) *MemberReceiver {
	return &MemberReceiver{
		bdl: bdl,
	}
}

// Name .....
func (mr *MemberReceiver) Name() string {
	return "member_receiver"
}

// SendAudits .....
func (mr *MemberReceiver) SendAudits(ucaudits *apistructs.UCAuditsListResponse) ([]int64, error) {
	var ucIDs []int64
	var destroyMembers []string
	updateMembersMap := make(map[int64]apistructs.Member)
	for _, audit := range ucaudits.Result {
		if audit.EventType == "UPDATE_USER_INFO" {
			updateMembersMap[audit.UserInfo.ID] = apistructs.Member{
				UserID: strconv.FormatInt(audit.UserInfo.ID, 10),
				Email:  audit.UserInfo.Email,
				Mobile: audit.UserInfo.Mobile,
				Name:   audit.UserInfo.UserName,
				Nick:   audit.UserInfo.Nick,
				Avatar: audit.UserInfo.Avatar,
			}
		}
		if audit.EventType == "DESTROY" {
			// 不从userinfo里取是因为uc的注销事件 userinfo里是空
			destroyMembers = append(destroyMembers, strconv.FormatInt(audit.UserID, 10))
			// 只补偿删除用户的事件，修改用户信息的事件不做补偿
			ucIDs = append(ucIDs, audit.ID)
		}
	}

	l := len(updateMembersMap) + len(destroyMembers)
	logrus.Infof("%v is starting sync %v data", mr.Name(), l)
	if l == 0 {
		return nil, nil
	}

	members := make([]apistructs.Member, 0, len(updateMembersMap))
	for _, v := range updateMembersMap {
		members = append(members, v)
	}

	if err := mr.bdl.DestroyUsers(apistructs.MemberDestroyRequest{UserIDs: destroyMembers}); err != nil {
		return ucIDs, err
	}

	if err := mr.bdl.UpdateMemberUserInfo(apistructs.MemberUserInfoUpdateRequest{Members: members}); err != nil {
		return nil, err
	}

	return nil, nil
}
