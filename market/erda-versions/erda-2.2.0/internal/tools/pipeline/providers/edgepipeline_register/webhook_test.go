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

package edgepipeline_register

import (
	"context"
	"fmt"
	nethttp "net/http"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc"

	"github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher"
	httpinput "github.com/erda-project/erda/internal/core/messenger/eventbox/input/http"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
)

type MockRegister struct{}

func (m *MockRegister) Add(method string, path string, handler http.HandlerFunc) {}

func (m *MockRegister) RegisterService(desc *grpc.ServiceDesc, impl interface{}) {}

func Test_initWebHookEndpoints(t *testing.T) {
	p := &provider{}

	p.Register = &MockRegister{}

	t.Run("initWebHookEndpoints", func(t *testing.T) {
		p.initWebHookEndpoints(context.Background())
	})
}

//func Test_startEventDispatcher(t *testing.T) {
//	p := &provider{}
//	dispatcherImpl := &dispatcher.DispatcherImpl{}
//	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(dispatcherImpl), "Start", func(_ *dispatcher.DispatcherImpl) {})
//	defer pm1.Unpatch()
//	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(dispatcherImpl), "Stop", func(_ *dispatcher.DispatcherImpl) {})
//	defer pm3.Unpatch()
//	pm2 := monkey.Patch(dispatcher.NewImpl, func() (*dispatcher.DispatcherImpl, error) {
//		return dispatcherImpl, nil
//	})
//	defer pm2.Unpatch()
//	p.eventDispatcher, _ = dispatcher.NewImpl()
//	t.Run("startEventDispatcher", func(t *testing.T) {
//		ctx, cancel := context.WithCancel(context.Background())
//		p.startEventDispatcher(ctx)
//		time.Sleep(time.Second)
//		cancel()
//	})
//}

func Test_newEventDispatcher(t *testing.T) {
	p := &provider{}
	p.httpI = &httpinput.HttpInput{}
	dispatcherImpl := &dispatcher.DispatcherImpl{}
	pm1 := monkey.Patch(dispatcher.NewImpl, func() (*dispatcher.DispatcherImpl, error) {
		return dispatcherImpl, nil
	})
	defer pm1.Unpatch()

	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(dispatcherImpl), "RegisterSubscriber", func(_ *dispatcher.DispatcherImpl, s subscriber.Subscriber) {
		return
	})
	defer pm2.Unpatch()

	pm3 := monkey.Patch(dispatcher.NewRouter, func(*dispatcher.DispatcherImpl) (*dispatcher.Router, error) {
		return &dispatcher.Router{}, nil
	})
	defer pm3.Unpatch()

	t.Run("newEventDispatcher", func(t *testing.T) {
		_, err := p.newEventDispatcher()
		if err != nil {
			t.Logf("newEventDispatcher error: %v", err)
		}
	})
}

func TestCreateMessageEvent(t *testing.T) {
	httpI := &httpinput.HttpInput{}
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(httpI), "CreateMessage", func(b *httpinput.HttpInput, ctx context.Context, request *pb.CreateMessageRequest, vars map[string]string) error {
		return nil
	})
	defer pm1.Unpatch()
	p := &provider{
		httpI: httpI,
	}
	err := p.CreateMessageEvent(&apistructs.EventCreateRequest{})
	assert.NoError(t, err)
}

type mockResponseWriter struct {
}

func (m mockResponseWriter) Header() nethttp.Header {
	return map[string][]string{}
}

func (m mockResponseWriter) Write(bytes []byte) (int, error) {
	return 0, nil
}

func (m mockResponseWriter) WriteHeader(statusCode int) {

}

func Test_wrapBadRequest(t *testing.T) {
	p := &provider{}
	t.Run("wrapBadRequest", func(t *testing.T) {
		p.wrapBadRequest(&mockResponseWriter{}, fmt.Errorf("test error"))
	})
}
