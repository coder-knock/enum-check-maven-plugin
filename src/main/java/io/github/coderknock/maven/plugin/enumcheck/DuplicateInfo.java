package io.github.coderknock.maven.plugin.enumcheck;

import java.util.List;


public class DuplicateInfo {

    /** 存在重复值的枚举类全限定名，例如 {@code "com.example.StatusEnum"}。 */
    private final String enumClassName;

    /** 出现重复值的字段名，例如 {@code "code"}。 */
    private final String fieldName;

    /**
     * 重复的具体值，例如整数 {@code 200} 或字符串 {@code "active"}。
     *
     * <p>类型取决于枚举字段的声明类型，可能是 {@link Integer}、{@link String} 等。
     */
    private final Object value;

    /**
     * 共享该重复值的枚举常量名称列表。
     *
     * <p>列表中至少包含 2 个元素（否则不构成重复），例如 {@code ["SUCCESS", "OK"]}。
     */
    private final List<String> enumConstants;

    // -------------------------------------------------------------------------
    // 构造器
    // -------------------------------------------------------------------------

    /**
     * 创建一个 DuplicateInfo 实例。
     *
     * @param enumClassName  枚举类全限定名
     * @param fieldName      出现重复的字段名
     * @param value          重复的字段值
     * @param enumConstants  共享该值的枚举常量名列表（至少 2 个）
     */
    public DuplicateInfo(String enumClassName, String fieldName,
                         Object value, List<String> enumConstants) {
        this.enumClassName = enumClassName;
        this.fieldName = fieldName;
        this.value = value;
        this.enumConstants = enumConstants;
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    /**
     * 返回存在重复值的枚举类全限定名。
     *
     * @return 枚举类全限定名，例如 {@code "com.example.StatusEnum"}
     */
    public String getEnumClassName() {
        return enumClassName;
    }

    /**
     * 返回出现重复值的字段名。
     *
     * @return 字段名，例如 {@code "code"}
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * 返回重复的字段值。
     *
     * @return 字段值，类型为字段声明类型对应的包装类或 {@link String}
     */
    public Object getValue() {
        return value;
    }

    /**
     * 返回共享该重复值的枚举常量名称列表。
     *
     * @return 枚举常量名列表，例如 {@code ["SUCCESS", "OK"]}，至少包含 2 个元素
     */
    public List<String> getEnumConstants() {
        return enumConstants;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    /**
     * 返回该重复信息的简洁描述，用于日志输出。
     *
     * <p>格式：{@code 枚举类名.字段名: 重复值 in [常量列表]}
     * <br>示例：{@code com.example.StatusEnum.code: 200 in [SUCCESS, OK]}
     */
    @Override
    public String toString() {
        return String.format("%s.%s: %s in %s",
                enumClassName, fieldName, value, enumConstants);
    }
}