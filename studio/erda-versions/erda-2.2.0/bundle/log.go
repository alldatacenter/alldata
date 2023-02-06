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
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// PushLog 推日志
func (b *Bundle) PushLog(req *apistructs.LogPushRequest) error {
	host, err := b.urls.Collector()
	if err != nil {
		return err
	}
	hc := b.hc

	resp, err := hc.Post(host, httpclient.RetryErrResp).
		Path("/collect/logs/job").
		JSONBody(req.Lines).
		Header("Content-Type", "application/json").
		Do().DiscardBody()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to pushLog, error is %d", resp.StatusCode()))
	}
	return nil
}
