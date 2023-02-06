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

package clusterinfo

import (
	"bytes"
	"context"
	"text/template"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/strutil"
)

// clusterInfoPrefix Is the path prefix of the cluster configuration information in etcd
const (
	clusterInfoPrefix = "/dice/scheduler/clusterinfo/"
	queryTimeout      = 3 * time.Second
)

type ClusterInfo interface {
	Info(string) (apistructs.ClusterInfoData, error)
	List([]string) (apistructs.ClusterInfoDataList, error)
}

type ClusterInfoImpl struct {
	js jsonstore.JsonStore
}

func NewClusterInfoImpl(js jsonstore.JsonStore) ClusterInfo {
	return &ClusterInfoImpl{js: js}
}

func (c *ClusterInfoImpl) Info(name string) (apistructs.ClusterInfoData, error) {
	var data apistructs.ClusterInfoData

	ctx, cancel := context.WithTimeout(context.Background(), queryTimeout)
	defer cancel()

	if err := c.js.Get(ctx, strutil.Concat(clusterInfoPrefix, name), &data); err != nil {
		return apistructs.ClusterInfoData{}, err
	}

	return data, nil
}

func (c *ClusterInfoImpl) List(names []string) (apistructs.ClusterInfoDataList, error) {
	var dataList apistructs.ClusterInfoDataList
	var nameMap map[string]interface{}

	for _, name := range names {
		nameMap[name] = struct{}{}
	}

	ctx, cancel := context.WithTimeout(context.Background(), queryTimeout)
	defer cancel()

	err := c.js.ForEach(ctx, clusterInfoPrefix, apistructs.ClusterInfoData{}, func(key string, v interface{}) error {
		if _, ok := nameMap[key]; ok {
			data := v.(*apistructs.ClusterInfoData)
			dataList = append(dataList, *data)
		}
		return nil
	})
	if err != nil {
		return nil, err
	}

	return dataList, nil
}

// ParseJobHostBindTemplate Analyze the hostPath template and replace it with the value of cluster info
func ParseJobHostBindTemplate(hostPath string, clusterInfo apistructs.ClusterInfoData) (string, error) {
	var b bytes.Buffer

	if hostPath == "" {
		return "", errors.New("hostPath is empty")
	}

	t, err := template.New("jobBind").
		Option("missingkey=error").
		Parse(hostPath)
	if err != nil {
		return "", errors.Errorf("failed to parse bind, hostPath: %s, (%v)", hostPath, err)
	}

	err = t.Execute(&b, &clusterInfo)
	if err != nil {
		return "", errors.Errorf("failed to execute bind, hostPath: %s, (%v)", hostPath, err)
	}

	return b.String(), nil
}
