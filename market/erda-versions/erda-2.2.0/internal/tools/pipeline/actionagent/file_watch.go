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

package actionagent

import (
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"regexp"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/actionagent/filewatch"
	"github.com/erda-project/erda/pkg/filehelper"
)

func (agent *Agent) watchFiles() {
	if agent.Ctx == nil || agent.Cancel == nil {
		agent.AppendError(fmt.Errorf("init watchFiles need a cancelable context"))
		return
	}

	watcher, err := filewatch.New(agent.Ctx)
	if err != nil {
		agent.AppendError(err)
		return
	}
	agent.FileWatcher = watcher

	// ${METAFILE}
	watcher.RegisterFullHandler(agent.EasyUse.ContainerMetaFile, metaFileFullHandler(agent))
	// stdout && stderr
	watcher.RegisterTailHandler(agent.EasyUse.RunMultiStdoutFilePath, stdoutTailHandler(agent))
	watcher.RegisterTailHandler(agent.EasyUse.RunMultiStderrFilePath, stderrTailHandler(agent))

	if agent.EasyUse.EnablePushLog2Collector {
		go agent.asyncPushCollectorLog()
	}
}

func (agent *Agent) stopWatchFiles() {
	if agent.FileWatcher != nil {
		agent.FileWatcher.Close()
	}
}

// metaFileFullHandler 全量处理 metafile
func metaFileFullHandler(agent *Agent) filewatch.FullHandler {
	return func(r io.ReadCloser) error {
		// 一次性读取
		b, err := ioutil.ReadAll(r)
		if err != nil {
			return err
		}
		logrus.Debugf("监听到了 METAFILE 改动，时间: %v，全量内容: %s", time.Now().Format(time.RFC3339), string(b))
		cb := &Callback{}
		err = cb.HandleMetaFile(b)
		if err != nil {
			return err
		}
		err = agent.callbackToPipelinePlatform(cb)
		if err != nil {
			return err
		}
		return nil
	}
}

// stdoutTailHandler 以 tail 方式处理 stdout
func stdoutTailHandler(agent *Agent) filewatch.TailHandler {
	return func(line string, allLines []string) error {
		desensitizeLine(agent.TextBlackList, &line)
		fmt.Fprintf(os.Stdout, "%s\n", line)

		// meta
		tailHandlerForMeta(line, agent.EasyUse.ContainerMetaFile)

		// push to collector
		if agent.EasyUse.EnablePushLog2Collector {
			tailHandlerForPushCollectorLog(line, apistructs.CollectorLogPushStreamStdout, stdoutLogs, agent.EasyUse.TaskLogID, &stdoutLock)
		}

		return nil
	}
}

// stderrTailHandler 以 tail 方式处理 stderr
func stderrTailHandler(agent *Agent) filewatch.TailHandler {
	return func(line string, allLines []string) error {
		desensitizeLine(agent.TextBlackList, &line)
		fmt.Fprintf(os.Stderr, "%s\n", line)

		// meta
		tailHandlerForMeta(line, agent.EasyUse.ContainerMetaFile)

		// push to collector
		if agent.EasyUse.EnablePushLog2Collector {
			tailHandlerForPushCollectorLog(line, apistructs.CollectorLogPushStreamStderr, stderrLogs, agent.EasyUse.TaskLogID, &stderrLock)
		}

		if matchStdErrLine(agent.StdErrRegexpList, line) {
			agent.Errs = append(agent.Errs, errors.New(line))
		}

		return nil
	}
}

func desensitizeLine(txtBlackList []string, line *string) {
	values := make([]string, 0)
	for _, v := range txtBlackList {
		if strings.Contains(*line, v) {
			values = append(values, v)
		}
	}
	for _, v := range values {
		*line = strings.Replace(*line, v, "******", -1)
	}
}

const metaPrefix = "action meta:"

func tailHandlerForMeta(line, metafile string) {
	isMetaLog, k, v := getMetaFromLogLine(line)
	if isMetaLog {
		// write to metaFile, use universal callback logic
		if err := filehelper.Append(metafile, fmt.Sprintf("\n%s=%s\n", k, v)); err != nil {
			logrus.Warnf("failed to append meta to metafile, k: %s, v: %s, err: %v", k, v, err)
		}
	}
}

// getMetaFromLogLine 从日志行中分析 meta，格式如下：
// action meta: key=value
//考虑到 value 可能较复杂，每行只支持声明一个 meta
func getMetaFromLogLine(line string) (bool, string, string) {
	line = strings.TrimSpace(line)
	if !strings.HasPrefix(line, metaPrefix) {
		return false, "", ""
	}
	line = strings.TrimSpace(strings.TrimPrefix(line, metaPrefix))
	kv := strings.SplitN(line, "=", 2)
	var k string
	var v string
	if len(kv) > 0 {
		k = strings.TrimSpace(kv[0])
	}
	if len(kv) > 1 {
		v = strings.TrimSpace(kv[1])
	}
	return true, k, v
}

func matchStdErrLine(regexpList []*regexp.Regexp, line string) bool {
	var matched bool

	for _, reg := range regexpList {
		if reg.MatchString(line) {
			matched = true
			break
		}
	}
	return matched
}
