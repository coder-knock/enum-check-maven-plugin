package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * 虽然有注解但是 enabled = false，应该被跳过。
 * 即使有重复也不会检查。
 */
@EnumCheck(value = "code", enabled = false)
public enum EnumDisabledByAnnotation {
    FIRST(100),
    SECOND(100);  // 这里有重复，但是因为 disabled，不会被检测

    private final int code;

    EnumDisabledByAnnotation(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}