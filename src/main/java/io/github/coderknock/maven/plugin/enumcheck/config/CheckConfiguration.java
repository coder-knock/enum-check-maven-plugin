package io.github.coderknock.maven.plugin.enumcheck.config;


import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Check configuration for a single enum class.
 * <p>
 * Parses the checking rules from the {@link EnumCheck EnumCheck}
 * annotation and encapsulates them into this configuration object
 * for subsequent checking.
 */
public class CheckConfiguration {

    /**
     * Whether checking is enabled. false means skip this enum.
     */
    private final boolean enabled;

    /**
     * List of fields to check individually. Each field must have
     * a unique value by itself.
     */
    private final List<String> singleFields;

    /**
     * List of composite check groups. Within each group, the combination
     * of multiple fields must have a unique combination.
     */
    private final List<CheckGroupConfig> groupConfigs;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private CheckConfiguration(Builder builder) {
        this.enabled = builder.enabled;
        this.singleFields = Collections.unmodifiableList(
                new ArrayList<>(builder.singleFields));
        this.groupConfigs = Collections.unmodifiableList(
                new ArrayList<>(builder.groupConfigs));
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    /**
     * Returns whether checking is enabled.
     *
     * @return true means enabled, false means skip this enum
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the list of all field names that need individual checking.
     *
     * @return Unmodifiable list of field names
     */
    public List<String> getSingleFields() {
        return singleFields;
    }

    /**
     * Returns the list of all composite check group configurations.
     *
     * @return Unmodifiable list of group configurations
     */
    public List<CheckGroupConfig> getGroupConfigs() {
        return groupConfigs;
    }

    /**
     * Checks whether there is any check configuration (at least one
     * individual field or one group).
     *
     * @return true means there are checks to perform, false means no checks needed
     */
    public boolean hasChecks() {
        return !singleFields.isEmpty() || !groupConfigs.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Creates a new builder instance.
     *
     * @return The builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CheckConfiguration}.
     */
    public static class Builder {
        private boolean enabled = true;
        private final List<String> singleFields = new ArrayList<>();
        private final List<CheckGroupConfig> groupConfigs = new ArrayList<>();

        /**
         * Sets whether checking is enabled.
         *
         * @param enabled true enables, false skips
         * @return this
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Adds an individually checked field.
         *
         * @param fieldName Field name
         * @return this
         */
        public Builder addSingleField(String fieldName) {
            this.singleFields.add(fieldName);
            return this;
        }

        /**
         * Adds multiple individually checked fields.
         *
         * @param fieldNames List of field names
         * @return this
         */
        public Builder addSingleFields(Iterable<String> fieldNames) {
            for (String fieldName : fieldNames) {
                this.singleFields.add(fieldName);
            }
            return this;
        }

        /**
         * Adds a composite check group.
         *
         * @param groupConfig Group configuration
         * @return this
         */
        public Builder addGroup(CheckGroupConfig groupConfig) {
            this.groupConfigs.add(groupConfig);
            return this;
        }

        /**
         * Builds the final configuration object.
         *
         * @return Immutable configuration object
         */
        public CheckConfiguration build() {
            return new CheckConfiguration(this);
        }
    }
}