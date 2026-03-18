package test;

/**
 * Enum with duplicate String values - should be caught.
 */
public enum EnumWithDuplicateStrings {
    ENABLED(1, "active"),
    DISABLED(2, "disabled"),
    ACTIVE(3, "active");  // Duplicate "active"

    private final int code;
    private final String status;

    EnumWithDuplicateStrings(int code, String status) {
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }
}
