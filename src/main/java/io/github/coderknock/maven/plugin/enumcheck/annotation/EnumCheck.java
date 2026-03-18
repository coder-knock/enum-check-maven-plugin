package io.github.coderknock.maven.plugin.enumcheck.annotation;

/**
 * 枚举重复值检查标记注解。
 * <p>
 * 只有添加了此注解的枚举类才会被插件扫描检查。
 * 你可以通过此注解直接配置需要检查的字段：
 * <ul>
 *   <li>{@code value}：单独指定需要检查的字段，每个字段单独验重</li>
 *   <li>{@code groups}：指定组合检查分组，每组内多个字段组合起来必须唯一</li>
 * </ul>
 * <p>
 * 使用示例 1：单独检查多个字段，每个字段各自保证唯一
 * <pre>{@code
 * @EnumCheck({"code", "name"})
 * public enum StatusEnum {
 *     SUCCESS(200, "成功"),
 *     ERROR(500, "错误");
 *     // ...
 * }
 * }</pre>
 * <p>
 * 使用示例 2：组合字段检查，多个字段组合必须唯一
 * <pre>{@code
 * @EnumCheck(
 *     groups = {
 *         @CheckGroup(fields = {"type", "code"}),  // type + code 组合必须唯一
 *         @CheckGroup(fields = {"name"})           // name 单独检查必须唯一
 *     }
 * )
 * public enum ProductEnum {
 *     FOOD(1, 100, "食物"),
 *     DRINK(1, 101, "饮料"),
 *     CLOTH(2, 100, "服装");  // type + code = (2, 100) 不重复，可以通过
 *     // ...
 * }
 * }</pre>
 * <p>
 * 使用示例 3：同时使用单独检查和组合检查
 * <pre>{@code
 * @EnumCheck(
 *     value = "code",       // code 单独必须唯一
 *     groups = @CheckGroup(fields = {"type", "name"})  // type + name 组合必须唯一
 * )
 * public enum MyEnum { ... }
 * }</pre>
 * <p>
 * 如果既不指定 {@code value} 也不指定 {@code groups}，
 * 默认检查枚举类中所有非静态实例字段（每个字段单独验重）。
 */
public @interface EnumCheck {

    /**
     * 需要单独检查的字段名称数组。
     * <p>
     * 数组中的每个字段会被独立检查，要求该字段的值在所有枚举常量中唯一。
     * 如果不需要单独检查任何字段，可以留空。
     *
     * @return 需要单独检查的字段名称数组
     */
    String[] value() default {};

    /**
     * 需要组合检查的分组数组。
     * <p>
     * 每个 {@link CheckGroup} 定义一组字段，这组字段的值组合起来必须唯一。
     * 不同分组之间独立检查互不影响。
     * 如果不需要组合检查，可以留空。
     *
     * @return 组合检查分组数组
     */
    CheckGroup[] groups() default {};

    /**
     * 是否启用检查。
     * <p>
     * 可以通过设置此参数为 {@code false} 来临时禁用某个枚举的检查。
     *
     * @return true 表示启用检查（默认），false 表示跳过该枚举
     */
    boolean enabled() default true;
}
