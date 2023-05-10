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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GetMonitorReportTasksByID .
func (b *Bundle) GetMonitorReportTasksByID(id int64) (*apistructs.ReportTask, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.GetMonitorReportTaskResponse
	resp, err := hc.Get(host).Path("/api/org/report/tasks/"+strconv.FormatInt(id, 10)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}
