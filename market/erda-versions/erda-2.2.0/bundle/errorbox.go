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

package bundle

import (
	"bytes"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// CreateErrorLog 创建错误日志
func (b *Bundle) CreateErrorLog(errorLog *apistructs.ErrorLogCreateRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/task-error/actions/create").
		Header(httputil.InternalHeader, "bundle").JSONBody(&errorLog).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to create ErrorLog, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}
