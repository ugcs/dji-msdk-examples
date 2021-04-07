package com.example.ugcssample.model.dto;

import java.util.Objects;

public class ValuePair<A, B> {
    public final A a;
    public final B b;

    /**
     * Constructor for a Pair.
     *
     * @param a the first object in the Pair
     * @param b the second object in the pair
     */
    public ValuePair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link ValuePair} to which this one is to be checked for equality
     * @return true if the underlying objects of the Pair are both considered
     * equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValuePair)) {
            return false;
        }
        ValuePair<?, ?> p = (ValuePair<?, ?>)o;
        return Objects.equals(p.a, a) && Objects.equals(p.b, b);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the Pair
     */
    @Override
    public int hashCode() {
        return (a == null ? 0 : a.hashCode()) ^ (b == null ? 0 : b.hashCode());
    }

    @Override
    public String toString() {
        return "ValuePair{" + String.valueOf(a) + " " + String.valueOf(b) + "}";
    }

    /**
     * Convenience method for creating an appropriately typed pair.
     *
     * @param a the first object in the Pair
     * @param b the second object in the pair
     * @return a Pair that is templatized with the types of a and b
     */
    public static <A, B> ValuePair<A, B> create(A a, B b) {
        return new ValuePair<A, B>(a, b);
    }
}
