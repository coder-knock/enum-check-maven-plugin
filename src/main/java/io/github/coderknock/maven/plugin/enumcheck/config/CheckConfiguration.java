package io.github.coderknock.maven.plugin.enumcheck.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个枚举类的检查配置。
 * <p>
 * 从 {@link io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck EnumCheck}
 * 注解中解析出检查规则，封装为这个配置对象，供后续检查使用。
 */
public class CheckConfiguration {

    /**
     * 是否启用检查。false 表示跳过这个枚举。
     */
    private final boolean enabled;

    /**
     * 单独检查的字段列表。每个字段各自要求值唯一。
     */
    private final List<String> singleFields;

    /**
     * 组合检查分组列表。每个分组内多个字段组合要求唯一。
     */
    private final List<CheckGroupConfig> groupConfigs;

    // -------------------------------------------------------------------------
    // 构造器
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

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getSingleFields() {
        return singleFields;
    }

    public List<CheckGroupConfig> getGroupConfigs() {
        return groupConfigs;
    }

    /**
     * 判断是否有任何检查配置（至少一个单独字段或一个分组）。
     *
     * @return true 表示有需要检查的项目，false 表示不需要检查任何内容
     */
    public boolean hasChecks() {
        return !singleFields.isEmpty() || !groupConfigs.isEmpty();
    }

    // -------------------------------------------------------------------------
    // 构建器
    // -------------------------------------------------------------------------

    /**
     * 创建一个新的构建器实例。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link CheckConfiguration} 构建器。
     */
    public static class Builder {
        private boolean enabled = true;
        private final List<String> singleFields = new ArrayList<>();
        private final List<CheckGroupConfig> groupConfigs = new ArrayList<>();

        /**
         * 设置是否启用检查。
         *
         * @param enabled true 启用，false 跳过
         * @return this
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * 添加一个单独检查的字段。
         *
         * @param fieldName 字段名称
         * @return this
         */
        public Builder addSingleField(String fieldName) {
            this.singleFields.add(fieldName);
            return this;
        }

        /**
         * 添加多个单独检查的字段。
         *
         * @param fieldNames 字段名称列表
         * @return this
         */
        public Builder addSingleFields(Iterable<String> fieldNames) {
            for (String fieldName : fieldNames) {
                this.singleFields.add(fieldName);
            }
            return this;
        }

        /**
         * 添加一个组合检查分组。
         *
         * @param groupConfig 分组配置
         * @return this
         */
        public Builder addGroup(CheckGroupConfig groupConfig) {
            this.groupConfigs.add(groupConfig);
            return this;
        }

        /**
         * 构建最终配置对象。
         *
         * @return 不可变的配置对象
         */
        public CheckConfiguration build() {
            return new CheckConfiguration(this);
        }
    }
}
