/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package metriccache

import (
	"fmt"
	"time"

	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type storage struct {
	db *gorm.DB
}

func NewStorage() (*storage, error) {
	return newStorage("file::memory:?mode=memory&cache=shared&loc=auto&_busy_timeout=5000")
}
func newStorage(dsn string) (*storage, error) {
	db, err := gorm.Open(sqlite.Open(dsn),
		&gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("fail to create database, %v", err)
	}

	db.AutoMigrate(&nodeResourceMetric{}, &podResourceMetric{}, &containerResourceMetric{}, &beCPUResourceMetric{})
	db.AutoMigrate(&rawRecord{})
	db.AutoMigrate(&podThrottledMetric{}, &containerThrottledMetric{})
	db.AutoMigrate(&containerCPIMetric{}, &containerPSIMetric{}, &podPSIMetric{})

	database, err := db.DB()
	if err != nil {
		return nil, fmt.Errorf("fail to init database %v", err)
	}
	database.SetMaxOpenConns(1)

	s := &storage{
		db: db,
	}
	return s, nil
}

// for ut only, Close() is not necessary for gorm
func (s *storage) Close() error {
	d, err := s.db.DB()
	if err != nil {
		return err
	}
	return d.Close()
}

func (s *storage) InsertNodeResourceMetric(n *nodeResourceMetric) error {
	return s.db.Create(n).Error
}

func (s *storage) InsertPodResourceMetric(p *podResourceMetric) error {
	return s.db.Create(p).Error
}

func (s *storage) InsertContainerResourceMetric(m *containerResourceMetric) error {
	return s.db.Create(m).Error
}

func (s *storage) InsertBECPUResourceMetric(b *beCPUResourceMetric) error {
	return s.db.Create(b).Error
}

// InsertRawRecord inserts a raw record into the db
func (s *storage) InsertRawRecord(record *rawRecord) error {
	return s.db.Clauses(clause.OnConflict{
		UpdateAll: true,
	}).Create(&record).Error
}

func (s *storage) InsertPodThrottledMetric(m *podThrottledMetric) error {
	return s.db.Create(m).Error
}

func (s *storage) InsertContainerThrottledMetric(m *containerThrottledMetric) error {
	return s.db.Create(m).Error
}

func (s *storage) InsertContainerCPIMetric(m *containerCPIMetric) error {
	return s.db.Create(m).Error
}

func (s *storage) InsertContainerPSIMetric(m *containerPSIMetric) error {
	return s.db.Create(m).Error
}

func (s *storage) InsertPodPSIMetric(m *podPSIMetric) error {
	return s.db.Create(m).Error
}

func (s *storage) GetNodeResourceMetric(start, end *time.Time) ([]nodeResourceMetric, error) {
	var nodeMetrics []nodeResourceMetric
	err := s.db.Where("timestamp BETWEEN ? AND ? order by timestamp", start, end).Find(&nodeMetrics).Error
	return nodeMetrics, err
}

func (s *storage) GetPodResourceMetric(uid *string, start, end *time.Time) ([]podResourceMetric, error) {
	var podMetrics []podResourceMetric
	err := s.db.Where("pod_uid = ? AND timestamp BETWEEN ? AND ?", uid, start, end).Find(&podMetrics).Error
	return podMetrics, err
}

func (s *storage) GetContainerResourceMetric(containerID *string, start, end *time.Time) (
	[]containerResourceMetric, error) {
	var metrics []containerResourceMetric
	err := s.db.Where("container_id = ? AND timestamp BETWEEN ? AND ?", containerID, start, end).Find(
		&metrics).Error
	return metrics, err
}

func (s *storage) GetBECPUResourceMetric(start, end *time.Time) ([]beCPUResourceMetric, error) {
	var metrics []beCPUResourceMetric
	err := s.db.Where("timestamp BETWEEN ? AND ?", start, end).Find(&metrics).Error
	return metrics, err
}

func (s *storage) GetRawRecord(recordName string) (*rawRecord, error) {
	record := &rawRecord{}
	err := s.db.Where("record_type = ?", recordName).First(&record).Error
	return record, err
}

func (s *storage) GetPodThrottledMetric(uid *string, start, end *time.Time) ([]podThrottledMetric, error) {
	var metrics []podThrottledMetric
	err := s.db.Where("pod_uid = ? AND timestamp BETWEEN ? AND ?", uid, start, end).Find(&metrics).Error
	return metrics, err
}

func (s *storage) GetContainerThrottledMetric(id *string, start, end *time.Time) ([]containerThrottledMetric, error) {
	var metrics []containerThrottledMetric
	err := s.db.Where("container_id = ? AND timestamp BETWEEN ? AND ?", id, start, end).Find(&metrics).Error
	return metrics, err
}

func (s *storage) GetContainerCPIMetric(containerID *string, start, end *time.Time) ([]containerCPIMetric, error) {
	var metrics []containerCPIMetric
	err := s.db.Where("container_id = ? AND timestamp BETWEEN ? AND ?", containerID, start, end).Find(&metrics).Error
	return metrics, err
}

func (s *storage) GetContainerPSIMetric(containerID *string, start, end *time.Time) ([]containerPSIMetric, error) {
	var metrics []containerPSIMetric
	err := s.db.Where("container_id = ? AND timestamp BETWEEN ? AND ?", containerID, start, end).Find(&metrics).Error
	return metrics, err
}

func (s *storage) GetPodPSIMetric(uid *string, start, end *time.Time) ([]podPSIMetric, error) {
	var metrics []podPSIMetric
	err := s.db.Where("pod_uid = ? AND timestamp BETWEEN ? AND ?", uid, start, end).Find(&metrics).Error
	return metrics, err
}

func (s *storage) GetContainerCPIMetricByPodUid(podUid *string, start, end *time.Time) ([]containerCPIMetric, error) {
	var metrics []containerCPIMetric
	err := s.db.Where("pod_uid = ? AND timestamp BETWEEN ? AND ?", podUid, start, end).Find(&metrics).Error
	return metrics, err
}

func (s *storage) DeleteNodeResourceMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&nodeResourceMetric{}).Error
}

