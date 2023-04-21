package org.dromara.cloudeon.processor;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.extra.spring.SpringUtil;
import lombok.NoArgsConstructor;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.ServiceInstanceConfigRepository;
import org.dromara.cloudeon.dao.ServiceRoleInstanceRepository;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.ServiceRoleInstanceEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@NoArgsConstructor
public class RegisterDorisFeTask extends BaseCloudeonTask {

    @Override
    public void internalExecute() {
        ServiceInstanceConfigRepository configRepository = SpringUtil.getBean(ServiceInstanceConfigRepository.class);
        ServiceRoleInstanceRepository roleInstanceRepository = SpringUtil.getBean(ServiceRoleInstanceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);

        log.info("开始通过jdbc接口注册fe节点到doris中....");

        // 查询服务角色实例中的所有be
        Integer serviceInstanceId = taskParam.getServiceInstanceId();
        List<ServiceRoleInstanceEntity> feInstanceList = roleInstanceRepository
                .findByServiceInstanceIdAndServiceRoleName(serviceInstanceId, "DORIS_FE");

        ServiceRoleInstanceEntity masterFe = feInstanceList.stream().findFirst().get();

        ClusterNodeEntity masterFeNode = clusterNodeRepository.findById(masterFe.getNodeId()).get();
        // fe ip
        String masterFeIp = masterFeNode.getIp();
        // fe mysql port
        int masterFeMysqlPort = Integer.parseInt(configRepository.findByServiceInstanceIdAndName(serviceInstanceId, "query_port").getValue());
        // fe edit_log_port
        int feEditLogPort = Integer.parseInt(configRepository.findByServiceInstanceIdAndName(serviceInstanceId, "edit_log_port").getValue());

        // 建立mysql连接
        String url = "jdbc:mysql://" + masterFeIp + ":" + masterFeMysqlPort;
        log.info("jdbc连接为：{}", url);
        DataSource ds = new SimpleDataSource(url, "root", null);
        for (ServiceRoleInstanceEntity feRoleEntity : feInstanceList) {
            try (Connection conn = ds.getConnection();) {
                // 执行非查询语句，返回影响的行数
                String feIp = clusterNodeRepository.findById(feRoleEntity.getNodeId()).get().getIp();
                String sql = String.format("ALTER SYSTEM ADD FOLLOWER  \"%s:%s\" ", feIp, feEditLogPort);
                log.info("执行sql：{}", sql);
                SqlExecutor.execute(conn, sql);

            } catch (SQLException e) {
                e.printStackTrace();
                String message = e.getMessage();
                if (!message.contains("frontend already exists name")) {
                    throw new RuntimeException(e);
                }else {
                    log.info(message);
                    log.info("该fe:{}已经注册过了，忽略....");
                }

            }
        }

    }
}
