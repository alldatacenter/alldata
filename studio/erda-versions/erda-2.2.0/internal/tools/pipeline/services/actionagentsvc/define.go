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

package actionagentsvc

import (
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
)

type ActionAgentSvc struct {
	dbClient        *dbclient.Client
	bdl             *bundle.Bundle
	accessibleCache jsonstore.JsonStore
	etcdctl         *etcd.Store
}

func New(dbClient *dbclient.Client, bdl *bundle.Bundle, js jsonstore.JsonStore, etcdctl *etcd.Store) *ActionAgentSvc {
	s := ActionAgentSvc{}
	s.dbClient = dbClient
	s.bdl = bdl
	s.accessibleCache = js
	s.etcdctl = etcdctl
	return &s
}
