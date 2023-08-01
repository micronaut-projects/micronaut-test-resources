/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.codec;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A wrapper type which is used for all messages between the client and
 * the server as return types. This is done because types like String are
 * otherwise treated differently by Micronaut, bypassing our codec.
 *
 * Note that it is possible to skip this type for types like `List`
 * or `Map` but it makes the code less understandable since in some
 * cases the codec would be called and some others not.
 *
 * Therefore, for consistency, all argument types are wrapped in that
 * result type.
 *
 * @param value the value to be wrapped
 * @param <T> the type of the result
 */
public record Result<T>(T value) {
    public static final Result<Boolean> TRUE = new Result<>(true);
    public static final Result<Boolean> FALSE = new Result<>(false);

    private static final Result<?> EMPTY_LIST = new Result<>(Collections.emptyList());

    /**
     * Wraps a value into a result type.
     * @param value the value
     * @return the wrapped value
     * @param <V> the type of the value
     */
    public static <V> Result<V> of(@NonNull V value) {
        return new Result<>(value);
    }

    /**
     * Wraps a potentially null value into an optional.
     * If the value is null, then it returns an empty optional.
     * If the value is not null, then it returns an optional
     * which value is a wrapped result.
     * @param value the value
     * @return an optional of wrapped value
     * @param <V> the type of the value
     */
    public static <V> Optional<Result<V>> asOptional(@Nullable V value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new Result<>(value));
    }

    @SuppressWarnings("unchecked")
    public static <V> Result<List<V>> emptyList() {
        return (Result<List<V>>) EMPTY_LIST;
    }
}
