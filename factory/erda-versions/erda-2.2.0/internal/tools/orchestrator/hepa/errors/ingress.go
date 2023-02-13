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

package errors

import (
	"regexp"
	"strings"
)

func IsRouteOptionAlreadyDefinedInIngressError(err error) (namespace, ingressName string, ok bool) {
	if err == nil {
		return "", "", false
	}
	if !strings.Contains(strings.ToLower(err.Error()), "is already defined in ingress") {
		return "", "", false
	}
	compile := regexp.MustCompile(`([\w\-]*)/([\w\-]*)$`)
	found := compile.FindStringSubmatch(err.Error())
	if len(found) != 3 {
		return "", "", true
	}
	return found[1], found[2], true
}
