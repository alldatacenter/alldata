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

// Package kms provides unified Key Management Service.
package kms

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/kms/conf"
	"github.com/erda-project/erda/internal/tools/kms/endpoints"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/kms"
	"github.com/erda-project/erda/pkg/kms/stores/etcd"
)

// Initialize initialize and bootstrap the module.
func Initialize() error {
	conf.Load()

	// config logrus
	if conf.Debug() {
		logrus.SetLevel(logrus.DebugLevel)
	}

	kmsMgr, err := kms.GetManager(kms.WithStoreConfigs(map[string]string{
		etcd.EnvKeyEtcdEndpoints: conf.EtcdEndpoints(),
	}))
	if err != nil {
		return err
	}

	ep := endpoints.New(endpoints.WithKmsManager(kmsMgr))

	server := httpserver.New(conf.ListenAddr())
	server.RegisterEndpoint(ep.Routes())

	if err := do(); err != nil {
		return err
	}

	return server.ListenAndServe()
}

func do() error {
	return nil
}
