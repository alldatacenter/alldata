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

package component_protocol

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httputil"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	_ "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/action/components/actionForm"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/types"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/i18n"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	i18npkg "github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

var Render = apis.ApiSpec{
	Path:         "/api/component-protocol/actions/render",
	Scheme:       "http",
	Method:       "POST",
	Custom:       proxyAndLegacy,
	RequestType:  apistructs.ComponentProtocolRequest{},
	ResponseType: apistructs.ComponentProtocolResponse{},
	Doc:          "某场景下，用户操作，触发后端业务逻辑，重新渲染协议",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
}

func proxyAndLegacy(rw http.ResponseWriter, r *http.Request) {
	// get scenario from query params
	scenario := r.URL.Query().Get("scenario")

	// bind scenario proxy
	needProxy, proxyConfig := types.CPConfigs.ScenarioNeedProxy(scenario)
	if !needProxy {
		// not found bind, use legacy
		logrus.Infof("scenario %s no need proxy, execute legacy openapi protocol render", scenario)
		legacyProtocolRender(rw, r)
		return
	}

	// new proxy
	// get addr by app
	if proxyConfig.Addr == "" {
		if proxyConfig.App == "" {
			http.Error(rw, fmt.Sprintf("no addr or app for scenario proxy, scenario: %s", scenario), http.StatusBadRequest)
			return
		}
		addr, err := discover.GetEndpoint(proxyConfig.App)
		if err != nil {
			http.Error(rw, fmt.Sprintf("failed to get addr through discover, scenario: %s, app: %s, err: %v",
				scenario, proxyConfig.App, err), http.StatusInternalServerError)
			return
		}
		proxyConfig.Addr = addr
	}
	logrus.Infof("scenario %s need proxy, proxy to app: %s, addr: %s", scenario, proxyConfig.App, proxyConfig.Addr)
	proxy := httputil.ReverseProxy{
		Director:       newProxyDirector(*proxyConfig),
		FlushInterval:  -1,
		ModifyResponse: modifyProxyResponse(*proxyConfig),
		ErrorHandler:   errorHandler,
	}
	proxy.ServeHTTP(rw, r)
	return
}

func newProxyDirector(proxyConfig types.ProxyConfig) func(*http.Request) {
	return func(r *http.Request) {
		schema := "http"
		if strings.HasPrefix(schema, "https://") {
			schema = "https"
		}
		proxyConfig.Addr = strings.TrimPrefix(proxyConfig.Addr, schema+"://")
		r.URL.Scheme = schema
		r.Host = proxyConfig.Addr
		r.URL.Host = proxyConfig.Addr
		path := r.URL.EscapedPath()
		path = strutil.Concat("/", strutil.TrimPrefixes(path, "/"))
		r.Header.Set("Origin-Path", path)
	}
}

func legacyProtocolRender(w http.ResponseWriter, r *http.Request) {
	req := apistructs.ComponentProtocolRequest{}
	d := json.NewDecoder(r.Body)
	if err := d.Decode(&req); err != nil {
		err := fmt.Errorf("decode compent render request failed, error: %v", err)
		logrus.Errorf("%s, buffered:%v", err.Error(), d.Buffered())
		_ = errorresp.ErrWrite(err, w)
		return
	}
	logrus.Infof("request header:%v", r.Header)

	var bundleOpts = []bundle.Option{
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Second*90),
				httpclient.WithEnableAutoRetry(false),
			)),
		bundle.WithAllAvailableClients(),
	}
	bdl := bundle.New(bundleOpts...)
	i18nPrinter := i18n.I18nPrinter(r)
	// get locale from request
	locale := i18npkg.GetLocaleNameByRequest(r)
	// UserID 来自session, OrgID 来自url xxx-org.xx
	i, _ := GetIdentity(r)
	ctxBdl := protocol.ContextBundle{
		Bdl:         bdl,
		I18nPrinter: i18nPrinter,
		Identity:    i,
		InParams:    req.InParams,
		Locale:      locale,
	}
	ctx := context.Background()
	ctx1 := context.WithValue(ctx, protocol.GlobalInnerKeyCtxBundle.String(), ctxBdl)

	err := protocol.RunScenarioRender(ctx1, &req)
	if err != nil {
		err := fmt.Errorf("run scenario render failed: %v", err)
		logrus.Errorf("%s, scenario: %+v, event: %+v", err.Error(), req.Scenario, req.Event)
		_ = errorresp.ErrWrite(err, w)
		return
	}

	rsp := RenderResponse(&req)
	httpserver.WriteJSON(w, rsp)
}

func RenderResponse(req *apistructs.ComponentProtocolRequest) *apistructs.ComponentProtocolResponse {
	rsp := apistructs.ComponentProtocolResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.ComponentProtocolResponseData{
			Scenario: req.Scenario,
			Protocol: *req.Protocol,
		},
	}
	userIDs := protocol.GetGlobalStateKV(req.Protocol, protocol.GlobalInnerKeyUserIDs.String())
	if userIDs != nil {
		rsp.UserIDs = userIDs.([]string)
		rsp.UserIDs = strutil.DedupSlice(rsp.UserIDs, true)
		delete(*req.Protocol.GlobalState, protocol.GlobalInnerKeyUserIDs.String())
	}

	err := protocol.GetGlobalStateKV(req.Protocol, protocol.GlobalInnerKeyError.String())

	if err != nil {
		errStr, ok := err.(string)
		if ok && len(errStr) > 0 {
			rsp.Error = apistructs.ErrorResponse{
				Code: strconv.Itoa(http.StatusInternalServerError),
				Msg:  err.(string),
			}
			rsp.Success = false
		}
	}

	return &rsp
}

func GetIdentity(r *http.Request) (i apistructs.Identity, err error) {
	uid := r.Header.Get("User-ID")
	oid := r.Header.Get("Org-ID")
	if uid == "" || oid == "" {
		if uid == "" {
			err = fmt.Errorf("failed to get user id in http header")
		} else {
			err = fmt.Errorf("failed to get org id in http header")
		}
	}
	i.OrgID = oid
	i.UserID = uid
	return
}
