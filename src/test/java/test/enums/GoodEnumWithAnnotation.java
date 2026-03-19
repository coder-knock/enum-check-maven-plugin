package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * Enum without duplicates, has {@link EnumCheck} annotation, should pass the check.
 */
@EnumCheck({"code", "name"})
public enum GoodEnumWithAnnotation {
    FIRST(1, "first"),
    SECOND(2, "second"),
    THIRD(3, "third");

    private final int code;
    private final String name;

    GoodEnumWithAnnotation(int code, String name) {
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