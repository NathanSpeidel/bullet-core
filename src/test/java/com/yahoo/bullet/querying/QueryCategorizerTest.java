/*
 *  Copyright 2018, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.querying;

import com.yahoo.bullet.record.BulletRecord;
import com.yahoo.bullet.result.RecordBox;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class QueryCategorizerTest {
    public static Querier makeQuerier(boolean isDone, boolean isRateLimited, boolean isClosed, boolean hasData) {
        Querier querier = mock(Querier.class);
        // querier.consume returns void and does nothing. No need to mock.
        doReturn(isRateLimited).when(querier).isExceedingRateLimit();
        doReturn(isClosed).when(querier).isClosed();
        doReturn(isDone).when(querier).isDone();
        doReturn(hasData).when(querier).hasNewData();
        return querier;
    }

    private static Map<String, Querier> make(Querier... queries) {
        Map<String, Querier> queriers = new HashMap<>();
        IntStream.range(0, queries.length).forEach(i -> queriers.put(String.valueOf(i), queries[i]));
        return queriers;
    }

    @Test
    public void testNoCategory() {
        QueryCategorizer categorized = new QueryCategorizer().categorize(make(makeQuerier(false, false, false, false)));
        Assert.assertEquals(categorized.getClosed().size(), 0);
        Assert.assertEquals(categorized.getDone().size(), 0);
        Assert.assertEquals(categorized.getRateLimited().size(), 0);
        Assert.assertEquals(categorized.getHasData().size(), 0);
    }

    @Test
    public void testDoning() {
        Map<String, Querier> queries;
        queries = make(makeQuerier(true, true, true, true), makeQuerier(true, true, true, false),
                       makeQuerier(true, true, false, true), makeQuerier(true, true, false, false),
                       makeQuerier(true, false, false, false), makeQuerier(true, false, false, false),
                       makeQuerier(true, false, true, true), makeQuerier(true, false, true, false));

        QueryCategorizer categorized = new QueryCategorizer().categorize(queries);
        Map<String, Querier> done = categorized.getDone();
        Map<String, Querier> rateLimited = categorized.getRateLimited();
        Map<String, Querier> closed = categorized.getClosed();
        Map<String, Querier> hasData = categorized.getHasData();

        Assert.assertEquals(done.size(), 8);
        Assert.assertTrue(done.containsKey("0"));
        Assert.assertTrue(done.containsKey("1"));
        Assert.assertTrue(done.containsKey("2"));
        Assert.assertTrue(done.containsKey("3"));
        Assert.assertTrue(done.containsKey("4"));
        Assert.assertTrue(done.containsKey("5"));
        Assert.assertTrue(done.containsKey("6"));
        Assert.assertTrue(done.containsKey("7"));

        Assert.assertEquals(rateLimited.size(), 0);

        Assert.assertEquals(closed.size(), 0);

        Assert.assertEquals(hasData.size(), 0);
    }

    @Test
    public void testRateLimiting() {
        Map<String, Querier> queries;
        queries = make(makeQuerier(false, true, false, true), makeQuerier(false, true, false, false),
                       makeQuerier(true, true, true, true), makeQuerier(true, true, true, false),
                       makeQuerier(true, true, false, true), makeQuerier(true, true, false, false),
                       makeQuerier(false, true, true, true), makeQuerier(false, true, true, false));

        QueryCategorizer categorized = new QueryCategorizer().categorize(queries);
        Map<String, Querier> done = categorized.getDone();
        Map<String, Querier> rateLimited = categorized.getRateLimited();
        Map<String, Querier> closed = categorized.getClosed();
        Map<String, Querier> hasData = categorized.getHasData();

        Assert.assertEquals(done.size(), 4);
        Assert.assertTrue(done.containsKey("2"));
        Assert.assertTrue(done.containsKey("3"));
        Assert.assertTrue(done.containsKey("4"));
        Assert.assertTrue(done.containsKey("5"));

        Assert.assertEquals(rateLimited.size(), 4);
        Assert.assertTrue(rateLimited.containsKey("0"));
        Assert.assertTrue(rateLimited.containsKey("1"));
        Assert.assertTrue(rateLimited.containsKey("6"));
        Assert.assertTrue(rateLimited.containsKey("7"));

        Assert.assertEquals(closed.size(), 0);

        Assert.assertEquals(hasData.size(), 0);
    }

    @Test
    public void testClosing() {
        Map<String, Querier> queries;
        queries = make(makeQuerier(false, true, true, true), makeQuerier(false, true, true, false),
                       makeQuerier(true, true, true, true), makeQuerier(true, true, true, false),
                       makeQuerier(true, false, true, true), makeQuerier(true, false, true, false),
                       makeQuerier(false, false, true, true), makeQuerier(false, false, true, false));

        QueryCategorizer categorized = new QueryCategorizer().categorize(queries);
        Map<String, Querier> done = categorized.getDone();
        Map<String, Querier> rateLimited = categorized.getRateLimited();
        Map<String, Querier> closed = categorized.getClosed();
        Map<String, Querier> hasData = categorized.getHasData();

        Assert.assertEquals(done.size(), 4);
        Assert.assertTrue(done.containsKey("2"));
        Assert.assertTrue(done.containsKey("3"));
        Assert.assertTrue(done.containsKey("4"));
        Assert.assertTrue(done.containsKey("5"));

        Assert.assertEquals(rateLimited.size(), 2);
        Assert.assertTrue(rateLimited.containsKey("0"));
        Assert.assertTrue(rateLimited.containsKey("1"));

        Assert.assertEquals(closed.size(), 2);
        Assert.assertTrue(closed.containsKey("6"));
        Assert.assertTrue(closed.containsKey("7"));

        Assert.assertEquals(hasData.size(), 0);
    }

    @Test
    public void testHavingData() {
        Map<String, Querier> queries;
        queries = make(makeQuerier(true, true, true, true), makeQuerier(false, true, true, true),
                       makeQuerier(false, false, true, true), makeQuerier(false, false, false, true),
                       makeQuerier(false, true, true, true), makeQuerier(true, true, false, true),
                       makeQuerier(false, true, false, true), makeQuerier(true, false, false, true));

        QueryCategorizer categorized = new QueryCategorizer().categorize(queries);
        Map<String, Querier> done = categorized.getDone();
        Map<String, Querier> rateLimited = categorized.getRateLimited();
        Map<String, Querier> closed = categorized.getClosed();
        Map<String, Querier> hasData = categorized.getHasData();

        Assert.assertEquals(done.size(), 3);
        Assert.assertTrue(done.containsKey("0"));
        Assert.assertTrue(done.containsKey("5"));
        Assert.assertTrue(done.containsKey("7"));

        Assert.assertEquals(rateLimited.size(), 3);
        Assert.assertTrue(rateLimited.containsKey("1"));
        Assert.assertTrue(rateLimited.containsKey("4"));
        Assert.assertTrue(rateLimited.containsKey("6"));

        Assert.assertEquals(closed.size(), 1);
        Assert.assertTrue(closed.containsKey("2"));

        Assert.assertEquals(hasData.size(), 1);
        Assert.assertTrue(hasData.containsKey("3"));
    }


    @Test
    public void testConsuming() {
        Map<String, Querier> queries;
        queries = make(makeQuerier(true, false, false, false),
                       makeQuerier(false, true, false, false),
                       makeQuerier(false, false, true, false),
                       makeQuerier(false, false, false, true),
                       makeQuerier(false, false, false, false));

        BulletRecord record = RecordBox.get().getRecord();

        QueryCategorizer categorized = new QueryCategorizer().categorize(record, queries);
        Map<String, Querier> done = categorized.getDone();
        Map<String, Querier> rateLimited = categorized.getRateLimited();
        Map<String, Querier> closed = categorized.getClosed();
        Map<String, Querier> hasData = categorized.getHasData();

        Assert.assertEquals(done.size(), 1);
        Assert.assertTrue(done.containsKey("0"));

        Assert.assertEquals(rateLimited.size(), 1);
        Assert.assertTrue(rateLimited.containsKey("1"));

        Assert.assertEquals(closed.size(), 1);
        Assert.assertTrue(closed.containsKey("2"));

        Assert.assertEquals(hasData.size(), 1);
        Assert.assertTrue(hasData.containsKey("3"));

        // Test record was consumed by all the queries, including the one that was not categorized
        for (Querier querier : queries.values()) {
            Mockito.verify(querier, times(1)).consume(record);
        }
    }
}
