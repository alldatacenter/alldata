/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { setLang } from '@antv/s2';
import { SheetComponent } from '@antv/s2-react';
import '@antv/s2-react/dist/style.min.css';
import { getLang } from 'locales/i18n';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import { FONT_SIZE_LABEL } from 'styles/StyleConstants';
import { AndvS2Config } from './types';

setLang(['zh_CN', 'en_US'].find(lang => lang.includes(getLang()!)) as any);

const AntVS2Wrapper: FC<AndvS2Config> = memo(
  ({
    dataCfg,
    options,
    theme,
    palette,
    onCollapseRowsAll,
    onRowCellCollapseTreeRows,
    onSelected,
    getSpreadSheet,
    onDataCellClick,
  }) => {
    if (!dataCfg) {
      return <div></div>;
    }

    const onDataCellHover = ({ event, viewMeta }) => {
      viewMeta.spreadsheet.tooltip.show({
        position: {
          x: event.clientX,
          y: event.clientY,
        },
        content: (
          <TableDataCellTooltip
            datas={viewMeta.data}
            meta={viewMeta.spreadsheet.dataCfg.meta}
          />
        ),
      });
    };

    return (
      <StyledAntVS2Wrapper
        sheetType="pivot"
        dataCfg={dataCfg}
        options={options}
        themeCfg={{ theme, palette }}
        onCollapseRowsAll={onCollapseRowsAll}
        onRowCellCollapseTreeRows={onRowCellCollapseTreeRows}
        onDataCellHover={onDataCellHover}
        onSelected={onSelected}
        getSpreadSheet={getSpreadSheet}
        onDataCellClick={onDataCellClick}
      />
    );
  },
);

const TableDataCellTooltip: FC<{
  datas?: object;
  meta?: Array<{ field: string; name: string; formatter }>;
}> = ({ datas, meta }) => {
  if (!datas) {
    return null;
  }

  return (
    <StyledTableDataCellTooltip>
      {(meta || [])
        .map(m => {
          const uniqKey = m?.field;
          if (uniqKey in datas) {
            return (
              <li key={uniqKey}>{`${m?.name}: ${m?.formatter(
                datas[uniqKey],
              )}`}</li>
            );
          }
          return null;
        })
        .filter(Boolean)}
    </StyledTableDataCellTooltip>
  );
};

const StyledTableDataCellTooltip = styled.ul`
  padding: 4px;
  font-size: ${FONT_SIZE_LABEL};
  color: ${p => p.theme.textColorLight};
`;

const StyledAntVS2Wrapper = styled(SheetComponent)``;

export default AntVS2Wrapper;
