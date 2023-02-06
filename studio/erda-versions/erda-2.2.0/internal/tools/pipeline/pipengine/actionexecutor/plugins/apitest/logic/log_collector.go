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

package logic

import (
	"bytes"
	"context"
	"fmt"
	"io/ioutil"
	"sync"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

func init() {
	go asyncPushCollectorLog()
}

const (
	CtxKeyCollectorLogID = "logID"
	CtxKeyLogger         = "logger"

	FieldOrgName   = "orgName"
	FieldOrgID     = "orgID"
	TagDiceOrgName = "dice_org_name"
	TagDiceOrgID   = "dice_org_id"
)

// clog means collector log.
func clog(ctx context.Context) *logrus.Entry {
	return ctx.Value(CtxKeyLogger).(*logrus.Entry)
}

type actionLogFormatter struct {
	logrus.TextFormatter
}

func (f *actionLogFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	_bytes, err := f.TextFormatter.Format(entry)
	if err != nil {
		return nil, err
	}
	return append([]byte("[Action Log] "), _bytes...), nil
}

// newLogger return logger.
func newLogger() *logrus.Logger {
	// set logrus
	l := logrus.New()
	l.SetFormatter(&actionLogFormatter{
		logrus.TextFormatter{
			ForceColors:            true,
			DisableTimestamp:       true,
			DisableLevelTruncation: true,
		},
	})
	l.SetOutput(ioutil.Discard)
	l.AddHook(&CollectorHook{})
	return l
}

type CollectorHook struct{}

func (c *CollectorHook) Levels() []logrus.Level { return logrus.AllLevels }
func (c *CollectorHook) Fire(entry *logrus.Entry) error {
	logLock.Lock()
	defer logLock.Unlock()
	*logLines = append(*logLines, apistructs.LogPushLine{
		ID:        entry.Context.Value(CtxKeyCollectorLogID).(string),
		Source:    string(apistructs.DashboardSpotLogSourceJob),
		Stream:    &apistructs.CollectorLogPushStreamStdout,
		Timestamp: time.Now().UnixNano(),
		Content:   entry.Message,
		Tags: map[string]interface{}{
			TagDiceOrgName: entry.Data[FieldOrgName],
			TagDiceOrgID:   entry.Data[FieldOrgID],
		},
	})
	return nil
}

const (
	asyncPushInterval = time.Second * 3
)

var logLines = &[]apistructs.LogPushLine{}
var logLock = &sync.Mutex{}

// asyncPushCollectorLog async push log to collector
func asyncPushCollectorLog() {
	handleFunc := func() {
		logLock.Lock()
		defer logLock.Unlock()

		if logLines == nil || len(*logLines) == 0 {
			return
		}
		err := pushCollectorLog(logLines)
		if err != nil {
			logrus.Error(err)
			// not refresh logs, try together at next time
			return
		}
		// refresh logs if success
		*logLines = nil
	}

	ticker := time.NewTicker(asyncPushInterval)
	for range ticker.C {
		handleFunc()
	}
}

func pushCollectorLog(logLines *[]apistructs.LogPushLine) error {
	var respBody bytes.Buffer
	resp, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Post(discover.Collector()).
		Path("/collect/logs/job").
		JSONBody(logLines).
		Header("Content-Type", "application/json").
		Do().
		Body(&respBody)
	if err != nil {
		return fmt.Errorf("failed to push log to collector, err: %v", err)
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to push log to collector, resp body: %s", respBody.String())
	}
	return nil
}
