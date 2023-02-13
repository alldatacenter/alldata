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
	"reflect"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"
	"github.com/xormplus/builder"
	"github.com/xormplus/xorm"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
)

// type IntBool bool

// // Value implements the driver.Valuer interface,
// // and turns the IntBool into an integer for MySQL storage.
// func (i IntBool) Value() (driver.Value, error) {
// 	if i {
// 		return 1, nil
// 	}
// 	return 0, nil
// }

// // Scan implements the sql.Scanner interface,
// // and turns the int incoming from MySQL into an IntBool
// func (i *IntBool) Scan(src interface{}) error {
// 	v, ok := src.(int)
// 	if !ok {
// 		return errors.New("bad int type assertion")
// 	}
// 	*i = v == 1
// 	return nil
// }

type OptionType int

const (
	FuzzyMatch OptionType = iota
	ExactMatch
	PrefixMatch
	Contains
	DescOrder
	AscOrder
)

type SelectOption struct {
	Type   OptionType
	Column string
	Value  interface{}
}

type BaseRow struct {
	Id           string          `json:"id" xorm:"not null pk default '' comment('唯一id') VARCHAR(32)"`
	IsDeleted    string          `json:"is_deleted" xorm:"not null default 'N' comment('逻辑删除') VARCHAR(1)"`
	CreateTime   time.Time       `json:"create_time" xorm:"not null default 'CURRENT_TIMESTAMP' comment('创建时间') TIMESTAMP"`
	UpdateTime   time.Time       `json:"update_time" xorm:"not null default 'CURRENT_TIMESTAMP' comment('更新时间') TIMESTAMP"`
	mustCondCols map[string]bool `json:"-" xorm:"-"`
}

type BaseAbility interface {
	SetDeleted()
	SetRecover()
	GetMustCondCols() map[string]bool
	GetPK() map[string]interface{}
}

func (row *BaseRow) GetPK() map[string]interface{} {
	res := map[string]interface{}{}
	res["id"] = row.Id
	return res
}

func (row *BaseRow) SetMustCondCols(cols ...string) {
	if row.mustCondCols == nil {
		row.mustCondCols = map[string]bool{}
	}
	for _, col := range cols {
		row.mustCondCols[col] = true
	}
}

func (row *BaseRow) GetMustCondCols() map[string]bool {
	return row.mustCondCols
}

func (row *BaseRow) BeforeInsert() {

	if len(row.Id) == 0 {
		uuid, err := uuid.NewRandom()
		if err != nil {
			log.Errorf("uuid generate failed:%s", err)
			return
		}
		row.Id = strings.Replace(uuid.String(), "-", "", -1)
	}
	row.IsDeleted = NOT_DELETED_VALUE
	row.CreateTime = time.Now()
	row.UpdateTime = time.Now()
}

func (row *BaseRow) BeforeUpdate() {
	if len(row.IsDeleted) == 0 {
		row.IsDeleted = NOT_DELETED_VALUE
	}
	row.UpdateTime = time.Now()
}

func (row *BaseRow) SetDeleted() {
	row.IsDeleted = IS_DELETED_VALUE
	row.UpdateTime = time.Now()
}

func (row *BaseRow) SetRecover() {
	row.IsDeleted = NOT_DELETED_VALUE
	row.UpdateTime = time.Now()
}

func Insert(engine xorm.Interface, rows BaseAbility) (int64, error) {
	// rowSlice := make([]interface{}, len(rows))
	// for _, row := range rows {
	// 	log.Debugf("%+v", row) // output for debug
	// 	rowSlice = append(rowSlice, row)
	// }
	return engine.Insert(rows)
}

func Distinct(engine xorm.Interface, cols []string, condAndArgs ...interface{}) (*xorm.Session, error) {
	if len(condAndArgs) > 1 {
		cond := condAndArgs[0]
		condArgs := condAndArgs[1:]
		switch cond.(type) {
		case string, map[string]interface{}:
			return engine.Distinct(cols...).Where(cond, condArgs...).And("is_deleted = ?", NOT_DELETED_VALUE), nil
		default:
			return nil, errors.New("invalid cond")
		}
	}
	return engine.Distinct(cols...).Where("is_deleted = ?", NOT_DELETED_VALUE), nil
}

func Desc(engine xorm.Interface, colName string) *xorm.Session {
	return engine.Where("is_deleted = ?", NOT_DELETED_VALUE).Desc(colName)
}

func In(engine xorm.Interface, colName string, colValues interface{}) *xorm.Session {
	return engine.Where("is_deleted = ?", NOT_DELETED_VALUE).In(colName, colValues)
}

