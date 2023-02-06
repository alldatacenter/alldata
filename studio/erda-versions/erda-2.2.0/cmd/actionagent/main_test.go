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

package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/pipeline/actionagent"
)

func TestGenerateArg(t *testing.T) {
	agentArg := actionagent.NewAgentArgForPull(10000098, 340)
	reqByte, err := json.Marshal(agentArg)
	assert.NoError(t, err)
	fmt.Println(base64.StdEncoding.EncodeToString(reqByte))
}

func TestLogger(t *testing.T) {
	// set logrus
	logrus.SetFormatter(&logrus.TextFormatter{
		ForceColors:            true,
		DisableTimestamp:       true,
		DisableLevelTruncation: true,
		PadLevelText:           true,
	})
	logger := logrus.WithField("type", "[Platform Log]")
	logger.Println("hello world")
}

//func Test_realMain(t *testing.T) {
//	os.Setenv("DICE_OPENAPI_PUBLIC_URL", "http://openapi.dev.terminus.io")
//	os.Setenv("DICE_OPENAPI_ADDR", "openapi.default.svc.cluster.local:9529")
//	os.Setenv("DICE_IS_EDGE", "true")
//	os.Setenv("DICE_OPENAPI_TOKEN_FOR_ACTION_BOOTSTRAP", "")
//	os.Setenv("UPLOADDIR", "/tmp/uploaddir")
//	os.Setenv("CONTEXTDIR", "/tmp/actionagent")
//	os.Setenv("WORKDIR", "/tmp/actionagent/wd")
//	os.Setenv("METAFILE", "/tmp/actionagent/metafile")
//	os.Setenv("DICE_OPENAPI_TOKEN", "1234")
//
//	agentArg := actionagent.NewAgentArgForPull(10006527, 55534)
//	reqByte, err := json.Marshal(agentArg)
//	assert.NoError(t, err)
//	args := base64.StdEncoding.EncodeToString(reqByte)
//
//	realMain(args)
//}
