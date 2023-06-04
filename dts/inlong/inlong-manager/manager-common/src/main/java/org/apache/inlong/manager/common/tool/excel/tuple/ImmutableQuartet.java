/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.tool.excel.tuple;

/**
 * <p>An immutable quartet consisting of four {@code Object} elements.</p>
 *
 * <p>Although the implementation is immutable, there is no restriction on the objects
 * that may be stored. If mutable objects are stored in the quartet, then the quartet
 * itself effectively becomes mutable. The class is also {@code final}, so a subclass
 * can not add undesirable behavior.</p>
 *
 * <p>#ThreadSafe# if all four objects are thread-safe</p>
 *
 * @param <L> the f1 element type
 * @param <M> the f2 element type
 * @param <R> the f3 element type
 * @param <S> the f4 element type
 */
public final class ImmutableQuartet<L, M, R, S> extends Quartet<L, M, R, S> {

    /**
     * An empty array.
     * <p>
     * Consider using {@link #emptyArray()} to avoid generics warnings.
     * </p>
     */
    public static final ImmutableQuartet<?, ?, ?, ?>[] EMPTY_ARRAY = new ImmutableQuartet[0];

    /**
     * An immutable quartet of nulls.
     */
    private static final ImmutableQuartet NULL = of(null, null, null, null);

    /**
     * Serialization version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Returns the empty array singleton that can be assigned without compiler warning.
     *
     * @param <L> the f1 element type
     * @param <M> the f2 element type
     * @param <R> the f3 element type
     * @param <S> the f4 element type
     * @return the empty array singleton that can be assigned without compiler warning.
     */
    @SuppressWarnings("unchecked")
    public static <L, M, R, S> ImmutableQuartet<L, M, R, S>[] emptyArray() {
        return (ImmutableQuartet<L, M, R, S>[]) EMPTY_ARRAY;
    }

    /**
     * Returns an immutable quartet of nulls.
     *
     * @param <L> the f1 element of this quartet. Value is {@code null}.
     * @param <M> the f2 element of this quartet. Value is {@code null}.
     * @param <R> the f3 element of this quartet. Value is {@code null}.
     * @param <S> the f4 element of this quartet. Value is {@code null}.
     * @return an immutable quartet of nulls.
     */
    public static <L, M, R, S> ImmutableQuartet<L, M, R, S> nullTriple() {
        return NULL;
    }

    /**
     * <p>Obtains an immutable quartet of four objects inferring the generic types.</p>
     *
     * <p>This factory allows the quartet to be created using inference to
     * obtain the generic types.</p>
     *
     * @param <L> the f1 element type
     * @param <M> the f2 element type
     * @param <R> the f3 element type
     * @param <S> the f4 element type
     * @param f1  the f1 element, may be null
     * @param f2  the f2 element, may be null
     * @param f3  the f3 element, may be null
     * @param f4  the f4 element, may be null
     * @return a quartet formed from the four parameters, not null
     */
    public static <L, M, R, S> ImmutableQuartet<L, M, R, S> of(final L f1, final M f2, final R f3, final S f4) {
        return new ImmutableQuartet<>(f1, f2, f3, f4);
    }

    /**
     * F1 object
     */
    public final L f1;
    /**
     * F2 object
     */
    public final M f2;

    /**
     * F3 object
     */
    public final R f3;

    /**
     * F4 object
     */
    public final S f4;

    /**
     * Create a new quartet instance.
     *
     * @param f1 the f1 value, may be null
     * @param f2 the f2 value, may be null
     * @param f3 the f3 value, may be null
     */
    public ImmutableQuartet(final L f1, final M f2, final R f3, final S f4) {
        super();
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
    }

    @Override
    public L getF1() {
        return f1;
    }

    @Override
    public M getF2() {
        return f2;
    }

    @Override
    public R getF3() {
        return f3;
    }

    @Override
    public S getF4() {
        return f4;
    }
}
