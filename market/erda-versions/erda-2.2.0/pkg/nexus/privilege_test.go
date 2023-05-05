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
//func TestGetNxRepositoryViewPrivileges(t *testing.T) {
//	fmt.Println(
//		GetNxRepositoryViewPrivileges(
//			"npm-hosted-publisher-1-deployment",
//			RepositoryFormatMaven,
//			PrivilegeActionADD,
//			PrivilegeActionEDIT,
//			PrivilegeActionBROWSE,
//			PrivilegeActionREAD,
//		),
//	)
//}
//
//func TestNexus_ListPrivileges(t *testing.T) {
//	users, err := n.ListPrivileges(PrivilegeListRequest{})
//	assert.NoError(t, err)
//	s, _ := json.MarshalIndent(&users, "", "  ")
//	fmt.Println(string(s))
//}
//
//func TestNexus_GetPrivilege(t *testing.T) {
//	users, err := n.GetPrivilege(PrivilegeGetRequest{
//		PrivilegeID: "test-content-selector-privilege-maven-all",
//	})
//	assert.NoError(t, err)
//	s, _ := json.MarshalIndent(&users, "", "  ")
//	fmt.Println(string(s))
//}
//
//func TestNexus_DeletePrivilege(t *testing.T) {
//	err := n.DeletePrivilege(PrivilegeDeleteRequest{
//		PrivilegeID: "nx-all",
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateRepositoryContentSelectorPrivilege(t *testing.T) {
//	privilegeName := "test-content-selector-privilege-npm"
//
//	err := n.CreateRepositoryContentSelectorPrivilege(RepositoryContentSelectorPrivilegeCreateRequest{
//		Name:            privilegeName,
//		Description:     "all maven repo read",
//		Actions:         []PrivilegeAction{PrivilegeActionREAD},
//		Format:          RepositoryFormatNpm,
//		Repository:      "*",
//		ContentSelector: "test-content-selector",
//	})
//	assert.NoError(t, err)
//
//	privilege, err := n.GetPrivilege(PrivilegeGetRequest{
//		PrivilegeID: privilegeName,
//	})
//	assert.NoError(t, err)
//	printJSON(privilege)
//}
//
//func TestNexus_UpdateRepositoryContentSelectorPrivilege(t *testing.T) {
//	privilegeName := "test-content-selector-privilege-npm"
//
//	err := n.UpdateRepositoryContentSelectorPrivilege(RepositoryContentSelectorPrivilegeUpdateRequest{
//		Name:            privilegeName,
//		Description:     "all maven repo readsss",
//		Actions:         []PrivilegeAction{PrivilegeActionREAD},
//		Format:          RepositoryFormatNpm,
//		Repository:      "*",
//		ContentSelector: "test-content-selector",
//	})
//	assert.NoError(t, err)
//
//	privilege, err := n.GetPrivilege(PrivilegeGetRequest{
//		PrivilegeID: privilegeName,
//	})
//	assert.NoError(t, err)
//	printJSON(privilege)
//}
