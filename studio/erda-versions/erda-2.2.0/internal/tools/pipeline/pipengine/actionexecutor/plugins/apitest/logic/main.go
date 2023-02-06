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
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"golang.org/x/net/publicsuffix"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/apitestsv2"
	"github.com/erda-project/erda/pkg/apitestsv2/cookiejar"
	"github.com/erda-project/erda/pkg/envconf"
	"github.com/erda-project/erda/pkg/http/customhttp"
)

const CookieJar = "cookieJar"

func mergeEnvs(task *spec.PipelineTask) map[string]string {
	envs := make(map[string]string)
	for k, v := range task.Extra.PublicEnvs {
		envs[k] = v
	}
	for k, v := range task.Extra.PrivateEnvs {
		envs[k] = v
	}
	return envs
}

func parseConfFromTask(task *spec.PipelineTask) (EnvConfig, error) {
	envs := mergeEnvs(task)

	var cfg EnvConfig
	if err := envconf.Load(&cfg, envs); err != nil {
		return EnvConfig{}, fmt.Errorf("failed to parse action envs, err: %v", err)
	}
	return cfg, nil
}

// Do do api test.
//
// attention:
//   push log to collector
//   save metadata
func Do(ctx context.Context, task *spec.PipelineTask) {
	logger := newLogger().
		WithContext(context.WithValue(context.Background(), CtxKeyCollectorLogID, task.Extra.UUID)).
		WithField(FieldOrgName, task.Extra.Labels[apistructs.EnvDiceOrgName]).
		WithField(FieldOrgID, task.Extra.Labels[apistructs.EnvDiceOrgID])
	ctx = context.WithValue(ctx, CtxKeyLogger, logger)

	// print logo
	printLogo(ctx)

	// parse conf from task
	cfg, err := parseConfFromTask(task)
	if err != nil {
		clog(ctx).Errorf("failed to parse config from task, err: %v", err)
		return
	}

	// get api info and pretty print
	apiInfo := generateAPIInfoFromEnv(cfg)

	printOriginalAPIInfo(ctx, apiInfo)

	// success
	var success = true
	defer func() {
		if !success {
			return
		}
	}()

	// defer create metafile
	meta := NewMeta()
	meta.OutParamsDefine = apiInfo.OutParams
	defer writeMetaFile(ctx, task, meta)

	// global config
	var apiTestEnvData *apistructs.APITestEnvData
	caseParams := make(map[string]*apistructs.CaseParams)
	if cfg.GlobalConfig != nil {
		apiTestEnvData = &apistructs.APITestEnvData{}
		apiTestEnvData.Domain = cfg.GlobalConfig.Domain
		apiTestEnvData.Header = cfg.GlobalConfig.Header
		apiTestEnvData.Global = make(map[string]*apistructs.APITestEnvVariable)
		for name, item := range cfg.GlobalConfig.Global {
			apiTestEnvData.Global[name] = &apistructs.APITestEnvVariable{
				Value: item.Value,
				Type:  item.Type,
			}
			caseParams[name] = &apistructs.CaseParams{
				Key:   name,
				Type:  item.Type,
				Value: item.Value,
			}
		}
	}

	// add cookie jar
	cookieJar, _ := cookiejar.New(&cookiejar.Options{PublicSuffixList: publicsuffix.List})
	if apiTestEnvData != nil && apiTestEnvData.Header != nil && len(apiTestEnvData.Header[CookieJar]) > 0 {
		var cookies cookiejar.Cookies
		err := json.Unmarshal([]byte(apiTestEnvData.Header[CookieJar]), &cookies)
		if err != nil {
			success = false
			clog(ctx).Errorf("failed to unmarshal cookieJar from header, err: %v\n", err)
			return
		}
		cookieJar.SetEntries(cookies)
	}
	hc := http.Client{Jar: cookieJar}
	printGlobalAPIConfig(ctx, apiTestEnvData)

	// do apiTest
	clusterName := ctx.Value(apistructs.ClusterNameContextKey)
	if clusterName == nil {
		clusterName = ""
	}
	apiTest := apitestsv2.New(apiInfo, apitestsv2.WithNetportalConfigs(customhttp.GetNetPortalUrl(clusterName.(string))))
	apiReq, apiResp, err := apiTest.Invoke(&hc, apiTestEnvData, caseParams)
	printRenderedHTTPReq(ctx, apiReq)
	meta.Req = apiReq
	meta.Resp = apiResp
	meta.CookieJar = cookieJar.GetEntries()
	if apiResp != nil {
		printHTTPResp(ctx, apiResp)
	}
	if err != nil {
		meta.Result = ResultFailed
		clog(ctx).Errorf("failed to do api test, err: %v", err)
		success = false
		return
	}

	// outParams store in metafile for latter use
	outParams := apiTest.ParseOutParams(apiTest.API.OutParams, apiResp, caseParams)
	printOutParams(ctx, outParams, meta)

	// judge asserts
	if len(apiTest.API.Asserts) > 0 {
		// 目前有且只有一组 asserts
		for _, group := range apiTest.API.Asserts {
			succ, assertResults := apiTest.JudgeAsserts(outParams, group)
			printAssertResults(ctx, succ, assertResults)
			if !succ {
				addNewLine(ctx)
				clog(ctx).Errorf("API Test Success, but asserts failed")
				success = false
				return
			}
		}
	}

	meta.Result = ResultSuccess

	addNewLine(ctx, 2)
	clog(ctx).Println("API Test Success")
}
