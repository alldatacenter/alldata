// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import i18n from 'i18n';

export const ArtifactsStatusMap = {
  public: {
    name: i18n.t('publisher:released'),
    status: 'success',
  },
  unpublic: {
    name: i18n.t('publisher:withdrawn'),
    status: 'default',
  },
};

export const ArtifactsTypeMap = {
  LIBRARY: {
    name: i18n.t('dop:library/module'),
    value: 'LIBRARY',
  },
  MOBILE: {
    name: i18n.t('dop:mobile app'),
    value: 'MOBILE',
  },
};
