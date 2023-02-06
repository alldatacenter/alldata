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

package lifecycle_hook_client

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-proto-go/core/pipeline/lifecycle_hook_client/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
)

func Test_loadLifecycleHookClient(t *testing.T) {
	var table = []struct {
		clients []*dbclient.PipelineLifecycleHookClient
	}{
		{
			clients: []*dbclient.PipelineLifecycleHookClient{
				{
					Name:   "FDP",
					ID:     1,
					Host:   "FDP.svc.default",
					Prefix: "/api/workflow",
				},
				{
					Name:   "dice",
					ID:     2,
					Host:   "dice.svc.default",
					Prefix: "/api/dice",
				},
			},
		},
		{
			clients: []*dbclient.PipelineLifecycleHookClient{},
		},
	}

	for _, v := range table {
		var e dbclient.Client
		monkey.PatchInstanceMethod(reflect.TypeOf(&e), "FindLifecycleHookClientList", func(_ *dbclient.Client, ops ...dbclient.SessionOption) (clients []*dbclient.PipelineLifecycleHookClient, err error) {
			return v.clients, nil
		})
		s := &LifeCycleService{
			dbClient:      &e,
			hookClientMap: map[string]*pb.LifeCycleClient{},
		}
		err := s.loadLifecycleHookClient()
		assert.NoError(t, err)

		assert.Equal(t, len(v.clients), len(s.hookClientMap))
		for _, client := range v.clients {
			assert.Equal(t, s.hookClientMap[client.Name].Name, client.Name)
			assert.Equal(t, s.hookClientMap[client.Name].ID, client.ID)
			assert.Equal(t, s.hookClientMap[client.Name].Prefix, client.Prefix)
			assert.Equal(t, s.hookClientMap[client.Name].Host, client.Host)
		}
		s.hookClientMap = map[string]*pb.LifeCycleClient{}
	}
}
