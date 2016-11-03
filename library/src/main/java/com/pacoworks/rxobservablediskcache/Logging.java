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

import android.util.Log;

import java.util.Locale;

import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Private class containing logging methods for {@link RxObservableDiskCache}
 *
 * @author pakoito
 */
class Logging {
    private static final String TAG = "RxObservableDiskCache";

    private Logging() {
        // No instances
    }

    static <V, P> Action1<Cached<V, P>> logCacheHit(final String key) {
        return new Action1<Cached<V, P>>() {
            @Override
            public void call(Cached<V, P> valuePolicyCached) {
                Log.d(TAG, "Cache hit: " + key);
            }
        };
    }

    static Action1<Throwable> logCacheMiss(final String key) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                Log.e(TAG,
                        String.format(Locale.US, "Cache miss: %s%nCaused by: %s", key,
                                t.getMessage()));
            }
        };
    }

    static Action0 logCacheInvalid(final String key) {
        return new Action0() {
            @Override
            public void call() {
                Log.d(TAG, "Cache invalid: " + key);
            }
        };
    }
}
