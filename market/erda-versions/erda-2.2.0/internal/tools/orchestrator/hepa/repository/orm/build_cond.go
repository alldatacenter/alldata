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

package orm

import (
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"reflect"
	"strings"
	"time"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"
	"github.com/xormplus/builder"
	"github.com/xormplus/core"
	"github.com/xormplus/xorm"
)

func getFlagForColumn(m map[string]bool, col *core.Column) (val bool, has bool) {
	if len(m) == 0 {
		return false, false
	}

	n := len(col.Name)

	for mk := range m {
		if len(mk) != n {
			continue
		}
		if strings.EqualFold(mk, col.Name) {
			return m[mk], true
		}
	}

	return false, false
}

func BuildConds(ormEngine *OrmEngine, bean interface{}, mustColumnMap map[string]bool) (builder.Cond, error) {
	engine, gerr := ormEngine.GetEngine()
	if gerr != nil {
		return nil, errors.Wrap(gerr, "GetEngine failed")
	}
	table := engine.TableInfo(bean).Table
	var conds []builder.Cond
	for _, col := range table.Columns() {
		if engine.Dialect().DBType() == core.MSSQL && (col.SQLType.Name == core.Text || col.SQLType.IsBlob() || col.SQLType.Name == core.TimeStampz) {
			continue
		}
		if col.SQLType.IsJson() {
			continue
		}

		colName := engine.Quote(col.Name)

		fieldValuePtr, err := col.ValueOf(bean)
		if err != nil {
			if !strings.Contains(err.Error(), "is not valid") {
				log.Warn(err)
			}
			continue
		}

		fieldValue := *fieldValuePtr
		if fieldValue.Interface() == nil {
			continue
		}

		fieldType := reflect.TypeOf(fieldValue.Interface())
		requiredField := false

		if b, ok := getFlagForColumn(mustColumnMap, col); ok {
			if b {
				requiredField = true
			} else {
				continue
			}
		}

		if fieldType.Kind() == reflect.Ptr {
			if fieldValue.IsNil() {
				continue
			} else if !fieldValue.IsValid() {
				continue
			} else {
				// dereference ptr type to instance type
				fieldValue = fieldValue.Elem()
				fieldType = reflect.TypeOf(fieldValue.Interface())
				requiredField = true
			}
		}

		var val interface{}
		switch fieldType.Kind() {
		case reflect.Bool:
			if requiredField || fieldValue.Bool() {
				val = fieldValue.Interface()
			} else {
				// if a bool in a struct, it will not be as a condition because it default is false,
				// please use Where() instead
				continue
			}
		case reflect.String:
			if !requiredField && fieldValue.String() == "" {
				continue
			}
			// for MyString, should convert to string or panic
			if fieldType.String() != reflect.String.String() {
				val = fieldValue.String()
			} else {
				val = fieldValue.Interface()
			}
		case reflect.Int8, reflect.Int16, reflect.Int, reflect.Int32, reflect.Int64:
			if !requiredField && fieldValue.Int() == 0 {
				continue
			}
			val = fieldValue.Interface()
		case reflect.Float32, reflect.Float64:
			if !requiredField && fieldValue.Float() == 0.0 {
				continue
			}
			val = fieldValue.Interface()
		case reflect.Uint8, reflect.Uint16, reflect.Uint, reflect.Uint32, reflect.Uint64:
			if !requiredField && fieldValue.Uint() == 0 {
				continue
			}
			t := int64(fieldValue.Uint())
			val = reflect.ValueOf(&t).Interface()
		case reflect.Struct:
			if fieldType.ConvertibleTo(core.TimeType) {
				t := fieldValue.Convert(core.TimeType).Interface().(time.Time)
				if !requiredField && (t.IsZero() || !fieldValue.IsValid()) {
					continue
				}
				val = formatColTime(engine, col, t)
			} else if _, ok := reflect.New(fieldType).Interface().(core.Conversion); ok {
				continue
			} else if valNul, ok := fieldValue.Interface().(driver.Valuer); ok {
				val, _ = valNul.Value()
				if val == nil {
					continue
				}
			} else {
				if col.SQLType.IsJson() {
					if col.SQLType.IsText() {
						bytes, err := json.Marshal(fieldValue.Interface())
						if err != nil {
							log.Error(err)
							continue
						}
						val = string(bytes)
					} else if col.SQLType.IsBlob() {
						var bytes []byte
						var err error
						bytes, err = json.Marshal(fieldValue.Interface())
						if err != nil {
							log.Error(err)
							continue
						}
						val = bytes
					}
				} else {
					engine.TableInfo(fieldValue.Interface())
					if table, ok := engine.Tables[fieldValue.Type()]; ok {
						if len(table.PrimaryKeys) == 1 {
							pkField := reflect.Indirect(fieldValue).FieldByName(table.PKColumns()[0].FieldName)
							// fix non-int pk issues
							//if pkField.Int() != 0 {
							if pkField.IsValid() && !isZero(pkField.Interface()) {
								val = pkField.Interface()
							} else {
								continue
							}
						} else {
							//TODO: how to handler?
							return nil, fmt.Errorf("not supported %v as %v", fieldValue.Interface(), table.PrimaryKeys)
						}
					} else {
						val = fieldValue.Interface()
					}
				}
			}
		case reflect.Array:
			continue
		case reflect.Slice, reflect.Map:
			if fieldValue == reflect.Zero(fieldType) {
				continue
			}
			if fieldValue.IsNil() || !fieldValue.IsValid() || fieldValue.Len() == 0 {
				continue
			}

			if col.SQLType.IsText() {
				bytes, err := json.Marshal(fieldValue.Interface())
				if err != nil {
					log.Error(err)
					continue
				}
				val = string(bytes)
			} else if col.SQLType.IsBlob() {
				var bytes []byte
				var err error
				if (fieldType.Kind() == reflect.Array || fieldType.Kind() == reflect.Slice) &&
					fieldType.Elem().Kind() == reflect.Uint8 {
					if fieldValue.Len() > 0 {
						val = fieldValue.Bytes()
					} else {
						continue
					}
				} else {
					bytes, err = json.Marshal(fieldValue.Interface())
					if err != nil {
						log.Error(err)
						continue
					}
					val = bytes
				}
			} else {
				continue
			}
		default:
			val = fieldValue.Interface()
		}

		conds = append(conds, builder.Eq{colName: val})
	}

	return builder.And(conds...), nil
}

