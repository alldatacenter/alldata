import { Button, Radio, Spin } from 'antd';
import { useState, useEffect } from 'react';
import { checkKerberosAPI } from '@/services/ant-design-pro/colony';
const ConfigSecurity: React.FC<{
    selectListIds:any[],
    setKerberosToParams: any
}> = ({selectListIds, setKerberosToParams}) =>{

    const [securityMode, setSecurityMode] = useState('简单认证模式');
    const [showMode, setShowMode] = useState(true);
    const [loading, setLoading] = useState(false);
    const checkKerber = async()=>{
        setLoading(true)
        try {
            const res = await checkKerberosAPI(selectListIds);
            if(res.success && res.data){
                setShowMode(true)
                setKerberosToParams(true)
            }else{
                setShowMode(false)
            }
        } catch (error) {
            
        }
        setLoading(false)
        return
    }
    useEffect(()=>{
        checkKerber()
    },[])
    return (
        <Spin tip="Loading" size="small" spinning={loading}>
            {showMode ? (
                <Radio.Group value={securityMode} onChange={e => setSecurityMode(e.target.value)}>
                    <Radio.Button value="简单认证模式">简单认证模式</Radio.Button>
                    <Radio.Button value="Kerberos认证模式" disabled>Kerberos认证模式</Radio.Button>
                </Radio.Group>
            ):'无'}
        </Spin>
    )
}

export default ConfigSecurity