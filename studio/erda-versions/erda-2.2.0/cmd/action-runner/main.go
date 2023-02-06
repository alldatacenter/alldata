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
	"encoding/json"
	"flag"
	"io/ioutil"
	"os"
	"strconv"

	"github.com/sirupsen/logrus"

	_ "github.com/erda-project/erda-infra/base/version"
	actionrunner "github.com/erda-project/erda/internal/tools/pipeline/action-runner"
)

var config = flag.String("config", "./config.json", "file path")

func main() {
	flag.Parse()
	conf := readConfig(*config)
	runner := actionrunner.New(conf)
	err := runner.Run()
	if err != nil {
		logrus.Error(err)
		os.Exit(-1)
	}
}

func readConfig(path string) *actionrunner.Conf {
	var conf actionrunner.Conf
	if len(path) > 0 {
		byts, err := ioutil.ReadFile(path)
		if err != nil {
			logrus.Error(err)
			os.Exit(-1)
		}
		err = json.Unmarshal(byts, &conf)
		if err != nil {
			logrus.Error(err)
			os.Exit(-1)
		}
	}
	conf.BuildPath = getEnv("BUILD_ROOT_PATH", conf.BuildPath)
	if len(conf.BuildPath) <= 0 {
		conf.BuildPath = os.TempDir()
	}
	conf.OpenAPI = getEnv("OPENAPI_UEL", conf.OpenAPI)
	conf.Token = getEnv("TOKEN", conf.Token)
	if conf.MaxTask < 1 {
		conf.MaxTask = 1
	}
	if conf.FailedTaskKeepHours < 1 {
		conf.FailedTaskKeepHours = 3
	}
	conf.MaxTask = convInt(getEnv("MAX_TASK", strconv.Itoa(conf.MaxTask)))
	return &conf
}

func getEnv(key, def string) string {
	val := os.Getenv(key)
	if len(val) > 0 {
		return val
	}
	return def
}

func convInt(str string) int {
	val, err := strconv.Atoi(str)
	if err != nil {
		logrus.Error(err)
		os.Exit(-1)
	}
	return val
}
