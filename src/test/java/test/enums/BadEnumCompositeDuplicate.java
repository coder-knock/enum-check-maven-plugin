package test.enums;

import com.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;
import com.coderknock.maven.plugin.enumcheck.annotation.CheckGroup;

/**
 * 组合字段重复，应该被检测出来。
 * <p>
 * type + code 的组合重复了：(1, 100) 出现在两个常量中。
 */
@EnumCheck(
    groups = @CheckGroup(fields = {"type", "code"})
)
public enum BadEnumCompositeDuplicate {
    FOOD(1, 100, "食物"),
    DRINK(1, 100, "饮料"),  // type + code 重复
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