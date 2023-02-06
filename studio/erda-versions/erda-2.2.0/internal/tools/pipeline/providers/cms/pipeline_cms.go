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

package cms

import (
	"context"
	"fmt"
	"reflect"
	"regexp"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms/db"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/limit_sync_group"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	CtxKeyPipelineSource = "pipelineSource"
	CtxKeyForceDelete    = "forceDelete"
)

type pipelineCm struct {
	dbClient *db.Client
	rsaCrypt *encryption.RsaCrypt
}

func NewPipelineCms(dbClient *db.Client, rsaCrypt *encryption.RsaCrypt) *pipelineCm {
	var cm pipelineCm
	cm.dbClient = dbClient
	cm.rsaCrypt = rsaCrypt
	return &cm
}

func (c *pipelineCm) IdempotentCreateNs(ctx context.Context, ns string) error {
	pipelineSource, err := getPipelineSourceFromContext(ctx)
	if err != nil {
		return err
	}
	_, err = c.dbClient.IdempotentCreateCmsNs(pipelineSource, ns)
	return err
}

func (c *pipelineCm) IdempotentDeleteNs(ctx context.Context, ns string) error {
	pipelineSource, err := getPipelineSourceFromContext(ctx)
	if err != nil {
		return err
	}
	return c.dbClient.IdempotentDeleteCmsNs(pipelineSource, ns)
}

func (c *pipelineCm) PrefixListNs(ctx context.Context, nsPrefix string) ([]*pb.PipelineCmsNs, error) {
	pipelineSource, err := getPipelineSourceFromContext(ctx)
	if err != nil {
		return nil, err
	}
	namespaces, err := c.dbClient.PrefixListNs(pipelineSource, nsPrefix)
	if err != nil {
		return nil, err
	}
	var result []*pb.PipelineCmsNs
	for _, ns := range namespaces {
		result = append(result, &pb.PipelineCmsNs{
			PipelineSource: ns.PipelineSource.String(),
			Ns:             ns.Ns,
			TimeCreated:    timestamppb.New(*ns.TimeCreated),
			TimeUpdated:    timestamppb.New(*ns.TimeUpdated),
		})
	}
	return result, nil
}

func (c *pipelineCm) UpdateConfigs(ctx context.Context, ns string, kvs map[string]*pb.PipelineCmsConfigValue) error {
	pipelineSource, err := getPipelineSourceFromContext(ctx)
	if err != nil {
		return err
	}
	cmsNs, exist, err := c.dbClient.GetCmsNs(pipelineSource, ns)
	if err != nil {
		return err
	}

	var cmsNsID = cmsNs.ID
	if !exist {
		if err := c.IdempotentCreateNs(ctx, ns); err != nil {
			return err
		}
		// 获取 ns
		newCmsNs, exist, err := c.dbClient.GetCmsNs(pipelineSource, ns)
		if err != nil {
			return err
		}
		if !exist {
			return errors.Errorf("failed to get cms ns after create, pipelineSource: %s, ns: %s", pipelineSource, ns)
		}
		cmsNsID = newCmsNs.ID
	}
	var configs []db.PipelineCmsConfig
	for k, v := range kvs {
		vv, err := c.encryptValueIfNeeded(v.EncryptInDB, v.Value)
		if err != nil {
			return err
		}
		setDefault(v)
		if err := validateConfigWhenUpdate(k, v); err != nil {
			return errors.Errorf("config key: %s, err: %v", k, err)
		}
		configs = append(configs, db.PipelineCmsConfig{
			NsID:    cmsNsID,
			Key:     k,
			Value:   vv,
			Encrypt: &[]bool{v.EncryptInDB}[0],
			Type:    v.Type,
			Extra: db.PipelineCmsConfigExtra{
				Operations: v.Operations,
				Comment:    v.Comment,
				From:       v.From,
			},
		})
	}
	// tx
	txSession := c.dbClient.NewSession()
	defer txSession.Close()
	if err := txSession.Begin(); err != nil {
		return err
	}
	err = c.dbClient.UpdateCmsNsConfigs(cmsNs, configs, mysqlxorm.WithSession(txSession))
	if err != nil {
		if rbErr := txSession.Rollback(); rbErr != nil {
			logrus.Errorf("[alert] failed to rollback tx session when update pipeline cms ns configs failed, pipelineSource: %s, ns: %s, rbErr: %v, err: %v",
				pipelineSource, ns, rbErr, err)
			return err
		}
		return err
	}
	if cmErr := txSession.Commit(); cmErr != nil {
		logrus.Errorf("[alert] failed to commit tx session when update pipeline cms ns configs success, pipelineSource: %s, ns: %s, cmErr: %v, err: %v",
			pipelineSource, ns, cmErr, err)
		return cmErr
	}
	return nil
}

