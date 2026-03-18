package com.coderknock.maven.plugin.enumcheck.config;

import com.coderknock.maven.plugin.enumcheck.annotation.CheckGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个组合检查分组的配置。
 * <p>
 * 对应 {@link CheckGroup CheckGroup}
 * 注解，包含该分组需要检查的字段列表。
 */
public class CheckGroupConfig {

    /**
     * 该分组中需要参与组合检查的字段列表。
     */
    private final List<String> fields;

    // -------------------------------------------------------------------------
    // 构造器
    // -------------------------------------------------------------------------

    /**
     * 创建一个组合分组配置。
     *
     * @param fields 参与组合检查的字段列表
     */
    public CheckGroupConfig(List<String> fields) {
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    /**
     * 获取参与组合检查的字段列表。
     *
     * @return 不可修改的字段名称列表
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * 获取该分组包含的字段数量。
     *
     * @return 字段数量
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