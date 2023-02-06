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

package legacy

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"net"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api"
	apispec "github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/hooks"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/hooks/posthandle"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/monitor"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/proxy"
	phttp "github.com/erda-project/erda/internal/tools/openapi/legacy/proxy/http"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/proxy/ws"
)

type ReverseProxyWithAuth struct {
	httpProxy http.Handler
	wsProxy   http.Handler
	auth      *auth.Auth
	bundle    *bundle.Bundle
	cache     *sync.Map
}

func NewReverseProxyWithAuth(auth *auth.Auth, bundle *bundle.Bundle) (http.Handler, error) {
	director := proxy.NewDirector()
	httpProxy := phttp.NewReverseProxyWithCustom(director, modifyResponse)
	wsProxy := ws.NewReverseProxyWithCustom(director)
	return &ReverseProxyWithAuth{httpProxy: httpProxy, wsProxy: wsProxy, auth: auth, bundle: bundle, cache: &sync.Map{}}, nil
}

func (r *ReverseProxyWithAuth) ServeHTTP(rw http.ResponseWriter, req *http.Request) {
	logrus.Infof("handle request: %v", req.URL)

	spec := api.API.Find(req)
	if spec == nil {
		errStr := fmt.Sprintf("not found path: %v", req.URL)
		logrus.Error(errStr)
		http.Error(rw, errStr, 404)
		return
	}
	if authr := r.auth.Auth(spec, req); authr.Code != auth.AuthSucc {
		errStr := fmt.Sprintf("auth failed: %v", authr.Detail)
		logrus.Error(errStr)
		http.Error(rw, errStr, authr.Code)
		return
	}
	switch spec.Scheme {
	case apispec.HTTP:
		monitor.Notify(monitor.Info{
			Tp:     monitor.APIInvokeCount,
			Detail: spec.Path.String(),
		})
		start := time.Now()
		if !spec.ChunkAPI && spec.Audit != nil {
			reqBody, err := ioutil.ReadAll(req.Body)
			errStr := fmt.Sprintf("read body failed: %v", err)
			if err != nil {
				logrus.Error(errStr)
				http.Error(rw, errStr, http.StatusBadRequest)
				return
			}
			c := context.WithValue(req.Context(), "reqBody", ioutil.NopCloser(bytes.NewReader(reqBody)))
			c = context.WithValue(c, "bundle", r.bundle)
			c = context.WithValue(c, "beginTime", time.Now())
			c = context.WithValue(c, "cache", r.cache)
			req = req.WithContext(c)
			req.Body = ioutil.NopCloser(bytes.NewReader(reqBody))
			r.httpProxy.ServeHTTP(rw, req)
		} else {
			r.httpProxy.ServeHTTP(rw, req)
		}

		elapsed := time.Since(start)
		monitor.Notify(monitor.Info{
			Tp:     monitor.APIInvokeDuration,
			Detail: spec.Path.String(),
			Value:  elapsed.Nanoseconds() / 1000000, // ms
		})
	case apispec.WS:
		r.wsProxy.ServeHTTP(rw, req)
	default:
		errStr := fmt.Sprintf("not support scheme: %v", spec.Scheme)
		logrus.Error(errStr)
		http.Error(rw, errStr, http.StatusBadRequest)
		return
	}
}

func modifyResponse(res *http.Response) error {
	spec := api.API.FindOriginPath(res.Request)
	if spec == nil {
		// unreachable
		logrus.Errorf("failed to modifyResponse: not found spec (unreachable):%v", res.Request.URL)
		return nil
	}
	defer func() {
		if res.StatusCode/100 == 5 {
			monitor.Notify(monitor.Info{
				Tp:     monitor.API50xCount,
				Detail: spec.Path.String(),
			})
		} else if res.StatusCode/100 == 4 {
			monitor.Notify(monitor.Info{
				Tp:     monitor.API40xCount,
				Detail: spec.Path.String(),
			})
		}
	}()
	var err error
	if spec.CustomResponse != nil {
		err = spec.CustomResponse(res)
	}
	if err != nil {
		logrus.Errorf("failed to modifyResponse: %v", err)
		return err
	}
	if !spec.ChunkAPI {
		if hooks.Enable {
			if err = posthandle.InjectUserInfo(res, spec.NeedDesensitize); err != nil {
				logrus.Errorf("failed to inject userinfo: %v", err)
				return err
			}
		}
	}

	if spec.Audit != nil && res.StatusCode/100 == 2 {
		request := res.Request
		reqBody := request.Context().Value("reqBody").(io.ReadCloser)
		bdl := request.Context().Value("bundle").(*bundle.Bundle)
		beginTime := request.Context().Value("beginTime").(time.Time)
		cache := request.Context().Value("cache").(*sync.Map)
		request.Body = reqBody
		resBody, err := ioutil.ReadAll(res.Body)
		if err != nil {
			return err
		}
		res.Body = ioutil.NopCloser(bytes.NewReader(resBody))
		orgIDStr := request.Header.Get("Org-ID")
		orgID, _ := strconv.ParseInt(orgIDStr, 10, 64)
		auditContext := &apispec.AuditContext{
			UrlParams: spec.Path.Vars(request.RequestURI),
			UserID:    request.Header.Get("User-ID"),
			OrgID:     orgID,
			Request:   request,
			Response:  res,
			Bundle:    bdl,
			BeginTime: beginTime,
			EndTime:   time.Now(),
			Result:    apistructs.SuccessfulResult,
			UserAgent: request.Header.Get("User-Agent"),
			ClientIP:  GetRealIP(request),
			Cache:     cache,
		}

		go func() {
			defer func() {
				if err := recover(); err != nil {
					logrus.Errorf("audit panic url:%s , err:%s", request.RequestURI, err)
				}
			}()
			auditContext.Response.Body = ioutil.NopCloser(bytes.NewReader(resBody))
			err := spec.Audit(auditContext)
			if err != nil {
				logrus.Errorf("audit failed url:%s , err:%s", request.RequestURI, err)
			}
		}()
	}

	res.Header.Set("Access-Control-Allow-Origin", "*")
	return err
}

// GetRealIP 获取真实ip
func GetRealIP(request *http.Request) string {
	ra := request.RemoteAddr
	if ip := request.Header.Get("X-Forwarded-For"); ip != "" {
		// filter space like '42.120.75.131,::ffff:10.112.1.1, 10.112.3.224'
		forwardedFilterSpace := strings.ReplaceAll(ip, " ", "")
		ra = strings.Split(forwardedFilterSpace, ",")[0]
	} else if ip := request.Header.Get("X-Real-IP"); ip != "" {
		ra = ip
	} else {
		ra, _, _ = net.SplitHostPort(ra)
	}
	return ra
}
