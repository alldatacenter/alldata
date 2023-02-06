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

package discover

import (
	"fmt"
	"net/url"

	"github.com/erda-project/erda-infra/base/servicehub"
	servicediscover "github.com/erda-project/erda/internal/pkg/service-discover"
)

type config struct {
	URLs map[string][]string `file:"urls"`
}

// +provider
type provider struct {
	Cfg  *config
	urls map[string][]*url.URL
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.urls = make(map[string][]*url.URL)
	for service, urls := range p.Cfg.URLs {
		for _, item := range urls {
			u, err := url.Parse(item)
			if err != nil {
				return err
			}
			p.urls[service] = append(p.urls[service], u)
		}
	}
	return nil
}

var _ servicediscover.Interface = (*provider)(nil)

func (p *provider) Endpoint(scheme, service string) (string, error) {
	for _, u := range p.urls[service] {
		if u.Scheme == scheme {
			return u.Host, nil
		}
	}
	return "", fmt.Errorf("not found endpoint %q", service)
}

func (p *provider) ServiceURL(scheme, service string) (string, error) {
	for _, u := range p.urls[service] {
		if u.Scheme == scheme {
			return u.String(), nil
		}
	}
	return "", fmt.Errorf("not found %s url for service %q", scheme, service)
}

func init() {
	servicehub.Register("fixed-discover", &servicehub.Spec{
		Services:    []string{"discover"},
		Description: "discover all services",
		ConfigFunc:  func() interface{} { return &config{} },
		Creator:     func() servicehub.Provider { return &provider{} },
	})
}
