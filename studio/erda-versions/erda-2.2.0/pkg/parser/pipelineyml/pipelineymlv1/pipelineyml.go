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

package pipelineymlv1

import (
	"encoding/json"
	"fmt"
	"path/filepath"
	"reflect"
	"strings"

	"github.com/ghodss/yaml"
	"github.com/hashicorp/go-multierror"
	"github.com/mitchellh/mapstructure"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/pipelineymlvars"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/steptasktype"
)

type PipelineYml struct {
	// byteData represents byte format pipeline.yml
	byteData []byte
	// obj represents the struct parsed according to byteData field
	obj *Pipeline

	option   *Option
	metadata PipelineMetadata
}

// PipelineMetadata contains extra info needs by parse process.
type PipelineMetadata struct {
	pipelineID string // pipelineID
	instanceID string // pipeline instance ID
	ContextDir string // context dir related with instanceID, under nfs mount point or oss path dir

	taskNameMap map[string]struct{}

	contextMap map[string][]string

	PlaceHolderEnvs map[string]string
}

func New(b []byte) *PipelineYml {
	return &PipelineYml{
		byteData: b,
	}
}

func (y *PipelineYml) Version() string {
	return y.obj.Version
}

func (y *PipelineYml) Object() *Pipeline {
	return y.obj
}

func (y *PipelineYml) GetOptions() *Option {
	return y.option
}

func (y *PipelineYml) GetMetadata() PipelineMetadata {
	return y.metadata
}

func (y *PipelineYml) StringData() string {
	return string(y.byteData)
}

func (y *PipelineYml) GetInstanceID() string {
	return y.metadata.instanceID
}

func (y *PipelineYml) GetPlaceholders() []metadata.MetadataField {
	return y.option.placeholders
}

func (y *PipelineYml) GetPlaceHolderTransformedEnvs() map[string]string {
	return y.metadata.PlaceHolderEnvs
}

func (y *PipelineYml) transformPlaceholdersToEnvs(placeholder []metadata.MetadataField) {
	if y.metadata.PlaceHolderEnvs == nil {
		y.metadata.PlaceHolderEnvs = make(map[string]string)
	}
	// placeholder 转换为 环境变量
	for _, ph := range placeholder {
		trimK := strings.TrimSuffix(strings.TrimPrefix(ph.Name, "(("), "))")
		trimV := strings.TrimSuffix(strings.TrimPrefix(ph.Value, "'"), "'")
		newK := strings.Replace(strings.Replace(strings.ToUpper(trimK), ".", "_", -1), "-", "_", -1)
		y.metadata.PlaceHolderEnvs[newK] = trimV
	}
}

func (y *PipelineYml) SetPipelineID(pipelineID uint64) {
	y.metadata.pipelineID = fmt.Sprintf("%d", pipelineID)
}

