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

package http

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/input"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/monitor"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

type HttpInput struct {
	handler   input.Handler
	stopch    chan struct{}
	InputName string
}

func New() (*HttpInput, error) {
	return &HttpInput{
		stopch: make(chan struct{}),
	}, nil
}

func (h *HttpInput) Start(handler input.Handler) error {
	h.handler = handler
	<-h.stopch
	return nil
}

func (h *HttpInput) Stop() error {
	h.stopch <- struct{}{}
	return nil
}

func (h *HttpInput) Name() string {
	return "HTTP"
}

func (h *HttpInput) CreateMessage(ctx context.Context, request *pb.CreateMessageRequest, vars map[string]string) error {
	var m types.Message
	data, err := json.Marshal(request)
	if err != nil {
		return fmt.Errorf("marshal message is failed err is %v", err)
	}
	err = json.Unmarshal(data, &m)
	if err != nil {
		return fmt.Errorf("unmarshal message failed err is %v", err)
	}
	logrus.Debugf("%s input message timestamp:%d", h.Name(), m.Time)
	monitor.Notify(monitor.MonitorInfo{Tp: monitor.HTTPInput})
	e := h.handler(&m)
	err = genResponse(e)
	if err != nil {
		return err
	}
	return nil
}
