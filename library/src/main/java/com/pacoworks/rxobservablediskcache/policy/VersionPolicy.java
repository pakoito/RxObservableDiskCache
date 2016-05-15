
package com.pacoworks.rxobservablediskcache.policy;

import rx.functions.Func1;

public class VersionPolicy {
    public final int version;

    public VersionPolicy(int version) {
        this.version = version;
    }

    public static <T> Func1<T, VersionPolicy> create(final int version) {
        return new Func1<T, VersionPolicy>() {
            @Override
            public VersionPolicy call(T t) {
                return new VersionPolicy(version);
            }
        };
    }

    private static Func1<VersionPolicy, Boolean> validate(final long currentVersion) {
        return new Func1<VersionPolicy, Boolean>() {
            @Override
            public Boolean call(VersionPolicy myPolicy) {
                return myPolicy.version <= currentVersion;
            }
        };
    }
}
