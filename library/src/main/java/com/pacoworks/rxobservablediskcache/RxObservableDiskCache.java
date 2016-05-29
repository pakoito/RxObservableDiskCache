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

import com.pacoworks.rxpaper.RxPaperBook;

import android.content.Context;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Func1;

/**
 * Static methods to add disk caching behaviour to {@link Single} objects.
 * <p/>
 * Make sure to call {@link RxPaperBook#init(Context)} at least once beforehand to initialize the
 * underlying database.
 *
 * @param <V> type of the data to store
 * @param <P> type of the policy to store
 * @author pakoito
 */
public class RxObservableDiskCache<V, P> {
    private static final String POLICY_APPEND = "_policy";

    private final RxPaperBook book;

    private final Func1<V, P> policyCreator;

    private final Func1<P, Boolean> policyValidator;

    RxObservableDiskCache(RxPaperBook book, Func1<V, P> policyCreator,
            Func1<P, Boolean> policyValidator) {
        this.book = book;
        this.policyValidator = policyValidator;
        this.policyCreator = policyCreator;
    }

    /**
     * Creates a reusable {@link RxObservableDiskCache} for the same {@link RxPaperBook}, Policy and
     * Value types.
     *
     * @param book {@link RxPaperBook} storage book
     * @param policyCreator lazy method to construct a Policy object
     * @param policyValidator lazy method to validate a Policy object
     */
    public static <Value, Policy> RxObservableDiskCache<Value, Policy> create(RxPaperBook book,
            Func1<Value, Policy> policyCreator, Func1<Policy, Boolean> policyValidator) {
        return new RxObservableDiskCache<>(book, policyCreator, policyValidator);
    }

    /**
     * Transforms a {@link Single} into an {@link Observable} returning a disk cached version of the
     * latest Value seen for the same key followed by the {@link Single} result.
     * <p/>
     * The execution assures that the cached Value, if available, will be returned first. If no
     * Value is cached, or its Policy is not validated, then the current Value and Policy are
     * deleted silently and just the value of the {@link Single} is returned.
     * <p/>
     * This version uses a default {@link RxPaperBook}.
     * 
     * @param single {@link Single} operation whose result is to be cached
     * @param key string value under where the values will be stored
     * @param policyCreator lazy method to construct a Policy object
     * @param policyValidator lazy method to validate a Policy object
     * @param <Value> type of the data to store
     * @param <Policy> type of the policy to store
     * @return an {@link Observable} that will return a cached Value followed by the result of
     *         executing single
     */
    public static <Value, Policy> Observable<Cached<Value, Policy>> transform(Single<Value> single,
            String key, Func1<Value, Policy> policyCreator, Func1<Policy, Boolean> policyValidator) {
        return transform(single, key, RxPaperBook.with(BuildConfig.APPLICATION_ID), policyCreator,
                policyValidator);
    }

    /**
     * Transforms a {@link Single} into an {@link Observable} returning a disk cached version of the
     * latest Value seen for the same key followed by the {@link Single} result.
     * <p/>
     * The execution assures that the cached Value, if available, will be returned first. If no
     * Value is cached, or its Policy is not validated, then the current Value and Policy are
     * deleted silently and just the value of the {@link Single} is returned.
     *
     * @param single {@link Single} operation whose result is to be cached
     * @param key string value under where the values will be stored
     * @param paperBook storage book
     * @param policyCreator lazy method to construct a Policy object
     * @param policyValidator lazy method to validate a Policy object
     * @param <Value> type of the data to store
     * @param <Policy> type of the policy to store
     * @return an {@link Observable} that will return a cached Value followed by the result of
     *         executing single
     */
    public static <Value, Policy> Observable<Cached<Value, Policy>> transform(
            final Single<Value> single, final String key, final RxPaperBook paperBook,
            final Func1<Value, Policy> policyCreator, final Func1<Policy, Boolean> policyValidator) {
        return Observable.concat(RxObservableDiskCache.<Value, Policy> requestCachedValue(key,
                paperBook, policyValidator),
                requestFreshValue(single, key, paperBook, policyCreator));
    }

