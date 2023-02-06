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

package entrance

import (
	"regexp"
	"runtime"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/strutil"
)

func FindMainEntranceFileName() (string, bool) {
	pcs := make([]uintptr, 100) // 100 is enough for invoke chain
	n := runtime.Callers(0, pcs)
	pcs = pcs[:n]

	frames := runtime.CallersFrames(pcs)
	for {
		frame, more := frames.Next()
		if !more {
			return "", false
		}
		if frame.Function == "main.main" {
			fileName := frame.File // such as: /go/src/github.com/erda-project/erda/cmd/monitor/monitor/main.go
			return fileName, true
		}
	}
}

func GetModulePath() string {
	mainFileName, found := FindMainEntranceFileName()
	if !found {
		logrus.Fatalf("failed to find main entrance")
	}
	modulePath := getModulePathFromMainEntranceFileName(mainFileName)
	if len(modulePath) == 0 {
		logrus.Fatalf("failed to find MODULE_PATH from main file name: %s", mainFileName)
	}
	return modulePath
}

var modulePathRegex = regexp.MustCompile(`.*/cmd/(.*)/main\.go`) // such as: /go/src/github.com/erda-project/erda/cmd/monitor/monitor/main.go
func getModulePathFromMainEntranceFileName(mainFileName string) string {
	ss := modulePathRegex.FindStringSubmatch(mainFileName)
	if len(ss) < 2 {
		return ""
	}
	modulePath := ss[1]
	return modulePath
}

func GetAppName() string {
	return getAppNameFromModulePath(GetModulePath())
}

func getAppNameFromModulePath(modulePath string) string {
	ss := strutil.Split(modulePath, "/")
	appName := ss[len(ss)-1]
	return appName
}
