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

package linters

import (
	"bytes"
	"fmt"
	"strconv"
	"strings"

	"github.com/pingcap/parser/ast"
	"github.com/pingcap/parser/mysql"
	"github.com/pingcap/parser/types"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

var columnTypes = map[string]byte{
	"decimal":    mysql.TypeDecimal,
	"tiny":       mysql.TypeTiny,
	"short":      mysql.TypeShort,
	"long":       mysql.TypeLong,
	"float":      mysql.TypeFloat,
	"double":     mysql.TypeDouble,
	"null":       mysql.TypeNull,
	"timestamp":  mysql.TypeTimestamp,
	"longlong":   mysql.TypeLonglong,
	"int24":      mysql.TypeInt24,
	"date":       mysql.TypeDate,
	"duration":   mysql.TypeDuration,
	"time":       mysql.TypeDuration,
	"datetime":   mysql.TypeDatetime,
	"year":       mysql.TypeYear,
	"newdate":    mysql.TypeNewDate,
	"varchar":    mysql.TypeVarchar,
	"bit":        mysql.TypeBit,
	"json":       mysql.TypeJSON,
	"newdecimal": mysql.TypeNewDecimal,
	"enum":       mysql.TypeEnum,
	"set":        mysql.TypeSet,
	"tinyblob":   mysql.TypeTinyBlob,
	"mediumblob": mysql.TypeMediumBlob,
	"longblob":   mysql.TypeLongBlob,
	"blob":       mysql.TypeBlob,
	"varstring":  mysql.TypeVarString,
	"char":       mysql.TypeString,
	"string":     mysql.TypeString,
	"geometry":   mysql.TypeGeometry,
}

var columnFlags = map[string]uint{
	"NotNullFlag":           mysql.NotNullFlag,
	"PriKeyFlag":            mysql.PriKeyFlag,
	"UniqueKeyFlag":         mysql.UniqueKeyFlag,
	"MultipleKeyFlag":       mysql.MultipleKeyFlag,
	"BlobFlag":              mysql.BlobFlag,
	"UnsignedFlag":          mysql.UnsignedFlag,
	"ZerofillFlag":          mysql.ZerofillFlag,
	"BinaryFlag":            mysql.BinaryFlag,
	"EnumFlag":              mysql.EnumFlag,
	"AutoIncrementFlag":     mysql.AutoIncrementFlag,
	"TimestampFlag":         mysql.TimestampFlag,
	"SetFlag":               mysql.SetFlag,
	"NoDefaultValueFlag":    mysql.NoDefaultValueFlag,
	"OnUpdateNowFlag":       mysql.OnUpdateNowFlag,
	"PartKeyFlag":           mysql.PartKeyFlag,
	"NumFlag":               mysql.NumFlag,
	"GroupFlag":             mysql.GroupFlag,
	"UniqueFlag":            mysql.UniqueFlag,
	"BinCmpFlag":            mysql.BinCmpFlag,
	"ParseToJSONFlag":       mysql.ParseToJSONFlag,
	"IsBooleanFlag":         mysql.IsBooleanFlag,
	"PreventNullInsertFlag": mysql.PreventNullInsertFlag,
}

type columnTypeLinter struct {
	baseLinter
	meta columnTypeLinterMeta
	c    sqllint.Config
}

// ColumnTypeLinter check column's type
func (hub) ColumnTypeLinter(s script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var meta columnTypeLinterMeta
	if err := yaml.Unmarshal(c.Meta, &meta); err != nil || meta.ColumnName == "" {
		return nil, errors.Wrap(err, "failed to parse ColumnTypeLinter.meta, please reconfigure\n")
	}
	if meta.ColumnName == "" {
		return nil, errors.Errorf("ColumnTypeLinter.meta.columnName can not be empty, please reconfigure")
	}
	if len(meta.Types) == 0 {
		return nil, errors.Errorf("there is no type confiured in ColumnTypeLinter.meta, please reconfigure")
	}
	for _, typ := range meta.Types {
		if _, ok := columnTypes[strings.ToLower(typ.Type)]; !ok {
			var typeList []string
			for k := range columnTypes {
				typeList = append(typeList, k)
			}
			return nil, errors.Errorf("unsupport type in ColumnTypeLinter.meta: %s. please refer the following list and reconfigure:\n%v", typ.Type, typeList)
		}
		for _, flag := range typ.Flags {
			var flagsList []string
			for k := range columnFlags {
				flagsList = append(flagsList, k)
			}
			return nil, errors.Errorf("unsupport ColumnTypeLinter.meta flag: %s, please refer the following list and reconfigure:\n%v", flag, flagsList)
		}
	}

	return &columnTypeLinter{
		baseLinter: newBaseLinter(s),
		meta:       meta,
		c:          c,
	}, nil
}

func (l *columnTypeLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	createStmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	var col *ast.ColumnDef
	for _, item := range createStmt.Cols {
		if item.Name != nil && item.Name.OrigColName() == l.meta.ColumnName {
			col = item
			break
		}
	}
	if col == nil {
		return in, false
	}

	for _, typ := range l.meta.Types {
		tp := columnTypes[strings.ToLower(typ.Type)]
		ft := types.NewFieldType(tp)
		if typ.Flen != nil {
			ft.Flen = *typ.Flen
		}
		for _, flag := range typ.Flags {
			ft.Flag |= columnFlags[flag]
		}
		if typ.Decimal != nil {
			ft.Decimal = *typ.Decimal
		}

		if ft.Tp == col.Tp.Tp &&
			(typ.Flen == nil || ft.Flen == col.Tp.Flen) &&
			(len(typ.Flags) == 0 || ft.Flag == col.Tp.Flag) &&
			(typ.Decimal == nil || ft.Decimal == col.Tp.Decimal) {
			return in, false
		}
	}
	find := func(line []byte) bool {
		return bytes.Contains(bytes.ToLower(line), []byte(col.Name.OrigColName()))
	}
	l.err = linterror.New(l.s, l.text, fmt.Sprintf("column type not in the configuration，see more at：%s", strconv.Quote(l.c.Alias)), find)
	return in, false
}

func (l *columnTypeLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (l *columnTypeLinter) Error() error {
	return l.err
}

type columnTypeLinterMeta struct {
	ColumnName string                      `json:"columnName" yaml:"columnName"`
	Types      []columnTypeLinterMetaTypes `json:"types" yaml:"types"`
}

type columnTypeLinterMetaTypes struct {
	Type    string   `json:"type" yaml:"type"`
	Flags   []string `json:"flag" yaml:"flag"`
	Flen    *int     `json:"flen" yaml:"flen"`
	Decimal *int     `json:"decimal" yaml:"decimal"`
}
