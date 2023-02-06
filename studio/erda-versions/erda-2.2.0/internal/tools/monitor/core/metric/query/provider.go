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
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"reflect"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricmeta"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	indexloader "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"

	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"

	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats/chartv2"  //
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats/dict"     //
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats/influxdb" //
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/influxql"         //
)

type config struct {
	MetricMeta struct {
		Sources        []string `file:"sources"`
		GroupFiles     []string `file:"group_files"`
		MetricMetaPath string   `file:"metric_meta_path"`
	} `file:"metric_meta"`
}

// +provider
type provider struct {
	Cfg        *config
	Log        logs.Logger
	Register   transport.Register    `autowired:"service-register" optional:"true"`
	DB         *gorm.DB              `autowired:"mysql-client"`
	MetricTran i18n.I18n             `autowired:"i18n@metric"`
	Index      indexloader.Interface `autowired:"elasticsearch.index.loader@metric"`

	meta              *metricmeta.Manager
	metricService     *metricService
	metricMetaService *metricMetaService
}

func (p *provider) Init(ctx servicehub.Context) error {
	meta := metricmeta.NewManager(
		p.Cfg.MetricMeta.Sources,
		p.DB,
		p.Index,
		p.Cfg.MetricMeta.MetricMetaPath,
		p.Cfg.MetricMeta.GroupFiles,
		p.MetricTran,
		p.Log,
	)
	p.meta = meta
	err := meta.Init()
	if err != nil {
		return fmt.Errorf("init metricmeta manager: %w", err)
	}

	p.metricMetaService = &metricMetaService{
		p:    p,
		meta: meta,
	}
	p.metricService = &metricService{
		p:     p,
		query: query.New(&query.MetricIndexLoader{Interface: p.Index}),
	}
	if p.Register != nil {
		pb.RegisterMetricServiceImp(p.Register, p.metricService, apis.Options(),
			transport.WithHTTPOptions(
				transhttp.WithEncoder(func(rw http.ResponseWriter, r *http.Request, data interface{}) error {
					// compatibility for influxdb format
					if resp, ok := data.(*apis.Response); ok && resp != nil {
						if list, ok := data.([]*pb.Result); ok {
							data = convertInfluxDBResults(list)
						}
					}
					return encoding.EncodeResponse(rw, r, data)
				}),
				transhttp.WithDecoder(func(r *http.Request, data interface{}) error {
					var filters *[]*pb.Filter
					var options *map[string]string
					var statement *string
					if req, ok := data.(*pb.QueryWithInfluxFormatRequest); ok {
						filters, options, statement = &req.Filters, &req.Options, &req.Statement
					} else if req, ok := data.(*pb.QueryWithTableFormatRequest); ok {
						filters, options, statement = &req.Filters, &req.Options, &req.Statement
					} else if req, ok := data.(*pb.GeneralQueryRequest); ok {
						values := r.URL.Query()
						req.Format = values.Get("format")
						req.Ql = values.Get("ql")
						values.Del("format")
						values.Del("ql")
						req.Params, statement = parseValuesToParams(values), &req.Statement
						return nil
					}
					if filters != nil {
						_fs, _opts := query.ParseFilters(r.URL.Query())
						fs, err := parseFilters(_fs)
						if err != nil {
							return errors.NewInvalidParameterError("filters", err.Error())
						}
						if len(fs) > 0 {
							*filters = fs
						}
						opts := parseOptions(_opts)
						if len(opts) > 0 {
							*options = opts
						}
					}
					if statement != nil {
						body, err := ioutil.ReadAll(r.Body)
						if err != nil {
							return errors.NewInvalidParameterError("statement", err.Error())
						}
						r.Body = ioutil.NopCloser(bytes.NewBuffer(body))
						*statement = string(body)
						if len(*statement) > 0 {
							if _, ok := data.(*pb.QueryWithTableFormatRequest); ok {
								if r.URL.Query().Get("ql") == "influxql:ast" {
									*statement, err = query.ConvertAstToStatement(*statement)
									if err != nil {
										return errors.NewInvalidParameterError("statement", err.Error())
									}
								}
							}
						}
						return nil
					}
					return encoding.DecodeRequest(r, data)
				}),
			),
		)
		pb.RegisterMetricMetaServiceImp(p.Register, p.metricMetaService, apis.Options())
	}
	return nil
}

var metricmetaType = reflect.TypeOf((*metricmeta.Manager)(nil)).Elem()

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.monitor.metric.MetricService" || ctx.Type() == pb.MetricServiceServerType() || ctx.Type() == pb.MetricServiceHandlerType():
		return p.metricService
	case ctx.Service() == "erda.core.monitor.metric.MetricMetaService" || ctx.Type() == pb.MetricMetaServiceServerType() || ctx.Type() == pb.MetricMetaServiceHandlerType():
		return p.metricMetaService
	case ctx.Service() == "erda.core.monitor.metric.meta" || ctx.Type() == metricmetaType:
		return p.meta
	}
	return p
}

func init() {
	servicehub.Register("erda.core.monitor.metric", &servicehub.Spec{
		Services:             append(pb.ServiceNames(), "erda.core.monitor.metric.meta"),
		Types:                append(pb.Types(), metricmetaType),
		OptionalDependencies: []string{"service-register"},
		Description:          "metrics query api",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
