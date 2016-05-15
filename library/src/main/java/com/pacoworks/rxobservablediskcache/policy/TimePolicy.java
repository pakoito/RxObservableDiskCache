
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

package com.pacoworks.rxobservablediskcache.policy;

import rx.functions.Func1;

public class TimePolicy {
    public final long timestamp;

    public TimePolicy() {
        timestamp = System.currentTimeMillis();
    }

    public TimePolicy(long timestampMillis) {
        this.timestamp = timestampMillis;
    }

    public static <T> Func1<T, TimePolicy> create() {
        return new Func1<T, TimePolicy>() {
            @Override
            public TimePolicy call(T t) {
                return new TimePolicy();
            }
        };
    }

    public static <T> Func1<T, TimePolicy> create(final long timestampMillis) {
        return new Func1<T, TimePolicy>() {
            @Override
            public TimePolicy call(T t) {
                return new TimePolicy(timestampMillis);
            }
        };
    }

    private static Func1<TimePolicy, Boolean> validate(final long maxCacheDurationMillis) {
        return new Func1<TimePolicy, Boolean>() {
            @Override
            public Boolean call(TimePolicy myPolicy) {
                return System.currentTimeMillis() - myPolicy.timestamp < maxCacheDurationMillis;
            }
        };
    }
}
