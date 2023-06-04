import { QuestionCircleOutlined, AppstoreFilled } from '@ant-design/icons';
import { Space } from 'antd';
import React from 'react';
import { SelectLang, useModel, history } from 'umi';
import HeaderSearch from '../HeaderSearch';
import Avatar from './AvatarDropdown';
import styles from './index.less';

export type SiderTheme = 'light' | 'dark';

const GlobalHeaderRight: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  // const { colonyData, setClusterId } = useModel('colonyModel', model => ({ colonyData: model.colonyData, setClusterId: model.setClusterId }));

  if (!initialState || !initialState.settings) {
    return null;
  }
  const getData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')
  // 防止刷新页面数据丢失，应该存放在本地
  // if(history.location.pathname!= '/colony/colonyMg' && history.location.query){
  //   if(history.location.query.clusterId && !(colonyData && colonyData.clusterId) ){
  //     setClusterId(Number(history.location.query.clusterId))
  //   }
  // }
  const { navTheme, layout } = initialState.settings;
  let className = styles.right;

  if ((navTheme === 'dark' && layout === 'top') || layout === 'mix') {
    className = `${styles.right}  ${styles.dark}`;
  }
  return (
    <Space className={className}>
      {
        (getData && getData.clusterId && history.location.pathname!= '/colony/colonyMg') ? (
          <div
            className={`${styles.mgColonyBtn}`}
            onClick={() => {
              // setClusterId(0)
              history.replace('/colony/colonyMg');
            }}
          >
            <AppstoreFilled />
            &nbsp;集群管理【当前集群：{getData.clusterName}】
          </div>
        ):''
      }
      {/* <HeaderSearch
        className={`${styles.action} ${styles.search}`}
        placeholder="站内搜索"
        defaultValue="umi ui"
        options={[
          { label: <a href="https://umijs.org/zh/guide/umi-ui.html">umi ui</a>, value: 'umi ui' },
          {
            label: <a href="next.ant.design">Ant Design</a>,
            value: 'Ant Design',
          },
          {
            label: <a href="https://protable.ant.design/">Pro Table</a>,
            value: 'Pro Table',
          },
          {
            label: <a href="https://prolayout.ant.design/">Pro Layout</a>,
            value: 'Pro Layout',
          },
        ]}
        // onSearch={value => {
        //   console.log('input', value);
        // }}
      /> */}
      {/* <span
        className={styles.action}
        onClick={() => {
          window.open('https://pro.ant.design/docs/getting-started');
        }}
      >
        <QuestionCircleOutlined />
      </span> */}
      <Avatar />
      {/* <SelectLang className={styles.action} /> */}
    </Space>
  );
};
export default GlobalHeaderRight;
