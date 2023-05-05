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
	"context"
	"os"
	"regexp"
	"sync"
	"time"

	"github.com/c2h5oh/datasize"

	"github.com/erda-project/erda/internal/tools/pipeline/actionagent/filewatch"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

const (
	CONTEXTDIR = "CONTEXTDIR"
	WORKDIR    = "WORKDIR"
	METAFILE   = "METAFILE"
	UPLOADDIR  = "UPLOADDIR"
)

type Agent struct {
	Arg *AgentArg

	EasyUse EasyUse

	// errs collect occurred errors
	Errs []error

	FileWatcher *filewatch.Watcher
	FWLock      sync.Mutex

	// PushedMetaFileMap store key value already pushed
	// first-level key: meta name; second-level key: meta value
	// for situation that one key has multiple values
	PushedMetaFileMap     map[string]map[string]struct{}
	LockPushedMetaFileMap sync.RWMutex

	Ctx      context.Context
	Cancel   context.CancelFunc // cancel when logic done
	ExitCode int

	StdErrRegexpList   []*regexp.Regexp
	MaxCacheFileSizeMB datasize.ByteSize

	CallbackReporter

	TextBlackList []string // enciphered data will Replaced by '******' when log output
}

type AgentArg struct {
	PullBootstrapInfo bool `json:"pullBootstrapInfo"`

	Commands []string                 `json:"commands,omitempty"` // custom action commands -> script -> run
	Context  spec.PipelineTaskContext `json:"context,omitempty"`  // 上下文

	PrivateEnvs map[string]string `json:"privateEnvs,omitempty"`

	PipelineID     uint64 `json:"pipelineID"`
	PipelineTaskID uint64 `json:"pipelineTaskID"`

	EncryptSecretKeys []string `json:"encryptSecretKeys"` // Encrypt envs, it will Replaced by '******' when log output
}

type EasyUse struct {
	ContainerContext  string // 容器内程序运行时上下文目录
	ContainerWd       string // 执行 run 程序时所在目录
	ContainerMetaFile string // metadata 文件

	ContainerUploadDir        string // uploadDir，该目录下的文件在执行结束后会自动上传，并提供下载
	ContainerTempTarUploadDir string // temp tar dir，需要 prepare 时预先创建，用于存放 upload 生成的 tar

	IsEdgeCluster  bool // is edge cluster
	IsEdgePipeline bool // is running on edge pipeline

	RunScript              string // run 文件
	RunProcess             *os.Process
	RunMultiStdoutFilePath string   // multiWriter(os.Stdout) 的文件路径
	RunMultiStdout         *os.File // multiWriter(os.Stdout) 的文件
	RunMultiStderrFilePath string   // multiWriter(os.Stderr) 的文件路径
	RunMultiStderr         *os.File // multiWriter(os.Stderr) 的文件
	FlagEndLineForTail     string   // flag end line for tail to stop

	OpenAPIAddr       string
	OpenAPIToken      string
	TokenForBootstrap string
	PipelineAddr      string

	EnablePushLog2Collector bool   // 是否推送日志到 collector
	CollectorAddr           string // collector 地址
	TaskLogID               string // 日志 ID，推送和查询时需要一致

	// Machine stat
	MachineStat taskinspect.PipelineTaskMachineStat

	FileStreamTimeoutSec time.Duration // download or upload file stream timeout
}

type RunningEnvironment struct {
	HostIP string
}

func NewAgentArgForPull(pipelineID, pipelineTaskID uint64) *AgentArg {
	var req AgentArg
	req.PullBootstrapInfo = true
	req.PipelineID = pipelineID
	req.PipelineTaskID = pipelineTaskID
	return &req
}

func (agent *Agent) AppendError(err error) {
	if err == nil {
		return
	}
	agent.ExitCode = 1
	agent.Errs = append(agent.Errs, err)
}

func (agent *Agent) MergeErrors() taskerror.OrderedErrors {
	if len(agent.Errs) == 0 {
		return nil
	}
	agent.ExitCode = 1
	var errs taskerror.OrderedErrors
	for _, err := range agent.Errs {
		errs = append(errs, &taskerror.Error{Msg: err.Error()})
	}
	return errs
}
