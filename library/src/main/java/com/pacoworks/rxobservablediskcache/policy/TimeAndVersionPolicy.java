
package com.pacoworks.rxobservablediskcache.policy;

import rx.functions.Func1;

public class TimeAndVersionPolicy {
    public final long timestamp;

    public final int version;

    public TimeAndVersionPolicy(final int version) {
        timestamp = System.currentTimeMillis();
        this.version = version;
    }

    public TimeAndVersionPolicy(final long timestampMillis, final int version) {
        this.timestamp = timestampMillis;
        this.version = version;
    }

    public static <T> Func1<T, TimeAndVersionPolicy> create(final int version) {
        return new Func1<T, TimeAndVersionPolicy>() {
            @Override
            public TimeAndVersionPolicy call(T t) {
                return new TimeAndVersionPolicy(version);
            }
        };
    }

    public static <T> Func1<T, TimeAndVersionPolicy> create(final long timestampMillis,
            final int version) {
        return new Func1<T, TimeAndVersionPolicy>() {
            @Override
            public TimeAndVersionPolicy call(T t) {
                return new TimeAndVersionPolicy(timestampMillis, version);
            }
        };
    }

    private static Func1<TimeAndVersionPolicy, Boolean> validate(final long maxCacheDurationMillis,
            final int version) {
        return new Func1<TimeAndVersionPolicy, Boolean>() {
            @Override
            public Boolean call(TimeAndVersionPolicy myPolicy) {
                final boolean isTimeCorrect = System.currentTimeMillis() - myPolicy.timestamp < maxCacheDurationMillis;
                final boolean isVersionCorrect = myPolicy.version <= version;
                return isTimeCorrect && isVersionCorrect;
            }
        };
    }
}
