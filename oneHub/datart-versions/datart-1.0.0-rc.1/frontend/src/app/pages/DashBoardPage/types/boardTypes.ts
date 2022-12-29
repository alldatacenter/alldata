import {
  ChartI18NSectionConfig,
  ChartStyleConfig,
} from 'app/types/ChartConfig';
import { BoardType } from '../pages/Board/slice/types';

export interface BoardConfig {
  version: string;
  type: BoardType;
  jsonConfig: {
    props: ChartStyleConfig[];
    i18ns: ChartI18NSectionConfig[];
  };
}
