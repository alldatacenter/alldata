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

package httpserver

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"io"
	"io/ioutil"
	"net/http"
	"net/http/httputil"
	"net/http/pprof"
	"net/url"
	"strings"
	"time"

	"github.com/gorilla/mux"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/goroutine_context"
	"github.com/erda-project/erda/pkg/http/httpserver/ierror"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	readTimeout       = 60 * time.Second
	writeTimeout      = 60 * time.Second
	readHeaderTimeout = 60 * time.Second

	ContentTypeJSON          = "application/json"
	ResponseWriter           = "responseWriter"
	Base64EncodedRequestBody = "base64-encoded-request-body"
	TraceID                  = "dice-trace-id"
)

// Server provides ability to run a http server and handler registered URL paths.
type Server struct {
	router       *mux.Router
	listenAddr   string
	localeLoader *i18n.LocaleResourceLoader
}

// Endpoint contains URL path and corresponding handler
type Endpoint struct {
	Path           string
	Method         string
	Handler        Handler
	WriterHandler  WriterHandler
	ReverseHandler ReverseHandler
}

type Handler func(context.Context, *http.Request, map[string]string) (Responser, error)
type WriterHandler func(context.Context, http.ResponseWriter, *http.Request, map[string]string) error
type ReverseHandler func(context.Context, *http.Request, map[string]string) error

// New create an http server.
func New(addr string) *Server {
	return &Server{
		router:       mux.NewRouter(),
		listenAddr:   addr,
		localeLoader: i18n.NewLoader(),
	}
}

// Router 返回 server's router.
func (s *Server) Router() *mux.Router {
	return s.router
}

// ListenAndServe boot the server to lisen and accept requests.
func (s *Server) ListenAndServe() error {
	srv := &http.Server{
		Addr:              s.listenAddr,
		Handler:           s.router,
		ReadTimeout:       readTimeout,
		WriteTimeout:      writeTimeout,
		ReadHeaderTimeout: readHeaderTimeout,
	}
	err := srv.ListenAndServe()
	if err != nil {
		logrus.Errorf("Failed to listen and serve: %s", err)
	}
	return err
}

// WithLocaleLoader
func (s *Server) WithLocaleLoader(loader *i18n.LocaleResourceLoader) {
	s.localeLoader = loader
}

// RegisterEndpoint match URL path to corresponding handler
func (s *Server) RegisterEndpoint(endpoints []Endpoint) {
	for _, ep := range endpoints {
		if ep.WriterHandler != nil {
			s.router.Path(ep.Path).Methods(ep.Method).HandlerFunc(s.internalWriterHandler(ep.WriterHandler))
		} else if ep.ReverseHandler != nil {
			s.router.Path(ep.Path).Methods(ep.Method).Handler(s.internalReverseHandler(ep.ReverseHandler))
		} else {
			s.router.Path(ep.Path).Methods(ep.Method).HandlerFunc(s.internal(ep.Handler))
		}
		logrus.Infof("Added endpoint: %s %s", ep.Method, ep.Path)
	}

	// add pprof
	subroute := s.router.PathPrefix("/debug/").Subrouter()
	subroute.HandleFunc("/pprof/profile", pprof.Profile)
	subroute.HandleFunc("/pprof/trace", pprof.Trace)
	subroute.HandleFunc("/pprof/block", pprof.Handler("block").ServeHTTP)
	subroute.HandleFunc("/pprof/heap", pprof.Handler("heap").ServeHTTP)
	subroute.HandleFunc("/pprof/goroutine", pprof.Handler("goroutine").ServeHTTP)
	subroute.HandleFunc("/pprof/threadcreate", pprof.Handler("threadcreate").ServeHTTP)
}

