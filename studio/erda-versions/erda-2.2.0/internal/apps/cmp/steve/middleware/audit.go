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

package middleware

import (
	"bufio"
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/bugaolengdeyuxiaoer/go-ansiterm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/steve/websocket"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// audit template name
	AuditCordonNode      = "cordonNode"
	AuditUncordonNode    = "uncordonNode"
	AuditLabelNode       = "labelNode"
	AuditUnLabelNode     = "unLabelNode"
	AuditDrainNode       = "drainNode"
	AuditOfflineNode     = "offlineNode"
	AuditOnlineNode      = "onlineNode"
	AuditUpdateResource  = "updateK8SResource"
	AuditCreateResource  = "createK8SResource"
	AuditDeleteResource  = "deleteK8SResource"
	AuditKubectlShell    = "kubectlShell"
	AuditRestartWorkload = "restartWorkload"

	// audit template params
	AuditClusterName  = "clusterName"
	AuditNamespace    = "namespace"
	AuditResourceType = "resourceType"
	AuditResourceName = "name"
	AuditTargetLabel  = "targetLabel"
	AuditCommands     = "commands"

	maxAuditLength = 1 << 16
)

type Auditor struct {
	bdl *bundle.Bundle
}

// NewAuditor return a steve Auditor with bundle.
// bdl needs withCoreServices to create audit events.
func NewAuditor(bdl *bundle.Bundle) *Auditor {
	return &Auditor{bdl: bdl}
}

// AuditMiddleWare audit for steve server by bundle.
func (a *Auditor) AuditMiddleWare(next http.Handler) http.Handler {
	return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
		vars, _ := req.Context().Value(varsKey).(map[string]string)
		var (
			body []byte
			ctx  map[string]interface{}
		)
		if req.Body != nil {
			body, _ = ioutil.ReadAll(req.Body)
		}
		req.Body = ioutil.NopCloser(bytes.NewBuffer(body))

		clusterName := vars["clusterName"]
		typ := vars["type"]

		namespace := vars["namespace"]
		name := vars["name"]
		isInternal := req.Header.Get(httputil.InternalHeader) != ""
		userID := req.Header.Get(httputil.UserHeader)
		orgID := req.Header.Get(httputil.OrgHeader)
		scopeID, _ := strconv.ParseUint(orgID, 10, 64)
		now := strconv.FormatInt(time.Now().Unix(), 10)

		ctx = map[string]interface{}{
			AuditClusterName: clusterName,
		}
		auditReq := apistructs.AuditCreateRequest{
			Audit: apistructs.Audit{
				UserID:       userID,
				ScopeType:    apistructs.OrgScope,
				ScopeID:      scopeID,
				OrgID:        scopeID,
				Result:       "success",
				StartTime:    now,
				EndTime:      now,
				ClientIP:     getRealIP(req),
				UserAgent:    req.UserAgent(),
				Context:      ctx,
				TemplateName: AuditKubectlShell,
			},
		}

		writer := &wrapWriter{
			ResponseWriter: resp,
			statusCode:     http.StatusOK,
			auditReq:       auditReq,
			bdl:            a.bdl,
			ctx:            req.Context(),
		}

		next.ServeHTTP(writer, req)
		if writer.statusCode < 200 || writer.statusCode >= 300 {
			return
		}

		if body == nil {
			return
		}

		if vars["kubectl-shell"] != "true" {
			if typ == "" {
				return
			}
			ctx[AuditResourceName] = name
			switch req.Method {
			case http.MethodPatch:
				if isInternal && strutil.Equal(typ, "node", true) {
					var rb reqBody
					if err := json.Unmarshal(body, &rb); err != nil {
						logrus.Errorf("failed to unmarshal in steve audit")
						return
					}

					// audit for label/unlabel node
					if rb.Metadata != nil && rb.Metadata["labels"] != nil {
						labels, _ := rb.Metadata["labels"].(map[string]interface{})
						var (
							k string
							v interface{}
						)
						for k, v = range labels {
						} // there can only be one piece of k/v
						if v == nil {
							auditReq.Audit.TemplateName = AuditUnLabelNode
							ctx[AuditTargetLabel] = k
						} else {
							auditReq.Audit.TemplateName = AuditLabelNode
							ctx[AuditTargetLabel] = fmt.Sprintf("%s=%s", k, v.(string))
						}
						break
					}

					// audit for cordon/uncordon node
					if rb.Spec != nil && rb.Spec["unschedulable"] != nil {
						v, ok := rb.Spec["unschedulable"].(bool)
						if ok && v {
							auditReq.Audit.TemplateName = AuditCordonNode
						} else {
							auditReq.Audit.TemplateName = AuditUncordonNode
						}
					}
					break
				}
				fallthrough
			case http.MethodPut:
				ctx[AuditNamespace] = namespace
				ctx[AuditResourceType] = typ
				auditReq.Audit.TemplateName = AuditUpdateResource
			case http.MethodPost:
				var rb reqBody
				if err := json.Unmarshal(body, &rb); err != nil {
					logrus.Errorf("failed to unmarshal in steve audit")
					return
				}
				data := rb.Metadata["name"]
				if n, ok := data.(string); ok && n != "" {
					name = n
				}
				data = rb.Metadata["namespace"]
				if ns, ok := data.(string); ok && namespace != "" {
					namespace = ns
				}
				ctx[AuditNamespace] = namespace
				ctx[AuditResourceName] = name
				ctx[AuditResourceType] = typ
				auditReq.Audit.TemplateName = AuditCreateResource
			case http.MethodDelete:
				ctx[AuditNamespace] = namespace
				ctx[AuditResourceType] = typ
				auditReq.Audit.TemplateName = AuditDeleteResource
			default:
				return
			}
			if err := a.bdl.CreateAuditEvent(&auditReq); err != nil {
				logrus.Errorf("faild to audit in steve audit, %v", err)
			}
		}

	})
}

