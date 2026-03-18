package test.enums;

import com.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * 没有重复的枚举，带有 {@link EnumCheck} 注解，应该通过检查。
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