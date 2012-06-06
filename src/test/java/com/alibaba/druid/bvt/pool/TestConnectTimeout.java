package com.alibaba.druid.bvt.pool;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcUtils;

public class TestConnectTimeout extends TestCase {

    private DruidDataSource dataSource;

    protected void setUp() throws Exception {
        dataSource = new DruidDataSource();

        dataSource.setUsername("xxx1");
        dataSource.setPassword("ppp");
        dataSource.setUrl("jdbc:mock:xx");
        dataSource.setFilters("stat");
        dataSource.setMaxOpenPreparedStatements(30);
        dataSource.setMaxActive(4);
        dataSource.setMaxWait(100);
        dataSource.setMinIdle(0);
        dataSource.setInitialSize(1);
        dataSource.init();
    }

    public void testConnectTimeout() throws Exception {
        {
            Connection conn = dataSource.getConnection();
            conn.close();
            dataSource.shrink();
            Assert.assertEquals(0, dataSource.getPoolingCount());
        }

        final List<Connection> connections = new ArrayList<Connection>();
        for (int i = 0; i < 3; ++i) {
            Connection conn = dataSource.getConnection();
            connections.add(conn);
        }

        final int THREAD_COUNT = 10;
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; ++i) {
            Thread thread = new Thread() {

                public void run() {
                    try {
                        for (int i = 0; i < 100; ++i) {
                            Connection conn = dataSource.getConnection();
                            Thread.sleep(1);
                            conn.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            };
            thread.start();
        }

        latch.await();
    }

    protected void tearDown() throws Exception {
        JdbcUtils.close(dataSource);
    }
}