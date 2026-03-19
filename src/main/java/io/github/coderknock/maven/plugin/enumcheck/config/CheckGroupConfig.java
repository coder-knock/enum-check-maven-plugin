package io.github.coderknock.maven.plugin.enumcheck.config;

import io.github.coderknock.maven.plugin.enumcheck.annotation.CheckGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for a single composite check group.
 * <p>
 * Corresponds to the {@link CheckGroup CheckGroup}
 * annotation, contains the list of fields that need to be checked in this group.
 */
public class CheckGroupConfig {

    /**
     * List of fields that need to participate in this composite check.
     */
    private final List<String> fields;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a composite group configuration.
     *
     * @param fields List of fields participating in the composite check
     */
    public CheckGroupConfig(List<String> fields) {
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    /**
     * Gets the list of fields participating in the composite check.
     *
     * @return Unmodifiable list of field names
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Gets the number of fields contained in this group.
     *
     * @return Number of fields
     */
    public int size() {
        return fields.size();
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "CheckGroupConfig" + fields;
    }
}