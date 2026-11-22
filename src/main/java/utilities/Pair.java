package utilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Pair<T,V> {
    public T a;
    public V b;

    public static <T,V> Pair<T,V> of(T a, V b) {
        return new Pair<>(a, b);
    }
    @JsonCreator
    public Pair(@JsonProperty("a") T a, @JsonProperty("b") V b) {
        this.a = a;
        this.b = b;
    }

    public void swap() {
        T c = a;
        a = (T) b;
        b = (V) c;
    }

    public Pair<T, V> copy() {
        return new Pair<>(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(a, pair.a) &&
                Objects.equals(b, pair.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public String toString() {
        return "<" + a.toString() + ";" + b.toString() + ">";
    }
}
