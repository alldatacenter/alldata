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

package models

import (
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
)

func (svc *Service) TriggerEvent(repository *gitmodule.Repository, eventName string, contentData interface{}) {
	err := svc.bundle.CreateEvent(&apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			ApplicationID: strconv.FormatInt(repository.ApplicationId, 10),
			ProjectID:     strconv.FormatInt(repository.ProjectId, 10),
			OrgID:         strconv.FormatInt(repository.OrgId, 10),
			Event:         eventName,
			Action:        eventName,
		},
		Sender:  "gittar",
		Content: contentData,
	})
	if err != nil {
		logrus.Errorf("failed to trigger event %s err:%s", eventName, err)
	}
}
