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

package dbclient

import (
	"fmt"
	"sort"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/retry"
	"github.com/erda-project/erda/pkg/strutil"
)

func (client *Client) GetLabel(id uint64) (label *spec.PipelineLabel, err error) {
	defer func() {
		if err != nil {
			err = errors.Wrapf(err, "failed to get pipeline label by id: %v", id)
		}
	}()
	found, err := client.ID(id).Get(label)
	if err != nil {
		return nil, err
	}
	if !found {
		return nil, errors.New("not found")
	}
	return label, nil
}

func (client *Client) BatchInsertLabels(labels []spec.PipelineLabel, ops ...SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()
	defer func() { err = errors.Wrap(err, "failed to create pipeline label") }()
	_, err = session.Insert(labels)
	return err
}

func (client *Client) CreatePipelineLabels(p *spec.Pipeline, ops ...SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	defer func() { err = errors.Wrap(err, "failed to create pipeline label") }()
	labels := make([]spec.PipelineLabel, 0, len(p.Labels))
	for k, v := range p.Labels {
		label := spec.PipelineLabel{
			Type:            apistructs.PipelineLabelTypeInstance,
			PipelineSource:  p.PipelineSource,
			PipelineYmlName: p.PipelineYmlName,
			TargetID:        p.ID,
			Key:             k,
			Value:           v,
			ID:              uuid.SnowFlakeIDUint64(),
		}
		labels = append(labels, label)
	}
	_, err = session.InsertMulti(labels)
	return err
}

func (client *Client) ListPipelineLabels(req *apistructs.PipelineLabelListRequest, ops ...SessionOption) ([]spec.PipelineLabel, int64, error) {
	sqlSession := client.NewSession(ops...)
	defer sqlSession.Close()

	var labels []spec.PipelineLabel
	sql := sqlSession.Table(spec.PipelineLabel{}.TableName())

	if len(req.PipelineSource) > 0 {
		sql = sql.Where("pipeline_source = ?", req.PipelineSource)
	}

	if len(req.PipelineYmlName) > 0 {
		sql = sql.Where("pipeline_yml_name = ?", req.PipelineYmlName)
	}

	if len(req.TargetIDs) > 0 {
		sql = sql.In("target_id", req.TargetIDs)
	}

	if len(req.MatchKeys) > 0 {
		sql = sql.In("key", req.MatchKeys)
	}

	err := sql.Find(&labels)
	if err != nil {
		return nil, 0, err
	}

	return labels, 0, nil
}

// ListLabelsByPipelineID 根据 pipelineID 获取 labels
func (client *Client) ListLabelsByPipelineID(pipelineID uint64, ops ...SessionOption) ([]spec.PipelineLabel, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var labels []spec.PipelineLabel
	if err := session.Find(&labels, spec.PipelineLabel{Type: apistructs.PipelineLabelTypeInstance, TargetID: pipelineID}); err != nil {
		return nil, err
	}
	return labels, nil
}

func (client *Client) SelectTargetIDsByLabels(req apistructs.TargetIDSelectByLabelRequest, ops ...SessionOption) (targetIDs []uint64, err error) {
	defer func() {
		if err != nil {
			err = errors.Errorf("failed to get targetIDs match by labels, req: %+v, err: %v", req, err)
		}
	}()

	session := client.NewSession(ops...)
	defer session.Close()

	// 校验
	if !req.Type.Valid() {
		return nil, fmt.Errorf("type must be specified")
	}
	if len(req.PipelineSources) == 0 {
		req.PipelineSources = []apistructs.PipelineSource{apistructs.PipelineSourceDefault}
	}
	if req.AllowNoPipelineSources {
		req.PipelineSources = []apistructs.PipelineSource{}
	}
	if len(req.MustMatchLabels) > 0 && len(req.AnyMatchLabels) > 0 {
		return nil, errors.Errorf("please only set one of mustMatchLabels and anyMatchLabels")
	}
	if !req.AllowNoMatchLabels && (len(req.MustMatchLabels) == 0 && len(req.AnyMatchLabels) == 0) {
		return nil, errors.Errorf("neither mustMathLabels nor anyMathLabels set")
	}

	// SQL
	sqlSegments := []string{
		"SELECT `target_id` FROM `pipeline_labels`",
		"FORCE INDEX (`idx_type_source_key_value_targetid`, `idx_type_source_ymlname_key_value_targetid`)",
		"WHERE `type` = ?",
	}
	sqlArgs := []interface{}{req.Type}

	// segment: pipeline_source
	if len(req.PipelineSources) > 0 {
		sqlSegments = append(sqlSegments, fmt.Sprintf("AND `pipeline_source` IN (%s)", questionMarks(len(req.PipelineSources))))
		for _, source := range req.PipelineSources {
			sqlArgs = append(sqlArgs, source.String())
		}
	}

	// segment: pipeline_yml_name
	if len(req.PipelineYmlNames) > 0 {
		sqlSegments = append(sqlSegments, fmt.Sprintf("AND `pipeline_yml_name` IN (%s)", questionMarks(len(req.PipelineYmlNames))))
		for _, name := range req.PipelineYmlNames {
			sqlArgs = append(sqlArgs, name)
		}
	}

	// 标签处理
	// 每组 kye/values 单独处理，然后在内存中进行组合
	var handleResult []uint64

	if len(req.MustMatchLabels) > 0 {
		var allNeedFilterTargetIDs [][]uint64
		for key, values := range req.MustMatchLabels {
			if len(values) == 0 {
				continue
			}
			innerSegments := append(sqlSegments, fmt.Sprintf("AND `key` = ? AND `value` IN (%s)", questionMarks(len(values))))
			innerArgs := append(sqlArgs, key)
			for _, v := range values {
				innerArgs = append(innerArgs, v)
			}
			// execute
			var innerPipelineIDs []uint64
			err := session.Prepare().SQL(strutil.Join(innerSegments, " "), innerArgs...).Find(&innerPipelineIDs)
			if err != nil {
				return nil, err
			}
			allNeedFilterTargetIDs = append(allNeedFilterTargetIDs, innerPipelineIDs)
		}
		handleResult = filter(allNeedFilterTargetIDs...)
	}

	if len(req.AnyMatchLabels) > 0 {
		var allNeedMergeTargetIDs [][]uint64
		for key, values := range req.AnyMatchLabels {
			if len(values) == 0 {
				continue
			}
			innerSegments := append(sqlSegments, fmt.Sprintf("AND `key` = ? AND `value` IN (%s)", questionMarks(len(values))))
			innerArgs := append(sqlArgs, key)
			for _, v := range values {
				innerArgs = append(innerArgs, v)
			}
			// execute
			var innerPipelineIDs []uint64
			err := session.SQL(strutil.Join(innerSegments, " "), innerArgs...).Find(&innerPipelineIDs)
			if err != nil {
				return nil, err
			}
			allNeedMergeTargetIDs = append(allNeedMergeTargetIDs, innerPipelineIDs)
		}
		handleResult = merge(allNeedMergeTargetIDs...)
	}

	targetIDs = handleResult

	// ORDER BY `target_id` DESC / ASC
	if req.OrderByTargetIDAsc {
		// ASC
		sort.Slice(targetIDs, func(i, j int) bool { return targetIDs[i] < targetIDs[j] })
	} else {
		// DESC
		sort.Slice(targetIDs, func(i, j int) bool { return targetIDs[i] > targetIDs[j] })
	}

	return targetIDs, nil
}

