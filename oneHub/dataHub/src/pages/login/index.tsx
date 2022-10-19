import { useNavigate } from 'react-router';
import styles from './index.module.scss';

const Login:React.FC = () => {
  const navigate = useNavigate();
  return (
    <div className={styles['container']}>
      <div onClick={() => navigate('/data-center')}>欢迎使用</div>
    </div>
    
  )
}

export default Login;