
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

    public static Func1<VersionPolicy, Boolean> validate(final long currentVersion) {
        return new Func1<VersionPolicy, Boolean>() {
            @Override
            public Boolean call(VersionPolicy myPolicy) {
                return myPolicy.version == currentVersion;
            }
        };
    }
}
