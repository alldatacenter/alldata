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

package resource

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/cmp_interface"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
)

const Lang = "Lang"

type Resource struct {
	Bdl        *bundle.Bundle
	Ctx        context.Context
	Server     cmp_interface.Provider
	I18N       i18n.Translator
	DB         *dbclient.DBClient
	ClusterSvc clusterpb.ClusterServiceServer
}

func (r *Resource) I18n(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	if len(args) == 0 {
		try := r.I18N.Text(lang, key)
		if try != key {
			return try
		}
	}
	return r.I18N.Sprintf(lang, key, args...)
}

// GetHeader .
func GetHeader(ctx context.Context, key string) string {
	header := transport.ContextHeader(ctx)
	if header != nil {
		for _, v := range header.Get(key) {
			if len(v) > 0 {
				return v
			}
		}
	}
	return ""
}

func New(ctx context.Context, i18n i18n.Translator, mServer cmp_interface.Provider, clusterSvc clusterpb.ClusterServiceServer) *Resource {
	r := &Resource{}
	r.I18N = i18n
	r.Ctx = ctx
	r.Server = mServer
	r.ClusterSvc = clusterSvc
	return r
}

func (r *Resource) Init(ctx servicehub.Context) error {
	r.Ctx = ctx
	return nil
}

type XAxis struct {
	Type string   `json:"type"`
	Data []string `json:"data"`
}

type YAxis struct {
	Type string `json:"type"`
	Name string `json:"name"`
}

type Series struct {
	Name string    `json:"name"`
	Type string    `json:"type"`
	Data []float64 `json:"data"`
}

type Histogram struct {
	XAxis  XAxis            `json:"xAxis"`
	YAxis  YAxis            `json:"yAxis"`
	Series []HistogramSerie `json:"series"`
	Name   string           `json:"name"`
}
