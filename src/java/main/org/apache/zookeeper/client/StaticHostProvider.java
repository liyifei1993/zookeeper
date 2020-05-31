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

package org.apache.zookeeper.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端连接服务端信息
 * <p>包括服务端信息列表</p>
 *<p>连接次数和最后一次连接成功时的次数</p>
 */
@InterfaceAudience.Public
public final class StaticHostProvider implements HostProvider {
    public interface Resolver {
        InetAddress[] getAllByName(String name) throws UnknownHostException;
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(StaticHostProvider.class);

    private final List<InetSocketAddress> serverAddresses = new ArrayList<InetSocketAddress>(5);

    /**
     * 最后一次连接成功时的次数
     */
    private int lastIndex = -1;

    //当前连接次数
    private int currentIndex = -1;

    private Resolver resolver;

    /**
     * 对服务器地址信息列表排序
     */
    public StaticHostProvider(Collection<InetSocketAddress> serverAddresses) {
        this.resolver = new Resolver() {
            @Override
            public InetAddress[] getAllByName(String name) throws UnknownHostException {
                return InetAddress.getAllByName(name);
            }
        };
        //服务器地址信息排序
        init(serverAddresses);
    }

    /**
     * Introduced for testing purposes. getAllByName() is a static method of InetAddress, therefore cannot be easily mocked.
     * By abstraction of Resolver interface we can easily inject a mocked implementation in tests.
     *
     * @param serverAddresses
     *            possibly unresolved ZooKeeper server addresses
     * @param resolver
     *            custom resolver implementation
     * @throws IllegalArgumentException
     *             if serverAddresses is empty or resolves to an empty list
     */
    public StaticHostProvider(Collection<InetSocketAddress> serverAddresses, Resolver resolver) {
        this.resolver = resolver;
        init(serverAddresses);
    }

    /**
     * 持有一份地址信息列表，并且对地址信息排序
     */
    private void init(Collection<InetSocketAddress> serverAddresses) {
        if (serverAddresses.isEmpty()) {
            throw new IllegalArgumentException(
                    "A HostProvider may not be empty!");
        }
        //地址信息列表
        this.serverAddresses.addAll(serverAddresses);
        //随机排序
        Collections.shuffle(this.serverAddresses);
    }

    /**
     * Evaluate to a hostname if one is available and otherwise it returns the
     * string representation of the IP address.
     *
     * In Java 7, we have a method getHostString, but earlier versions do not support it.
     * This method is to provide a replacement for InetSocketAddress.getHostString().
     *
     * @param addr
     * @return Hostname string of address parameter
     */
    private String getHostString(InetSocketAddress addr) {
        String hostString = "";

        if (addr == null) {
            return hostString;
        }
        if (!addr.isUnresolved()) {
            InetAddress ia = addr.getAddress();

            // If the string starts with '/', then it has no hostname
            // and we want to avoid the reverse lookup, so we return
            // the string representation of the address.
            if (ia.toString().startsWith("/")) {
                hostString = ia.getHostAddress();
            } else {
                hostString = addr.getHostName();
            }
        } else {
            // According to the Java 6 documentation, if the hostname is
            // unresolved, then the string before the colon is the hostname.
            String addrString = addr.toString();
            hostString = addrString.substring(0, addrString.lastIndexOf(':'));
        }

        return hostString;
    }

    public int size() {
        return serverAddresses.size();
    }

    public InetSocketAddress next(long spinDelay) {
        //设置连接次数
        currentIndex = ++currentIndex % serverAddresses.size();
        if (currentIndex == lastIndex && spinDelay > 0) {
            try {
                Thread.sleep(spinDelay);
            } catch (InterruptedException e) {
                LOG.warn("Unexpected exception", e);
            }
        } else if (lastIndex == -1) {
            // We don't want to sleep on the first ever connect attempt.
            lastIndex = 0;
        }

        InetSocketAddress curAddr = serverAddresses.get(currentIndex);
        try {
            String curHostString = getHostString(curAddr);
            List<InetAddress> resolvedAddresses = new ArrayList<InetAddress>(Arrays.asList(this.resolver.getAllByName(curHostString)));
            if (resolvedAddresses.isEmpty()) {
                return curAddr;
            }
            Collections.shuffle(resolvedAddresses);
            return new InetSocketAddress(resolvedAddresses.get(0), curAddr.getPort());
        } catch (UnknownHostException e) {
            return curAddr;
        }
    }

    /**
     * 连接成功，设置最后一次成功时的次数
     */
    @Override
    public void onConnected() {
        lastIndex = currentIndex;
    }
}
