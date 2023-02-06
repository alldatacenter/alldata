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

package db

import "github.com/jinzhu/gorm"

// ImageConfig .
type ImageConfigDB struct {
	*gorm.DB
}

// CreateImage
func (client *ImageConfigDB) CreateImage(image *Image) error {
	return client.Create(image).Error
}

// UpdateImage
func (client *ImageConfigDB) UpdateImage(image *Image) error {
	return client.Save(image).Error
}

// DeleteImage
func (client *ImageConfigDB) DeleteImage(imageID int64) error {
	return client.Where("id = ?", imageID).Delete(&Image{}).Error
}

// GetImagesByRelease Get image list by releaseID
func (client *ImageConfigDB) GetImagesByRelease(releaseID string) ([]Image, error) {
	var images []Image
	if err := client.Where("release_id = ?", releaseID).Find(&images).Error; err != nil {
		return nil, err
	}
	return images, nil
}

// GetImagesByID Get image by imageID
func (client *ImageConfigDB) GetImageByID(imageID int64) (*Image, error) {
	var image Image
	if err := client.Where("id = ?", imageID).Find(&image).Error; err != nil {
		return nil, err
	}
	return &image, nil
}

// GetImageByImage Get image by image string
func (client *ImageConfigDB) GetImageByImage(image string) (*Image, error) {
	var img Image
	if err := client.Where("image = ?", image).First(&img).Error; err != nil {
		return nil, err
	}
	return &img, nil
}

// ListImages Get image list
func (client *ImageConfigDB) ListImages(orgID, pageNo, pageSize int64) (int64, []Image, error) {
	var (
		total  int64
		images []Image
	)
	if orgID == 0 { // verification method Token
		if err := client.Order("updated_at DESC").Offset((pageNo - 1) * pageSize).Limit(pageSize).
			Find(&images).Error; err != nil {
			return 0, nil, err
		}
		if err := client.Model(&Image{}).Count(&total).Error; err != nil {
			return 0, nil, err
		}
	} else {
		if err := client.Where("org_id = ?", orgID).Order("updated_at DESC").
			Offset((pageNo - 1) * pageSize).Limit(pageSize).Find(&images).Error; err != nil {
			return 0, nil, err
		}
		if err := client.Where("org_id = ?", orgID).Model(&Image{}).Count(&total).Error; err != nil {
			return 0, nil, err
		}
	}
	return total, images, nil
}

// GetImageCount Get the quantity not equal releaseID
func (client *ImageConfigDB) GetImageCount(releaseID, image string) (int64, error) {
	var count int64
	if err := client.Where("image = ?", image).Not("release_id", releaseID).
		Model(&Image{}).Count(&count).Error; err != nil {
		return 0, err
	}
	return count, nil
}
