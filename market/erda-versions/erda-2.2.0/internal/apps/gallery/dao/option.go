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
	"strings"

	"gorm.io/gorm"
)

type Option interface {
	With(db *gorm.DB) *gorm.DB
}

func WhereOption(format string, args ...interface{}) Option {
	return whereOption{format: format, args: args}
}

func MapOption(m map[string]interface{}) Option {
	return mapOption{m: m}
}

func ByIDOption(id interface{}) Option {
	return byIDOption{id: id}
}

func InOption(col string, values map[interface{}]struct{}) Option {
	return inOption{col: col, values: values}
}

func PageOption(pageSize, pageNo int) Option {
	if pageSize < 0 {
		pageSize = 0
	}
	if pageNo < 1 {
		pageNo = 1
	}
	return pageOption{
		pageNo:   pageNo,
		pageSize: pageSize,
	}
}

func OrderByOption(col string, order string) Option {
	if !strings.EqualFold(order, "desc") && !strings.EqualFold(order, "acs") {
		order = "desc"
	}
	return orderByOption{
		col:   col,
		order: order,
	}
}

type whereOption struct {
	format string
	args   []interface{}
}

func (o whereOption) With(db *gorm.DB) *gorm.DB {
	return db.Where(o.format, o.args...)
}

type mapOption struct {
	m map[string]interface{}
}

func (o mapOption) With(db *gorm.DB) *gorm.DB {
	return db.Where(o.m)
}

type byIDOption struct {
	id interface{}
}

func (o byIDOption) With(db *gorm.DB) *gorm.DB {
	return db.Where("id = ?", o.id)
}

type inOption struct {
	col    string
	values map[interface{}]struct{}
}

func (o inOption) With(db *gorm.DB) *gorm.DB {
	var keys []interface{}
	for k := range o.values {
		keys = append(keys, k)
	}
	return db.Where(o.col+" IN (?)", keys)
}

type pageOption struct {
	pageNo, pageSize int
}

func (o pageOption) With(db *gorm.DB) *gorm.DB {
	return db.Limit(o.pageSize).Offset((o.pageNo - 1) * o.pageSize)
}

func (o pageOption) PageOption() {}

type orderByOption struct {
	col, order string
}

func (o orderByOption) With(db *gorm.DB) *gorm.DB {
	return db.Order(o.col + " " + strings.ToUpper(o.order))
}

type deleteOption struct {
	i interface{}
}

func (o deleteOption) With(db *gorm.DB) *gorm.DB {
	return db.Delete(o.i)
}

type updatesOption struct {
	i interface{}
	v interface{}
}

func (o updatesOption) With(db *gorm.DB) *gorm.DB {
	return db.Model(o.i).Updates(o.v)
}

type listOption struct {
	i        interface{}
	total    *int64
	pageSize int
	pageNo   int
}

func (o listOption) With(db *gorm.DB) *gorm.DB {
	db.Statement.Dest = o.i
	if o.pageNo > 0 && o.pageSize > 0 {
		return db.Count(o.total).Limit(o.pageSize).Offset((o.pageNo - 1) * o.pageSize).Find(o.i)
	}
	return db.Count(o.total).Find(o.i)
}

type firstOption struct {
	i interface{}
}

func (o firstOption) With(db *gorm.DB) *gorm.DB {
	return db.First(o.i)
}
