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

package user

import (
	"net/http"
	"strconv"

	"github.com/pkg/errors"
)

// GetOrgID 从 http request 的 header 中读取 org id.
func GetOrgID(r *http.Request) (uint64, error) {
	v := r.Header.Get("ORG-ID")

	orgID, err := strconv.ParseUint(v, 10, 64)
	if err == nil {
		return orgID, nil
	}

	return 0, errors.Errorf("invalid org id")
}
