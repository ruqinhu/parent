package util;

public class Holder<T> {
    private volatile T object;

    public T get() {
        return object;
    }

    public void set(T object) {
        this.object = object;
    }
}
