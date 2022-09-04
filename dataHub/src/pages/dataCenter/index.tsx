import { dataRouter } from '@router/routers';
import { Menu } from 'antd';
import { useNavigate, useOutlet } from 'react-router';
import styles from './index.module.scss';

const DataCenter:React.FC = props => {
  const Outlet = useOutlet();
  const navigate = useNavigate();
  const onClickMenu = (e: any) => {
    navigate(e.key);
  }
  return (
    <div className={styles['container']}>
      <header>大数据开发治理平台</header>
      <aside >
      <Menu
        mode='inline'
        items={dataRouter}
        onClick={onClickMenu}
        defaultSelectedKeys={['/data-center/']}
        style={{width:250}}
      >
      </Menu>
      </aside>
      <main>
        <div className={styles['content']}>
          {Outlet}
        </div>
        <footer>数据中台</footer>
      </main>
    </div>
  )
}

export default DataCenter