type reqBody struct {
	Metadata map[string]interface{} `json:"metadata,omitempty"`
	Spec     map[string]interface{} `json:"spec,omitempty"`
}

type wrapWriter struct {
	http.ResponseWriter
	ctx        context.Context
	statusCode int
	buf        bytes.Buffer
	auditReq   apistructs.AuditCreateRequest
	bdl        *bundle.Bundle
}

func (w *wrapWriter) WriteHeader(statusCode int) {
	w.ResponseWriter.WriteHeader(statusCode)
	w.statusCode = statusCode
}

func (w *wrapWriter) Write(body []byte) (int, error) {
	w.buf.Write(body)
	return w.ResponseWriter.Write(body)
}

func (w *wrapWriter) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	hijacker, ok := w.ResponseWriter.(http.Hijacker)
	if !ok {
		return nil, nil, fmt.Errorf("upstream ResponseWriter of type %v does not implement http.Hijacker", reflect.TypeOf(w.ResponseWriter))
	}
	conn, rw, err := hijacker.Hijack()
	if err != nil {
		return nil, nil, err
	}
	closeChan := make(chan struct{})
	auditReqChan := make(chan *cmdWithTimestamp)
	d := NewDispatcher(auditReqChan, closeChan)
	parser := ansiterm.CreateParser("Ground", d)
	wc := &wrapConn{
		Conn:       conn,
		parser:     parser,
		dispatcher: d,
		auditReq:   w.auditReq,
		ctx:        w.ctx,
	}
	go func(w *wrapWriter) {
		for {
			auditStr := ""
		LOOP:
			for {
				select {
				case cmd := <-d.auditReqChan:
					auditStr += fmt.Sprintf("\n%s: %s", cmd.start.Format(time.RFC3339), cmd.cmd)
					if len(auditStr) > maxAuditLength {
						break LOOP
					}
				case <-w.ctx.Done():
					w.sendWebsocketAudit(auditStr)
					return
				case <-closeChan:
					w.sendWebsocketAudit(auditStr)
					return
				}
			}
			w.sendWebsocketAudit(auditStr)
		}
	}(w)
	return wc, rw, nil
}

func (w *wrapWriter) sendWebsocketAudit(auditStr string) {
	if len(auditStr) > 0 {
		w.auditReq.Context[AuditCommands] = auditStr
		if err := w.bdl.CreateAuditEvent(&w.auditReq); err != nil {
			logrus.Errorf("faild to audit in steve audit, %v", err)
		} else {
			logrus.Infof("send audit message with websocket ,length : %d,", len(auditStr))
		}
	}
}

type wrapConn struct {
	net.Conn
	parser     *ansiterm.AnsiParser
	dispatcher *dispatcher
	auditReq   apistructs.AuditCreateRequest
	bdl        *bundle.Bundle
	ctx        context.Context
}

type cmdWithTimestamp struct {
	start time.Time
	cmd   string
}

func (w *wrapConn) Read(p []byte) (n int, err error) {
	n, err = w.Conn.Read(p)
	if n == 0 {
		return n, err
	}
	defer func() {
		if r := recover(); r != nil {
			logrus.Error(r)
		}
	}()
	data := websocket.DecodeFrames(p[:n])
	for _, datum := range data {
		if len(datum) == 0 {
			continue
		}
		if datum[0] != '0' {
			return
		}
		for _, d := range strings.Split(string(datum), "\n") {
			decoded, _ := base64.StdEncoding.DecodeString(d[1:])
			if len(decoded) == 0 {
				continue
			}
			_, err = w.parser.Parse(decoded)
			if err != nil {
				logrus.Errorf("audit message parse err :%v", err)
			}
		}
	}
	return
}

func getRealIP(request *http.Request) string {
	ra := request.RemoteAddr
	if ip := request.Header.Get("X-Forwarded-For"); ip != "" {
		ra = strings.Split(ip, ", ")[0]
	} else if ip := request.Header.Get("X-Real-IP"); ip != "" {
		ra = ip
	} else {
		ra, _, _ = net.SplitHostPort(ra)
	}
	return ra
}
