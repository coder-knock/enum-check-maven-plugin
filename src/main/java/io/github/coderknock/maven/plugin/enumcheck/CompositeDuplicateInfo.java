package io.github.coderknock.maven.plugin.enumcheck;

import java.util.Arrays;
import java.util.List;

/**
 * 组合字段重复值信息数据模型。
 * <p>
 * 当检测到多个枚举常量在<strong>一组字段</strong>上组合值完全相同时，
 * 创建此实例描述重复情况。
 * <p>
 * 例如，当 {@code type=1, code=100} 同时出现在多个枚举常量中，
 * 会生成一个 {@code CompositeDuplicateInfo} 记录该重复。
 *
 * <h3>示例场景</h3>
 * <pre>{@code
 * public enum ProductEnum {
 *     FOOD(1, 100, "食物"),
 *     DRINK(1, 100, "饮料");  // type + code = (1, 100) 与 FOOD 重复！
 *     // ...
 * }
 * }</pre>
 *
 * <p>上述情况会生成一个 {@code CompositeDuplicateInfo}，内容为：
 * <ul>
 *   <li>{@code enumClassName}：{@code "com.example.ProductEnum"}</li>
 *   <li>{@code fieldNames}：{@code ["type", "code"]}</li>
 *   <li>{@code values}：{@code [1, 100]}</li>
 *   <li>{@code enumConstants}：{@code ["FOOD", "DRINK"]}</li>
 * </ul>
 */
public class CompositeDuplicateInfo {

    /** 存在重复值的枚举类全限定名。 */
    private final String enumClassName;

    /** 参与组合检查的字段名称列表。 */
    private final List<String> fieldNames;

    /** 重复的组合值列表，顺序与 {@link #fieldNames} 对应。 */
    private final List<Object> values;

    /** 共享该组合值的枚举常量名称列表，至少包含 2 个元素。 */
    private final List<String> enumConstants;

    // -------------------------------------------------------------------------
    // 构造器
    // -------------------------------------------------------------------------

    /**
     * 创建一个 CompositeDuplicateInfo 实例。
     *
     * @param enumClassName   枚举类全限定名
     * @param fieldNames      参与组合的字段名称列表
     * @param values          重复的组合值列表
     * @param enumConstants   共享该组合值的枚举常量名列表（至少 2 个）
     */
    public CompositeDuplicateInfo(String enumClassName, List<String> fieldNames,
                                   List<Object> values, List<String> enumConstants) {
        this.enumClassName = enumClassName;
        this.fieldNames = fieldNames;
        this.values = values;
        this.enumConstants = enumConstants;
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    /**
     * 返回存在重复值的枚举类全限定名。
     *
     * @return 枚举类全限定名
     */
    public String getEnumClassName() {
        return enumClassName;
    }

    /**
     * 返回参与组合检查的字段名称列表。
     *
     * @return 字段名称列表，顺序为注解中声明的顺序
     */
    public List<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * 返回重复的组合值列表。
     *
     * @return 组合值列表，顺序与字段名称列表对应
     */
    public List<Object> getValues() {
        return values;
    }

    /**
     * 返回共享该组合值的枚举常量名称列表。
     *
     * @return 枚举常量名列表，至少包含 2 个元素
     */
    public List<String> getEnumConstants() {
        return enumConstants;
    }

    // -------------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------------

    /**
     * 格式化组合值为可读字符串，形如 "(value1, value2)"。
     *
     * @return 格式化后的组合值字符串
     */
    public String formatValues() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 格式化字段名称为可读字符串，形如 "field1 + field2"。
     *
     * @return 格式化后的字段名组合字符串
     */
    public String formatFieldNames() {
        return String.join(" + ", fieldNames);
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("%s(%s): %s in %s",
                enumClassName, formatFieldNames(), formatValues(), enumConstants);
    }
}