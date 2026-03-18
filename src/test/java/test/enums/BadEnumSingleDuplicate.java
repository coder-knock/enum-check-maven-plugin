package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * 单个字段重复，应该被检测出来。
 */
@EnumCheck("code")
public enum BadEnumSingleDuplicate {
    FIRST(100, "first"),
    SECOND(100, "second"),  // code 重复
    THIRD(300, "third");

    private final int code;
    private final String name;

    BadEnumSingleDuplicate(int code, String name) {
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