func (c *pipelineCm) DeleteConfigs(ctx context.Context, ns string, keys ...string) error {
	pipelineSource, err := getPipelineSourceFromContext(ctx)
	if err != nil {
		return err
	}
	cmsNs, exist, err := c.dbClient.GetCmsNs(pipelineSource, ns)
	if err != nil {
		return err
	}
	if !exist {
		return nil
	}

	// validate keys before delete
	configs, err := c.GetConfigs(ctx, ns, false, func() []*pb.PipelineCmsConfigKey {
		var getKeys []*pb.PipelineCmsConfigKey
		for _, key := range keys {
			getKeys = append(getKeys, &pb.PipelineCmsConfigKey{
				Key:     key,
				Decrypt: false,
			})
		}
		return getKeys
	}()...)
	if err != nil {
		return err
	}
	var cannotDelKeys []string
	forceDel, ok := ctx.Value(CtxKeyForceDelete).(bool)
	if !ok {
		return errors.New("failed to get force delete key")
	}
	if !forceDel {
		for k, v := range configs {
			if !v.Operations.CanDelete {
				cannotDelKeys = append(cannotDelKeys, k)
			}
		}
		if len(cannotDelKeys) > 0 {
			return errors.Errorf("cannot delete keys: %s", strutil.Join(cannotDelKeys, ", ", true))
		}
	}

	return c.dbClient.DeleteCmsNsConfigs(cmsNs, keys)
}

func (c *pipelineCm) GetConfigs(ctx context.Context, ns string, globalDecrypt bool, keys ...*pb.PipelineCmsConfigKey) (map[string]*pb.PipelineCmsConfigValue, error) {
	pipelineSource, err := getPipelineSourceFromContext(ctx)
	if err != nil {
		return nil, err
	}
	reqConfigKeyMap := make(map[string]*pb.PipelineCmsConfigKey, len(keys))
	for _, key := range keys {
		reqConfigKeyMap[key.Key] = key
	}
	cmsNs, exist, err := c.dbClient.GetCmsNs(pipelineSource, ns)
	if err != nil {
		return nil, err
	}
	if !exist {
		return nil, nil
	}
	configs, err := c.dbClient.GetCmsNsConfigs(cmsNs, transformKeysToStrSlice(keys...))
	if err != nil {
		return nil, err
	}
	result := make(map[string]*pb.PipelineCmsConfigValue, len(configs))
	var lock sync.Mutex
	wait := limit_sync_group.NewSemaphore(10)
	for _, config := range configs {
		wait.Add(1)
		go func(config db.PipelineCmsConfig) {
			defer wait.Done()
			vv, err := c.DecryptConfigValue(globalDecrypt, config, reqConfigKeyMap)
			if err != nil {
				logrus.Errorf("failed to DecryptConfigValue, err: %v", err)
				return
			}
			lock.Lock()
			defer lock.Unlock()
			// 配置项级别的展示配置
			result[config.Key] = &pb.PipelineCmsConfigValue{
				Value:       vv,
				EncryptInDB: *config.Encrypt,
				Type:        config.Type,
				Operations:  config.Extra.Operations,
				Comment:     config.Extra.Comment,
				From:        config.Extra.From,
				TimeCreated: getPbTimestamp(config.TimeCreated),
				TimeUpdated: getPbTimestamp(config.TimeUpdated),
			}
		}(config)
	}
	wait.Wait()
	return result, nil
}

