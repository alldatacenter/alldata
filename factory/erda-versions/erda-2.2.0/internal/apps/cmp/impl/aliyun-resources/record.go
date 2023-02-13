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

package aliyun_resources

import (
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
)

func ProcessFailedRecord(ctx Context, source string, addonID string, record *dbclient.Record,
	detail *apistructs.CreateCloudResourceRecord, err error) {
	if len(detail.Steps) == 0 {
		return
	}
	i := len(detail.Steps) - 1
	detail.Steps[i].Status = string(dbclient.StatusTypeFailed)
	detail.Steps[i].Reason = err.Error()
	content, err := json.Marshal(detail)
	if err != nil {
		logrus.Errorf("marshal record detail failed, error:%+v", err)
	}
	record.Status = dbclient.StatusTypeFailed
	record.Detail = string(content)
	if err := ctx.DB.RecordsWriter().Update(*record); err != nil {
		logrus.Errorf("failed to update record: %v", err)
	}

	if source == "addon" {
		if addonID != "" {
			_, err := ctx.Bdl.AddonConfigCallbackProvison(addonID, apistructs.AddonCreateCallBackResponse{IsSuccess: false})
			if err != nil {
				logrus.Errorf("add call back provision failed, error:%v", err)
			}
		} else {
			logrus.Errorf("addon with no addonID")
		}
	}
}