func Count(engine xorm.Interface, row BaseAbility, cond interface{}, condArgs ...interface{}) (int64, error) {
	return engine.Where(cond, condArgs...).And("is_deleted = ?", NOT_DELETED_VALUE).Count(row)
}
func Get(engine xorm.Interface, row BaseAbility, cond interface{}, condArgs ...interface{}) (bool, error) {
	return engine.Where(cond, condArgs...).And("is_deleted = ?", NOT_DELETED_VALUE).Get(row)
}

func GetForUpdate(session *xorm.Session, engine *OrmEngine, row BaseAbility, cond string, condArgs ...interface{}) (bool, error) {
	return session.SQL("select * from "+engine.TableName(row)+" where is_deleted = 'N' and "+cond+" for update", condArgs...).Get(row)
}

func GetByAnyI(engine xorm.Interface, bCond builder.Cond, row BaseAbility) (bool, error) {
	return engine.Where(bCond).And("is_deleted = ?", NOT_DELETED_VALUE).Get(row)
}

func GetRawByAnyI(engine xorm.Interface, bCond builder.Cond, row BaseAbility) (bool, error) {
	return engine.Where(bCond).Get(row)
}

func GetByAny(engine *OrmEngine, row BaseAbility, cond BaseAbility) (bool, error) {
	bCond, berr := BuildConds(engine, cond, cond.GetMustCondCols())
	if berr != nil {
		return false, errors.Wrap(berr, "buildConds failed")
	}
	return GetByAnyI(engine, bCond, row)
}

func SelectByAnyI(engine xorm.Interface, bCond builder.Cond, rows interface{}, descColumn ...string) error {
	if len(descColumn) != 0 {
		return engine.Desc(descColumn...).Where(bCond).And("is_deleted = ?", NOT_DELETED_VALUE).Find(rows)
	}
	return engine.Where(bCond).And("is_deleted = ?", NOT_DELETED_VALUE).Find(rows)
}

func DeleteByAnyI(engine xorm.Interface, bCond builder.Cond, row BaseAbility) (int64, error) {
	row.SetDeleted()
	return engine.Cols("is_deleted").Where(bCond).And("is_deleted = ?", NOT_DELETED_VALUE).Update(row)
}

func SelectByAny(engine *OrmEngine, rows interface{}, cond BaseAbility, descColumn ...string) error {
	bCond, berr := BuildConds(engine, cond, cond.GetMustCondCols())
	if berr != nil {
		return errors.Wrap(berr, "buildConds failed")
	}
	return SelectByAnyI(engine, bCond, rows, descColumn...)
}

func SelectPageByAnyI(engine xorm.Interface, bCond builder.Cond, rows interface{}, page *common.Page, descColumn ...string) error {
	if len(descColumn) != 0 {
		return engine.Desc(descColumn...).Where(bCond).And("is_deleted = ?", NOT_DELETED_VALUE).Limit(int(page.GetPageSize()), int(page.GetStartIndex())).Find(rows)
	}
	return engine.Where(bCond).And("is_deleted = ?", NOT_DELETED_VALUE).Limit(int(page.GetPageSize()), int(page.GetStartIndex())).Find(rows)

}

func SelectPageByAny(engine *OrmEngine, rows interface{}, page *common.Page, cond BaseAbility, descColumn ...string) error {
	bCond, berr := BuildConds(engine, cond, cond.GetMustCondCols())
	if berr != nil {
		return errors.Wrap(berr, "buildConds failed")
	}
	return SelectPageByAnyI(engine, bCond, rows, page, descColumn...)
}

func SelectNoCond(engine xorm.Interface, rows interface{}) error {
	return engine.Where("is_deleted = ?", NOT_DELETED_VALUE).Find(rows)
}

func SelectNoCondMissing(engine xorm.Interface, rows interface{}) error {
	return engine.Where("is_deleted = ?", IS_DELETED_VALUE).Find(rows)
}

func Select(engine xorm.Interface, rows interface{}, cond interface{}, condArgs ...interface{}) error {
	return engine.Where(cond, condArgs...).And("is_deleted = ?", NOT_DELETED_VALUE).Find(rows)
}

func SelectPageNoCond(engine xorm.Interface, rows interface{}, page *common.Page) error {
	return engine.Where("is_deleted = ?", NOT_DELETED_VALUE).Limit(int(page.GetPageSize()), int(page.GetStartIndex())).Find(rows)
}

func SelectPage(engine xorm.Interface, rows interface{}, page *common.Page, cond interface{}, condArgs ...interface{}) error {
	return engine.Where(cond, condArgs...).And("is_deleted = ?", NOT_DELETED_VALUE).Limit(int(page.GetPageSize()), int(page.GetStartIndex())).Find(rows)
}

