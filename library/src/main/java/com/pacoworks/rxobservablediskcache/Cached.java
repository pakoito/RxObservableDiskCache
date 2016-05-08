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

public class Cached<Value, Policy> {
    public final Value value;

    public final Policy policy;

    public final boolean isFromDisk;

    Cached(Value value, Policy policy, boolean isFromDisk) {
        this.value = value;
        this.policy = policy;
        this.isFromDisk = isFromDisk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Cached<?, ?> cached = (Cached<?, ?>)o;
        if (isFromDisk != cached.isFromDisk) {
            return false;
        }
        if (!value.equals(cached.value)) {
            return false;
        }
        return policy.equals(cached.policy);
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + policy.hashCode();
        result = 31 * result + (isFromDisk ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cached{" + "isFromDisk=" + isFromDisk + ", policy=" + policy + ", value=" + value
                + '}';
    }
}
