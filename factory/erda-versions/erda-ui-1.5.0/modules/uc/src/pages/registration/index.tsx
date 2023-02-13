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
import { Container, FormInput, history, i18n } from 'src/common';
import { getErrorValid, getValidText } from 'src/common/utils';
import ucStore from 'src/store/uc';

const defaultValid = {
  page: '',
  email: '',
  password: '',
  nickname: '',
  username: '',
  confirmPw: '',
};

export default function Login() {
  const [email, setEmail] = React.useState('');
  // const [phone, setPhone] = React.useState('');
  const [username, setUsername] = React.useState('');
  const [password, setPassword] = React.useState('');
  const [confirmPw, setConfirmPw] = React.useState('');
  const [nickname, setNickname] = React.useState('');
  const [validTips, setValidTips] = React.useState(defaultValid);

  const updateValid = (updateObj: Partial<typeof defaultValid>) => {
    setValidTips((prev) => ({
      ...prev,
      ...updateObj,
    }));
  };

  const updateEmial = (v: string) => {
    setEmail(v);
    updateValid({ email: getValidText(v, 'email') });
  };

  const updateUsername = (v: string) => {
    setUsername(v);
    updateValid({ username: getValidText(v) });
  };

  const updateNick = (v: string) => {
    setNickname(v);
    updateValid({ nickname: getValidText(v) });
  };

  // const updatePhone = (v: string) => {
  //   setPhone(v);
  // };

  const updatePassword = (v: string) => {
    setPassword(v);
    updateValid({ password: getValidText(v, 'password'), confirmPw: '' });
  };

  const updateConfirmPw = (v: string) => {
    setConfirmPw(v);
    updateValid({ confirmPw: v !== password ? i18n.t('inconsistent passwords') : '' });
  };

  const checkValid = () => {
    const { page, ...rest } = validTips;
    return Object.values(rest).filter((item) => !!item).length === 0;
  };

  const handleSubmit = () => {
    if (checkValid() && confirmPw === password) {
      ucStore
        .registration({ email, password, nickname, username })
        .then(() => {
          // registration success, logout
          ucStore.logout();
        })
        .catch((e) => {
          const errRes: UC.IKratosData = e.response?.data;
          updateValid(getErrorValid<typeof defaultValid>(errRes));
        });
    } else {
      updateValid({
        email: getValidText(email, 'email'),
        password: getValidText(password, 'password'),
        username: getValidText(username),
        nickname: getValidText(nickname),
        confirmPw: confirmPw !== password ? i18n.t('inconsistent passwords') : '',
      });
    }
  };

  const goToLogin = () => {
    history.push('/uc/login');
  };

  return (
    <Container>
      <h2 className="text-center text-4xl text-indigo-800 font-display font-semibold lg:text-left xl:text-5xl xl:text-bold">
        {i18n.t('Sign up')}
      </h2>
      <div className="mt-12">
        {validTips.page ? (
          <div className="mb-8">
            <span className="text-red-500 -bottom-6 left-0 text-sm">{validTips.page}</span>
          </div>
        ) : null}
        <FormInput
          label={i18n.t('email')}
          value={email}
          onChange={updateEmial}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('email') })}
          errorTip={validTips.email}
        />

        <FormInput
          label={i18n.t('username')}
          value={username}
          onChange={updateUsername}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('username') })}
          errorTip={validTips.username}
        />

        <FormInput
          label={i18n.t('nick name')}
          value={nickname}
          onChange={updateNick}
          errorTip={validTips.nickname}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('nick name') })}
        />

        {/* <FormInput
          label={i18n.t('mobile')}
          value={phone}
          onChange={updatePhone}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('mobile') })}
          errorTip={phoneValid}
        /> */}

        <FormInput
          label={i18n.t('password')}
          value={password}
          onChange={updatePassword}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('password') })}
          errorTip={validTips.password}
          type="password"
        />

        <FormInput
          label={i18n.t('confirm password')}
          value={confirmPw}
          onChange={updateConfirmPw}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('confirm password') })}
          errorTip={validTips.confirmPw}
          type="password"
        />

        <div className="mt-10">
          <button
            onClick={handleSubmit}
            type="submit"
            className="bg-indigo-500 text-gray-100 p-4 w-full rounded-full tracking-wide font-semibold font-display focus:outline-none focus:shadow-outline hover:bg-indigo-600 shadow-lg"
          >
            {i18n.t('Sign up')}
          </button>
        </div>
        <div className="my-12 text-sm font-display font-semibold text-gray-700 text-center">
          {i18n.t('Already have an account?')}{' '}
          <a className="cursor-pointer text-indigo-600 hover:text-indigo-800" onClick={goToLogin}>
            {i18n.t('Login')}
          </a>
        </div>
      </div>
    </Container>
  );
}
