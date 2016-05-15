
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
