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

package server

import (
	"sync"
	"time"

	"github.com/mohae/deepcopy"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

var (
	clientMutex sync.Mutex
	clientDatas = map[apistructs.ClusterManagerClientType]apistructs.ClusterManagerClientMap{}
	bdl         *bundle.Bundle
)

func initClientData(b *bundle.Bundle) {
	bdl = b
}

func updateClientDetailWithEvent(clientType apistructs.ClusterManagerClientType, clusterKey string, data apistructs.ClusterManagerClientDetail) {
	clientMutex.Lock()
	defer clientMutex.Unlock()
	if clusterKey == "" || clientType == "" {
		return
	}
	if _, ok := clientDatas[clientType]; !ok {
		clientDatas[clientType] = apistructs.ClusterManagerClientMap{}
	}
	clientDatas[clientType][clusterKey] = data
	if err := bdl.CreateEvent(&apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:     clientType.GenEventName(apistructs.ClusterManagerClientEventRegister),
			Action:    bundle.UpdateAction,
			OrgID:     "-1",
			ProjectID: "-1",
			TimeStamp: time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderClusterManager,
		Content: data,
	}); err != nil {
		logrus.Errorf("[cluster-manager] create event failed: %v", err)
	}
}

func getClientDetail(clientType apistructs.ClusterManagerClientType, clusterKey string) (apistructs.ClusterManagerClientDetail, bool) {
	clientMutex.Lock()
	defer clientMutex.Unlock()
	if data, ok := clientDatas[clientType]; ok {
		if clientData, existed := data[clusterKey]; existed {
			return clientData, true
		}
	}
	return apistructs.ClusterManagerClientDetail{}, false
}

func listClientDetailByType(clientType apistructs.ClusterManagerClientType) []apistructs.ClusterManagerClientDetail {
	clientMutex.Lock()
	defer clientMutex.Unlock()
	if data, existed := clientDatas[clientType]; existed {
		var clientDetails []apistructs.ClusterManagerClientDetail
		for _, clientData := range data {
			clientDataDup, ok := deepcopy.Copy(clientData).(apistructs.ClusterManagerClientDetail)
			if !ok {
				continue
			}
			clientDetails = append(clientDetails, clientDataDup)
		}
		return clientDetails
	}
	return []apistructs.ClusterManagerClientDetail{}
}
