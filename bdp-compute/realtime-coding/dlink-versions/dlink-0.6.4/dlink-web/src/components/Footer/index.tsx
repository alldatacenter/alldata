import { useIntl } from 'umi';
import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-layout';

export default () => {
  const intl = useIntl();
  const defaultMessage = intl.formatMessage({
    id: 'app.copyright.produced',
    defaultMessage: 'AllData',
  });

  return (
    <DefaultFooter
      copyright={`2022 ${defaultMessage}`}
      links={[
        {
          key: 'AllData',
          title: 'AllData',
          href: '',
          blankTarget: true,
        },
        {
          key: 'github',
          title: <GithubOutlined />,
          href: 'https://github.com/authorwlh/alldata',
          blankTarget: true,
        },
      ]}
    />
  );
};
