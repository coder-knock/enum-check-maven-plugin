package io.github.coderknock.maven.plugin.enumcheck;

import java.util.Arrays;
import java.util.List;

/**
 * Data model holding information about a composite field duplicate.
 * <p>
 * When multiple enum constants have the <strong>same combination</strong>
 * of values across multiple fields, this instance is created to describe
 * the duplication.
 * <p>
 * For example, when {@code type=1, code=100} appears in multiple enum
 * constants, a {@code CompositeDuplicateInfo} is created to record this.
 *
 * <h3>Example Scenario</h3>
 * <pre>{@code
 * public enum ProductEnum {
 *     FOOD(1, 100, "food"),
 *     DRINK(1, 100, "drink");  // type + code = (1, 100) duplicates FOOD!
 *     // ...
 * }
 * }</pre>
 *
 * <p>In this case, a {@code CompositeDuplicateInfo} would be created with:
 * <ul>
 *   <li>{@code enumClassName}: {@code "com.example.ProductEnum"}</li>
 *   <li>{@code fieldNames}: {@code ["type", "code"]}</li>
 *   <li>{@code values}: {@code [1, 100]}</li>
 *   <li>{@code enumConstants}: {@code ["FOOD", "DRINK"]}</li>
 * </ul>
 */
public class CompositeDuplicateInfo {

    /** Fully qualified name of the enum class that contains the composite duplicate. */
    private final String enumClassName;

    /** List of field names participating in this composite check. */
    private final List<String> fieldNames;

    /** List of duplicated composite values, in the same order as {@link #fieldNames}. */
    private final List<Object> values;

    /** List of enum constant names that share this composite value, contains at least 2 elements. */
    private final List<String> enumConstants;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Create a new CompositeDuplicateInfo instance.
     *
     * @param enumClassName   Fully qualified name of the enum class
     * @param fieldNames      List of field names in this composite group
     * @param values          List of duplicated composite values
     * @param enumConstants   List of enum constants sharing this composite value (at least 2)
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
     * Get the fully qualified name of the enum class with duplicates.
     *
     * @return Fully qualified name of the enum class
     */
    public String getEnumClassName() {
        return enumClassName;
    }

    /**
     * Get the list of field names in this composite check.
     *
     * @return List of field names in the same order as declared in the annotation
     */
    public List<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * Get the list of duplicated composite values.
     *
     * @return List of composite values in the same order as the field names
     */
    public List<Object> getValues() {
        return values;
    }

    /**
     * Get the list of enum constant names that share this composite value.
     *
     * @return List of enum constant names, contains at least 2 elements
     */
    public List<String> getEnumConstants() {
        return enumConstants;
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Format the composite values into a human-readable string like "(value1, value2)".
     *
     * @return Formatted composite values string
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
     * Format the field names into a human-readable string like "field1 + field2".
     *
     * @return Formatted field names string
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