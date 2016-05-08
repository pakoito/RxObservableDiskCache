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

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Func1;

public final class RxObservableDiskCache<Policy, Value> {
    private static final String POLICY_APPEND = "_policy";

    private final RxPaperBook book;

    private final Func1<Value, Policy> policyCreator;

    private final Func1<Policy, Boolean> policyValidator;

    public RxObservableDiskCache(Func1<Value, Policy> policyCreator,
            Func1<Policy, Boolean> policyValidator) {
        this(RxPaperBook.with(BuildConfig.APPLICATION_ID), policyCreator, policyValidator);
    }

    public RxObservableDiskCache(RxPaperBook book, Func1<Value, Policy> policyCreator,
            Func1<Policy, Boolean> policyValidator) {
        this.book = book;
        this.policyValidator = policyValidator;
        this.policyCreator = policyCreator;
    }

    public static <Value, Policy> Observable<Cached<Value, Policy>> wrap(Single<Value> single,
            String key, Func1<Value, Policy> policyCreator,
            Func1<Policy, Boolean> policyValidation) {
        return wrap(single, key, RxPaperBook.with(BuildConfig.APPLICATION_ID), policyCreator,
                policyValidation);
    }

    public static <Value, Policy> Observable<Cached<Value, Policy>> wrap(final Single<Value> single,
            final String key, final RxPaperBook cache, final Func1<Value, Policy> policyCreator,
            final Func1<Policy, Boolean> policyValidation) {
        return Observable.concat(RxObservableDiskCache.<Value, Policy> requestCachedValue(key,
                cache, policyValidation), requestFreshValue(single, key, cache, policyCreator));
    }

    private static <Value, Policy> Observable<Cached<Value, Policy>> requestCachedValue(
            final String key, final RxPaperBook cache, Func1<Policy, Boolean> keyInvalidator) {
        return cache.<Policy> read(composePolicyKey(key)).toObservable().filter(keyInvalidator)
                .switchIfEmpty(RxObservableDiskCache.<Policy> deleteValueAndPolicy(key, cache))
                .flatMap(RxObservableDiskCache.<Value, Policy> readValue(key, cache))
                .doOnNext(RxObservableDiskCacheLog.<Value, Policy> logCacheHit(key))
                .onErrorResumeNext(Observable.<Cached<Value, Policy>> empty());
    }

    private static <Policy> Observable<Policy> deleteValueAndPolicy(String key, RxPaperBook cache) {
        return Completable.mergeDelayError(cache.delete(key), cache.delete(composePolicyKey(key)))
                .doOnCompleted(RxObservableDiskCacheLog.logCacheMiss(key)).toObservable();
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

    private static <Value, Policy> Observable<Cached<Value, Policy>> requestFreshValue(
            Single<Value> single, String key, RxPaperBook cache,
            Func1<Value, Policy> policyCreator) {
        return single.toObservable().map(createObservableCached(policyCreator))
                .flatMap(RxObservableDiskCache.<Value, Policy> toStoreKeyAndValue(key, cache));
    }

    private static <Value, Policy> Func1<Cached<Value, Policy>, Observable<Cached<Value, Policy>>> toStoreKeyAndValue(
            final String key, final RxPaperBook cache) {
        return new Func1<Cached<Value, Policy>, Observable<Cached<Value, Policy>>>() {
            @Override
            public Observable<Cached<Value, Policy>> call(final Cached<Value, Policy> ktCached) {
                return Completable
                        .mergeDelayError(cache.write(key, ktCached.value),
                                cache.write(composePolicyKey(key), ktCached.policy))
                        .andThen(Observable.just(ktCached));
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

    public Observable<Cached<Value, Policy>> wrap(Observable<Value> singleObservable, String key) {
        return wrap(singleObservable.toSingle(), key, book, policyCreator, policyValidator);
    }
}
