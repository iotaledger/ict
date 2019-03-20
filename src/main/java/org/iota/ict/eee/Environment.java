package org.iota.ict.eee;

public class Environment {

    private final String id;

    public Environment(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Environment && id.equals(((Environment) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
