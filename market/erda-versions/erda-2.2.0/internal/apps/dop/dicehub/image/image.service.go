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

package image

import (
	"context"
	"strconv"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/dicehub/image/pb"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/image/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/strutil"
)

type imageService struct {
	p  *provider
	db *db.ImageConfigDB
}

func (s *imageService) GetImage(ctx context.Context, req *pb.ImageGetRequest) (*pb.ImageGetResponse, error) {
	_, err := s.getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrListImage.NotLogin()
	}

	imageIDOrImage := req.ImageIDOrImage

	image, err := s.Get(imageIDOrImage)
	if err != nil {
		return nil, apierrors.ErrGetImage.InternalError(err)
	}

	imageGetResponseData, err := s.convertToPB(image)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	return &pb.ImageGetResponse{
		Data: imageGetResponseData,
	}, nil

}

// List Image
func (s *imageService) ListImage(ctx context.Context, req *pb.ImageListRequest) (*pb.ImageListResponse, error) {
	orgID, err := s.getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrListImage.NotLogin()
	}

	if req.PageNum == 0 {
		req.PageNum = 1
	}

	if req.PageSize == 0 {
		req.PageSize = 20
	}
	resp, err := s.List(orgID, req.PageNum, req.PageSize)
	if err != nil {
		return nil, apierrors.ErrListImage.InternalError(err)
	}

	return &pb.ImageListResponse{Data: resp}, nil
}

// Get Image
func (s *imageService) Get(imageIDOrImage string) (*db.Image, error) {
	idOrImage, err := strutil.Atoi64(imageIDOrImage)
	if err == nil {
		image, err := s.db.GetImageByID(idOrImage)
		if err != nil {
			return nil, err
		}
		return image, nil
	}
	image, err := s.db.GetImageByImage(imageIDOrImage)
	if err != nil {
		return nil, err
	}
	return image, nil
}

// List Image
func (s *imageService) List(orgID, pageNo, pageSize int64) (*pb.ImageListResponseData, error) {
	total, images, err := s.db.ListImages(orgID, pageNo, pageSize)
	if err != nil {
		return nil, err
	}

	imgs := make([]*pb.ImageGetResponseData, 0, len(images))
	for _, v := range images {
		data, err := s.convertToPB(&v)
		if err != nil {
			return nil, err
		}
		imgs = append(imgs, data)
	}

	return &pb.ImageListResponseData{
		Total: total,
		List:  imgs,
	}, nil
}

func (s *imageService) convertToPB(image *db.Image) (*pb.ImageGetResponseData, error) {
	return &pb.ImageGetResponseData{
		ID:        int64(image.ID),
		ReleaseID: image.ReleaseID,
		ImageName: image.Image,
		ImageTag:  image.ImageTag,
		Image:     image.Image,
		CreatedAt: timestamppb.New(image.CreatedAt),
		UpdatedAt: timestamppb.New(image.UpdatedAt),
	}, nil
}

func (s imageService) getPermissionHeader(ctx context.Context) (int64, error) {
	orgIDStr := apis.GetOrgID(ctx)
	if orgIDStr == "" {
		return 0, nil
	}
	return strconv.ParseInt(orgIDStr, 10, 64)
}