func (s *storage) DeletePodResourceMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&podResourceMetric{}).Error
}

func (s *storage) DeleteContainerResourceMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&containerResourceMetric{}).Error
}

func (s *storage) DeleteBECPUResourceMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&beCPUResourceMetric{}).Error
}

func (s *storage) DeletePodThrottledMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&podThrottledMetric{}).Error
}

func (s *storage) DeleteContainerThrottledMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&containerThrottledMetric{}).Error
}

func (s *storage) DeleteContainerCPIMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&containerCPIMetric{}).Error
}

func (s *storage) DeleteContainerPSIMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&containerPSIMetric{}).Error
}

func (s *storage) DeletePodPSIMetric(start, end *time.Time) error {
	return s.db.Where("timestamp BETWEEN ? AND ?", start, end).Delete(&podPSIMetric{}).Error
}

func (s *storage) CountNodeResourceMetric() (int64, error) {
	count := int64(0)
	err := s.db.Model(&nodeResourceMetric{}).Count(&count).Error
	return count, err
}

func (s *storage) CountPodResourceMetric() (int64, error) {
	count := int64(0)
	err := s.db.Model(&podResourceMetric{}).Count(&count).Error
	return count, err
}

func (s *storage) CountContainerResourceMetric() (int64, error) {
	count := int64(0)
	err := s.db.Model(&containerResourceMetric{}).Count(&count).Error
	return count, err
}

func (s *storage) CountBECPUResourceMetric() (int64, error) {
	count := int64(0)
	err := s.db.Model(&beCPUResourceMetric{}).Count(&count).Error
	return count, err
}

func (s *storage) CountPodThrottledMetric() (int64, error) {
	count := int64(0)
	err := s.db.Model(&podThrottledMetric{}).Count(&count).Error
	return count, err
}

func (s *storage) CountContainerThrottledMetric() (int64, error) {
	count := int64(0)
	err := s.db.Model(&containerThrottledMetric{}).Count(&count).Error
	return count, err
}
