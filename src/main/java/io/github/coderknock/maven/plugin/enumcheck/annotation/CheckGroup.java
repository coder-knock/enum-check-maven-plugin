package io.github.coderknock.maven.plugin.enumcheck.annotation;

/**
 * 组合字段检查分组注解。
 * <p>
 * 用于在 {@link EnumCheck} 中声明一组需要组合验重的字段，
 * 表示"这多个字段的值组合起来必须唯一，不能有重复"。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @EnumCheck(groups = {
 *     @CheckGroup(fields = {"code", "type"}),  // code + type 组合必须唯一
 *     @CheckGroup(fields = {"name"})           // name 单独检查必须唯一
 * })
 * public enum MyEnum { ... }
 * }</pre>
 */
public @interface CheckGroup {

    /**
     * 该分组中需要参与组合验重的字段名称数组。
     * <p>
     * 数组不能为空，至少需要指定一个字段。
     * 如果只指定一个字段，效果等同于单独检查该字段。
     *
     * @return 字段名称数组
     */
    String[] fields();
}
