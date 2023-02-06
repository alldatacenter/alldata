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

package nexus

//import (
//	"encoding/json"
//	"fmt"
//	"testing"
//
//	"github.com/stretchr/testify/assert"
//)
//
//func TestNexus_ListRoles(t *testing.T) {
//	users, err := n.ListRoles(RoleListRequest{
//		Source: "default",
//	})
//	assert.NoError(t, err)
//	s, _ := json.MarshalIndent(&users, "", "  ")
//	fmt.Println(string(s))
//}
//
//func TestNexus_CreateRole(t *testing.T) {
//	id := "sdk-npm-hosted-100-deployment"
//
//	err := n.CreateRole(RoleCreateRequest{
//		ID:          RoleID(id),
//		Name:        id,
//		Description: "desc",
//		Privileges: []PrivilegeID{
//			"nx-repository-view-npm-sdk-npm-hosted-100-add",
//			"nx-repository-view-npm-sdk-npm-hosted-100-browse",
//			"nx-repository-view-npm-sdk-npm-hosted-100-edit",
//			"nx-repository-view-npm-sdk-npm-hosted-100-read",
//		},
//		Roles: []string{},
//	})
//	assert.NoError(t, err)
//
//	role, err := n.GetRole(RoleGetRequest{
//		ID: RoleID(id),
//	})
//	assert.NoError(t, err)
//	printJSON(role)
//}
//
//func TestNexus_UpdateRole(t *testing.T) {
//	id := "b489a2f09421431eb8d49f83d8b81c20"
//
//	err := n.UpdateRole(RoleUpdateRequest{
//		ID:          RoleID(id),
//		Name:        id,
//		Description: "desc",
//		Privileges:  []PrivilegeID{"test-content-selector-privilege-npm"},
//		Roles:       []string{"id2"},
//	})
//	assert.NoError(t, err)
//
//	role, err := n.GetRole(RoleGetRequest{
//		ID: RoleID(id),
//	})
//	assert.NoError(t, err)
//	printJSON(role)
//}
//
//func TestNexus_GetRole(t *testing.T) {
//	role, err := n.GetRole(RoleGetRequest{
//		ID: "id2",
//	})
//	assert.NoError(t, err)
//	printJSON(role)
//}
//
//func TestNexus_DeleteRole(t *testing.T) {
//	err := n.DeleteRole(RoleDeleteRequest{
//		ID: "id2",
//	})
//	assert.NoError(t, err)
//}
