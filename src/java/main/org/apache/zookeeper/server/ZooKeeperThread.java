/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zookeeper.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 增加未捕获异常处理，打印警告日志
 */
public class ZooKeeperThread extends Thread {

    private static final Logger LOG = LoggerFactory
            .getLogger(ZooKeeperThread.class);

    private UncaughtExceptionHandler uncaughtExceptionalHandler = new UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            handleException(t.getName(), e);
        }
    };

    public ZooKeeperThread(Runnable thread, String threadName) {
        super(thread, threadName);
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    public ZooKeeperThread(String threadName) {
        super(threadName);
        //设置线程因为未捕获的异常停止时的处理程序
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    /**
     * 打印警告日志
     */
    protected void handleException(String thName, Throwable e) {
        LOG.warn("Exception occurred from thread {}", thName, e);
    }
}
