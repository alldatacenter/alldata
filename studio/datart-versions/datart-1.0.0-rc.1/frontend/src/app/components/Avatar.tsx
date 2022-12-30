import { Avatar as AntdAvatar, AvatarProps } from 'antd';
import { CSSProperties, useState } from 'react';
import styled from 'styled-components/macro';

export function Avatar(props: AvatarProps) {
  let style: CSSProperties = {};
  let { src, size, ...rest } = props;
  const [safeSrc, setSafeSrc] = useState<any>(src);

  if (typeof size === 'number') {
    style.fontSize = `${size * 0.375}px`;
  }
  if (
    typeof safeSrc === 'string' &&
    (safeSrc.endsWith('null') || safeSrc.endsWith('undefined'))
  ) {
    setSafeSrc('');
  }

  return (
    <StyledAvatar {...rest} src={safeSrc} size={size} style={style}>
      {props.children}
    </StyledAvatar>
  );
}

const StyledAvatar = styled(AntdAvatar)`
  &.ant-avatar {
    color: ${p => p.theme.textColorLight};
    background-color: ${p => p.theme.emphasisBackground};
  }
`;
