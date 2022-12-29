import { SearchOutlined } from '@ant-design/icons';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { Helmet } from 'react-helmet-async';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';

export function NotFoundPage() {
  const t = useI18NPrefix('notfound');
  return (
    <Wrapper>
      <Helmet>
        <title>{`404 ${t('title')}`}</title>
        <meta name="description" content={t('title')} />
      </Helmet>
      <h1>
        <SearchOutlined className="icon" />
        404
      </h1>
      <h2>{t('title')}</h2>
    </Wrapper>
  );
}

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: ${p => p.theme.textColorLight};

  h1 {
    position: relative;
    font-size: 128px;
    line-height: 1.2;

    .icon {
      position: absolute;
      top: ${SPACE_TIMES(4)};
      left: ${SPACE_TIMES(-20)};
      font-size: 64px;
    }
  }

  h2 {
    font-size: 48px;
  }
`;
