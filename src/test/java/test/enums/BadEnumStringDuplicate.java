package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * 字符串字段重复，应该被检测出来。
 */
@EnumCheck("status")
public enum BadEnumStringDuplicate {
    ENABLED(1, "active"),
    DISABLED(2, "disabled"),
    ACTIVE(3, "active");  // status "active" 重复

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
