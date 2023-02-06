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

package projectCache

import (
	"testing"

	"github.com/erda-project/erda/internal/core/legacy/model"
)

func Test_getFirstValidOwnerOrLead(t *testing.T) {
	var members = []model.Member{
		{
			UserID: "1",
			Roles:  []string{"Developer"},
		}, {
			UserID: "2",
			Roles:  []string{"Lead"},
		}, {
			UserID: "3",
			Roles:  []string{"Lead", "Owner"},
		}, {
			UserID: "4",
			Roles:  []string{"Owner"},
		},
	}
	var member *model.Member
	hitFirstValidOwnerOrLead(member, members)

	member = new(model.Member)
	hitFirstValidOwnerOrLead(member, members)
	if member.UserID != "3" {
		t.Fatal("hit error")
	}
}

func Test_newProjectClusterNamespaceItem(t *testing.T) {
	newProjectClusterNamespaceItem(1)
}
