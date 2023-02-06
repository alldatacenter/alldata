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
	"bytes"
	"context"
	"os"
	"time"

	"github.com/sirupsen/logrus"

	_ "github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda/internal/tools/pipeline/actionagent"
)

type PlatformLogFormatter struct {
	logrus.TextFormatter
}

func (f *PlatformLogFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	_bytes, err := f.TextFormatter.Format(entry)
	if err != nil {
		return nil, err
	}
	return append([]byte("[Platform Log] "), _bytes...), nil
}

func main() {
	args := os.Args[1]
	realMain(args)
}

func realMain(args string) {
	// set logrus
	logrus.SetFormatter(&PlatformLogFormatter{
		logrus.TextFormatter{
			ForceColors:            true,
			DisableTimestamp:       false,
			FullTimestamp:          true,
			TimestampFormat:        time.RFC3339Nano,
			DisableLevelTruncation: true,
			PadLevelText:           true,
		},
	})
	logrus.SetOutput(os.Stderr)

	if len(os.Args) == 1 {
		logrus.Fatal("failed to run action: no args passed in")
	}
	ctx, cancel := context.WithCancel(context.Background())
	agent := &actionagent.Agent{
		Errs:              make([]error, 0),
		PushedMetaFileMap: make(map[string]map[string]struct{}),
		// enciphered data will Replaced by '******' when log output
		TextBlackList: make([]string, 0),
		Ctx:           ctx,
		Cancel:        cancel,
	}
	agent.Execute(bytes.NewBufferString(args))
	agent.Teardown()
	agent.Exit()
}
