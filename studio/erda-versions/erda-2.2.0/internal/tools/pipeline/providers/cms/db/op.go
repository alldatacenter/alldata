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

package db

import (
	"sort"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
)

var (
	DefaultOperationsForKV        = pb.PipelineCmsConfigOperations{CanDownload: false, CanEdit: true, CanDelete: true}
	DefaultOperationsForDiceFiles = pb.PipelineCmsConfigOperations{CanDownload: true, CanEdit: true, CanDelete: true}
)

var (
	ConfigTypeKV       = "kv"
	ConfigTypeDiceFile = "dice-file"
)

type Client struct {
	mysqlxorm.Interface
}

func (client *Client) IdempotentCreateCmsNs(pipelineSource apistructs.PipelineSource, ns string, ops ...mysqlxorm.SessionOption) (PipelineCmsNs, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	// get cmsNs by source + ns
	cmsNs, exist, err := client.GetCmsNs(pipelineSource, ns, ops...)
	if err != nil {
		return PipelineCmsNs{}, err
	}
	if exist {
		return cmsNs, nil
	}

	// not exist, create
	var newNS PipelineCmsNs
	newNS.PipelineSource = pipelineSource
	newNS.Ns = ns
	_, err = session.InsertOne(&newNS)
	return newNS, err
}

func (client *Client) IdempotentDeleteCmsNs(pipelineSource apistructs.PipelineSource, ns string, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	// get cmsNs by source + ns
	cmsNs, exist, err := client.GetCmsNs(pipelineSource, ns, ops...)
	if err != nil {
		return err
	}
	if !exist {
		return nil
	}

	// delete ns by id
	if cmsNs.ID == 0 {
		return errors.Errorf("cms ns missing id")
	}
	_, err = session.ID(cmsNs.ID).Delete(&PipelineCmsNs{})
	return err
}

func (client *Client) GetCmsNs(pipelineSource apistructs.PipelineSource, ns string, ops ...mysqlxorm.SessionOption) (PipelineCmsNs, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var cmsNs PipelineCmsNs
	cmsNs.PipelineSource = pipelineSource
	cmsNs.Ns = ns
	exist, err := session.Get(&cmsNs)
	if err != nil {
		return PipelineCmsNs{}, false, err
	}
	return cmsNs, exist, nil
}

func (client *Client) BatchGetCmsNamespaces(pipelineSource string, namespaces []string, ops ...mysqlxorm.SessionOption) ([]PipelineCmsNs, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var cmsNsArray []PipelineCmsNs
	if err := session.Where("pipeline_source = ?", pipelineSource).In("ns", namespaces).Find(&cmsNsArray); err != nil {
		return nil, err
	}
	return cmsNsArray, nil
}

func (client *Client) PrefixListNs(pipelineSource apistructs.PipelineSource, nsPrefix string, ops ...mysqlxorm.SessionOption) ([]PipelineCmsNs, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var namespaces []PipelineCmsNs
	if err := session.Where("ns LIKE ?", nsPrefix+"%").Find(&namespaces); err != nil {
		return nil, err
	}
	return namespaces, nil
}

func (client *Client) UpdateCmsNsConfigs(cmsNs PipelineCmsNs, configs []PipelineCmsConfig, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	// Not sorting in multi-transaction scenarios will cause deadlock
	sort.Slice(configs, func(i, j int) bool {
		return configs[i].Key > configs[j].Key
	})
	for _, config := range configs {
		if err := client.InsertOrUpdateCmsNsConfig(cmsNs, config, ops...); err != nil {
			return err
		}
	}

	return nil
}

func (client *Client) InsertOrUpdateCmsNsConfig(cmsNs PipelineCmsNs, config PipelineCmsConfig, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	var query PipelineCmsConfig
	query.NsID = config.NsID
	query.Key = config.Key

	exist, err := session.Get(&query)
	if err != nil {
		return err
	}

	// update
	if exist {
		// no need update
		if query.Equal(config) {
			return nil
		}
		_, err = session.ID(query.ID).Update(&config)
		return err
	}

	// create
	_, err = session.InsertOne(&config)
	return err
}

func (client *Client) DeleteCmsNsConfigs(cmsNs PipelineCmsNs, keys []string, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	if len(keys) == 0 {
		return nil
	}

	_, err := session.In("key", keys).Delete(&PipelineCmsConfig{NsID: cmsNs.ID})
	return err
}

func (client *Client) GetCmsNsConfigs(cmsNs PipelineCmsNs, keys []string, ops ...mysqlxorm.SessionOption) ([]PipelineCmsConfig, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var configs []PipelineCmsConfig
	if len(keys) > 0 {
		session.In("key", keys)
	}
	err := session.Find(&configs, &PipelineCmsConfig{NsID: cmsNs.ID})
	if err != nil {
		return nil, err
	}
	return configs, nil
}

func (client *Client) BatchGetCmsNsConfigs(cmsNsIDs []uint64, ops ...mysqlxorm.SessionOption) ([]PipelineCmsConfig, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var configs []PipelineCmsConfig
	session.In("ns_id", cmsNsIDs)
	err := session.Find(&configs)
	if err != nil {
		return nil, err
	}
	return configs, nil
}
