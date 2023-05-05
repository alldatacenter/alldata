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

package endpoints

import (
	"context"
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	"github.com/sirupsen/logrus"
)

var (
	successCode         = 200
	limitCostTime int64 = 500 // ms
)

type benchmarkPipelineCreateRes struct {
	Name       string `json:"name"`
	CostTime   int64  `json:"costTime"` // ms
	StatusCode int    `json:"statusCode"`
	Body       string `json:"body"`
	err        error  `json:"err"`
}

func createV2(url string) (result benchmarkPipelineCreateRes) {
	if len(url) == 0 {
		return
	}
	method := "POST"

	payload := strings.NewReader(`{
    "appID": 10,
    "pipelineYml": "version: \"1.1\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          description: 代码仓库克隆\n          version: \"1.0\"\n          params:\n            branch: ((gittar.branch))\n            depth: 1\n            password: ((gittar.password))\n            uri: ((gittar.repo))\n            username: ((gittar.username))\n          timeout: 3600\n  - stage:\n      - golang:\n          alias: go-demo\n          description: golang action\n          params:\n            command: go build -o web-server main.go\n            context: ${git-checkout}\n            service: web-server\n            target: web-server\n  - stage:\n      - release:\n          alias: release\n          description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            image:\n              go-demo: ${go-demo:OUTPUT:image}\n  - stage:\n      - dice:\n          alias: dice\n          description: 用于 Erda 平台部署应用服务\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n",
    "pipelineSource": "dice",
    "pipelineYmlName": "pipeline.yml",
    "clusterName": "dev",
    "autoRunAtOnce": false,
    "autoStartCron": false,
    "labels": {
        "branch": "develop",
        "diceWorkspace": "TEST",
        "has-report-basic": "true"
    },
    "normalLabels": {
        "appName": "go-demo",
        "projectName": "go-demo",
        "orgName": "erda"
    },
    "configManageNamespaces": []
}`)

	client := &http.Client{Timeout: 5 * time.Second}
	req, err := http.NewRequest(method, url, payload)

	if err != nil {
		result.err = err
		return
	}
	req.Header.Add("Internal-Client", "bundle")
	req.Header.Add("Content-Type", "application/json")

	timeStart := time.Now()
	res, err := client.Do(req)
	if err != nil {
		result.err = err
		return
	}
	defer res.Body.Close()

	body, err := ioutil.ReadAll(res.Body)
	if err != nil {
		result.err = err
		return
	}
	timeEnd := time.Now()
	result.CostTime = timeEnd.Sub(timeStart).Milliseconds()
	result.StatusCode = res.StatusCode
	result.Body = string(body)
	result.Name = "createV2"
	return
}

func newCreateV2Work(workerID int) (count int) {
	resChan := make(chan benchmarkPipelineCreateRes, 0)
	execChan := make(chan struct{}, 0)
	defer close(resChan)
	defer close(execChan)
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	go func() {
		for {
			select {
			case <-execChan:
				data := createV2("")
				resChan <- data
			case <-ctx.Done():
				return
			}
		}
	}()
	execChan <- struct{}{}
	for {
		select {
		case data := <-resChan:
			count++
			logrus.Infof("worker: %d execute pipeline, statusCode: %d, costTime: %dms", workerID, data.StatusCode, data.CostTime)
			if data.err != nil {
				logrus.Errorf("%s: %s", data.Name, data.err.Error())
				return
			}
			if data.StatusCode != successCode {
				logrus.Errorf("status code is not %d, but %d", successCode, data.StatusCode)
				return
			}
			if count >= 1 {
				return
			}
			execChan <- struct{}{}
		}
	}
	return
}

//func Test_benchmarkCreateV2(t *testing.T) {
//	wait := &sync.WaitGroup{}
//	// concurrency of create pipeline
//	for i := 0; i < 1; i++ {
//		wait.Add(1)
//		go func(idx int) {
//			defer wait.Done()
//			count := newCreateV2Work(idx)
//			logrus.Infof("worker %d created %d pipelines", idx, count)
//		}(i)
//	}
//	wait.Wait()
//}