func (c *pipelineCm) DecryptConfigValue(globalDecrypt bool, config db.PipelineCmsConfig, reqConfigKeyMap map[string]*pb.PipelineCmsConfigKey) (string, error) {
	// Global decryption settings are used by default
	needDecrypt := globalDecrypt
	// Configure item level decryption settings to override global decryption settings
	if reqConfig, ok := reqConfigKeyMap[config.Key]; ok {
		needDecrypt = reqConfig.Decrypt
	}
	// Non-encrypted storage in db does not need to be decrypted
	needDecrypt = needDecrypt && *config.Encrypt
	vv, err := c.decryptValueIfNeeded(needDecrypt, config.Value)
	if err != nil {
		return "", err
	}

	// Whether the encrypted storage and undecrypted value is displayed
	if *config.Encrypt && !needDecrypt {
		// Not shown by default
		needShowEncryptedValue := false
		// Configuration item level display configuration
		if reqConfig, ok := reqConfigKeyMap[config.Key]; ok {
			needShowEncryptedValue = reqConfig.ShowEncryptedValue
		}
		// No need to display, then value is left blank
		if !needShowEncryptedValue {
			vv = ""
		}
	}

	return vv, nil
}

func (c *pipelineCm) BatchGetConfigs(ctx context.Context, req *pb.CmsNsConfigsBatchGetRequest) ([]*pb.PipelineCmsConfig, error) {
	cmsNsList, err := c.dbClient.BatchGetCmsNamespaces(req.PipelineSource, req.Namespaces)
	if err != nil {
		return nil, err
	}

	var cmsNsIDList []uint64
	var cmsNsIDMap = map[uint64]db.PipelineCmsNs{}
	for _, ns := range cmsNsList {
		cmsNsIDList = append(cmsNsIDList, ns.ID)
		cmsNsIDMap[ns.ID] = ns
	}

	configs, err := c.dbClient.BatchGetCmsNsConfigs(cmsNsIDList)
	if err != nil {
		return nil, err
	}

	var newConfigs []*pb.PipelineCmsConfig
	var lock sync.Mutex
	wait := limit_sync_group.NewSemaphore(10)
	for _, config := range configs {
		wait.Add(1)
		go func(config db.PipelineCmsConfig) {
			defer wait.Done()
			var newConfig = pb.PipelineCmsConfig{
				Key:         config.Key,
				Value:       config.Value,
				EncryptInDB: *config.Encrypt,
				Type:        config.Type,
				Operations:  config.Extra.Operations,
				Comment:     config.Extra.Comment,
				From:        config.Extra.From,
				TimeCreated: getPbTimestamp(config.TimeCreated),
				TimeUpdated: getPbTimestamp(config.TimeUpdated),
			}

			var relationNs, ok = cmsNsIDMap[config.NsID]
			if !ok {
				logrus.Errorf("not find config %v cmsNs", config.NsID)
				return
			}

			newConfig.Ns = &pb.PipelineCmsNs{
				PipelineSource: relationNs.PipelineSource.String(),
				Ns:             relationNs.Ns,
				TimeCreated:    getPbTimestamp(config.TimeCreated),
				TimeUpdated:    getPbTimestamp(config.TimeUpdated),
			}

			vv, err := c.DecryptConfigValue(req.GlobalDecrypt, config, nil)
			if err != nil {
				logrus.Errorf("failed to DecryptConfigValue, err: %v", err)
				return
			}
			newConfig.Value = vv
			lock.Lock()
			defer lock.Unlock()
			newConfigs = append(newConfigs, &newConfig)
		}(config)
	}
	wait.Wait()
	return newConfigs, nil
}

