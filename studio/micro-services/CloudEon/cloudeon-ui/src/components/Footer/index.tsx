import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import { useIntl } from 'umi';

const Footer: React.FC = () => {
  const intl = useIntl();
  const defaultMessage = intl.formatMessage({
    id: 'app.copyright.produced',
    defaultMessage: 'CloudEon',
  });

  const currentYear = new Date().getFullYear();

  return (
    <div></div>
    // <DefaultFooter
    //   copyright={`${currentYear} ${defaultMessage}`}
    //   links={[
    //     {
    //       key: 'CloudEon',
    //       title: 'CloudEon',
    //       href: 'https://github.com/dromara/CloudEon',
    //       blankTarget: true,
    //     },
    //     {
    //       key: 'github',
    //       title: <GithubOutlined />,
    //       href: 'https://github.com/dromara/CloudEon',
    //       blankTarget: true,
    //     },
    //     // {
    //     //   key: 'Ant Design',
    //     //   title: 'Ant Design',
    //     //   href: 'https://ant.design',
    //     //   blankTarget: true,
    //     // },
    //   ]}
    // />
  );
};

export default Footer;
