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
import ucStore from 'src/store/uc';
import { getValidText, getErrorValid } from 'src/common/utils';
import { FormInput, Container, i18n } from 'src/common';

const defaultValid = {
  pageInfo: '',
  pagePassword: '',
  email: '',
  password: '',
  username: '',
  nickname: '',
  confirmPw: '',
  page: '',
};

export default function Setting() {
  const user = ucStore.useStore((s) => s.user);
  const [email, setEmail] = React.useState(user?.email);
  // const [phone, setPhone] = React.useState(user?.phone);
  const [username, setUsername] = React.useState('');
  const [password, setPassword] = React.useState('');
  const [confirmPw, setConfirmPw] = React.useState('');
  const [nickname, setNick] = React.useState(user?.nickname);
  const [validTips, setValidTips] = React.useState(defaultValid);

  React.useEffect(() => {
    if (!user?.id) {
      ucStore.whoAmI();
    } else {
      setEmail(user.email);
      setNick(user.nickname);
      setUsername(user.username);
    }
  }, [user]);

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
    setNick(v);
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

  const submitInfo = () => {
    if (checkValid() && nickname && email && username) {
      ucStore.updateUser({ email, nickname, username }).catch((e) => {
        const errRes: UC.IKratosData = e.response?.data;
        const errTips = getErrorValid<typeof defaultValid>(errRes);
        updateValid({ ...errTips, pageInfo: errTips.page });
      });
    } else {
      updateValid({
        email: getValidText(email, 'email'),
        nickname: getValidText(nickname),
        username: getValidText(username),
      });
    }
  };

  const checkValid = () => {
    const { page, pageInfo, pagePassword, password, ...rest } = validTips;
    return Object.values(rest).filter((item) => !!item).length === 0;
  };

  const submitPassword = () => {
    if (!validTips.password && password && password === confirmPw) {
      ucStore
        .updatePassword(password)
        .then(() => {
          goToLogout();
        })
        .catch((e) => {
          const errRes: UC.IKratosData = e.response?.data;
          const errTips = getErrorValid<typeof defaultValid>(errRes);
          updateValid({ ...errTips, pagePassword: errTips.page });
        });
    } else {
      updateValid({
        password: getValidText(password, 'password'),
        confirmPw: confirmPw !== password ? i18n.t('inconsistent passwords') : '',
      });
    }
  };

  const goToLogout = () => {
    ucStore.logout();
  };

  return (
    <Container>
      <h2 className="text-center text-4xl text-indigo-800 font-display font-semibold lg:text-left xl:text-5xl xl:text-bold">
        {i18n.t('Hello {name}', { name: user?.nickname })}
      </h2>
      <div className="mt-12">
        {validTips.pageInfo ? (
          <div className="mb-8">
            <span className="text-red-500 -bottom-6 left-0 text-sm">{validTips.pageInfo}</span>
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
          errorTip={validTips.username}
          onChange={updateUsername}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('username') })}
        />

        <FormInput
          label={i18n.t('nick name')}
          value={nickname}
          errorTip={validTips.nickname}
          onChange={updateNick}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('nick name') })}
        />

        {/* <FormInput
          label={i18n.t('mobile')}
          value={phone}
          onChange={updatePhone}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('mobile') })}
          errorTip={phoneValid}
        /> */}

        <div className="mt-10">
          <button
            onClick={submitInfo}
            type="submit"
            className="bg-indigo-500 text-gray-100 p-4 w-full rounded-full tracking-wide font-semibold font-display focus:outline-none focus:shadow-outline hover:bg-indigo-600 shadow-lg"
          >
            {i18n.t('Update')}
          </button>
        </div>

        {validTips.pagePassword ? (
          <div className="my-8">
            <span className="text-red-500 -bottom-6 left-0 text-sm">{validTips.pagePassword}</span>
          </div>
        ) : null}

        <FormInput
          label={i18n.t('new password')}
          value={password}
          onChange={updatePassword}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('new password') })}
          errorTip={validTips.password}
          type="password"
        />

        <FormInput
          label={i18n.t('confirm new password')}
          value={confirmPw}
          onChange={updateConfirmPw}
          placeholder={i18n.t('enter your {name}', { name: i18n.t('confirm new password') })}
          errorTip={validTips.confirmPw}
          type="password"
        />

        <div className="mt-10">
          <button
            onClick={submitPassword}
            type="submit"
            className="bg-indigo-500 text-gray-100 p-4 w-full rounded-full tracking-wide font-semibold font-display focus:outline-none focus:shadow-outline hover:bg-indigo-600 shadow-lg"
          >
            {i18n.t('Reset password')}
          </button>
        </div>

        <div className="my-12 text-sm font-display font-semibold text-gray-700 text-center">
          <a className="cursor-pointer text-indigo-600 hover:text-indigo-800" onClick={goToLogout}>
            {i18n.t('Logout')}
          </a>
        </div>
      </div>
    </Container>
  );
}
