import React, { memo } from 'react';
import logoPng from 'assets/images/logo-light.png';

export default memo(({ style }:any) => <img style={style} className="logo" src={logoPng} />);
