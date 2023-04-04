import {
  FileExcelOutlined,
  FolderFilled,
  FolderOpenFilled,
} from '@ant-design/icons';
import { Avatar } from 'app/components';
import { useCallback } from 'react';
import { errorHandle } from 'utils/utils';
import { SourceSimpleViewModel } from '../pages/MainPage/pages/SourcePage/slice/types';
import { renderIcon } from './useGetVizIcon';

const getType = (type: string, config: string) => {
  if (type === 'JDBC') {
    let desc = '';
    try {
      const { dbType } = JSON.parse(config);
      desc = dbType;
    } catch (error) {
      errorHandle(error);
      throw error;
    }
    return desc;
  } else {
    return type;
  }
};

function useGetSourceDbTypeIcon() {
  return useCallback(({ isFolder, type, config }: SourceSimpleViewModel) => {
    if (isFolder)
      return p => (p.expanded ? <FolderOpenFilled /> : <FolderFilled />);
    const dbType = getType(type, config);
    switch (dbType) {
      case 'INTERBASE':
      case 'FIREBIRD':
      case 'IMPALA':
      case 'NETEZZA':
      case 'PHOENIX':
      case 'SQLSTREAM':
      case 'PRESTO':
      case 'SPARK':
      case 'DORIS':
      case 'DERBY':
      case 'CLICKHOUSE':
      case 'INFORMIX':
      case 'SYBASE':
      case 'HIVE':
      case 'H2':
      case 'POSTGRESQL':
      case 'ACCESS':
      case 'NEOVIEW':
      case 'BIG_QUERY':
      case 'MSSQL':
      case 'DB2':
      case 'MYSQL':
      case 'ORACLE':
      case 'HTTP':
        return renderIcon(dbType);
      case 'FILE':
        return <FileExcelOutlined />;
      default:
        // NOTE: The font size of the icon library is 20.
        return <Avatar size={20}>{dbType.substr(0, 1).toUpperCase()}</Avatar>;
    }
  }, []);
}

export default useGetSourceDbTypeIcon;
