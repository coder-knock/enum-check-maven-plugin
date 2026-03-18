package test;

/**
 * Enum with duplicate values - should fail the check.
 */
public enum BadEnumWithDuplicates {
    FIRST(100, "first"),
    SECOND(100, "second"),  // Same code as FIRST!
    THIRD(300, "third");

    private final int code;
    private final String name;

    BadEnumWithDuplicates(int code, String name) {
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
