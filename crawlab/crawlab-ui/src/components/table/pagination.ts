import {SetupContext} from 'vue';
import {sendEvent} from '@/admin/umeng';

const usePagination = (props: TableProps, ctx: SetupContext) => {
  const {emit} = ctx;

  const onCurrentChange = (page: number) => {
    const {pageSize} = props;
    emit('pagination-change', {
      page,
      size: pageSize,
    } as TablePagination);

    sendEvent('click_table_pagination_change_page', {
      page: () => page,
    });
  };

  const onSizeChange = (size: number) => {
    const {page} = props;
    emit('pagination-change', {
      page,
      size,
    } as TablePagination);

    sendEvent('click_table_pagination_change_size', {
      size: () => size,
    });
  };

  return {
    onCurrentChange,
    onSizeChange,
  };
};

export default usePagination;
