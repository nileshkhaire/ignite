/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridgain.grid.kernal;

import org.apache.ignite.cache.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.marshaller.optimized.*;
import org.apache.ignite.spi.discovery.tcp.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests for methods that run job locally with multiple arguments.
 */
public class GridProjectionLocalJobMultipleArgumentsSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final TcpDiscoveryIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    /** */
    private static Collection<Integer> ids;

    /** */
    private static AtomicInteger res;

    /**
     * Starts grid.
     */
    public GridProjectionLocalJobMultipleArgumentsSelfTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setMarshaller(new IgniteOptimizedMarshaller(false));

        CacheConfiguration cache = defaultCacheConfiguration();

        cache.setCacheMode(PARTITIONED);
        cache.setBackups(1);

        cfg.setCacheConfiguration(cache);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        ids = new GridConcurrentHashSet<>();
        res = new AtomicInteger();
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinityCall() throws Exception {
        Collection<Integer> res = new ArrayList<>();

        for (int i : F.asList(1, 2, 3)) {
            res.add(grid().compute().affinityCall(null, i, new IgniteCallable<Integer>() {
                @Override public Integer call() {
                    ids.add(System.identityHashCode(this));

                    return 10;
                }
            }));
        }

        assertEquals(30, F.sumInt(res));
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinityRun() throws Exception {
        for (int i : F.asList(1, 2, 3)) {
            grid().compute().affinityRun(null, i, new IgniteRunnable() {
                @Override public void run() {
                    ids.add(System.identityHashCode(this));

                    res.addAndGet(10);
                }
            });
        }

        assertEquals(30, res.get());
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testCall() throws Exception {
        Collection<Integer> res = grid().compute().apply(new C1<Integer, Integer>() {
            @Override public Integer apply(Integer arg) {

                ids.add(System.identityHashCode(this));

                return 10 + arg;
            }
        }, F.asList(1, 2, 3));

        assertEquals(36, F.sumInt(res));
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testCallWithProducer() throws Exception {
        Collection<Integer> args = Arrays.asList(1, 2, 3);

        Collection<Integer> res = grid().compute().apply(new C1<Integer, Integer>() {
            @Override public Integer apply(Integer arg) {
                ids.add(System.identityHashCode(this));

                return 10 + arg;
            }
        }, args);

        assertEquals(36, F.sumInt(res));
        assertEquals(3, ids.size());
    }
}
