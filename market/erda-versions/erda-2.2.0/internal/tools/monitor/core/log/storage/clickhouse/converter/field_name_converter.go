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

package converter

import (
	"fmt"
	"strings"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

type FieldNameConverter interface {
	Convert(field string) string
}

func NewFieldNameConverter(tableMeta *loader.TableMeta, fieldNameMapper map[string]string) FieldNameConverter {
	return &defaultFieldNameConverter{
		tableMeta:       tableMeta,
		fieldNameMapper: fieldNameMapper,
	}
}

type defaultFieldNameConverter struct {
	tableMeta       *loader.TableMeta
	fieldNameMapper map[string]string
}

func (c *defaultFieldNameConverter) Convert(field string) string {
	return convertUnknownField(c.tableMeta, c.fieldNameMapper, field)
}

func convertUnknownField(tableMeta *loader.TableMeta, fieldNameMapper map[string]string, field string) string {
	if tableMeta == nil {
		return field
	}
	_, ok := tableMeta.Columns[field]
	if ok {
		return field
	}

	if mapperField, ok := fieldNameMapper[field]; ok && tableMeta.Columns[mapperField] != nil {
		return mapperField
	}

	splits := strings.SplitN(field, ".", 2)
	if len(splits) != 2 {
		return field
	}
	prefixCol, ok := tableMeta.Columns[splits[0]]
	if !ok {
		return field
	}
	switch prefixCol.Type {
	case loader.MapStringString:
		return fmt.Sprintf("%s['%s']", splits[0], splits[1])
	case loader.String:
		return fmt.Sprintf("JSONExtractString(%s,'%s')", splits[0], splits[1])
	default:
		return field
	}
}
