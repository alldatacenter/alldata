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

package dao

import (
	"fmt"
	"strings"

	"gorm.io/gorm"
)

var (
	DESC Order = "DESC"
	ASC  Order = "ASC"
)

type Option func(db *gorm.DB) *gorm.DB

type Column interface {
	WhereColumn
	OrderColumn
	SetColumn
}

type WhereColumn interface {
	Is(value interface{}) Option
	In(values ...interface{}) Option
	InMap(values map[interface{}]struct{}) Option
	Like(value interface{}) Option
	GreaterThan(value interface{}) Option
	EqGreaterThan(value interface{}) Option
	LessThan(value interface{}) Option
	EqLessThan(value interface{}) Option
}

type OrderColumn interface {
	DESC() Option
	ASC() Option
}

type SetColumn interface {
	Set(value interface{}) Option
}

func Where(format string, args ...interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(format, args...)
	}
}

func Wheres(m interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(m)
	}
}

func Col(col string) Column {
	return column{col: col}
}

type column struct {
	col string
}

func (w column) Is(value interface{}) Option {
	if value == nil {
		return func(db *gorm.DB) *gorm.DB {
			return db.Where(w.col + " IS NULL")
		}
	}
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(w.col+" = ?", value)
	}
}

func (w column) In(values ...interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(w.col+" IN ?", values)
	}
}

func (w column) InMap(values map[interface{}]struct{}) Option {
	var values_ []interface{}
	for key := range values {
		values_ = append(values_, key)
	}
	return w.In(values_...)
}

func (w column) Like(value interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(w.col+" LIKE ?", value)
	}
}

func (w column) GreaterThan(value interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(w.col+" > ?", value)
	}
}

func (w column) EqGreaterThan(value interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(w.col+" >= ?", value)
	}
}

func (w column) LessThan(value interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(w.col+" < ?", value)
	}
}

func (w column) EqLessThan(value interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(w.col+" <= ?", value)
	}
}

func (w column) DESC() Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Order(w.col + " DESC")
	}
}

func (w column) ASC() Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Order(w.col + " ASC")
	}
}

func (w column) Set(value interface{}) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Update(w.col, value)
	}
}

type WhereValue interface {
	In(cols ...string) Option
}

func Value(value interface{}) whereValue {
	return whereValue{value: value}
}

type whereValue struct {
	value interface{}
}

func (w whereValue) In(cols ...string) Option {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(fmt.Sprintf("? IN (%s)", strings.Join(cols, ",")), w.value)
	}
}

func Paging(size, no int) Option {
	if size < 0 {
		size = 0
	}
	if no < 1 {
		no = 1
	}
	return func(db *gorm.DB) *gorm.DB {
		return db.Limit(size).Offset((no - 1) * size)
	}
}

type Order string

func OrderBy(col string, order Order) Option {
	if !strings.EqualFold(string(order), string(DESC)) &&
		!strings.EqualFold(string(order), string(ASC)) {
		order = "DESC"
	}
	return func(db *gorm.DB) *gorm.DB {
		return db.Order(col + " " + strings.ToUpper(string(order)))
	}
}

func NotSoftDeleted(db *gorm.DB) *gorm.DB {
	return db.Where("soft_deleted_at = ?", 0)
}
