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

package temporary

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"strings"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	fclient "github.com/erda-project/erda-infra/providers/remote-forward/client"
	common "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/routes/dynamic"
	httpapi "github.com/erda-project/erda/pkg/common/httpapi"
)

type (
	apiAuth struct {
		CheckLogin     bool `file:"check_login"`
		TryCheckLogin  bool `file:"try_check_login"`
		CheckToken     bool `file:"check_token"`
		CheckBasicAuth bool `file:"check_basic_auth"`
	}
	apiProxy struct {
		Method      string   `file:"method"`
		Path        string   `file:"path"`
		BackendPath string   `file:"backend_path"`
		Auth        *apiAuth `file:"auth"`
	}
	config struct {
		OpenAPI string     `file:"openapi_url"`
		Proxies []apiProxy `file:"proxies"`
	}
	provider struct {
		Cfg        *config
		Log        logs.Logger
		FClient    fclient.Interface `autowired:"remote-forward-client"`
		openapiURL string
		proxies    []*dynamic.APIProxy
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	if len(p.Cfg.OpenAPI) <= 0 {
		return fmt.Errorf("openapi url must not be empty")
	}
	o_url, err := url.Parse(p.Cfg.OpenAPI)
	if err != nil {
		return fmt.Errorf("invalid openapi url: %w", err)
	}
	p.openapiURL = o_url.String()

	serviceURL, err := p.getServiceURL()
	if err != nil {
		return fmt.Errorf("failed to get remote forward server url: %s", err)
	}
	p.proxies = make([]*dynamic.APIProxy, len(p.Cfg.Proxies))
	for i, item := range p.Cfg.Proxies {
		proxy := &dynamic.APIProxy{
			Method:      item.Method,
			Path:        item.Path,
			ServiceURL:  serviceURL,
			BackendPath: item.BackendPath,
		}

		if item.Auth != nil {
			proxy.Auth = &common.APIAuth{
				CheckLogin:     item.Auth.CheckLogin,
				TryCheckLogin:  item.Auth.TryCheckLogin,
				CheckToken:     item.Auth.CheckToken,
				CheckBasicAuth: item.Auth.CheckBasicAuth,
			}
			if !proxy.Auth.CheckLogin && !proxy.Auth.TryCheckLogin && !proxy.Auth.CheckToken && !proxy.Auth.CheckBasicAuth {
				proxy.Auth.NoCheck = true
			}
		}
		if err := proxy.Validate(); err != nil {
			return err
		}
		p.proxies[i] = proxy
	}
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	if len(p.proxies) <= 0 {
		return nil
	}
	body, err := json.Marshal(map[string]interface{}{
		"list": p.proxies,
	})
	if err != nil {
		return err
	}
	baseURL := strings.TrimRight(p.openapiURL, "/")
	req, err := http.NewRequest(http.MethodPut, fmt.Sprintf("%s/openapi/apis-batch-keepalive", baseURL), bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Add("Content-Type", "application/json")
	req = req.WithContext(ctx)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		if errors.Is(err, context.Canceled) {
			return nil
		}
		return err
	}
	var respBody httpapi.Response
	err = json.NewDecoder(resp.Body).Decode(&respBody)
	if err != nil {
		return fmt.Errorf("invalid response body: %w", err)
	}
	if !respBody.Success {
		return respBody.Err
	}
	return nil
}

func (p *provider) getServiceURL() (string, error) {
	_, port, err := net.SplitHostPort(p.FClient.RemoteShadowAddr())
	if err != nil {
		return "", err
	}
	host, ok := p.FClient.Values()["host"].(string)
	if !ok || len(host) <= 0 {
		return "", fmt.Errorf("not found remote forward server host")
	}
	return fmt.Sprintf("http://%s", net.JoinHostPort(host, port)), nil
}

func init() {
	servicehub.Register("openapi-temporary-proxy", &servicehub.Spec{
		Services:   []string{"openapi-temporary-proxy"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
