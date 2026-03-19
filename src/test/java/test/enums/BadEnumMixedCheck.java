package test.enums;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;
import io.github.coderknock.maven.plugin.enumcheck.annotation.CheckGroup;

/**
 * Uses both individual and composite checking, contains multiple duplicates.
 */
@EnumCheck(
    value = "code",
    groups = {
        @CheckGroup(fields = {"type", "category"}),
        @CheckGroup(fields = {"name"})
    }
)
public enum BadEnumMixedCheck {
    PRODUCT_A(1, 101, "electronics", "Phone"),
    PRODUCT_B(1, 102, "electronics", "Laptop"),
    PRODUCT_C(2, 101, "clothing", "Shirt"),  // code 101 duplicates with PRODUCT_A
    PRODUCT_D(2, 101, "clothing", "Tablet");  // code 101 duplicates with PRODUCT_A and PRODUCT_C

    private final int type;
    private final int code;
    private final String category;
    private final String name;

    BadEnumMixedCheck(int type, int code, String category, String name) {
        this.type = type;
        this.code = code;
        this.category = category;
        this.name = name;
    }
}