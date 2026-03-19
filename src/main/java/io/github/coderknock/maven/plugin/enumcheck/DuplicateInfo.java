package io.github.coderknock.maven.plugin.enumcheck;

import java.util.List;


public class DuplicateInfo {

    /** Fully qualified name of the enum class that contains duplicate values, e.g., {@code "com.example.StatusEnum"}. */
    private final String enumClassName;

    /** Name of the field that has duplicate values, e.g., {@code "code"}. */
    private final String fieldName;

    /**
     * The duplicated value, e.g., integer {@code 200} or string {@code "active"}.
     *
     * <p>The type depends on the declared type of the enum field, could be {@link Integer}, {@link String}, etc.
     */
    private final Object value;

    /**
     * List of enum constant names that share this duplicated value.
     *
     * <p>The list contains at least 2 elements (otherwise it's not a duplicate), e.g., {@code ["SUCCESS", "OK"]}.
     */
    private final List<String> enumConstants;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Create a new DuplicateInfo instance.
     *
     * @param enumClassName  Fully qualified name of the enum class
     * @param fieldName      Name of the field with duplicates
     * @param value          The duplicated field value
     * @param enumConstants  List of enum constant names sharing this value (at least 2)
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
     * Get the fully qualified name of the enum class with duplicates.
     *
     * @return Fully qualified name of the enum class, e.g., {@code "com.example.StatusEnum"}
     */
    public String getEnumClassName() {
        return enumClassName;
    }

    /**
     * Get the name of the field that has duplicates.
     *
     * @return Field name, e.g., {@code "code"}
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Get the duplicated field value.
     *
     * @return The field value, type is the wrapper type corresponding to the field declaration or {@link String}
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get the list of enum constant names that share this duplicated value.
     *
     * @return List of enum constant names, e.g., {@code ["SUCCESS", "OK"]}, contains at least 2 elements
     */
    public List<String> getEnumConstants() {
        return enumConstants;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    /**
     * Returns a concise description of this duplicate information for logging.
     *
     * <p>Format: {@code enumClass.field: value in [constant list]}
     * <br>Example: {@code com.example.StatusEnum.code: 200 in [SUCCESS, OK]}
     */
    @Override
    public String toString() {
        return String.format("%s.%s: %s in %s",
                enumClassName, fieldName, value, enumConstants);
    }
}