package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;
import io.github.coderknock.maven.plugin.enumcheck.annotation.CheckGroup;

/**
 * Composite field duplicate, should be detected.
 * <p>
 * The combination of type + code is duplicated: (1, 100) appears in two constants.
 */
@EnumCheck(
    groups = @CheckGroup(fields = {"type", "code"})
)
public enum BadEnumCompositeDuplicate {
    FOOD(1, 100, "食物"),
    DRINK(1, 100, "饮料"),  // type + code duplicate
    CLOTHING(2, 100, "服装");

    private final int type;
    private final int code;
    private final String name;

    BadEnumCompositeDuplicate(int type, int code, String name) {
        this.type = type;
        this.code = code;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}