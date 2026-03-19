package io.github.coderknock.maven.plugin.enumcheck.annotation;


/**
 * Enum duplicate value check marker annotation.
 * <p>
 * Only enum classes annotated with this annotation will be scanned
 * and checked by the plugin. You can configure which fields to check
 * directly through this annotation:
 * <ul>
 *   <li>{@code value}: Specify fields to be checked individually,
 *       each field must have a unique value by itself</li>
 *   <li>{@code groups}: Specify composite check groups, the combination
 *       of fields within each group must be unique</li>
 * </ul>
 * <p>
 * Usage example 1: Check multiple fields individually, each field
 * must guarantee its own uniqueness:
 * <pre>{@code
 * @EnumCheck({"code", "name"})
 * public enum StatusEnum {
 *     SUCCESS(200, "success"),
 *     ERROR(500, "error");
 *     // ...
 * }
 * }</pre>
 * <p>
 * Usage example 2: Composite field checking, the combination of multiple
 * fields must be unique:
 * <pre>{@code
 * @EnumCheck(
 *     groups = {
 *         @CheckGroup(fields = {"type", "code"}),  // type + code combination must be unique
 *         @CheckGroup(fields = {"name"})           // name alone must be unique
 *     }
 * )
 * public enum ProductEnum {
 *     FOOD(1, 100, "food"),
 *     DRINK(1, 101, "drink"),
 *     CLOTH(2, 100, "cloth");  // type + code = (2, 100) is unique, passes
 *     // ...
 * }
 * }</pre>
 * <p>
 * Usage example 3: Using both individual and composite checks together:
 * <pre>{@code
 * @EnumCheck(
 *     value = "code",       // code alone must be unique
 *     groups = @CheckGroup(fields = {"type", "name"})  // type + name combination must be unique
 * )
 * public enum MyEnum { ... }
 * }</pre>
 * <p>
 * If neither {@code value} nor {@code groups} is specified, the plugin
 * defaults to checking all non-static instance fields in the enum class
 * (each field is checked individually for uniqueness).
 */
public @interface EnumCheck {

    /**
     * Array of field names to check individually.
     * <p>
     * Each field in the array will be checked independently, requiring
     * that the value of this field is unique among all enum constants.
     * Leave empty if you don't need any individual checking.
     *
     * @return Array of field names to check individually
     */
    String[] value() default {};

    /**
     * Array of composite check groups.
     * <p>
     * Each {@link CheckGroup} defines a group of fields, and the
     * combination of values from these fields must be unique.
     * Different groups are checked independently of each other.
     * Leave empty if you don't need any composite checking.
     *
     * @return Array of composite check groups
     */
    CheckGroup[] groups() default {};

    /**
     * Whether this check is enabled.
     * <p>
     * You can temporarily disable checking for a specific enum by
     * setting this parameter to {@code false}.
     *
     * @return true enables checking (default), false skips this enum
     */
    boolean enabled() default true;
}