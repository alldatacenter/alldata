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

package dao

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// TestPlanMember 测试计划成员表
type TestPlanMember struct {
	dbengine.BaseModel
	TestPlanID uint64                        `json:"testPlanID"`
	Role       apistructs.TestPlanMemberRole `json:"role"`
	UserID     string                        `json:"userID"`
}

func (TestPlanMember) TableName() string {
	return "dice_test_plan_members"
}

func (client *DBClient) GetUserTestPlanRole(userID string, testPlanID uint64) (apistructs.TestPlanMemberRole, error) {
	var mem TestPlanMember
	if err := client.DB.First(&mem, TestPlanMember{UserID: userID, TestPlanID: testPlanID}).Error; err != nil {
		return "", err
	}
	return mem.Role, nil
}

func (client *DBClient) CreateTestPlanMember(mem *TestPlanMember) error {
	return client.DB.Create(mem).Error
}

func (client *DBClient) UpdateTestPlanMember(mem *TestPlanMember) error {
	return client.DB.Save(mem).Error
}

func (client *DBClient) BatchCreateTestPlanMembers(members []TestPlanMember) error {
	return client.BulkInsert(members)
}

// OverwriteTestPlanMembers 使用新的成员列表覆盖之前的成员列表
func (client *DBClient) OverwriteTestPlanMembers(testPlanID uint64, members []TestPlanMember) error {
	if testPlanID == 0 {
		return fmt.Errorf("missing testPlanID")
	}
	// 删除原有成员
	if err := client.Where("`test_plan_id` = ?", testPlanID).Delete(&TestPlanMember{}).Error; err != nil {
		return err
	}
	// 插入新成员
	return client.BatchCreateTestPlanMembers(members)
}

// OverwriteTestPlanOwner 使用新的 owner 覆盖之前的 owner
func (client *DBClient) OverwriteTestPlanOwner(testPlanID uint64, ownerID string) error {
	if testPlanID == 0 {
		return fmt.Errorf("missing testPlanID")
	}
	// 删除原有 owner
	if err := client.Where("`test_plan_id` = ?", testPlanID).
		Where("`role` = ?", apistructs.TestPlanMemberRoleOwner).
		Delete(&TestPlanMember{}).Error; err != nil {
		return err
	}
	// 插入新成员
	return client.BatchCreateTestPlanMembers([]TestPlanMember{{
		TestPlanID: testPlanID,
		Role:       apistructs.TestPlanMemberRoleOwner,
		UserID:     ownerID,
	}})
}

// OverwriteTestPlanPartners 使用新的 partner 列表覆盖之前的 partner 列表
func (client *DBClient) OverwriteTestPlanPartners(testPlanID uint64, partnerIDs []string) error {
	if testPlanID == 0 {
		return fmt.Errorf("missing testPlanID")
	}
	// 删除原有 partners
	if err := client.Where("`test_plan_id` = ?", testPlanID).
		Where("`role` = ?", apistructs.TestPlanMemberRolePartner).
		Delete(&TestPlanMember{}).Error; err != nil {
		return err
	}
	// 插入新成员
	var partners []TestPlanMember
	for _, partnerID := range partnerIDs {
		partners = append(partners, TestPlanMember{
			TestPlanID: testPlanID,
			Role:       apistructs.TestPlanMemberRolePartner,
			UserID:     partnerID,
		})
	}
	return client.BatchCreateTestPlanMembers(partners)
}

func (client *DBClient) ListTestPlanMembersByPlanID(testPlanID uint64, roles ...apistructs.TestPlanMemberRole) ([]TestPlanMember, error) {
	sql := client.Where("`test_plan_id` = ?", testPlanID)
	if len(roles) > 0 {
		sql = sql.Where("`role` IN (?)", roles)
	}
	sql = sql.Order("`id` ASC")

	var members []TestPlanMember
	if err := sql.Find(&members).Error; err != nil {
		return nil, err
	}
	return members, nil
}

func (client *DBClient) ListTestPlanPartnersByPlanID(testPlanID uint64) ([]TestPlanMember, error) {
	return client.ListTestPlanMembersByPlanID(testPlanID, apistructs.TestPlanMemberRolePartner)
}

func (client *DBClient) ListTestPlanOwnersByPlanID(testPlanID uint64) ([]TestPlanMember, error) {
	return client.ListTestPlanMembersByPlanID(testPlanID, apistructs.TestPlanMemberRoleOwner)
}

func (client *DBClient) ListTestPlanMembersByPlanIDs(testPlanIDs []uint64, roles ...apistructs.TestPlanMemberRole) (map[uint64][]TestPlanMember, error) {
	sql := client.Where("`test_plan_id` IN (?)", testPlanIDs)
	if len(roles) > 0 {
		sql = sql.Where("`role` IN (?)", roles)
	}
	sql = sql.Order("`id` ASC")

	var members []TestPlanMember
	if err := sql.Find(&members).Error; err != nil {
		return nil, err
	}

	testPlanMemberMap := make(map[uint64][]TestPlanMember, len(testPlanIDs))
	for _, mem := range members {
		testPlanMemberMap[mem.TestPlanID] = append(testPlanMemberMap[mem.TestPlanID], mem)
	}
	return testPlanMemberMap, nil
}

func (client *DBClient) ListTestPlanIDsByOwnerIDs(ownerIDs []string) ([]uint64, error) {
	return client.ListTestPlanIDsByUserIDs(ownerIDs, apistructs.TestPlanMemberRoleOwner)
}
func (client *DBClient) ListTestPlanIDsByPartnerIDs(partnerIDs []string) ([]uint64, error) {
	return client.ListTestPlanIDsByUserIDs(partnerIDs, apistructs.TestPlanMemberRolePartner)
}
func (client *DBClient) ListTestPlanIDsByUserIDs(userIDs []string, roles ...apistructs.TestPlanMemberRole) ([]uint64, error) {
	sql := client.Where("`user_id` IN (?)", userIDs)
	if len(roles) > 0 {
		sql = sql.Where("`role` IN (?)", roles)
	}
	var users []TestPlanMember
	if err := sql.Find(&users).Error; err != nil {
		return nil, err
	}
	var tpIDs []uint64
	for _, user := range users {
		tpIDs = append(tpIDs, user.TestPlanID)
	}
	return strutil.DedupUint64Slice(tpIDs, true), nil
}

func (client *DBClient) ListTestPlanOwnersByOwners(owners []string) ([]TestPlanMember, error) {
	var members []TestPlanMember
	if err := client.Where("user_id in (?) and role = ?", owners, apistructs.TestPlanMemberRoleOwner).
		Find(&members).Error; err != nil {
		return nil, err
	}

	return members, nil
}

func (client *DBClient) DeleteTestPlanMemberByPlanID(planID uint64) error {
	return client.Where("test_plan_id = ?", planID).Delete(TestPlanMember{}).Error
}