func formatColTime(engine *xorm.Engine, col *core.Column, t time.Time) (v interface{}) {
	if t.IsZero() {
		if col.Nullable {
			return nil
		}
		return ""
	}

	if col.TimeZone != nil {
		return formatTime(engine, col.SQLType.Name, t.In(col.TimeZone))
	}
	return formatTime(engine, col.SQLType.Name, t.In(engine.DatabaseTZ))
}

// formatTime format time as column type
func formatTime(engine *xorm.Engine, sqlTypeName string, t time.Time) (v interface{}) {
	switch sqlTypeName {
	case core.Time:
		s := t.Format("2006-01-02 15:04:05") //time.RFC3339
		v = s[11:19]
	case core.Date:
		v = t.Format("2006-01-02")
	case core.DateTime, core.TimeStamp:
		v = t.Format("2006-01-02 15:04:05.999")
		if engine.Dialect().DBType() == "sqlite3" {
			v = t.UTC().Format("2006-01-02 15:04:05.999")
		}
	case core.TimeStampz:
		if engine.Dialect().DBType() == core.MSSQL {
			v = t.Format("2006-01-02T15:04:05.9999999Z07:00")
		} else {
			v = t.Format(time.RFC3339Nano)
		}
	case core.BigInt, core.Int:
		v = t.Unix()
	default:
		v = t
	}
	return
}

type zeroable interface {
	IsZero() bool
}

func isZero(k interface{}) bool {
	switch k := k.(type) {
	case int:
		return k == 0
	case int8:
		return k == 0
	case int16:
		return k == 0
	case int32:
		return k == 0
	case int64:
		return k == 0
	case uint:
		return k == 0
	case uint8:
		return k == 0
	case uint16:
		return k == 0
	case uint32:
		return k == 0
	case uint64:
		return k == 0
	case float32:
		return k == 0
	case float64:
		return k == 0
	case bool:
		return !k
	case string:
		return k == ""
	case zeroable:
		return k.IsZero()
	}
	return false
}
