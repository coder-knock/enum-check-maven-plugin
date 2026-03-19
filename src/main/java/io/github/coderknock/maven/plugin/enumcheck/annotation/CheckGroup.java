package io.github.coderknock.maven.plugin.enumcheck.annotation;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

/**
 * Composite field check group annotation.
 * <p>
 * Used within {@link EnumCheck} to declare a group of fields that should be
 * checked for duplicate combinations. It means "the combination of values
 * from these multiple fields must be unique, no duplicates allowed".
 * <p>
 * Usage example:
 * <pre>{@code
 * @EnumCheck(groups = {
 *     @CheckGroup(fields = {"code", "type"}),  // code + type combination must be unique
 *     @CheckGroup(fields = {"name"})           // name alone must be unique
 * })
 * public enum MyEnum { ... }
 * }</pre>
 */
public @interface CheckGroup {

    /**
     * Array of field names that should participate in this composite check.
     * <p>
     * The array cannot be empty, at least one field must be specified.
     * If only a single field is specified, this is equivalent to checking
     * that field individually.
     *
     * @return Array of field names
     */
    String[] fields();
}