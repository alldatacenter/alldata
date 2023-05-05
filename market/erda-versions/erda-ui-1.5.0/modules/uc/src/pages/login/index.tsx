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
import { i18n, history, FormInput, Container } from 'src/common';
import { getErrorValid, getValidText } from 'src/common/utils';
import ucStore from 'src/store/uc';
import { parse } from 'query-string';

const defaultValid = {
  page: '',
  identifier: '',
  password: '',
};

export default function Login() {
  const [identifier, setIdentifier] = React.useState('');
  const [password, setPassword] = React.useState('');
  const [validTips, setValidTips] = React.useState(defaultValid);

  const updateValid = (updateObj: Partial<typeof defaultValid>) => {
    setValidTips((prev) => ({
      ...prev,
      ...updateObj,
    }));
  };

  const updateIdentifier = (v: string) => {
    setIdentifier(v);
    updateValid({ identifier: getValidText(v) });
  };

  const updatePassword = (v: string) => {
    setPassword(v);
    updateValid({ password: getValidText(v) });
  };

  const handleSubmit = () => {
    if (identifier && password) {
      ucStore
        .login({ identifier, password })
        .then(() => {
          const query = parse(window.location.search);
          window.location.href = (query?.redirectUrl || '/') as string;
          // history.push('/uc/settings');
        })
        .catch((e) => {
          const errRes: UC.IKratosData = e.response?.data;
          updateValid(getErrorValid<typeof defaultValid>(errRes));
        });
    } else {
      updateValid({
        identifier: getValidText(identifier),
        password: getValidText(password),
      });
    }
  };

  const goToRegistration = () => {
    history.push('/uc/registration');
  };

  return (
    <Container>
      <h2 className="text-center text-4xl text-indigo-800 font-display font-semibold lg:text-left xl:text-5xl xl:text-bold">
        {i18n.t('Welcome to Erda')}
      </h2>
      <div className="mt-12">
        {validTips.page ? (
          <div className="mb-8">
            <span className="text-red-500 -bottom-6 left-0 text-sm">{validTips.page}</span>
          </div>
        ) : null}
        <FormInput
          name="identifier"
          label={`${i18n.t('email')}/${i18n.t('username')}`}
          value={identifier}
          onChange={updateIdentifier}
          placeholder={i18n.t('enter your email/username')}
          errorTip={validTips.identifier}
        />

        <FormInput
          name="password"
          label={i18n.t('password')}
          value={password}
          onChange={updatePassword}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('password') })}
          type="password"
          errorTip={validTips.password}
          // labelExtra={(
          // <div>
          //   <a className="text-xs font-display font-semibold text-indigo-600 hover:text-indigo-800 cursor-pointer">Forgot Password?</a>
          // </div>
          // )}
        />

        {/* <div className="mt-8 flex items-center">
          <input type="checkbox" onChange={handleRember} className="flex items-center" checked={remember} />
          <span className="text-sm font-bold text-gray-700 ml-2 tracking-wide">{i18n.t('keep login')}</span>
        </div> */}

        <div className="mt-10">
          <button
            onClick={handleSubmit}
            type="submit"
            className="bg-indigo-500 text-gray-100 p-4 w-full rounded-full tracking-wide font-semibold font-display focus:outline-none focus:shadow-outline hover:bg-indigo-600 shadow-lg"
          >
            {i18n.t('Login')}
          </button>
        </div>
        <div className="my-12 text-sm font-display font-semibold text-gray-700 pb-2 text-center">
          {i18n.t('Do not have an account?')}{' '}
          <a className="cursor-pointer text-indigo-600 hover:text-indigo-800" onClick={goToRegistration}>
            {i18n.t('Sign up')}
          </a>
        </div>
      </div>
    </Container>
  );
}
