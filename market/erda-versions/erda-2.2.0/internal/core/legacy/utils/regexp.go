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

package utils

import "regexp"

// org regexp modify from pkg/strutil/regexp.go/reg, but org name can not be pure digital
var orgReg = regexp.MustCompile(`^(?:[a-z]+|[0-9]+[a-z]+|[0-9]+[-]+[a-z0-9])+(?:(?:(?:[-]*)[a-z0-9]+)+)?$`)

// IsValidOrgName check org name can contain a-z0-9- but can not pure 0-9
func IsValidOrgName(repo string) bool {
	return orgReg.MatchString(repo)
}
