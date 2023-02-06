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

package monitor

import (
	"os"
	"testing"
	"time"

	"github.com/sirupsen/logrus"
)

var _ = func() int {
	os.Setenv("DINGDING", "https://oapi.dingtalk.com/robot/send?access_token=a")
	os.Setenv("DINGDING_pipeline", "https://oapi.dingtalk.com/robot/send?access_token=b")
	return 0
}()

func TestInitMonitor(t *testing.T) {
	logrus.Errorf("[alert] hello [alert]")
	logrus.Errorf("[pipeline] hello [pipeline]")
	time.Sleep(time.Second * 1)
}
