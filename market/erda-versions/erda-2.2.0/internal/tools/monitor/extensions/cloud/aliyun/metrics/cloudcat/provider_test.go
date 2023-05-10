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

package cloudcat

import (
	"fmt"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/globals"
)

func TestMain(m *testing.M) {
	setup()
	code := m.Run()
	os.Exit(code)
}

func setup() {
	{
		os.Setenv("OPS_ADDR", "ops.default.svc.cluster.local:9027")
		os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
	}
}

func createManager() *manager {
	m := &manager{}
	m.writer = &fakeWriter{}
	m.Cfg = &globals.Config{GatherWindow: time.Minute * 1, Products: []string{"VPC", "WAF", "CMS"}}
	_ = m.initScheduler()
	return m
}

// func TestMetaChange(t *testing.T) {
// 	m := createManager()
// 	m.start()
// 	time.Sleep(time.Second * 2)
// 	m.schedulerChangedSig <- 0
// 	select {}
// }

type fakeWriter struct {
}

func (fw *fakeWriter) Write(data interface{}) error {
	item := data.(*kafka.Message)
	fmt.Printf("topic: %s data: %s\n", *item.Topic, string(item.Data))
	return nil
}

func (fw *fakeWriter) WriteN(data ...interface{}) (int, error) {
	return 0, nil
}

func (fw *fakeWriter) Close() error {
	return nil
}

func TestXXX(t *testing.T) {
	fmt.Println(strings.Split("", ","))
}
