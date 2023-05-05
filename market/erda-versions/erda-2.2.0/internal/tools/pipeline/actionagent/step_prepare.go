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
	"bytes"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"syscall"

	"github.com/c2h5oh/datasize"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/filehelper"
)

const (
	EnvStdErrRegexpList = "ACTIONAGENT_STDERR_REGEXP_LIST"
	EnvMaxCacheFileMB   = "ACTIONAGENT_MAX_CACHE_FILE_MB"
)

// 对于 custom action，需要将 commands 转换为 script 来执行
func (agent *Agent) prepare() {
	// 1. create contextDir/workDir/dir(metaFile)/uploadDir/tempTarUploadDir
	if err := os.MkdirAll(agent.EasyUse.ContainerContext, 0755); err != nil {
		agent.AppendError(err)
	}
	if err := os.MkdirAll(agent.EasyUse.ContainerWd, 0755); err != nil {
		agent.AppendError(err)
	}
	if err := os.MkdirAll(filepath.Dir(agent.EasyUse.ContainerMetaFile), 0755); err != nil {
		agent.AppendError(err)
	}
	if agent.EasyUse.ContainerUploadDir != "" {
		if err := os.MkdirAll(agent.EasyUse.ContainerUploadDir, 0755); err != nil {
			agent.AppendError(err)
		}
	}
	if err := os.Mkdir(agent.EasyUse.ContainerTempTarUploadDir, 0755); err != nil {
		agent.AppendError(err)
	}
	// {
	//     "name":"a.cert",
	//     "value":"/.pipeline/container/context/.cms/dice_files/a.cert",
	//     "labels":{
	//         "diceFileUUID":"d31b0b31e85c467c8a54e4a9786363b7"
	//     }
	// }
	for _, f := range agent.Arg.Context.CmsDiceFiles {
		if err := os.MkdirAll(filepath.Dir(f.Value), 0755); err != nil {
			agent.AppendError(err)
		}
	}

	// 2. create custom script
	if len(agent.Arg.Commands) > 0 {
		if err := agent.setupScript(); err != nil {
			agent.AppendError(err)
			return
		}
	}

	// 3. compatible when_sigterm -> when_sig_15
	const oldSigTERMScript = "/opt/action/when_sigterm"
	if err := filehelper.CheckExist(oldSigTERMScript, false); err == nil {
		if err := filehelper.Copy(oldSigTERMScript, getSigScriptPath(syscall.SIGTERM)); err != nil {
			agent.AppendError(err)
			return
		}
	}

	// 4. multiWriter of stdout/stderr
	if f, err := filehelper.CreateFile3(agent.EasyUse.RunMultiStdoutFilePath, bytes.NewBufferString(""), 0644); err != nil {
		logrus.Printf("failed to create multi stdout, err: %v\n", err)
	} else {
		agent.EasyUse.RunMultiStdout = f
	}
	if f, err := filehelper.CreateFile3(agent.EasyUse.RunMultiStderrFilePath, bytes.NewBufferString(""), 0644); err != nil {
		logrus.Printf("failed to create multi stderr, err: %v\n", err)
	} else {
		agent.EasyUse.RunMultiStderr = f
	}
	agent.EasyUse.FlagEndLineForTail = "[Platform Log] [Action-Run End]"

	// 5. set stderr regexp list
	envStdErrRegexpStr := os.Getenv(EnvStdErrRegexpList)
	regexpStrList := []string{}
	if err := json.Unmarshal([]byte(envStdErrRegexpStr), &regexpStrList); err != nil {
		agent.AppendError(err)
		return
	}
	for i := range regexpStrList {
		reg, err := regexp.Compile(regexpStrList[i])
		if err != nil {
			agent.AppendError(err)
		} else {
			agent.StdErrRegexpList = append(agent.StdErrRegexpList, reg)
		}
	}
	var maxCacheFileSizeMB uint64 = 500
	var err error
	envMaxCacheMBStr := os.Getenv(EnvMaxCacheFileMB)
	if envMaxCacheMBStr != "" {
		maxCacheFileSizeMB, err = strconv.ParseUint(envMaxCacheMBStr, 10, 64)
		if err != nil {
			agent.AppendError(err)
		}
	}
	agent.MaxCacheFileSizeMB = datasize.ByteSize(maxCacheFileSizeMB) * datasize.MB

	// 6. watch files
	agent.watchFiles()

	// 7. listen signal
	go agent.ListenSignal()
}

func (agent *Agent) setupScript() error {
	var buf bytes.Buffer
	for _, command := range agent.Arg.Commands {
		escaped := fmt.Sprintf("%q", command)
		escaped = strings.Replace(escaped, `$`, `\$`, -1)
		buf.WriteString(fmt.Sprintf(
			traceScript,
			escaped,
			command,
		))
	}
	script := fmt.Sprintf(
		buildScript,
		buf.String(),
	)

	if err := filehelper.CreateFile(agent.EasyUse.RunScript, script, 0777); err != nil {
		return err
	}

	return nil
}

// buildScript is a helper script which add a shebang
// to the generated script.
const buildScript = `#!/bin/sh
set -e
%s
`

// traceScript is a helper script which is added to the
// generated script to trace each command.
const traceScript = `
echo + %s
%s || ((echo "- FAIL! exit code: $?") && false)
echo
`
