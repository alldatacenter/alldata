// 分销商状态列表
export const distributionStatusList= [ 
    {
        value:'APPLY',
        label:'申请中'
    },
    {
        value:'RETREAT',
        label:'已清退'
    },
    {
        value:'REFUSE',
        label:'审核拒绝'
    },
    {
        value:'PASS',
        label:'审核通过'
    },
]
// 分销佣金状态列表
export const cashStatusList = [
    {
        value:'APPLY',
        label:'待处理'
    },
    {
        value:'REFUSE',
        label:'拒绝'
    },
    {
        value:'PASS',
        label:'通过'
    }
]
// 分销订单状态列表
export const orderStatusList = [
    {
        value:'WAIT_BILL',
        label:'待结算'
    },
    {
        value:'WAIT_CASH',
        label:'待提现'
    },
    {
        value:'COMPLETE_CASH',
        label:'提现完成'
    }
]