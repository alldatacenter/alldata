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

package subscriber

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/apistructs"
)

func SaveNotifyHistories(request *apistructs.CreateNotifyHistoryRequest, messenger pb.NotifyServiceServer) {
	var createRequest pb.CreateNotifyHistoryRequest
	data, err := json.Marshal(request)
	if err != nil {
		logrus.Errorf("创建通知历史记录失败: %v", err)
		return
	}
	err = json.Unmarshal(data, &createRequest)
	if err != nil {
		logrus.Errorf("创建通知历史记录失败: %v", err)
		return
	}
	_, err = messenger.CreateNotifyHistory(context.Background(), &createRequest)
	if err != nil {
		logrus.Errorf("创建通知历史记录失败: %v", err)
	}
}
