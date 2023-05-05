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

package query

import (
	"fmt"
	"net/http"
	"regexp"
	"time"

	"github.com/recallsong/go-utils/reflectx"

	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/common"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/pkg/common/errors"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) initRoutes(routes httpserver.Router) {
	routes.GET("/api/logs/actions/download", p.downloadLog)

	// runtime
	routes.GET("/api/runtime/logs/actions/download", p.downloadRuntimeLog, permission.Intercepter(
		permission.ScopeApp, permission.QueryValue("applicationId"),
		common.ResourceRuntime, permission.ActionGet,
	))

	// org
	routes.GET("/api/orgCenter/logs/actions/download", p.downloadOrgLog, permission.Intercepter(
		permission.ScopeOrg, permission.OrgIDByCluster("clusterName"),
		common.ResourceOrgCenter, permission.ActionGet,
	))
}

// LogRequest .
type LogRequest struct {
	RequestID     string `form:"requestId"`
	LogID         string `form:"requestId"`
	Source        string `form:"source"`
	ID            string `form:"id"`
	Stream        string `form:"stream"`
	Start         int64  `form:"start"`
	End           int64  `form:"end"`
	Count         int64  `form:"count"`
	ApplicationID string `from:"applicationId"`
	ClusterName   string `from:"clusterName"`
	Pattern       string `from:"pattern"`
	Offset        int64  `from:"offset"`
	Live          bool   `from:"live"`
	Debug         bool   `from:"debug"`
	PodName       string `form:"podName"`
	PodNamespace  string `form:"podNamespace"`
	ContainerName string `form:"containerName"`
	IsFallBack    bool   `form:"isFallBack"`
	regexp        *regexp.Regexp
}

func (r *LogRequest) GetStart() int64      { return r.Start }
func (r *LogRequest) GetEnd() int64        { return r.End }
func (r *LogRequest) GetCount() int64      { return r.Count }
func (r *LogRequest) GetPattern() string   { return r.Pattern }
func (r *LogRequest) GetRequestId() string { return r.RequestID }
func (r *LogRequest) GetId() string        { return r.ID }
func (r *LogRequest) GetSource() string    { return r.Source }
func (r *LogRequest) GetStream() string    { return r.Stream }
func (r *LogRequest) GetOffset() int64     { return r.Offset }
func (r *LogRequest) GetLive() bool        { return r.Live }
func (r *LogRequest) GetDebug() bool       { return r.Debug }

var lineBreak = []byte("\n")

const maxDownloadTimeRange = 1 * int64(time.Hour)

func (p *provider) downloadLog(w http.ResponseWriter, r *http.Request, req *LogRequest) interface{} {
	filename := getFilename(req)
	w.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Expires", "0")
	w.Header().Set("charset", "utf-8")
	w.Header().Set("Content-Disposition", "attachment;filename="+filename)
	w.Header().Set("Content-Type", "application/octet-stream")
	flusher := w.(http.Flusher)

	if req.Count == 0 {
		req.Count = 1000000
	}

	var count int
	err := p.logQueryService.walkLogItems(r.Context(), req,
		func(sel *storage.Selector) (*storage.Selector, error) {
			if sel.End-sel.Start > maxDownloadTimeRange {
				return sel, errors.NewInvalidParameterError("(start,end]", "time range is too large for download")
			}
			if len(req.ClusterName) > 0 && len(req.ID) == 0 {
				sel.Filters = append(sel.Filters, &storage.Filter{
					Key:   "tags.dice_cluster_name",
					Op:    storage.EQ,
					Value: req.ClusterName,
				})
			}
			if len(req.ApplicationID) > 0 {
				sel.Filters = append(sel.Filters, &storage.Filter{
					Key:   "tags.dice_application_id",
					Op:    storage.EQ,
					Value: req.ApplicationID,
				})
			}
			if len(req.ClusterName) > 0 {
				sel.Options[storage.ClusterName] = req.ClusterName
			}
			if len(req.PodName) > 0 {
				sel.Options[storage.PodName] = req.PodName
			}
			if len(req.PodNamespace) > 0 {
				sel.Options[storage.PodNamespace] = req.PodNamespace
			}
			if len(req.ContainerName) > 0 {
				sel.Options[storage.ContainerName] = req.ContainerName
			}
			if req.IsFallBack {
				sel.Options[storage.IsFallBack] = true
			}
			p.logQueryService.tryFillQueryMeta(r.Context(), sel, r.Header.Get("org"))
			return sel, nil
		},
		func(item *pb.LogItem) error {
			w.Write(reflectx.StringToBytes(item.Content))
			w.Write(lineBreak)
			if count >= 100 {
				flusher.Flush()
				count = 0
			} else {
				count++
			}
			return nil
		},
	)
	if err != nil {
		if herr, ok := err.(transhttp.Error); ok && herr.HTTPStatus() < 500 {
			return api.Errors.InvalidParameter(err)
		}
		return api.Errors.Internal(err)
	}
	if count > 0 {
		flusher.Flush()
	}
	return nil
}

func (p *provider) downloadRuntimeLog(w http.ResponseWriter, r *http.Request, req *LogRequest) interface{} {
	if len(req.ApplicationID) <= 0 {
		return api.Errors.MissingParameter("applicationId")
	}
	return p.downloadLog(w, r, req)
}

func (p *provider) downloadOrgLog(w http.ResponseWriter, r *http.Request, req *LogRequest) interface{} {
	if len(req.ClusterName) <= 0 {
		return api.Errors.MissingParameter("clusterName")
	}
	return p.downloadLog(w, r, req)
}

func getFilename(req ByContainerIdRequest) string {
	if len(req.GetRequestId()) > 0 {
		return fmt.Sprintf("%s-%d-%d.log", req.GetRequestId(), req.GetStart(), req.GetEnd())
	}
	return fmt.Sprintf("%s-%d-%d.log", req.GetId(), req.GetStart(), req.GetEnd())
}
