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
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) GetPipelineReportSet(pipelineID uint64, types []string) (*apistructs.PipelineReportSet, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var pipelineResp apistructs.PipelineReportSetGetResponse
	req := hc.Get(host).Path(fmt.Sprintf("/api/pipeline-reportsets/%d", pipelineID))
	if types != nil {
		for _, v := range types {
			req.Param("type", v)
		}
	}
	httpResp, err := req.Header(httputil.InternalHeader, "bundle").
		Do().JSON(&pipelineResp)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !httpResp.IsOK() || !pipelineResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), pipelineResp.Error)
	}
	return pipelineResp.Data, nil
}
