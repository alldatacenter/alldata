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

package apistructs

import "time"

type TestPlanMember struct {
	ID         uint64             `json:"id"`
	TestPlanID uint64             `json:"testPlanID"`
	Role       TestPlanMemberRole `json:"role"`
	UserID     string             `json:"userID"`
	CreatedAt  time.Time          `json:"createdAt"`
	UpdatedAt  time.Time          `json:"updatedAt"`
}

type TestPlanMemberRole string

var (
	TestPlanMemberRoleOwner   TestPlanMemberRole = "Owner"
	TestPlanMemberRolePartner TestPlanMemberRole = "Partner"
)

func (role TestPlanMemberRole) Valid() bool {
	switch role {
	case TestPlanMemberRoleOwner, TestPlanMemberRolePartner:
		return true
	default:
		return false
	}
}
func (role TestPlanMemberRole) Invalid() bool {
	return !role.Valid()
}
func (role TestPlanMemberRole) IsOwner() bool {
	return role == TestPlanMemberRoleOwner
}
func (role TestPlanMemberRole) IsPartner() bool {
	return role == TestPlanMemberRolePartner
}
