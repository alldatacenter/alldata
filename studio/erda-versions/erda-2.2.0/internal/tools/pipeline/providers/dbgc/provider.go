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

package dbgc

import (
	"context"
	"reflect"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/dbgc/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
)

type config struct {
	// default 2h
	PipelineDBGCDuration time.Duration `file:"pipeline_dbgc_duration" env:"PIPELINE_DBGC_DURATION" default:"2h"`
	// default 1 day
	AnalyzedPipelineArchiveDefaultRetainHour time.Duration `file:"analyzed_pipeline_archive_default_retain_hour" default:"24h"`
	// default 30 day
	FinishedPipelineArchiveDefaultRetainHour time.Duration `file:"finished_pipeline_archive_default_retain_hour" default:"720h"`
}

type provider struct {
	Cfg      *config
	Log      logs.Logger
	js       jsonstore.JsonStore
	etcd     *etcd.Store
	dbClient *db.Client

	MySQL mysqlxorm.Interface
	LW    leaderworker.Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	js, err := jsonstore.New()
	if err != nil {
		return err
	}
	etcdStore, err := etcd.New()
	if err != nil {
		return err
	}
	p.js = js
	p.etcd = etcdStore

	p.dbClient = &db.Client{Client: dbclient.Client{Engine: p.MySQL.DB()}}
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	p.LW.OnLeader(p.PipelineDatabaseGC)
	return nil
}

func init() {
	servicehub.Register("dbgc", &servicehub.Spec{
		Services:     []string{"dbgc"},
		Types:        []reflect.Type{reflect.TypeOf((*Interface)(nil)).Elem()},
		Dependencies: nil,
		Description:  "pipeline dbgc",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
