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

package mysqlhelper

import (
	"net"
	"testing"
)

func TestSplitHost(t *testing.T) {
	host, port, err := net.SplitHostPort("mysql-slave.group-addon-mysql--df161e4cd23934dfeb75b96203a7970ea.svc.cluster.local:3306")
	if err != nil {
		t.Errorf("should not error")
	}
	if port != "3306" {
		t.Error("port error")
	}
	if host != "mysql-slave.group-addon-mysql--df161e4cd23934dfeb75b96203a7970ea.svc.cluster.local" {
		t.Errorf("host error")
	}
}
