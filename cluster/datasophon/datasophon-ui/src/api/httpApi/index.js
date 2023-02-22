/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-06-20 16:29:40
 * @FilePath: \ddh-ui\src\api\httpApi\index.js
 */
import cluster from './cluster'
import host from './host'
import services from './services'
import user from './user'
import system from './system'



export default {...cluster, ...host, ...user, ...services, ...system}
