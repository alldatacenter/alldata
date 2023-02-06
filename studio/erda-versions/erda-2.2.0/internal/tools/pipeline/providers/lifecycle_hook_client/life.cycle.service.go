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
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/core/pipeline/lifecycle_hook_client/pb"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type LifeCycleService struct {
	sync.Mutex
	hookClientMap map[string]*pb.LifeCycleClient
	logger        logs.Logger
	dbClient      *dbclient.Client
}

func (s *LifeCycleService) LifeCycleRegister(ctx context.Context, req *pb.LifeCycleClientRegisterRequest) (*pb.LifeCycleClientRegisterResponse, error) {
	if req.Name == "" {
		return nil, apierrors.ErrInvalidParameter.InternalError(fmt.Errorf("client name is required"))
	}
	if req.Host == "" {
		return nil, apierrors.ErrInvalidParameter.InternalError(fmt.Errorf("client host is required"))
	}
	hookClient := &dbclient.PipelineLifecycleHookClient{
		Name:   req.Name,
		Host:   req.Host,
		Prefix: req.Prefix,
	}
	err := s.dbClient.InsertOrUpdateLifeCycleClient(hookClient)
	if err != nil {
		return nil, err
	}
	s.Lock()
	defer s.Unlock()
	s.hookClientMap[hookClient.Name] = &pb.LifeCycleClient{
		ID:     hookClient.ID,
		Name:   hookClient.Name,
		Host:   hookClient.Host,
		Prefix: hookClient.Prefix,
	}
	return &pb.LifeCycleClientRegisterResponse{Data: hookClient.ID}, nil
}

func (s *LifeCycleService) loadLifecycleHookClient() error {
	clients, err := s.dbClient.FindLifecycleHookClientList()
	if err != nil {
		return fmt.Errorf("not find lifecycleHook hook client list: error %v", err)
	}

	s.Lock()
	for _, dbHookClient := range clients {
		s.hookClientMap[dbHookClient.Name] = &pb.LifeCycleClient{
			ID:     dbHookClient.ID,
			Name:   dbHookClient.Name,
			Host:   dbHookClient.Host,
			Prefix: dbHookClient.Prefix,
		}
	}
	s.Unlock()
	return nil
}

func (s *LifeCycleService) PostLifecycleHookHttpClient(source string, req interface{}, resp interface{}) error {

	s.logger.Debugf("postLifecycleHookHttpClient source: %v, request: %v", source, req)

	s.Lock()
	client := s.hookClientMap[source]
	s.Unlock()
	if client == nil {
		return fmt.Errorf("not find this source: %v client", source)
	}

	var httpClient = httpclient.New(
		httpclient.WithTimeout(time.Second, time.Second*5),
	)
	var buffer bytes.Buffer
	r, err := httpClient.Post(client.Host).
		Header(httputil.InternalHeader, "pipeline_lifecycle_hook").
		Path(client.Prefix + "/actions/lifecycle").
		JSONBody(&req).
		Do().
		Body(&buffer)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	if !r.IsOK() {
		return apierrors.ErrInvoke.InternalError(fmt.Errorf("request pipeline lifecycle hook failed httpcode: %v, body: %s", r.StatusCode(), buffer.String()))
	}

	err = json.NewDecoder(&buffer).Decode(resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(fmt.Errorf("body: %s, decode error %v", buffer.String(), err))
	}

	s.logger.Debugf("postLifecycleHookHttpClient response: %v", buffer.String())
	return nil
}
