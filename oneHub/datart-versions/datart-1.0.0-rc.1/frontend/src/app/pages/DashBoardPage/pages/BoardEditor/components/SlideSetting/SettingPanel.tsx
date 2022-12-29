import { ReactElement } from 'react';
import styled from 'styled-components/macro';
import { SPACE_LG, SPACE_MD } from 'styles/StyleConstants';
import { stopPPG } from 'utils/utils';

interface SettingPanelProps {
  title?: string;
  children?: ReactElement;
}

export function SettingPanel({ title, children }: SettingPanelProps) {
  return (
    <Wrapper>
      <div onClick={stopPPG} className="form-wrapper">
        {children}
      </div>
    </Wrapper>
  );
}

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: ${SPACE_MD};

  .form-wrapper {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
    overflow-y: auto;
  }
`;

export const Group = styled.div`
  padding: 0 ${SPACE_LG};
`;
