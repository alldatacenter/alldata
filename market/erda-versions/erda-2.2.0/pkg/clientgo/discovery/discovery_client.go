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

package discovery

import (
	"time"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	"k8s.io/client-go/discovery"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"

	"github.com/erda-project/erda/pkg/clientgo/restclient"
)

func NewDiscoveryClient(addr string) (*discovery.DiscoveryClient, error) {
	var (
		client rest.Interface
		err    error
		config *rest.Config
	)
	if addr != "" {
		config = restclient.GetDefaultConfig("")
		config.APIPath = ""
		config.GroupVersion = nil
		if config.Timeout == 0 {
			config.Timeout = 32 * time.Second
		}
		codec := runtime.NoopEncoder{Decoder: scheme.Codecs.UniversalDecoder()}
		config.NegotiatedSerializer = serializer.NegotiatedSerializerWrapper(runtime.SerializerInfo{Serializer: codec})
		client, err = restclient.NewInetRESTClient(addr, config)
	} else {
		config, err = rest.InClusterConfig()
		if err != nil {
			return nil, err
		}
		config.NegotiatedSerializer = scheme.Codecs.WithoutConversion()
		client, err = rest.UnversionedRESTClientFor(config)
	}
	if err != nil {
		return nil, err
	}
	return discovery.NewDiscoveryClient(client), nil
}
