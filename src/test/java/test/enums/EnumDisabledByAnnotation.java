package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * Although annotated, enabled = false, should be skipped.
 * Won't check even if there are duplicates.
 */
@EnumCheck(value = "code", enabled = false)
public enum EnumDisabledByAnnotation {
    FIRST(100),
    SECOND(100);  // There's a duplicate here, but since it's disabled, won't be detected

    private final int code;

    EnumDisabledByAnnotation(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}