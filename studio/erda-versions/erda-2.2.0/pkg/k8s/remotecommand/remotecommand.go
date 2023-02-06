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

package remotecommand

import (
	"net/url"

	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/remotecommand"

	httpstreamspdy "github.com/erda-project/erda/pkg/k8s/httpstream/spdy"
	spdy "github.com/erda-project/erda/pkg/k8s/spdy"
)

// NewSPDYExecutor connects to the provided server and upgrades the connection to
// multiplexed bidirectional streams.
func NewSPDYExecutor(config *restclient.Config, method string, url *url.URL) (remotecommand.Executor, error) {
	wrapper, upgradeRoundTripper, err := spdy.RoundTripperFor(config)
	if err != nil {
		return nil, err
	}
	// request by cluster-dialer
	if config.Dial != nil {
		spdyRoundTripper, ok := upgradeRoundTripper.(*httpstreamspdy.SpdyRoundTripper)
		if ok {
			spdyRoundTripper.Dialer = config.Dial
		}
	}
	return remotecommand.NewSPDYExecutorForTransports(wrapper, upgradeRoundTripper, method, url)
}
