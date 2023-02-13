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

package semver

import (
	"fmt"
	"regexp"
)

// Semantic Version
// see: https://semver.org
// see: https://github.com/semver/semver/issues/232#issuecomment-405596809
var SemverRegexp = regexp.MustCompile(`^(v)?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$`)

func Valid(ver string) bool {
	return SemverRegexp.MatchString(ver)
}

// New .
func New(major int, vers ...int) string {
	minor := 0
	patch := 0
	if len(vers) > 0 {
		if len(vers) > 0 {
			minor = vers[0]
		}
		if len(vers) > 1 {
			patch = vers[1]
		}
	}
	return fmt.Sprintf("%d.%d.%d", major, minor, patch)
}
