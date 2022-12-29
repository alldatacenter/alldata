import { ReactElement } from 'react';

interface AuthorizedProps {
  authority: boolean | undefined;
  denied?: ReactElement | null;
  children?: ReactElement | null;
}

export function Authorized({
  authority,
  denied = null,
  children = null,
}: AuthorizedProps) {
  return authority ? children : denied;
}
