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

package adaptor

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

// AuditReceiver audit接收uc审计对象
type AuditReceiver struct {
	bdl *bundle.Bundle
}

// NewAuditReceiver 初始化审计事件接收者
func NewAuditReceiver(bdl *bundle.Bundle) *AuditReceiver {
	return &AuditReceiver{
		bdl: bdl,
	}
}

// Name ....
func (ar *AuditReceiver) Name() string {
	return "audit_receiver"
}

// SendAudits .....
func (ar *AuditReceiver) SendAudits(ucaudits *apistructs.UCAuditsListResponse) ([]int64, error) {
	if len(ucaudits.Result) == 0 {
		return nil, nil
	}
	batchReq, _, ucIDs := ucaudits.Convert2SysUCAuditBatchCreateRequest()
	logrus.Infof("%v is starting sync %v data", ar.Name(), len(batchReq.Audits))

	if err := ar.bdl.BatchCreateAuditEvent(&batchReq); err != nil {
		return ucIDs, err
	}

	return nil, nil
}