    private static <Value, Policy> Observable<Cached<Value, Policy>> requestCachedValue(
            final String key, final RxPaperBook cache, Func1<Policy, Boolean> policyValidator) {
        return cache
                .<Policy> read(composePolicyKey(key))
                .toObservable()
                .filter(policyValidator)
                .switchIfEmpty(
                        RxObservableDiskCache.<Policy> deleteValueAndPolicy(key, cache)
                                .doOnCompleted(Logging.logCacheInvalid(key)))
                .flatMap(RxObservableDiskCache.<Value, Policy> readValue(key, cache))
                .doOnNext(Logging.<Value, Policy> logCacheHit(key))
                .doOnError(Logging.<Value, Policy> logCacheMiss(key))
                .onErrorResumeNext(RxObservableDiskCache.<Value, Policy> handleErrors(key, cache));
    }

    private static <Policy> Observable<Policy> deleteValueAndPolicy(String key, RxPaperBook cache) {
        return Completable.mergeDelayError(cache.delete(key), cache.delete(composePolicyKey(key)))
                .toObservable();
    }

    private static <Value, Policy> Func1<Policy, Observable<Cached<Value, Policy>>> readValue(
            final String key, final RxPaperBook cache) {
        return new Func1<Policy, Observable<Cached<Value, Policy>>>() {
            @Override
            public Observable<Cached<Value, Policy>> call(final Policy policy) {
                return cache.<Value> read(key)
                        .map(RxObservableDiskCache.<Value, Policy> createDiskCached(policy))
                        .toObservable();
            }
        };
    }

    private static <Value, Policy> Func1<Throwable, Observable<Cached<Value, Policy>>> handleErrors(
            final String key, final RxPaperBook cache) {
        return new Func1<Throwable, Observable<Cached<Value, Policy>>>() {
            @Override
            public Observable<Cached<Value, Policy>> call(final Throwable throwable) {
                return RxObservableDiskCache.<Cached<Value, Policy>> deleteValueAndPolicy(key,
                        cache).flatMap(
                        new Func1<Cached<Value, Policy>, Observable<Cached<Value, Policy>>>() {
                            @Override
                            public Observable<Cached<Value, Policy>> call(
                                    Cached<Value, Policy> valuePolicyCached) {
                                return Observable.error(throwable);
                            }
                        });
            }
        };
    }

    private static <Value, Policy> Observable<Cached<Value, Policy>> requestFreshValue(
            Single<Value> single, String key, RxPaperBook cache, Func1<Value, Policy> policyCreator) {
        return single.toObservable().map(createObservableCached(policyCreator))
                .flatMap(RxObservableDiskCache.<Value, Policy> toStoreKeyAndValue(key, cache));
    }

    private static <Value, Policy> Func1<Cached<Value, Policy>, Observable<Cached<Value, Policy>>> toStoreKeyAndValue(
            final String key, final RxPaperBook cache) {
        return new Func1<Cached<Value, Policy>, Observable<Cached<Value, Policy>>>() {
            @Override
            public Observable<Cached<Value, Policy>> call(final Cached<Value, Policy> ktCached) {
                return Completable.mergeDelayError(cache.write(key, ktCached.value),
                        cache.write(composePolicyKey(key), ktCached.policy)).andThen(
                        Observable.just(ktCached));
            }
        };
    }

    private static String composePolicyKey(String key) {
        return key + POLICY_APPEND;
    }

    private static <Value, Policy> Func1<Value, Cached<Value, Policy>> createDiskCached(
            final Policy policy) {
        return new Func1<Value, Cached<Value, Policy>>() {
            @Override
            public Cached<Value, Policy> call(Value value) {
                return new Cached<>(value, policy, true);
            }
        };
    }

    private static <Value, Policy> Func1<Value, Cached<Value, Policy>> createObservableCached(
            final Func1<Value, Policy> policyCreator) {
        return new Func1<Value, Cached<Value, Policy>>() {
            @Override
            public Cached<Value, Policy> call(Value value) {
                return new Cached<>(value, policyCreator.call(value), false);
            }
        };
    }

    /**
     * Transforms a {@link Single} into an {@link Observable} returning a disk cached version of the
     * latest Value seen for the same key followed by the {@link Single} result.
     * <p/>
     * The execution assures that the cached Value, if available, will be returned first. If no
     * Value is cached, or its Policy is not validated, then the current Value and Policy are
     * deleted silently and just the value of the {@link Single} is returned.
     *
     * @param single {@link Single} operation whose result is to be cached
     * @param key string value under where the values will be stored
     * @return an {@link Observable} that will return a cached Value followed by the result of
     *         executing single
     */
    public Observable<Cached<V, P>> transform(Single<V> single, String key) {
        return transform(single, key, book, policyCreator, policyValidator);
    }
}
