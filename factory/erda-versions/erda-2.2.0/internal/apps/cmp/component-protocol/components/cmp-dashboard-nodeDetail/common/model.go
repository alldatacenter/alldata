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

package common

import (
	"errors"
	"strings"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

var (
	CMPDashboardRemoveLabel cptype.OperationKey = "deleteLabel"

	NothingToBeDoneErr = errors.New("nothing to be done")

	ResourceNotFoundErr = errors.New("resource type not available")

	//util error
	PtrRequiredErr = errors.New("ptr is required")
)

func GetStatus(s string) string {
	if strings.ToLower(s) == "ready" {
		return "success"
	}
	return "error"
}
