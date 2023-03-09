/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package dispatcher

import (
	"context"
	"fmt"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"

	"github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/client"
	mock_hookclient "github.com/koordinator-sh/koordinator/pkg/runtimeproxy/client/mock"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
	mock_config "github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config/mock"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/mock"
)

func TestRuntimeHookDispatcher_Dispatch(t *testing.T) {
	tests := []struct {
		name               string
		requestPath        config.RuntimeRequestPath
		allHooks           []*config.RuntimeHookConfig
		request            interface{}
		hookSeverReturnErr error
		expectedOperation  config.FailurePolicyType
		expectReturnErr    bool
	}{
		{
			name:              "no hook registered",
			requestPath:       config.RunPodSandbox,
			request:           &v1alpha1.PodSandboxHookRequest{},
			allHooks:          []*config.RuntimeHookConfig{},
			expectedOperation: config.PolicyNone,
			expectReturnErr:   false,
		},
		{
			name:        "normal hook hit, and hook server access ok",
			requestPath: config.RunPodSandbox,
			request:     &v1alpha1.PodSandboxHookRequest{},
			allHooks: []*config.RuntimeHookConfig{
				{
					RemoteEndpoint: "endpoint0",
					FailurePolicy:  config.PolicyFail,
					RuntimeHooks: []config.RuntimeHookType{
						config.PreRunPodSandbox,
					},
				},
			},
			expectedOperation: config.PolicyFail,
			expectReturnErr:   false,
		},
		{
			name:               "normal hook hit, and hook server access fail, should return err and PolicyFail",
			requestPath:        config.RunPodSandbox,
			request:            &v1alpha1.PodSandboxHookRequest{},
			hookSeverReturnErr: fmt.Errorf("should return err to kubelet instead of skipping"),
			allHooks: []*config.RuntimeHookConfig{
				{
					RemoteEndpoint: "endpoint0",
					FailurePolicy:  config.PolicyFail,
					RuntimeHooks: []config.RuntimeHookType{
						config.PreRunPodSandbox,
					},
				},
			},
			expectedOperation: config.PolicyFail,
			expectReturnErr:   true,
		},
		{
			name:        "has hook but not the requested one",
			requestPath: config.RunPodSandbox,
			request:     &v1alpha1.PodSandboxHookRequest{},
			allHooks: []*config.RuntimeHookConfig{
				{
					RemoteEndpoint: "endpoint0",
					FailurePolicy:  config.PolicyFail,
					RuntimeHooks: []config.RuntimeHookType{
						config.PreUpdateContainerResources,
					},
				},
			},
			expectedOperation: config.PolicyNone,
			expectReturnErr:   false,
		},
	}
	for _, tt := range tests {
		ctl := gomock.NewController(t)
		configManager := mock_config.NewMockManagerInterface(ctl)
		configManager.EXPECT().GetAllHook().Return(tt.allHooks).AnyTimes()

		runtimeProxyClient := mock.NewMockRuntimeHookServiceClient(ctl)
		runtimeProxyClient.EXPECT().PreRunPodSandboxHook(gomock.Any(), gomock.Any()).Return(&v1alpha1.PodSandboxHookResponse{},
			tt.hookSeverReturnErr).AnyTimes()

		clientManager := mock_hookclient.NewMockHookServerClientManagerInterface(ctl)

		runtimeHookClient := client.RuntimeHookClient{
			RuntimeHookServiceClient: runtimeProxyClient,
		}
		clientManager.EXPECT().RuntimeHookServerClient(gomock.Any()).Return(&runtimeHookClient, nil).AnyTimes()

		// construct the real runtimeHookDispatcher
		runtimeHookDispatcher := &RuntimeHookDispatcher{
			hookManager: configManager,
			cm:          clientManager,
		}
		_, err, operation := runtimeHookDispatcher.Dispatch(context.TODO(), tt.requestPath, config.PreHook, tt.request)
		assert.Equal(t, operation, tt.expectedOperation, tt.name)
		assert.Equal(t, err != nil, tt.expectReturnErr)
	}
}
