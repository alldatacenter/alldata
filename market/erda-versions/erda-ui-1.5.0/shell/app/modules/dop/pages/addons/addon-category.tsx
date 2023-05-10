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

import React from 'react';
import { AddonCardList } from 'addonPlatform/pages/common/components/addon-card-list';
import i18n from 'i18n';
import dopStore from 'dop/stores';
import { useLoading } from 'core/stores/loading';
import { useEffectOnce } from 'react-use';

const AddonCategory = () => {
  const addonCategory = dopStore.useStore((s) => s.addonCategory);
  const [loading] = useLoading(dopStore, ['getDopAddons']);
  useEffectOnce(() => {
    dopStore.getDopAddons();

    return () => dopStore.clearDopAddons();
  });
  return (
    <AddonCardList
      addonCategory={addonCategory}
      isFetching={loading}
      searchProps={['projectName', 'applicationName']}
      searchPlaceHolder={i18n.t('dop:search by owned project')}
    />
  );
};

const AddonCategoryWrapper = AddonCategory;

export { AddonCategoryWrapper as AddonCategory };
