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

package prehandle

import (
	"context"
	"net/http"

	"github.com/erda-project/erda/pkg/http/httputil"
)

// FilterHeader remove internal header if exist in request
func FilterHeader(ctx context.Context, w http.ResponseWriter, req *http.Request) {
	req.Header.Del(httputil.UserHeader)
	req.Header.Del(httputil.OrgHeader)
	req.Header.Del(httputil.InternalHeader)
	req.Header.Del(httputil.ClientIDHeader)
	req.Header.Del(httputil.ClientNameHeader)
}
