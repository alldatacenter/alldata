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

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>A triple consisting of four elements.</p>
 *
 * <p>This class is an abstract implementation defining the basic API.
 * It refers to the elements as 'f1', 'f2' , 'f3' and 'f4'.</p>
 *
 * <p>Subclass implementations may be mutable or immutable.
 * However, there is no restriction on the type of the stored objects that may be stored.
 * If mutable objects are stored in the triple, then the triple itself effectively becomes mutable.</p>
 *
 * @param <L> the f1 element type
 * @param <M> the f2 element type
 * @param <R> the f3 element type
 * @param <S> the f4 element type
 */
public abstract class Quartet<L, M, R, S> implements Comparable<Quartet<L, M, R, S>>, Serializable {

    private static final class QuarterAdapter<L, M, R, S> extends Quartet<L, M, R, S> {

        private static final long serialVersionUID = 1L;

        @Override
        public L getF1() {
            return null;
        }

        @Override
        public M getF2() {
            return null;
        }

        @Override
        public R getF3() {
            return null;
        }

        @Override
        public S getF4() {
            return null;
        }
    }

    /**
     * Serialization version
     */
    private static final long serialVersionUID = 1L;

    /**
     * An empty array.
     * <p>
     * Consider using {@link #emptyArray()} to avoid generics warnings.
     * </p>
     *
     */
    public static final QuarterAdapter<?, ?, ?, ?>[] EMPTY_ARRAY = new QuarterAdapter[0];

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
    public static <L, M, R, S> Quartet<L, M, R, S>[] emptyArray() {
        return (QuarterAdapter<L, M, R, S>[]) EMPTY_ARRAY;
    }

    /**
     * <p>Obtains an immutable triple of four objects inferring the generic types.</p>
     *
     * <p>This factory allows the triple to be created using inference to
     * obtain the generic types.</p>
     *
     * @param <L> the f1 element type
     * @param <M> the f2 element type
     * @param <R> the f3 element type
     * @param f1  the f1 element, may be null
     * @param f2  the f2 element, may be null
     * @param f3  the f3 element, may be null
     * @return a triple formed from the four parameters, not null
     */
    public static <L, M, R, S> Quartet<L, M, R, S> of(final L f1, final M f2, final R f3, final S f4) {
        return new ImmutableQuartet<>(f1, f2, f3, f4);
    }

    // -----------------------------------------------------------------------

    /**
     * <p>Compares the triple based on the f1 element, followed by the f2 element and f3 element,
     * finally the f4 element.
     * The types must be {@code Comparable}.</p>
     *
     * @param other the other triple, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final Quartet<L, M, R, S> other) {
        return new CompareToBuilder().append(getF1(), other.getF1())
                .append(getF2(), other.getF2())
                .append(getF3(), other.getF3())
                .append(getF4(), other.getF4())
                .toComparison();
    }

    /**
     * <p>Compares this triple to another based on the four elements.</p>
     *
     * @param obj the object to compare to, null returns false
     * @return true if the elements of the triple are equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Quartet<?, ?, ?, ?>) {
            final Quartet<?, ?, ?, ?> other = (Quartet<?, ?, ?, ?>) obj;
            return Objects.equals(getF1(), other.getF1())
                    && Objects.equals(getF2(), other.getF2())
                    && Objects.equals(getF3(), other.getF3())
                    && Objects.equals(getF4(), other.getF4());
        }
        return false;
    }

    /**
     * <p>Gets the f1 element from this triple.</p>
     *
     * @return the f1 element, may be null
     */
    public abstract L getF1();

    /**
     * <p>Gets the f2 element from this triple.</p>
     *
     * @return the f2 element, may be null
     */
    public abstract M getF2();

    /**
     * <p>Gets the f3 element from this triple.</p>
     *
     * @return the f3 element, may be null
     */
    public abstract R getF3();

    /**
     * <p>Gets the f4 element from this triple.</p>
     *
     * @return the f4 element, may be null
     */
    public abstract S getF4();

    /**
     * <p>Returns a suitable hash code.</p>
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getF1()) ^ Objects.hashCode(getF2()) ^ Objects.hashCode(getF3())
                ^ Objects.hashCode(getF4());
    }

    /**
     * <p>Returns a String representation of this triple using the format {@code ($f1,$f2,$f3,$4)}.</p>
     *
     * @return a string describing this object, not null
     */
    @Override
    public String toString() {
        return "(" + getF1() + "," + getF2() + "," + getF3() + "," + getF4() + ")";
    }

    /**
     * <p>Formats the receiver using the given format.</p>
     *
     * <p>This uses {@link java.util.Formattable} to perform the formatting. Three variables may
     * be used to embed the f1 and f3 elements. Use {@code %1$s} for the f1
     * element, {@code %2$s} for the f2 , {@code %3$s} for the f3 element and {@code %4$s} for the f4 element.
     * The default format used by {@code toString()} is {@code (%1$s,%2$s,%3$s,%4$s)}.</p>
     *
     * @param format the format string, optionally containing {@code %1$s}, {@code %2$s} and {@code %3$s}, not null
     * @return the formatted string, not null
     */
    public String toString(final String format) {
        return String.format(format, getF1(), getF2(), getF3(), getF4());
    }

}
