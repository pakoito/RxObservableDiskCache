/*
 * Copyright (c) pakoito 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pacoworks.rxobservablediskcache;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.pacoworks.rxpaper.RxPaperBook;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import rx.Single;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

@RunWith(AndroidJUnit4.class)
public class RxObservableDiskCacheTest {
    private static final String KEY = "test_key";

    @Rule
    public final ActivityTestRule<MainActivity> activity = new ActivityTestRule<>(
            MainActivity.class);

    private RxPaperBook testBook;

    @Before
    public void setUp() {
        RxPaperBook.init(activity.getActivity());
        testBook = RxPaperBook.with("test_book");
        testBook.destroy().subscribe();
    }

    public TestSubscriber initCache() {
        final List<Serializable> initialList = Arrays.<Serializable> asList(true, 1, "hello");
        final TestSubscriber<Cached<List<Serializable>, MyPolicy>> subscriber = TestSubscriber
                .create();
        RxObservableDiskCache.transform(Single.just(initialList), KEY, testBook,
                new Func1<List<Serializable>, MyPolicy>() {
                    @Override
                    public MyPolicy call(List<Serializable> serializables) {
                        return new MyPolicy();
                    }
                }, new Func1<MyPolicy, Boolean>() {
                    @Override
                    public Boolean call(MyPolicy myPolicy) {
                        return true;
                    }
                }).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        return subscriber;
    }

    @Test
    public void emptyCache_cacheFail_getObservable() {
        final TestSubscriber subscriber = initCache();
        /* Assert */
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertValueCount(1);
    }

    @Test
    public void emptyCache_observableFails_getException() {
        final TestSubscriber<Cached<Integer, MyPolicy>> subscriber = TestSubscriber.create();
        /* Act */
        RxObservableDiskCache.transform(Single.<Integer> error(new IllegalStateException()), KEY,
                testBook, new Func1<Integer, MyPolicy>() {
                    @Override
                    public MyPolicy call(Integer serializables) {
                        return new MyPolicy();
                    }
                }, new Func1<MyPolicy, Boolean>() {
                    @Override
                    public Boolean call(MyPolicy myPolicy) {
                        return true;
                    }
                }).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        /* Assert */
        subscriber.assertValueCount(0);
        subscriber.assertError(IllegalStateException.class);
    }

    @Test
    public void validCache_cacheHit_getCacheThenGetObservable() {
        initCache();
        final List<Serializable> list = Arrays.<Serializable> asList(true, 1, "hello");
        final TestSubscriber<Cached<List<Serializable>, MyPolicy>> subscriber = TestSubscriber
                .create();
        /* Act */
        RxObservableDiskCache.transform(Single.just(list), KEY, testBook,
                new Func1<List<Serializable>, MyPolicy>() {
                    @Override
                    public MyPolicy call(List<Serializable> serializables) {
                        return new MyPolicy();
                    }
                }, new Func1<MyPolicy, Boolean>() {
                    @Override
                    public Boolean call(MyPolicy myPolicy) {
                        return true;
                    }
                }).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        /* Assert */
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertValueCount(2);
    }

    @Test
    public void validCache_cacheMiss_deleteCacheThenGetObservable() {
        initCache();
        final List<Serializable> list = Arrays.<Serializable> asList(true, 1, "hello");
        final TestSubscriber<Cached<List<Serializable>, MyPolicy>> subscriber = TestSubscriber
                .create();
        /* Act */
        RxObservableDiskCache.transform(Single.just(list), KEY, testBook,
                new Func1<List<Serializable>, MyPolicy>() {
                    @Override
                    public MyPolicy call(List<Serializable> serializables) {
                        return new MyPolicy();
                    }
                }, new Func1<MyPolicy, Boolean>() {
                    @Override
                    public Boolean call(MyPolicy myPolicy) {
                        return false;
                    }
                }).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        /* Assert */
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertValueCount(1);
        Assert.assertTrue(testBook.exists(KEY).toBlocking().value());
    }

    @Test
    public void validCache_cacheHitAndObservableFails_getCacheThenGetException() {
        initCache();
        final TestSubscriber<Cached<Integer, MyPolicy>> subscriber = TestSubscriber.create();
        RxObservableDiskCache.transform(Single.<Integer> error(new IllegalStateException()), KEY,
                testBook, new Func1<Integer, MyPolicy>() {
                    @Override
                    public MyPolicy call(Integer serializables) {
                        return new MyPolicy();
                    }
                }, new Func1<MyPolicy, Boolean>() {
                    @Override
                    public Boolean call(MyPolicy myPolicy) {
                        return true;
                    }
                }).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        /* Assert */
        subscriber.assertValueCount(1);
        subscriber.assertError(IllegalStateException.class);
    }

    @Test
    public void validCache_cacheMissAndObservableFails_deleteCacheThenGetException() {
        initCache();
        final TestSubscriber<Cached<Integer, MyPolicy>> subscriber = TestSubscriber.create();
        RxObservableDiskCache.transform(Single.<Integer> error(new IllegalStateException()), KEY,
                testBook, new Func1<Integer, MyPolicy>() {
                    @Override
                    public MyPolicy call(Integer serializables) {
                        return new MyPolicy();
                    }
                }, new Func1<MyPolicy, Boolean>() {
                    @Override
                    public Boolean call(MyPolicy myPolicy) {
                        return false;
                    }
                }).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        /* Assert */
        subscriber.assertValueCount(0);
        subscriber.assertError(IllegalStateException.class);
        Assert.assertFalse(testBook.exists(KEY).toBlocking().value());
    }
}