func interfaceSlice(slice interface{}) ([]interface{}, error) {
	v := reflect.ValueOf(slice)
	if v.Kind() != reflect.Slice {
		return nil, errors.Errorf("invalid type:%v", v.Kind())
	}
	var sliceI []interface{}
	num := v.Len()
	for i := 0; i < num; i++ {
		sliceI = append(sliceI, v.Index(i).Interface())
	}
	return sliceI, nil
}

func ParseSelectOptions(options []SelectOption, engine xorm.Interface) xorm.Interface {
	for _, option := range options {
		switch option.Type {
		case FuzzyMatch:
			if valueStr, ok := option.Value.(string); ok {
				engine = engine.Where(option.Column+" like ?", "%"+strings.ReplaceAll(valueStr, "_", `\_`)+"%")
			} else {
				log.Errorf("parse value[%+v] to string failed", option.Value)
			}
		case PrefixMatch:
			if valueStr, ok := option.Value.(string); ok {
				engine = engine.Where(option.Column+" like ?", strings.ReplaceAll(valueStr, "_", `\_`)+"%")
			} else {
				log.Errorf("parse value[%+v] to string failed", option.Value)
			}
		case ExactMatch:
			engine = engine.Where(option.Column+" = ?", option.Value)
		case Contains:
			values, err := interfaceSlice(option.Value)
			if err != nil {
				log.Errorf("parse value[%+v] falied, err:%+v", option.Value, err)
				continue
			}
			if len(values) > 0 {
				engine = engine.In(option.Column, values...)
			}
		case DescOrder:
			engine = engine.Desc(option.Column)
		case AscOrder:
			engine = engine.Asc(option.Column)
		}
	}
	return engine
}

func SelectPageWithOption(options []SelectOption, engine xorm.Interface, rows interface{}, page *common.Page) error {
	return SelectPageNoCond(ParseSelectOptions(options, engine), rows, page)
}

func SelectWithOption(options []SelectOption, engine xorm.Interface, rows interface{}) error {
	return SelectNoCond(ParseSelectOptions(options, engine), rows)
}

func CountWithOption(options []SelectOption, engine xorm.Interface, row BaseAbility) (int64, error) {
	return ParseSelectOptions(options, engine).Where("is_deleted = ?", NOT_DELETED_VALUE).Count(row)
}

func update_cols(engine xorm.Interface, row BaseAbility, columns ...string) *xorm.Session {
	innerCols := []string{"is_deleted", "update_time"}
	if len(columns) != 0 {
		columns = append(columns, innerCols...)
		return engine.Cols(columns...)
	}
	pk := row.GetPK()
	getValue := reflect.Indirect(reflect.ValueOf(row))
	getType := reflect.TypeOf(getValue.Interface())
	count := getType.NumField()
	colSlice := make([]string, len(innerCols), count+len(innerCols)-len(pk))
	copy(colSlice, innerCols)
	for i := 0; i < count; i++ {
		fieldName := getType.Field(i).Name
		if fieldName == "BaseRow" {
			continue
		}
		if _, ok := pk[fieldName]; ok {
			continue
		}
		snakeFieldName := snakeCasedTrans(fieldName)
		if _, ok := pk[snakeFieldName]; ok {
			continue
		}
		colSlice = append(colSlice, snakeFieldName)

	}
	return engine.Cols(strings.Join(colSlice, ","))
	//	return engine.AllCols()
}

func Update(engine xorm.Interface, row BaseAbility, columns ...string) (int64, error) {
	pk := row.GetPK()
	var whereCond []string
	var whereCondArgs []interface{}
	for key, value := range pk {
		whereCond = append(whereCond, key+" = ?")
		whereCondArgs = append(whereCondArgs, value)
	}
	return update_cols(engine, row, columns...).Where(strings.Join(whereCond, " and "), whereCondArgs...).Update(row)
}

func Delete(engine xorm.Interface, row BaseAbility, cond interface{}, condArgs ...interface{}) (int64, error) {
	row.SetDeleted()
	return engine.Cols("is_deleted").Where(cond, condArgs...).And("is_deleted = ?", NOT_DELETED_VALUE).Update(row)
}

func RealDelete(engine xorm.Interface, row BaseAbility, cond interface{}, condArgs ...interface{}) (int64, error) {
	return engine.Where(cond, condArgs...).Delete(row)
}

func Recover(engine xorm.Interface, row BaseAbility, id string) (int64, error) {
	row.SetRecover()
	return engine.Cols("is_deleted").Where("id = ?", id).Update(row)
}

func snakeCasedTrans(name string) string {
	newstr := make([]rune, 0)
	for idx, chr := range name {
		if isUpper := 'A' <= chr && chr <= 'Z'; isUpper {
			if idx > 0 {
				newstr = append(newstr, '_')
			}
			chr -= ('A' - 'a')
		}
		newstr = append(newstr, chr)
	}
	return string(newstr)
}
