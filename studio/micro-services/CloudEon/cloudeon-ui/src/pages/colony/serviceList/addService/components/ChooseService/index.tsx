import { CheckCircleFilled, ClockCircleFilled } from '@ant-design/icons';
import { Image,Checkbox } from 'antd'
import styles from './index.less'

const ChooseService: React.FC<{
    serviceList: any[];
    changeStatus: any;
  }> = ({ serviceList, changeStatus }) => {

    const onChange = (values: any) => {
      console.log('values: ', values);
      changeStatus(values)
    }
    return (
      <div className={styles.selectedWrap}>
      <Checkbox.Group style={{ width: '100%' }} onChange={onChange}>
        {serviceList.map((sitem: any) => {
          return (<div className={styles.checkItem} key={sitem.id} >
          <Checkbox style={{ width: '100%' }} value={sitem.id} key={sitem.id} disabled={sitem.installedInCluster}>
            <ServiceItem key={sitem.id} item={sitem} type={1} changeStatus={changeStatus} />
          </Checkbox>
          </div>);
        })}
      </Checkbox.Group>
      </div>
    );
  }

  
  
  const ServiceItem: React.FC<{
    item: any;
    type: number;
    changeStatus: any;
  }> = ({ item, type, changeStatus }) => {
    return (
      <div key={item.id} className={styles.serviceItem}>
                {/* <div style={{display:'flex',flexDirection: 'row', alignItems: 'center',flexWrap:'wrap', width:'100%'}}> */}
                    <Image src={'data:image/jpeg;base64,'+item.iconApp} className={styles.serviceItemIcon} alt="" preview={false} />
                  <div style={{display:'flex',flexDirection: 'column', flex: 1, alignItems: 'flex-start',width:'500px', marginRight:'20px'}}>
                    <div className={styles.serviceItemTitle}>{item.label}</div> 
                    <div className={styles.serviceItemDesc}>{item.description}</div>
                  </div>
                  {/* ${item.installedInCluster?styles.disabledText:''} */}
                  <div className={`${styles.serviceImgWrap} ${styles.serviceItemDesc} `}>
                    <div>镜像：{item.dockerImage}</div>
                    <div>版本：{item.version}</div>
                  </div>
                  <div className={`serviceStatusWrap`}>
                       {
                        item.installedInCluster?(<><CheckCircleFilled style={{marginRight:'5px', color:'#52c41a'}} />已安装</>)
                        :
                        (<><ClockCircleFilled style={{marginRight:'5px',color: '#1890ff'}} />未安装</>)
                       } 
                  </div>
                {/* </div> */}
       </div>
    );
  } 
        {/* {type == 0 ? (
          <>
            <div className={styles.serviceItemIcon}>
              <Image src={'data:image/jpeg;base64,'+item.iconApp} className={styles.serviceItemIcon} alt="" />
            </div>
            <div className={styles.serviceItemCenter}>
              <div className={styles.serviceItemTitle}>{item.label}</div>
              <div className={styles.serviceItemDesc}>{item.description}</div>
            </div>
          </>
          ):(
            <div style={{display:'flex',flexDirection: 'column'}}>
              <div style={{display:'flex',flexDirection: 'row', alignItems: 'center'}}>
                  <Image src={'data:image/jpeg;base64,'+item.iconApp} className={styles.serviceItemIcon} alt="" />
                <div className={styles.serviceItemTitle}>{item.label}</div>
              </div>
              <div className={styles.serviceItemCenter}>
                <div className={styles.serviceItemDesc}>{item.description}</div>
                <div className={styles.serviceItemDesc}>镜像：{item.dockerImage}</div>
                <div className={styles.serviceItemDesc}>版本：{item.version}</div>
              </div>
            </div>
          ) */}
        {/* }
      </div> */}
        {/* <div>
          {type == 0 ? (
            item.installedInCluster?(
              <div className={styles.disabledBtn}>已安装</div>
            ):(

              item.selected ? (
                <div className={styles.disabledBtn}>已添加</div>
              ) : (
                <div
                  className={styles.activeBtn}
                  onClick={() => {
                    changeStatus(item.id);
                  }}
                >
                  添加
                </div>
              )

            )
          ) : (
            <div
                className={styles.activeBtn}
                style={{width:'30px'}}
                onClick={() => {
                  changeStatus(item.id);
                }}
            >移除</div>
          )}
        </div> */}
    {/* </div> */}
  

  export default ChooseService