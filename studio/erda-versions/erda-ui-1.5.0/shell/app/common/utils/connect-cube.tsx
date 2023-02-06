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

interface IMapper<P, M> {
  (props: Omit<P, keyof M>): M;
}

interface IConnectComp<P> {
  (p: P): JSX.Element;
}

export function connectCube<P, M>(Comp: IConnectComp<P> | React.ComponentType<P>, mapper: IMapper<P, M>) {
  return (props: Omit<P, keyof M>) => {
    const storeProps = mapper(props);
    const combinedProps = { ...props, ...storeProps } as any;
    return <Comp {...combinedProps} />;
  };
}
