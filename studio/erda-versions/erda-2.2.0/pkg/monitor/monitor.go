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

// 使用方法：
//     从环境变量中根据规则解析出 logrus 日志前缀 与 eventbox dest 的映射关系。
//
//     目前支持 DINGDING Webhook，通过 logrus 日志前缀 查找对应的钉钉告警地址，并发送告警。
//     通过环境变量解析日志前缀的规则如下：
//     1. DINGDING                   默认规则，前缀为 [alert]
//     2. DINGDING_[-._a-zA-Z0-9]*   DINGDING_ 后的字符串左右加上 [] 即为前缀。假设 key 为 DINGDING_error，则前缀为 [error]
package monitor

// import (
// 	"fmt"
// 	"os"
// 	"strings"

// 	"github.com/sirupsen/logrus"

// 	"github.com/erda-project/erda/apistructs"
// 	"github.com/erda-project/erda/internal/eventbox/api"
// 	"github.com/erda-project/erda/pkg/strutil"
// )

// type LogPrefix string

// const (
// 	LogPrefixAlert = "[alert]"
// )

// var (
// 	notifiers map[LogPrefix]api.Notifier
// )

// func init() {
// 	dest := getDestFromEnv()
// 	if len(dest) == 0 {
// 		return
// 	}

// 	sender := os.Getenv("MONITOR_FROM")
// 	if sender == "" {
// 		sender = "platform-monitor"
// 	}
// 	clusterName := os.Getenv("DICE_CLUSTER_NAME")
// 	if clusterName == "" {
// 		clusterName = os.Getenv("DICE_CLUSTER")
// 		if clusterName == "" {
// 			clusterName = "unknown-cluster"
// 		}
// 	}

// 	hook := &MonitorHook{
// 		name:        sender,
// 		levels:      []logrus.Level{logrus.FatalLevel, logrus.ErrorLevel},
// 		dest:        dest,
// 		clusterName: clusterName,
// 	}

// 	// register eventbox notifiers
// 	notifiers = make(map[LogPrefix]api.Notifier, len(dest))
// 	for prefix, eventboxDest := range dest {
// 		notifier, err := api.New(hook.name, eventboxDest)
// 		if err != nil {
// 			fmt.Printf("failed to init the eventbox notifier (%v)\n", err)
// 			continue
// 		}
// 		notifiers[prefix] = notifier
// 	}

// 	logrus.AddHook(hook)
// }

// // MonitorHook 是 logrus 日志库的一个 Hook 实现，将 Fatal 级别的日志消息发送到 eventbox 进行报警处理
// type MonitorHook struct {
// 	name        string
// 	levels      []logrus.Level
// 	dest        map[LogPrefix]map[string]interface{}
// 	clusterName string
// }

// func (h *MonitorHook) Levels() []logrus.Level {
// 	return h.levels
// }

// func (h *MonitorHook) Fire(entry *logrus.Entry) error {
// 	for prefix, notifier := range notifiers {
// 		if !strings.HasPrefix(entry.Message, string(prefix)) {
// 			continue
// 		}
// 		entry.Message = strutil.Concat(h.clusterName, " | ", entry.Message)
// 		go func() {
// 			if err := notifier.Send(fmt.Sprintf("%s %s", entry.Time.Format("2006-01-02 15:04:05"), entry.Message)); err != nil {
// 				fmt.Printf("failed to send message to eventbox (%v)\n", err)
// 			}
// 		}()
// 		break
// 	}
// 	return nil
// }

// // getDestFromEnv 从环境变量中根据规则解析出 [logPrefix] 与 eventbox dest(DINGDING 地址) 的映射关系
// // prefix 需要满足环境变量要求，即 DINGDING_[-._a-zA-Z0-9]*
// // 例如：DINGDING_abc 的 prefix 为 [abc]
// func getDestFromEnv() map[LogPrefix]map[string]interface{} {
// 	dest := make(map[LogPrefix]map[string]interface{})

// 	for _, kv := range os.Environ() {
// 		e := strings.SplitN(kv, "=", 2)
// 		if len(e) != 2 {
// 			continue
// 		}
// 		k := e[0]
// 		v := e[1]

// 		// 默认规则
// 		if k == "DINGDING" {
// 			if dest[LogPrefixAlert] == nil {
// 				dest[LogPrefixAlert] = make(map[string]interface{})
// 			}
// 			dest[LogPrefixAlert]["DINGDING"] = []apistructs.Target{apistructs.Target{Receiver: v, Secret: ""}}
// 		}

// 		// 规则：DINGDING_${logPrefix}
// 		if strings.HasPrefix(k, "DINGDING_") {
// 			prefix := strings.TrimPrefix(k, "DINGDING_")
// 			if prefix == "" {
// 				continue
// 			}
// 			prefix = fmt.Sprintf("[%s]", prefix)
// 			if dest[LogPrefix(prefix)] == nil {
// 				dest[LogPrefix(prefix)] = make(map[string]interface{})
// 			}
// 			dest[LogPrefix(prefix)]["DINGDING"] = []apistructs.Target{apistructs.Target{Receiver: v, Secret: ""}}
// 		}
// 	}

// 	return dest
// }
