/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.rewriting;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.sql.RowSet;

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;

import static org.testng.Assert.assertNotNull;

public class HippoRewriteFilterTest {

    private static final Logger log = LoggerFactory.getLogger(HippoRewriteFilterTest.class);
    public static final String KEY = "key";
    public static final int SLEEP_TIME = 3000;
    public static final int EXECUTION_TIME = SLEEP_TIME + 100;
    private LoadingCache<String, UrlRewriter> cache;

    @BeforeClass
    public void setUp() throws Exception {
        log.info("====setup=====");
        cache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(10, TimeUnit.DAYS)
                .build(new CacheLoader<String, UrlRewriter>() {
                    private final ExecutorService executor = Executors.newFixedThreadPool(1);

                    public UrlRewriter load(String key) {
                        return fetchRules();
                    }

                    @Override
                    public ListenableFuture<UrlRewriter> reload(final String key, final UrlRewriter oldValue) throws Exception {
                        ListenableFutureTask<UrlRewriter> task =
                                ListenableFutureTask.create(
                                        new Callable<UrlRewriter>() {
                                            public UrlRewriter call() {
                                                return load(key);
                                            }
                                        });
                        executor.execute(task);
                        return task;
                    }
                });
        // initialize
        cache.get(KEY);
        log.info("finished initial load");

    }


    /**
     * Should finish within ~10 ms.
     * NOTE: SLEEP_TIME is added to finish test execution,
     * otherwise interrupt exception is thrown.
     */
    @Test(threadPoolSize = 3, invocationCount = 100, timeOut = EXECUTION_TIME)
    public void testCache() throws Exception {
        final Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        final UrlRewriter urlRewriter = cache.getUnchecked(KEY);
        final long id = Thread.currentThread().getId();
        log.info("THREAD:{}, urlRewriter: {}", id, urlRewriter.getConf().getDecodeUsing());
        stopwatch.stop();
        log.info("# {}", stopwatch.elapsedMillis());
        assertNotNull(urlRewriter);
        cache.refresh(KEY);
        log.info("Trigger refreshed for THREAD: {}", id);


    }


    private UrlRewriter fetchRules() {

        FutureTask<UrlRewriter> futureTask = new FutureTask<UrlRewriter>(new Callable<UrlRewriter>() {
            @Override
            public UrlRewriter call() throws Exception {
                log.info("**** start sleeping for: {}", SLEEP_TIME);
                Thread.sleep(SLEEP_TIME);
                final Conf conf = new Conf();
                conf.setDecodeUsing(String.valueOf(Thread.currentThread().getId()));
                return new UrlRewriter(conf);
            }
        });

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(futureTask);

        try {
            return futureTask.get();
        } catch (InterruptedException e) {
            log.error("", e);
        } catch (ExecutionException e) {
            log.error("", e);
        }
        return null;
    }


}