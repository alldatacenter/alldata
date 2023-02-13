/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { FC, memo } from 'react';
import { Helmet } from 'react-helmet-async';
import { useSelector } from 'react-redux';
import { selectPageTitle } from '../slice/selectors';

const HelmetPageTitle: FC<{ lang: string }> = memo(({ lang }) => {
  const pageTitle = useSelector(selectPageTitle);

  return (
    <>
      <Helmet titleTemplate="Datart Share | %s" htmlAttributes={{ lang }}>
        <meta name="description" content="Data Art" />
      </Helmet>
      <Helmet>
        <title>{pageTitle}</title>
      </Helmet>
    </>
  );
});

export default HelmetPageTitle;
