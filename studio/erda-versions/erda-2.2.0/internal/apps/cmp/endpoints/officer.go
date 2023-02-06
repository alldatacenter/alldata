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
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/registry"
	"github.com/erda-project/erda/internal/pkg/colonyutil"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) RegistryReadonly(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	s := vars["clusterName"]
	clusterinfo, err := e.bdl.QueryClusterInfo(s)
	if err != nil {
		errstr := fmt.Sprintf("RegistryReadonly queryclusterinfo err: %v, cluster: %v", err, s)
		logrus.Errorf(errstr)
		return mkResponseErr("400", errstr)
	}
	u := discover.Soldier()
	if clusterinfo.MustGet(apistructs.DICE_IS_EDGE) == "true" {
		u = clusterinfo.MustGetPublicURL("soldier")
	}
	var v apistructs.RegistryReadonlyResponse
	res, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Get(u).
		Path("/registry/readonly").
		Param("registryctlURL", ""). // omit this param
		Do().JSON(&v)
	if err != nil {
		errstr := fmt.Sprintf("call soldier failed: %v", err)
		logrus.Errorf(errstr)
		return mkResponseErr("502", errstr)
	}
	if res.StatusCode() != http.StatusOK {
		errstr := fmt.Sprintf("call soldier failed: statuscode: %d", res.StatusCode())
		logrus.Errorf(errstr)
		return mkResponseErr("502", errstr)
	}
	if !v.Success {
		errstr := fmt.Sprintf("call soldier failed: %v", v.Error.Msg)
		logrus.Errorf(errstr)
		return mkResponseErr("502", errstr)
	}
	return mkResponseData(v.Data)
}

func (e *Endpoints) RegistryRemoveManifests(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.RegistryManifestsRemoveRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return mkResponseErr("400", err.Error())
	}
	s := vars["clusterName"]

	if err := registry.DeleteManifests(e.bdl, s, req.Images); err != nil {
		errstr := fmt.Sprintf("Registry deleteManifests failed: %v", err)
		logrus.Errorf(errstr)
		return mkResponseErr("502", errstr)
	}
	var v apistructs.RegistryManifestsRemoveResponse
	v.Data.Succeed = req.Images
	return mkResponseData(v.Data)
}

func (e *Endpoints) RegistryRemoveLayers(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	s := vars["clusterName"]
	clusterinfo, err := e.bdl.QueryClusterInfo(s)
	if err != nil {
		errstr := fmt.Sprintf("failed queryclusterinfo err: %v, cluster: %v", err, s)
		logrus.Errorf(errstr)
		return mkResponseErr("400", errstr)
	}
	u := discover.Soldier()
	if clusterinfo.MustGet(apistructs.DICE_IS_EDGE) == "true" {
		u = clusterinfo.MustGetPublicURL("soldier")
	}

	registryAppID := "/devops/registry"
	var v apistructs.RegistryReadonlyResponse // reuse
	res, err := httpclient.New().Post(u).
		Path("/registry/remove/layers").
		JSONBody(map[string]interface{}{
			"registryAppID": registryAppID}).
		Do().JSON(&v)
	if err != nil {
		errstr := fmt.Sprintf("failed to call soldier: %v", err)
		logrus.Errorf(errstr)
		return mkResponseErr("502", errstr)
	}
	if res.StatusCode() != http.StatusOK {
		errstr := fmt.Sprintf("call soldier failed: statuscode: %d", res.StatusCode())
		logrus.Errorf(errstr)
		return mkResponseErr("502", errstr)
	}
	if !v.Success {
		errstr := fmt.Sprintf("call soldier failed: %v", v.Error.Msg)
		logrus.Errorf(errstr)
		return mkResponseErr("502", errstr)
	}
	return mkResponseData(v.Data)
}

var scriptPath string = "/app/scripts.tar.gz"

// GetScriptInfo Get script info
func (e *Endpoints) GetScriptInfo(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	if !strutil.Contains(r.Header.Get("Client-ID"), "soldier") {
		return mkResponseErr("401", "unknown client access")
	}
	md5, err := colonyutil.CheckMd5(scriptPath)
	if err != nil {
		return mkResponseErr("500", err.Error())
	}
	pathList := strutil.Split(scriptPath, "/")
	fileName := pathList[len(pathList)-1]
	size, err := colonyutil.GetFileSize(scriptPath)
	if err != nil {
		return mkResponseErr("500", err.Error())
	}
	return mkResponseData(apistructs.ScriptInfo{
		Md5:             md5,
		Name:            fileName,
		Size:            size,
		ScriptBlackList: []string{},
	})
}

// ServeScript Offer script download.
func (e *Endpoints) ServeScript(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) error {
	if !strutil.Contains(r.Header.Get("Client-ID"), "soldier") {
		colonyutil.WriteErr(w, "401", "unknown client access")
		return nil
	}
	name := vars["Name"]
	if name != colonyutil.GetFileName(scriptPath) {
		colonyutil.WriteErr(w, "401", "script filename mismatch!")
		return nil
	}
	http.ServeFile(w, r, scriptPath)
	return nil
}

func mkResponseErr(code, msg string) (httpserver.Responser, error) {
	return mkResponse(map[string]interface{}{
		"success": false,
		"err": map[string]interface{}{
			"code": code,
			"msg":  msg,
			"ctx":  nil,
		},
	})
}

func mkResponseData(v interface{}) (httpserver.Responser, error) {
	return mkResponse(map[string]interface{}{
		"success": true,
		"data":    v,
	})
}
