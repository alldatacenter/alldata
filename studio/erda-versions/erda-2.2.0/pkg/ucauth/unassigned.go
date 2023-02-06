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

package ucauth

import (
	"strings"
)

const (
	UnassignedUserID USERID = "unassigned"
	emptyUserID      USERID = ""
)

func (u USERID) IsUnassigned() bool {
	return strings.EqualFold(u.String(), UnassignedUserID.String())
}

func PolishUnassignedAsEmptyStr(userIDs []string) (result []string) {
	for _, userID := range userIDs {
		polishedUserID := userID
		if USERID(userID).IsUnassigned() {
			polishedUserID = emptyUserID.String()
		}
		result = append(result, polishedUserID)
	}
	return result
}