func getPbTimestamp(t *time.Time) *timestamppb.Timestamp {
	if t == nil {
		return nil
	}
	return timestamppb.New(*t)
}

func getPipelineSourceFromContext(ctx context.Context) (apistructs.PipelineSource, error) {
	var source apistructs.PipelineSource
	switch ctx.Value(CtxKeyPipelineSource).(type) {
	case apistructs.PipelineSource:
		source = ctx.Value(CtxKeyPipelineSource).(apistructs.PipelineSource)
	case string:
		source = apistructs.PipelineSource(ctx.Value(CtxKeyPipelineSource).(string))
	default:
		return "", fmt.Errorf("invalid type of %q, type: %v", CtxKeyPipelineSource, reflect.TypeOf(ctx.Value(CtxKeyPipelineSource)).Name())
	}
	if source == "" {
		return "", errors.Errorf("missing %s", CtxKeyPipelineSource)
	}
	return source, nil
}

func (c *pipelineCm) encryptValueIfNeeded(needEncrypt bool, origValue string) (string, error) {
	if !needEncrypt {
		return origValue, nil
	}
	encryptedV, err := c.rsaCrypt.Encrypt(origValue, encryption.Base64)
	if err != nil {
		return "", err
	}
	return encryptedV, nil
}

func (c *pipelineCm) decryptValueIfNeeded(needDecrypt bool, origValue string) (string, error) {
	if !needDecrypt {
		return origValue, nil
	}
	decryptedV, err := c.rsaCrypt.Decrypt(origValue, encryption.Base64)
	if err != nil {
		return "", err
	}
	return decryptedV, nil
}

func setDefault(v *pb.PipelineCmsConfigValue) {
	if v.Type == "" {
		v.Type = ConfigTypeKV
	}
	if v.Operations == nil {
		v.Operations = &DefaultOperationsForKV
		if v.Type == ConfigTypeDiceFile {
			v.Operations.CanDownload = true
		}
	}
}

func validateConfigWhenUpdate(key string, v *pb.PipelineCmsConfigValue) error {
	// key
	if err := strutil.Validate(key, strutil.NoChineseValidator, KeyValidator, EnvTransferValidator, strutil.MaxLenValidator(191)); err != nil {
		return err
	}
	// value
	if v.Type == "" {
		return errors.New("missing config type")
	}
	if !configType(v.Type).IsValid() {
		return errors.Errorf("invalid config type: %s", v.Type)
	}
	if err := strutil.Validate(key, strutil.EnvValueLenValidator); err != nil {
		return err
	}
	// comment
	if err := strutil.Validate(v.Comment, strutil.MaxLenValidator(200)); err != nil {
		return errors.Errorf("failed to validate comment, err: %v", err)
	}
	return nil
}

var KeyValidator strutil.Validator = func(s string) error {
	// 支持字母、数字、下划线、中划线、`.`，不能以数字、中划线、`.` 开头
	keyRegexp := `^[a-zA-Z_]+[.a-zA-Z0-9_-]*$`
	valid := regexp.MustCompilePOSIX(keyRegexp).MatchString(s)
	if !valid {
		return fmt.Errorf("valid key regexp: %s", keyRegexp)
	}
	return nil
}

var EnvTransferValidator strutil.Validator = func(s string) error {
	// 转换为 env
	env := strings.Replace(strings.Replace(strings.ToUpper(s), ".", "_", -1), "-", "_", -1)
	if err := strutil.Validate(env, strutil.EnvKeyValidator); err != nil {
		return errors.Errorf("failed to transfer config key to env key, config key: %s, env key: %s, err: %v",
			s, env, err)
	}
	return nil
}