func (s *Server) internal(handler func(context.Context, *http.Request, map[string]string) (Responser, error)) http.HandlerFunc {
	pctx := context.Background()
	pctx = injectTraceID(pctx)

	return func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		logrus.Debugf("start %s %s", r.Method, r.URL.String())

		ctx, cancel := context.WithCancel(pctx)
		defer func() {
			cancel()
			logrus.Debugf("finished handle request %s %s (took %v)", r.Method, r.URL.String(), time.Since(start))
		}()
		ctx = context.WithValue(ctx, ResponseWriter, w)

		handleRequest(r)
		localeName := i18n.GetLocaleNameByRequest(r)
		locale := &i18n.LocaleResource{}
		if s.localeLoader != nil {
			locale = s.localeLoader.Locale(localeName)
		}

		// set global context bind goroutine id
		i18n.SetGoroutineBindLang(localeName)
		// clear all global context
		defer goroutine_context.ClearContext()

		// Manual decoding url var
		muxVars := mux.Vars(r)
		for k, v := range muxVars {
			decodedVar, err := url.QueryUnescape(v)
			if err != nil {
				continue
			}
			muxVars[k] = decodedVar
		}
		response, err := handler(ctx, r, muxVars)
		if err == nil && s.localeLoader != nil {
			response = response.GetLocaledResp(locale)
		}
		if err != nil {
			apiError, isApiError := err.(ierror.IAPIError)
			if isApiError {
				response = HTTPResponse{
					Status: apiError.HttpCode(),
					Content: Resp{
						Success: false,
						Err: apistructs.ErrorResponse{
							Code: apiError.Code(),
							Msg:  apiError.Render(locale),
						},
					},
				}
			} else {
				logrus.Errorf("failed to handle request: %s (%v)", r.URL.String(), err)

				statusCode := http.StatusInternalServerError
				if response != nil {
					statusCode = response.GetStatus()
				}
				w.WriteHeader(statusCode)
				io.WriteString(w, err.Error())
				return
			}
		}

		w.Header().Set("Content-Type", ContentTypeJSON)
		w.WriteHeader(response.GetStatus())

		encoder := json.NewEncoder(w)
		vals := r.URL.Query()
		pretty, ok := vals["pretty"]
		if ok && strings.Compare(pretty[0], "true") == 0 {
			encoder.SetIndent("", "    ")
		}

		if err := encoder.Encode(response.GetContent()); err != nil {
			logrus.Errorf("failed to send response: %s (%v)", r.URL.String(), err)
			return
		}
	}
}

func (s *Server) internalWriterHandler(handler func(context.Context, http.ResponseWriter, *http.Request, map[string]string) error) http.HandlerFunc {
	pctx := context.Background()
	pctx = injectTraceID(pctx)

	return func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		logrus.Debugf("start %s %s", r.Method, r.URL.String())

		ctx, cancel := context.WithCancel(pctx)
		defer func() {
			cancel()
			logrus.Debugf("finished handle request %s %s (took %v)", r.Method, r.URL.String(), time.Since(start))
		}()

		handleRequest(r)

		err := handler(ctx, w, r, mux.Vars(r))
		if err != nil {
			logrus.Errorf("failed to handle request: %s (%v)", r.URL.String(), err)

			statusCode := http.StatusInternalServerError
			w.WriteHeader(statusCode)
			io.WriteString(w, err.Error())
		}
	}
}

// internalReverseHandler handler 处理 r.URL 用于生成 ReverseProxy.Director
func (s *Server) internalReverseHandler(handler func(context.Context, *http.Request, map[string]string) error) http.Handler {
	pctx := context.Background()
	pctx = injectTraceID(pctx)

	return &httputil.ReverseProxy{
		Director: func(r *http.Request) {
			start := time.Now()
			logrus.Debugf("start %s %s", r.Method, r.URL.String())

			ctx, cancel := context.WithCancel(pctx)
			defer func() {
				cancel()
				logrus.Debugf("finished handle request %s %s (took %v)", r.Method, r.URL.String(), time.Since(start))
			}()

			handleRequest(r)

			err := handler(ctx, r, mux.Vars(r))
			if err != nil {
				logrus.Errorf("failed to handle request: %s (%v)", r.URL.String(), err)
				return
			}
		},
		FlushInterval: -1,
	}
}

func handleRequest(r *http.Request) {
	// base64 decode request body if declared in header
	if strutil.Equal(r.Header.Get(Base64EncodedRequestBody), "true", true) {
		r.Body = ioutil.NopCloser(base64.NewDecoder(base64.StdEncoding, r.Body))
	}
}

func injectTraceID(ctx context.Context) context.Context {
	return context.WithValue(ctx, TraceID, uuid.UUID())
}
