package keyconstraint.identifykey.ml.label;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Label {

    private final String name;

    protected Label(String name) {
        checkNotNull(name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label other = (Label) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