func (y *PipelineYml) JSON() (string, error) {
	b, err := json.Marshal(y.obj)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

func (y *PipelineYml) JSONPretty() (string, error) {
	b, err := json.MarshalIndent(y.obj, "", "  ")
	if err != nil {
		return "", err
	}
	return string(b), nil
}

func (y *PipelineYml) YAML() (string, error) {
	b, err := yaml.Marshal(y.obj)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

// ReRender 用于更改了结构体数据后重新渲染，保证其他字段同步
func (y *PipelineYml) ReRender() error {
	newYmlByte, err := y.YAML()
	if err != nil {
		return err
	}
	y.byteData = []byte(newYmlByte)
	return y.Parse()
}

func exitMultiError(errFuncs ...func() error) error {
	for _, f := range errFuncs {
		if err := f(); err != nil {
			return err
		}
	}
	return nil
}

// Unmarshal unmarshal byteData to obj with evaluate, need metadata templateVars.
func (y *PipelineYml) unmarshal() error {

	err := yaml.Unmarshal(y.byteData, &y.obj, func(decoder *json.Decoder) *json.Decoder {
		return decoder
	})
	if err != nil {
		return err
	}

	if y.obj == nil {
		return errNilPipelineYmlObj
	}

	if y.option.renderPlaceholder {
		err = y.evaluate(y.option.placeholders)
		if err != nil {
			return err
		}
	}

	// re unmarshal to obj, because byteData updated by evaluate
	err = yaml.Unmarshal(y.byteData, &y.obj)
	return err
}

// Parse parse yaml file, and validate fields content.
func (y *PipelineYml) Parse(ops ...OpOption) error {
	if y.option == nil {
		y.option = &Option{}
	}
	for _, op := range ops {
		op(y.option)
	}

	// resource type
	if y.option.builtinResourceTypeDockerImagePrefix == "" {
		y.option.builtinResourceTypeDockerImagePrefix = BuiltinResourceTypeDefaultDockerImagePrefix
	}
	y.option.builtinResourceTypeDockerImagePrefix = strings.TrimSuffix(y.option.builtinResourceTypeDockerImagePrefix, "/")
	if y.option.builtinResourceTypeDockerImageTag == "" {
		y.option.builtinResourceTypeDockerImageTag = BuiltinResourceTypeDefaultDockerImageTag
	}
	// container resource limit
	if y.option.containerResLimit.cpu == 0 {
		y.option.containerResLimit.cpu = float64(0.5)
	}
	if y.option.containerResLimit.mem == 0 {
		y.option.containerResLimit.mem = float64(2048)
	}

	// if y.metadata.pipelineID == "" {
	// 	y.metadata.pipelineID = ciuuid.NewUUID()
	// }
	if y.metadata.instanceID == "" {
		// y.metadata.instanceID = fmt.Sprintf("%s-%d", y.metadata.pipelineID, y.option.rerunVer)
		y.metadata.instanceID = y.metadata.pipelineID
	}
	y.metadata.ContextDir = filepath.Join(y.option.nfsRealPath, "pipelines", y.metadata.instanceID)

	return exitMultiError(
		y.unmarshal,
		y.checkVersion,
		func() error {
			y.transformPlaceholdersToEnvs(y.option.placeholders)
			return nil
		},
		y.validateTriggers,
		y.tempTaskConfigsSize,
		y.generateTasks,
		func() error {
			if !y.option.alreadyTransformed {
				return y.InsertDiceHub()
			}
			return nil
		},
		y.generateTasks,
		y.checkTimeout,
		y.validateResources,
		y.validateContext,
	)
}

func (y *PipelineYml) ParseWithAllFields(ops ...OpOption) error {
	if err := y.Parse(ops...); err != nil {
		return err
	}
	// other required task check TODO
	if len(y.option.clusterName) == 0 {
		return errNoClusterNameSpecify
	}
	if len(y.option.nfsRealPath) == 0 {
		return errMissingNFSRealPath
	}
	return nil
}

func (y *PipelineYml) checkVersion() error {
	if y.obj.Version != "1.0" {
		return errors.Wrap(errInvalidVersion, y.obj.Version)
	}
	return nil
}

func (y *PipelineYml) tempTaskConfigsSize() error {
	for i, stage := range y.obj.Stages {
		if len(stage.TaskConfigs) > 1 {
			return errors.Wrapf(errTempTaskConfigsSize, "stage[%d]", i)
		}
	}
	return nil
}

func (y *PipelineYml) ValidateResTypes() error {
	var duplicate, invalidType, invalidSource []string
	// 是否有重复的 resource type name
	nameMap := make(map[string]int, 0)
	for name := range y.builtinResTypes() {
		nameMap[name]++
	}
	for _, resType := range y.obj.ResourceTypes {
		nameMap[resType.Name]++
	}
	for name, count := range nameMap {
		if count > 1 {
			duplicate = append(duplicate, name)
		}
	}

	for _, resType := range y.obj.ResourceTypes {
		// 验证 resource_type type
		if resType.Type != DockerImageResType {
			invalidType = append(invalidType, resType.Name)
		}
		// 验证 resource_type source
		if _, ok := resType.Source["repository"]; !ok {
			invalidSource = append(invalidSource, resType.Name)
		}
	}

	var me *multierror.Error
	if len(duplicate) > 0 {
		me = multierror.Append(me, errors.Wrap(errDuplicateResTypes, fmt.Sprintf("%v", duplicate)))
	}
	if len(invalidType) > 0 {
		me = multierror.Append(me, errors.Wrap(errInvalidTypeOfResType, fmt.Sprintf("%v", invalidType)))
	}
	if len(invalidSource) > 0 {
		me = multierror.Append(me, errors.Wrap(errInvalidSourceOfResType, fmt.Sprintf("%v", invalidSource)))
	}

	// unknownResTypes := y.findUnknownResTypes()
	//
	// for _, typeName := range unknownResTypes {
	// 	_, err := dbClient.GetAction(typeName)
	// 	if err != nil {
	// 		me = multierror.Append(me, errors.Wrap(errUnknownResTypes, fmt.Sprintf("%v", unknownResTypes)))
	// 	}
	// }

	unusedResTypes := y.findUnusedUserDefinedResTypes()
	if len(unusedResTypes) > 0 {
		me = multierror.Append(me, errors.Wrap(errUnusedResTypes, fmt.Sprintf("%v", unusedResTypes)))
	}

	return me.ErrorOrNil()
}

func (y *PipelineYml) validateResources() error {

	var me *multierror.Error

	// resource 校验
	for _, res := range y.obj.Resources {
		if res.Name == "" {
			me = multierror.Append(me, errLackResName)
		}
		if res.Type == "" {
			me = multierror.Append(me, errors.Wrap(errLackResType, res.Name))
		}
	}

	// 是否有重复的 resource
	var duplicate []string
	nameMap := make(map[string]int, 0)
	for _, res := range y.obj.Resources {
		nameMap[res.Name]++
	}
	for name, count := range nameMap {
		if count > 1 {
			duplicate = append(duplicate, name)
		}
	}
	if len(duplicate) > 0 {
		me = multierror.Append(me, errors.Wrap(errDuplicateRes, fmt.Sprintf("%v", duplicate)))
	}

	// 是否有未使用的 resource
	unusedResources := y.findUnusedResources()
	if len(unusedResources) > 0 {
		me = multierror.Append(me, errors.Wrap(errUnusedResources, fmt.Sprintf("%v", unusedResources)))
	}

	return me.ErrorOrNil()
}

func (y *PipelineYml) generateTasks() error {
	taskNameMap := make(map[string]struct{})
	for _, stage := range y.obj.Stages {
		for _, taskConfig := range stage.TaskConfigs {
			tasks, _, err := y.taskConfig2StepTasks(taskConfig)
			if err != nil {
				return err
			}
			for _, task := range tasks {
				// task name must be unique
				if _, ok := taskNameMap[task.Name()]; ok {
					return errors.Wrap(errDuplicateTaskName, task.Name())
				}
				taskNameMap[task.Name()] = struct{}{}
			}
			stage.Tasks = tasks
		}
	}
	y.metadata.taskNameMap = taskNameMap
	return nil
}

func (y *PipelineYml) taskConfig2StepTasks(taskConfig map[string]interface{}) ([]StepTask, bool, error) {
	tasks := make([]StepTask, 0)
	// aggregate
	if _, ok := taskConfig["aggregate"]; ok {
		var agg aggregateTask
		aggDecoder, err := mapstructure.NewDecoder(&mapstructure.DecoderConfig{
			Result:      &agg,
			ErrorUnused: true,
		})
		if err != nil {
			return nil, true, err
		}
		err = aggDecoder.Decode(taskConfig)
		if err != nil {
			return nil, true, errors.Wrap(err, "decode aggregate")
		}
		for _, cfg := range agg.Aggregate {
			task, err := y.validateSingleTaskConfig(cfg)
			if err != nil {
				return nil, true, err
			}
			if task != nil {
				tasks = append(tasks, task)
			}
		}
		return tasks, true, nil
	} else {
		// get / put / task
		task, err := y.validateSingleTaskConfig(taskConfig)
		if err != nil {
			return nil, false, err
		}
		tasks = append(tasks, task)
		return tasks, false, nil
	}
	return tasks, false, nil
}

func (y *PipelineYml) validateSingleTaskConfig(tc TaskConfig) (StepTask, error) {

	// 判断 steptask 类型
	_, getOK := tc[pipelineymlvars.FieldGet.String()]
	_, putOK := tc[pipelineymlvars.FieldPut.String()]
	_, taskOK := tc[pipelineymlvars.FieldTask.String()]

	// steptask 均不匹配
	if !getOK && !putOK && !taskOK {
		return nil, errors.Wrapf(errInvalidStepTaskConfig, "%+v", tc)
	}
	// 同时匹配多个的情况由具体的 decoder.ErrorUnused 自己保证
	// put 类型可能隐含一个 get，所以不能简单地在外层判断，需要由 steptask 自己判断

	switch true {
	case getOK:
		return tc.decodeStepTaskWithValidate(steptasktype.GET, y)
	case putOK:
		return tc.decodeStepTaskWithValidate(steptasktype.PUT, y)
	case taskOK:
		return tc.decodeStepTaskWithValidate(steptasktype.TASK, y)
	}

	return nil, errors.Errorf("invalid task config, content: %v, type: %v\n", tc, reflect.TypeOf(tc))
}

// validResTypesMap return all valid resource type:
// 1. built-in
// 2. found in resource_types
func (y *PipelineYml) validResTypesMap() map[string]ResourceType {
	result := make(map[string]ResourceType)
	for name, resType := range y.builtinResTypes() {
		result[name] = resType
	}
	for _, resType := range y.obj.ResourceTypes {
		if _, ok := result[resType.Name]; !ok {
			result[resType.Name] = resType
		}
	}

	return result
}

// findUnknownResTypes find unknown resource types:
// 1. not built-in
// 2. not found in resource_types
func (y *PipelineYml) findUnknownResTypes() []string {

	m := y.validResTypesMap()

	unknownTypes := make([]string, 0)

	for _, res := range y.obj.Resources {
		if _, ok := m[res.Type]; !ok {
			unknownTypes = append(unknownTypes, res.Type)
		}
	}

	return unknownTypes
}

func (y *PipelineYml) validResourceMap() map[string]Resource {
	result := make(map[string]Resource)

	for _, res := range y.obj.Resources {
		if _, ok := result[res.Name]; !ok {
			result[res.Name] = res
		}
	}
	return result
}

func (y *PipelineYml) findUnusedResources() []string {
	unused := make([]string, 0)
	m := y.validResourceMap()

	for _, stage := range y.obj.Stages {
		for _, task := range stage.Tasks {
			for _, resName := range task.RequiredContext(*y) {
				if _, ok := m[resName]; ok {
					delete(m, resName)
				}
			}
			for _, resName := range task.OutputToContext() {
				if _, ok := m[resName]; ok {
					delete(m, resName)
				}
			}
		}
	}
	for resName := range m {
		unused = append(unused, resName)
	}

	return unused
}

func (y *PipelineYml) findUnusedUserDefinedResTypes() []string {
	unused := make([]string, 0)
	m := y.validResTypesMap()
	for _, res := range y.obj.Resources {
		delete(m, res.Type)
	}
	builtinResTypes := y.builtinResTypes()
	for resTypeName := range m {
		if _, ok := builtinResTypes[resTypeName]; !ok {
			unused = append(unused, resTypeName)
		}
	}
	return unused
}

func (y *PipelineYml) validateContext() error {
	y.metadata.contextMap = make(map[string][]string)
	currentContext := make(map[string]struct{})
	for _, stage := range y.obj.Stages { // TODO 区分 并行和串行 stage，防止 task 因为遍历的先后顺序拿到了不该拿到的 out resource
		// 目前均为并行

		var addToNextStage = make([]string, 0)
		for _, task := range stage.Tasks {
			if task.IsDisable() {
				continue
			}
			taskContext := shadowClone(currentContext)
			// 如果需要的 context 在 当前上下文 中没有找到，则报错
			for _, require := range task.RequiredContext(*y) {
				if _, ok := taskContext[require]; !ok {
					return errors.Wrapf(errNotAvailableInContext, "task: %s, require: %s", task.Name(), require)
				}
			}
			// 将 output 输出到 context，使后面的 task 可用
			for _, out := range task.OutputToContext() {
				_, ok := taskContext[out]
				if ok {
					return errors.Wrap(errDuplicateOutput, out)
				}
				taskContext[out] = struct{}{}
				addToNextStage = append(addToNextStage, out)
			}
			// 为每个 task 设置上下文
			for resName := range taskContext {
				y.metadata.contextMap[task.Name()] = append(y.metadata.contextMap[task.Name()], resName)
			}
		}

		for _, add := range addToNextStage {
			currentContext[add] = struct{}{}
		}
	}
	return nil
}

func shadowClone(src map[string]struct{}) map[string]struct{} {
	dst := make(map[string]struct{})
	for k, v := range src {
		dst[k] = v
	}
	return dst
}
