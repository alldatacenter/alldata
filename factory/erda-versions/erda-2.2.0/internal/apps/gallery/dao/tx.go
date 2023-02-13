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
// limitations under the License.o

package dao

import (
	"github.com/pkg/errors"
	"gorm.io/gorm"
)

var q *TX

var InvalidTransactionError = errors.New("invalid transaction, it is already committed or roll backed")

type TX struct {
	Error error

	tx    *gorm.DB
	inTx  bool
	valid bool
}

func Init(db *gorm.DB) {
	if q == nil {
		q = &TX{
			tx:    db,
			valid: true,
		}
	}
}

func Q() *TX {
	if q == nil || q.tx == nil {
		panic("q is not init")
	}
	return &TX{
		tx:    q.tx,
		valid: true,
	}
}

func Begin() *TX {
	return &TX{
		tx:    Q().DB().Begin(),
		valid: true,
		inTx:  true,
	}
}

func (tx *TX) Create(i interface{}) error {
	if tx.inTx && !tx.valid {
		return InvalidTransactionError
	}
	tx.Error = tx.tx.Create(i).Error
	return tx.Error
}

func (tx *TX) CreateInBatches(i interface{}, size int) error {
	if tx.inTx && !tx.valid {
		return InvalidTransactionError
	}
	tx.Error = tx.tx.CreateInBatches(i, size).Error
	return tx.Error
}

func (tx *TX) Delete(i interface{}, options ...Option) error {
	options = append(options, deleteOption{i: i})
	db := options[0].With(tx.tx)
	if tx.Error = db.Error; tx.Error != nil {
		return tx.Error
	}
	if len(options) > 1 {
		for _, opt := range options[1:] {
			db = opt.With(db)
			if tx.Error = db.Error; tx.Error != nil {
				return tx.Error
			}
		}
	}
	return tx.Error
}

func (tx *TX) Updates(i, v interface{}, options ...Option) error {
	options = append(options, updatesOption{i: i, v: v})
	db := options[0].With(tx.tx)
	if tx.Error = db.Error; tx.Error != nil {
		return tx.Error
	}
	if len(options) > 1 {
		for _, opt := range options[1:] {
			db = opt.With(db)
			if tx.Error = db.Error; tx.Error != nil {
				return tx.Error
			}
		}
	}
	return tx.Error
}

func (tx *TX) List(i interface{}, options ...Option) (int64, error) {
	var total int64
	var list = listOption{i: i, total: &total}
	var opts []Option
	for _, opt := range options {
		if paging, ok := opt.(pageOption); ok {
			list.pageSize, list.pageNo = paging.pageSize, paging.pageNo
			continue
		}
		opts = append(opts, opt)
	}
	opts = append(opts, list)
	db := opts[0].With(tx.tx)
	if db.Error != nil {
		if errors.Is(db.Error, gorm.ErrRecordNotFound) {
			return 0, nil
		}
		tx.Error = db.Error
		return 0, tx.Error
	}
	if len(opts) == 1 {
		return total, nil
	}
	for _, opt := range opts[1:] {
		db = opt.With(db)
		if db.Error != nil {
			if errors.Is(db.Error, gorm.ErrRecordNotFound) {
				return 0, nil
			}
			tx.Error = db.Error
			return 0, tx.Error
		}
	}
	return total, nil
}

func (tx *TX) Get(i interface{}, options ...Option) (bool, error) {
	options = append(options, firstOption{i: i})
	db := options[0].With(tx.tx)
	if db.Error != nil {
		if errors.Is(db.Error, gorm.ErrRecordNotFound) {
			return false, nil
		}
		tx.Error = db.Error
		return false, tx.Error
	}
	if len(options) == 1 {
		return true, nil
	}
	for _, opt := range options[1:] {
		db = opt.With(db)
		if db.Error != nil {
			if errors.Is(db.Error, gorm.ErrRecordNotFound) {
				return false, nil
			}
			tx.Error = db.Error
			return false, tx.Error
		}
	}
	return true, nil
}

func (tx *TX) CommitOrRollback() {
	if tx.inTx && !tx.valid {
		return
	}
	if tx.Error == nil {
		tx.tx.Commit()
	} else {
		tx.tx.Rollback()
	}
	tx.valid = false
}

func (tx *TX) DB() *gorm.DB {
	return tx.tx
}
