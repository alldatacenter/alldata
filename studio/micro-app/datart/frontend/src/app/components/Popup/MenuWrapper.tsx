import { Menu, MenuProps } from 'antd';
import React, { useCallback } from 'react';

interface MenuWrapperProps extends MenuProps {
  onClose?: () => void;
}

export function MenuWrapper({
  onClose,
  children,
  onClick,
  ...rest
}: MenuWrapperProps) {
  const handleClick = useCallback(
    v => {
      onClick?.(v);
      onClose && onClose();
    },
    [onClose, onClick],
  );

  return (
    <Menu {...rest} onClick={handleClick}>
      {children}
    </Menu>
  );
}
