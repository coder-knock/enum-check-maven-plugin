package test;

/**
 * Enum without duplicate values - should pass the check.
 */
public enum GoodEnum {
    FIRST(1, "first"),
    SECOND(2, "second"),
    THIRD(3, "third");

    private final int code;
    private final String name;

    GoodEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
