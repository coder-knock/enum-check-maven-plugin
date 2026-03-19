package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * String field duplicate, should be detected.
 */
@EnumCheck("status")
public enum BadEnumStringDuplicate {
    ENABLED(1, "active"),
    DISABLED(2, "disabled"),
    ACTIVE(3, "active");  // status "active" is duplicated

    private final int code;
    private final String status;

    BadEnumStringDuplicate(int code, String status) {
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