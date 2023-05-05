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

package webhook

import (
	"encoding/json"
	"regexp"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/strutil"
)

// EXAMPLE pipeline event json
// {
//     "action": "B_CREATE",
//     "applicationID": "178",
//     "content": {
//         "kind": "B_CREATE",
//         "operator": "10672",
//         "pipeline": {
//             "applicationID": 178,
//             "applicationName": "dice",
//             "basePipelineID": 4847,
//             "branch": "develop",
//             "clusterName": "terminus-y",
//             "commit": "bc6410f8e3282e6e5d667551013efac0ded79491",
//             "commitDetail": {
//                 "author": "郑嘉涛",
//                 "comment": "Merge branch 'feature/org-switch' into 'develop'\n\nfeature: org switch\n\nSee merge request !1645",
//                 "email": "ef@terminus.io",
//                 "repo": "http://gittar.app.terminus.io/dcos-terminus/dice",
//                 "repoAbbr": "dcos-terminus/dice",
//                 "time": "2019-03-13T10:41:01+08:00"
//             },
//             "costTimeSec": -1,
//             "cronID": null,
//             "extra": {
//                 "callbackURLs": null,
//                 "configManageNamespaceOfSecrets": "pipeline-secrets-app-178-develop",
//                 "configManageNamespaceOfSecretsDefault": "pipeline-secrets-app-178-default",
//                 "cronTriggerTime": "0001-01-01T00:00:00Z",
//                 "diceWorkspace": "TEST",
//                 "isAutoRun": true,
//                 "submitUser": {
//                     "avatar": "",
//                     "id": 10672,
//                     "name": "姜政冬"
//                 }
//             },
//             "id": 6461,
//             "orgID": 2,
//             "orgName": "terminus",
//             "pipelineYml": "...",
//             "pipelineYmlName": "pipeline.yml",
//             "pipelineYmlSource": "content",
//             "projectID": 70,
//             "projectName": "dice",
//             "snapshot": {},
//             "source": "qa",
//             "status": "Analyzed",
//             "timeBegin": "0001-01-01T00:00:00Z",
//             "timeCreated": "2019-03-13T10:43:47.945103124+08:00",
//             "timeEnd": "0001-01-01T00:00:00Z",
//             "timeUpdated": "2019-03-13T10:43:48.035938235+08:00",
//             "triggerMode": "manual",
//             "type": "normal"
//         }
//     },
//     "env": "",
//     "event": "pipeline",
//     "orgID": "2",
//     "projectID": "70",
//     "timestamp": "2019-03-13 10:43:49"
// }
var (
	//  所有具体 event 相关钉钉显示格式定义在 fmtMap
	fmtMap = map[string]string{
		"pipeline": strutil.Trim(`
event: <event>
action: <action>
org: <orgID>
project: <projectID>
application: <applicationID>
user: <content.pipeline.extra.submitUser.name>
branch: <content.pipeline.branch>
time: <timestamp>
`),
		"runtime": strutil.Trim(`
event: <event>
action: <action>
org: <orgID>
project: <projectID>
application: <applicationID>
time: <timestamp>
`),
	}
	regex = regexp.MustCompile("<.+>")

	// ErrNoEventField event 消息中没有 'event' 字段
	ErrNoEventField = errors.New("not found 'event' field")
	// ErrInvalidEvent 'event' 字段内容不是 string 类型
	ErrInvalidEvent = errors.New("invalid 'event' type")
	// ErrNoFormat 在 fmtMap 没有对应的格式
	ErrNoFormat = errors.New("not found related format")
)

// Format 格式化 'eventContent'
func Format(eventContent interface{}) (string, error) {
	event, ok := eventContent.(map[string]interface{})["event"]
	if !ok {
		return "", ErrNoEventField
	}
	eventstr, ok := event.(string)
	if !ok {
		return "", ErrInvalidEvent
	}
	format, ok := fmtMap[eventstr]
	if !ok {
		return "", ErrNoFormat
	}
	return render(format, eventContent), nil
}

func render(format string, eventcontent interface{}) string {
	return regex.ReplaceAllStringFunc(format, func(src string) string {
		locs := strutil.Split(src[1:len(src)-1], ".", true)
		extracted := extractjson(eventcontent, locs)
		switch v := extracted.(type) {
		case string:
			// 对 string 类型特殊处理，去掉两边的引号
			return v
		default:
			marshalled, err := json.Marshal(extracted)
			if err != nil {
				return ""
			}
			return string(marshalled)
		}
	})
}

func extractjson(js interface{}, locs []string) interface{} {
	for _, loc := range locs {
		nextjs, ok := js.(map[string]interface{})
		if !ok {
			return nil
		}
		if js, ok = nextjs[loc]; !ok {
			return nil
		}
	}
	return js
}
