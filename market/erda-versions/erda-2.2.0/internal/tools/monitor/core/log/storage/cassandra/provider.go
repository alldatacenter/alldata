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

package cassandra

import (
	"context"
	"fmt"

	"github.com/scylladb/gocqlx"
	"github.com/scylladb/gocqlx/qb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/cassandra"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

type (
	config struct {
		Cassandra    cassandra.SessionConfig `file:"cassandra"`
		ReadPageSize int                     `file:"read_page_size" default:"1024"`
	}
	provider struct {
		Cfg       *config
		Log       logs.Logger
		Cassandra cassandra.Interface `autowired:"cassandra"`

		queryFunc func(builder *qb.SelectBuilder, binding qb.M, dest interface{}) error
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	session, err := p.Cassandra.NewSession(&p.Cfg.Cassandra)
	if err != nil {
		return fmt.Errorf("fail to create cassandra session: %s", err)
	}
	p.queryFunc = func(builder *qb.SelectBuilder, binding qb.M, dest interface{}) error {
		stmt, names := builder.ToCql()
		cql := gocqlx.Query(session.Session().Query(stmt), names).BindMap(binding)
		return cql.SelectRelease(dest)
	}
	return nil
}

var _ storage.Storage = (*provider)(nil)

func (p *provider) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	return nil, storekit.ErrOpNotSupported
}

func init() {
	servicehub.Register("log-storage-cassandra", &servicehub.Spec{
		Services:   []string{"log-storage-cassandra-reader"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
