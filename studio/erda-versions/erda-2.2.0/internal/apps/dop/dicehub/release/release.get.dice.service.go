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

package release

import (
	"context"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/release/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
)

type releaseGetDiceService struct {
	p  *provider
	db *db.ReleaseConfigDB
}

func (s *releaseGetDiceService) PullDiceYAML(ctx context.Context, req *pb.ReleaseGetDiceYmlRequest) (*pb.ReleaseGetDiceYmlResponse, error) {
	return s.GetDiceYAML(ctx, req)
}

func (s *releaseGetDiceService) GetDiceYAML(ctx context.Context, req *pb.ReleaseGetDiceYmlRequest) (*pb.ReleaseGetDiceYmlResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrGetYAML.NotLogin()
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrGetYAML.NotLogin()
	}

	if orgID == 0 && !identityInfo.IsInternalClient() {
		return nil, apierrors.ErrGetYAML.NotLogin()
	}

	releaseID := req.ReleaseID
	if releaseID == "" {
		logrus.Warn("Param [ReleaseID] is missing when get release info.")
		return nil, apierrors.ErrGetYAML.MissingParameter("releaseID")
	}

	logrus.Infof("getting dice.yml...releaseId: %s\n", releaseID)

	diceYAML, err := s.GetDiceYAMLData(orgID, releaseID)
	if err != nil {
		return nil, apierrors.ErrGetYAML.InvalidState("release not found")
	}
	return &pb.ReleaseGetDiceYmlResponse{Data: diceYAML}, nil
}

// GetDiceYAML Get dice.yml context
func (r *releaseGetDiceService) GetDiceYAMLData(orgID int64, releaseID string) (string, error) {
	release, err := r.db.GetRelease(releaseID)
	if err != nil {
		return "", err
	}
	if orgID != 0 && release.OrgID != orgID { // When calling internally，orgID is 0
		return "", errors.Errorf("release not found")
	}

	return release.Dice, nil
}
