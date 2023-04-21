package org.dromara.cloudeon.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
@Slf4j
public class SftpFilesystemPool {
    private final GenericObjectPool<SftpFileSystem> pool;

    public SftpFilesystemPool(SshConnectionPool sshConnectionPool) {
        GenericObjectPoolConfig<SftpFileSystem> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(5);
        config.setMaxIdle(3);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofMinutes(30));
        config.setMinEvictableIdleTime(Duration.ofMinutes(5));

        pool = new GenericObjectPool<>(new SftpConnectionPool(sshConnectionPool), config);
    }

    public SftpFileSystem borrowObject() throws Exception {
        return pool.borrowObject();
    }

    public void returnObject(SftpFileSystem filesystem) {
        pool.returnObject(filesystem);
    }

    public void close() {
        pool.close();
    }

    private static class SftpConnectionPool extends BasePooledObjectFactory<SftpFileSystem> {
        private final SshConnectionPool sshConnectionPool;

        public SftpConnectionPool( SshConnectionPool sshConnectionPool) {
            this.sshConnectionPool = sshConnectionPool;
        }

        @Override
        public SftpFileSystem create() throws Exception {
            ClientSession clientSession = sshConnectionPool.borrowObject();
            SftpFileSystem SftpFileSystem = SftpClientFactory.instance().createSftpFileSystem(clientSession);
            return SftpFileSystem;
        }

        @Override
        public PooledObject<SftpFileSystem> wrap(SftpFileSystem filesystem) {
            return new DefaultPooledObject<>(filesystem);
        }

        @Override
        public void destroyObject(PooledObject<SftpFileSystem> pooledObject) throws Exception {
            pooledObject.getObject().close();
        }

        @Override
        public boolean validateObject(PooledObject<SftpFileSystem> pooledObject) {
            return pooledObject.getObject().isOpen();
        }
    }
}