func (client *Client) DeletePipelineLabelsByPipelineID(pipelineID uint64, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	return retry.DoWithInterval(func() error {
		_, err := session.Delete(&spec.PipelineLabel{Type: apistructs.PipelineLabelTypeInstance, TargetID: pipelineID})
		return err
	}, 3, time.Second)
}

// merge 合并 inputs 里的 id
func merge(inputs ...[]uint64) []uint64 {
	m := make(map[uint64]struct{})
	for _, input := range inputs {
		for _, id := range input {
			m[id] = struct{}{}
		}
	}
	var result []uint64
	for id := range m {
		result = append(result, id)
	}
	return result
}

// filter 保证 result 中的 key 在每组 input 中都存在
func filter(inputs ...[]uint64) []uint64 {
	if len(inputs) == 0 {
		return nil
	}
	// idCountMap: key: id, value: id 出现的次数
	idCountMap := make(map[uint64]int)
	for _, input := range inputs {
		for _, id := range input {
			idCountMap[id]++ // id 出现次数 +1
		}
	}
	for id, count := range idCountMap {
		// 若 id 在每组 input 中都存在，则出现的次数 count = input 组数
		if count < len(inputs) {
			delete(idCountMap, id)
		}
	}
	// idCountMap map -> slice
	var result []uint64
	for id := range idCountMap {
		result = append(result, id)
	}
	return result
}

// filterAndOrder filter 后，结果以 inputs[0] 的顺序为准
func filterAndOrder(inputs ...[]uint64) []uint64 {
	if len(inputs) == 0 {
		return nil
	}
	filterResult := filter(inputs...)

	// 以 inputs[0] 的顺序为准
	var orderedResult []uint64
	filterResultMap := make(map[uint64]struct{}, len(filterResult))
	for _, id := range filterResult {
		filterResultMap[id] = struct{}{}
	}
	for _, id := range inputs[0] {
		if _, ok := filterResultMap[id]; ok {
			orderedResult = append(orderedResult, id)
		}
	}
	return orderedResult
}

func paging(ids []uint64, pageNum, pageSize int) []uint64 {
	offset := (pageNum - 1) * pageSize
	limit := pageSize

	// 左边界
	if offset > len(ids) {
		return nil
	}
	// 右边界
	if (offset + limit) > len(ids) {
		return ids[offset:]
	}

	return ids[offset:(offset + limit)]
}

func questionMarks(length int) string {
	result := make([]string, length)
	for i := range result {
		result[i] = "?"
	}
	return strutil.Join(result, ",")
}

func (client *Client) ListPipelineLabelsByTypeAndTargetIDs(_type apistructs.PipelineLabelType, targetIDs []uint64, ops ...SessionOption) (map[uint64][]spec.PipelineLabel, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var labels []spec.PipelineLabel
	if err := session.Where("type = ?", _type).In("target_id", targetIDs).Find(&labels); err != nil {
		return nil, err
	}

	pipelineLabelsMap := make(map[uint64][]spec.PipelineLabel)
	for _, label := range labels {
		_, ok := pipelineLabelsMap[label.TargetID]
		if !ok {
			pipelineLabelsMap[label.TargetID] = make([]spec.PipelineLabel, 0)
		}
		pipelineLabelsMap[label.TargetID] = append(pipelineLabelsMap[label.TargetID], label)
	}

	return pipelineLabelsMap, nil
}
