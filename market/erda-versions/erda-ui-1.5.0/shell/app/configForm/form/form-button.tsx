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

import React, { useContext } from 'react';
import { Context } from './context';

export const Submit = (props: any) => {
  const { onSubmit, Button, text = 'submit', ...rest } = props;
  const context = useContext(Context);
  const { form } = context;
  const click = () => {
    if (form) {
      form.onSubmit(onSubmit);
    }
  };
  const ButtonComp = Button || 'button';
  return (
    <ButtonComp className="dice-form-button" onClick={click} {...rest}>
      {text}
    </ButtonComp>
  );
};

export const Reset = (props: any) => {
  const { onReset, Button, text = 'reset', ...rest } = props;
  const context = useContext(Context);
  const { form } = context;
  const click = () => {
    if (form) {
      form.reset();
    }
    onReset && onReset();
  };

  const ButtonComp = Button || 'button';
  return (
    <ButtonComp className="dice-form-button" onClick={click} {...rest}>
      {text}
    </ButtonComp>
  );
};
