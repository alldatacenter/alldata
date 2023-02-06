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
//func TestNexus_ListActiveRealms(t *testing.T) {
//	users, err := n.ListActiveRealms(RealmListActiveRequest{})
//	assert.NoError(t, err)
//	s, _ := json.MarshalIndent(&users, "", "  ")
//	fmt.Println(string(s))
//}
//
//func TestNexus_SetActiveRealms(t *testing.T) {
//	err := n.SetActiveRealms(RealmSetActivesRequest{
//		ActiveRealms: []RealmID{
//			NpmBearerTokenRealm.ID,
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_ListAvailableRealms(t *testing.T) {
//	realms, err := n.ListAvailableRealms(RealmListAvailableRequest{})
//	assert.NoError(t, err)
//	s, _ := json.MarshalIndent(&realms, "", "  ")
//	fmt.Println(string(s))
//}
//
//func TestNexus_EnsureAddRealms(t *testing.T) {
//	assert.NoError(t, n.EnsureAddRealms(RealmEnsureAddRequest{Realms: []RealmID{DockerTokenRealm.ID}}))
//}
