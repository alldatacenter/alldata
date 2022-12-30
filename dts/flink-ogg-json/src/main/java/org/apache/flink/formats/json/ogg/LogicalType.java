//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.apache.flink.formats.json.ogg;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.util.Preconditions;

@PublicEvolving
public abstract class LogicalType implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean isNullable;
    private final LogicalTypeRoot typeRoot;

    public LogicalType(boolean isNullable, LogicalTypeRoot typeRoot) {
        this.isNullable = isNullable;
        this.typeRoot = (LogicalTypeRoot)Preconditions.checkNotNull(typeRoot);
    }

    public boolean isNullable() {
        return this.isNullable;
    }

    public LogicalTypeRoot getTypeRoot() {
        return this.typeRoot;
    }

    public abstract LogicalType copy(boolean var1);

    public final LogicalType copy() {
        return this.copy(this.isNullable);
    }

    public abstract String asSerializableString();

    public String asSummaryString() {
        return this.asSerializableString();
    }

    public abstract boolean supportsInputConversion(Class<?> var1);

    public abstract boolean supportsOutputConversion(Class<?> var1);

    public abstract Class<?> getDefaultConversion();

    public abstract List<LogicalType> getChildren();

    public abstract <R> R accept(LogicalTypeVisitor<R> var1);

    public String toString() {
        return this.asSummaryString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            LogicalType that = (LogicalType)o;
            return this.isNullable == that.isNullable && this.typeRoot == that.typeRoot;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.isNullable, this.typeRoot});
    }

    protected String withNullability(String format, Object... params) {
        return !this.isNullable ? String.format(format + " NOT NULL", params) : String.format(format, params);
    }

    protected static Set<String> conversionSet(String... elements) {
        return new HashSet(Arrays.asList(elements));
    }

    /**
     * Returns whether the root of the type equals to the {@code typeRoot} or not.
     *
     * @param typeRoot The root type to check against for equality
     */
    public boolean is(LogicalTypeRoot typeRoot) {
        return this.typeRoot == typeRoot;
    }

    /**
     * Returns whether the root of the type equals to at least on of the {@code typeRoots} or not.
     *
     * @param typeRoots The root types to check against for equality
     */
    public boolean isAnyOf(LogicalTypeRoot... typeRoots) {
        return Arrays.stream(typeRoots).anyMatch(tr -> this.typeRoot == tr);
    }
}
