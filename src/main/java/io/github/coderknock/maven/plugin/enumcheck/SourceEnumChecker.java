package io.github.coderknock.maven.plugin.enumcheck;

import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;
import io.github.coderknock.maven.plugin.enumcheck.config.CheckConfiguration;
import io.github.coderknock.maven.plugin.enumcheck.config.CheckGroupConfig;
import org.apache.maven.plugin.logging.Log;
import org.jboss.forge.roaster.ParserException;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaEnumSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.AnnotationSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于源码解析的枚举重复值检查器。
 * <p>
 * 使用 <a href="https://github.com/forge/roaster">JBoss Forge Roaster</a> 解析 Java 源码文件，
 * 只处理带有 {@link EnumCheck @EnumCheck} 注解的枚举类。
 * <p>
 * 支持两种检查方式：
 * <ol>
 *   <li><b>单独字段检查</b>：每个指定字段的值在所有枚举常量中必须唯一</li>
 *   <li><b>组合字段检查</b>：一组字段的值组合起来在所有枚举常量中必须唯一</li>
 * </ol>
 * <p>
 * 检查流程：
 * <ol>
 *   <li>递归扫描源目录下所有 {@code .java} 源文件</li>
 *   <li>使用 Roaster 解析源码，判断是否为枚举类</li>
 *   <li>检查是否带有 {@link EnumCheck @EnumCheck} 注解，没有则跳过</li>
 *   <li>从注解中解析检查配置（单独字段和组合分组）</li>
 *   <li>编译后通过反射加载枚举类，读取每个常量的字段值</li>
 *   <li>按配置进行重复性检测，收集所有重复信息返回</li>
 * </ol>
 */
public class SourceEnumChecker {

    // -------------------------------------------------------------------------
    // 字段
    // -------------------------------------------------------------------------

    /** Maven 插件日志，用于输出调试和警告信息。 */
    private final Log log;

    /** 编译输出根目录，用于类加载器加载已编译的枚举类。 */
    private final Path outputDir;

    // -------------------------------------------------------------------------
    // 构造器
    // -------------------------------------------------------------------------

    /**
     * 创建源码枚举检查器实例。
     *
     * @param outputDir 编译输出目录（通常是 target/classes），用于加载已编译的 .class 文件
     * @param log       Maven 日志对象
     */
    public SourceEnumChecker(Path outputDir, Log log) {
        this.outputDir = outputDir;
        this.log = log;
    }

    // -------------------------------------------------------------------------
    // 公开 API
    // -------------------------------------------------------------------------

    /**
     * 扫描指定源目录下所有 Java 源码，执行枚举重复值检查。
     * <p>
     * 只处理带有 {@link EnumCheck @EnumCheck} 注解的枚举类。
     *
     * @param sourceDir 源码根目录（通常是 src/main/java）
     * @return 所有检测到的重复信息列表；无重复则返回空列表
     * @throws IOException 遍历文件或读取文件时发生 I/O 错误
     */
    public CheckResult checkDirectory(Path sourceDir) throws IOException {
        List<DuplicateInfo> singleDuplicates = new ArrayList<>();
        List<CompositeDuplicateInfo> compositeDuplicates = new ArrayList<>();

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(".java")) {
                    try {
                        CheckResult result = checkSourceFile(file, sourceDir);
                        if (result != null) {
                            singleDuplicates.addAll(result.getSingleDuplicates());
                            compositeDuplicates.addAll(result.getCompositeDuplicates());
                        }
                    } catch (Exception e) {
                        // 单个文件解析失败不中断整体扫描，降级为警告
                        log.warn("无法检查源码文件: " + file + "，原因: " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return new CheckResult(singleDuplicates, compositeDuplicates);
    }

    // -------------------------------------------------------------------------
    // 单个文件检查
    // -------------------------------------------------------------------------

    /**
     * 检查单个 Java 源码文件，自动推断源码根目录（基于全限定类名的package声明）。
     * <p>
     * 此方法主要用于单元测试，方便对单个枚举文件进行测试。
     *
     * @param sourceFile 源文件路径
     * @return 检查结果，如果不是枚举或没有注解则返回空结果
     * @throws IOException 读取文件错误
     */
    public CheckResult checkFile(Path sourceFile) throws IOException {
        // 推断源码根目录：从文件向上找，直到 package 的第一级目录匹配路径
        // 比如文件是 .../test/enums/GoodEnum.java，package 是 test.enums，那么根目录就是 ...
        Path parent = sourceFile.getParent();
        String packageName = null;
        try {
            JavaEnumSource enumSource = Roaster.parse(JavaEnumSource.class, sourceFile.toFile());
            packageName = enumSource.getPackage();
        } catch (ParserException e) {
            // 不是枚举，返回空结果
            return new CheckResult(new ArrayList<>(), new ArrayList<>());
        }

        // 如果无法获取 package，使用文件所在目录作为 baseDir
        if (packageName == null || packageName.isEmpty()) {
            return checkSourceFile(sourceFile, sourceFile.getParent());
        }

        // 从 packageName 推算需要向上走几级目录
        int packageLevels = packageName.split("\\.").length;
        Path baseDir = sourceFile;
        for (int i = 0; i < packageLevels; i++) {
            baseDir = baseDir.getParent();
        }

        CheckResult result = checkSourceFile(sourceFile, baseDir);
        return result != null ? result : new CheckResult(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * 检查单个 Java 源码文件。
     *
     * @param sourceFile 源文件路径
     * @param baseDir    源码根目录，用于计算全限定类名
     * @return 检查结果，如果不是枚举或没有注解则返回 null
     * @throws IOException 读取文件错误
     */
    private CheckResult checkSourceFile(Path sourceFile, Path baseDir) throws IOException {
        // 使用 Roaster 解析 Java 源码
        JavaEnumSource enumSource;
        try {
            enumSource = Roaster.parse(JavaEnumSource.class, sourceFile.toFile());
        } catch (ParserException e) {
            // 这不是一个枚举类，跳过
            return null;
        }

        // 检查是否带有 @EnumCheck 注解
        if (!enumSource.hasAnnotation(EnumCheck.class)) {
            // 没有注解，跳过
            return null;
        }

        // 获取全限定类名
        String className = getClassName(sourceFile, baseDir, enumSource);
        log.debug("发现带注解的枚举类: " + className);

        // 从注解解析检查配置
        CheckConfiguration config = parseAnnotation(enumSource);
        if (!config.isEnabled() || !config.hasChecks()) {
            log.debug("枚举类 " + className + " 检查已禁用或未配置检查项，跳过");
            return null;
        }

        // 加载已编译的枚举类，进行重复检测
        try {
            return checkEnumByReflection(className, config);
        } catch (ClassNotFoundException e) {
            log.warn("无法加载枚举类 " + className + " (请确保项目已编译)，跳过。原因: " + e.getMessage());
            return new CheckResult(new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * 根据源文件路径计算全限定类名。
     * <p>
     * 如果枚举源码中声明了package，直接使用package + 类名，避免路径计算错误。
     *
     * @param sourceFile 源文件路径
     * @param baseDir    源码根目录
     * @return 全限定类名
     */
    private String getClassName(Path sourceFile, Path baseDir, JavaEnumSource enumSource) {
        // Try to get package from source directly - more accurate than path calculation
        String packageName = enumSource.getPackage();
        if (packageName != null && !packageName.isEmpty()) {
            String simpleName = sourceFile.getFileName().toString();
            if (simpleName.endsWith(".java")) {
                simpleName = simpleName.substring(0, simpleName.length() - 5);
            }
            return packageName + "." + simpleName;
        }
        // Fallback to path-based calculation
        Path relative = baseDir.relativize(sourceFile);
        String className = relative.toString()
                .replace('/', '.')
                .replace('\\', '.');
        // 去掉 .java 后缀
        if (className.endsWith(".java")) {
            className = className.substring(0, className.length() - 5);
        }
        return className;
    }

    // -------------------------------------------------------------------------
    // 注解解析
    // -------------------------------------------------------------------------

    /**
     * 从枚举源码的 {@link EnumCheck} 注解中解析检查配置。
     *
     * @param enumSource 枚举源码对象
     * @return 解析后的检查配置
     */
    private CheckConfiguration parseAnnotation(JavaEnumSource enumSource) {
        CheckConfiguration.Builder builder = CheckConfiguration.builder();

        // 获取 @EnumCheck 注解
        AnnotationSource<JavaEnumSource> annotation = enumSource.getAnnotation(EnumCheck.class);

        // enabled 属性，默认 true
        boolean enabled = true;
        if (annotation != null) {
            Object enabledValue = annotation.getLiteralValue("enabled");
            if ("false".equals(enabledValue)) {
                enabled = false;
            }
        }
        builder.enabled(enabled);

        // value 属性：单独检查的字段
        if (annotation != null) {
            // Parse value attribute (single fields)
            Object valueObject = annotation.getLiteralValue("value");
            if (valueObject != null) {
                String literal = valueObject.toString().trim();
                if (!literal.equals("{}")) {
                    // Remove outer quotes if it's a single string
                    if (literal.startsWith("\"") && literal.endsWith("\"") && !literal.contains(",")) {
                        String fieldName = literal.substring(1, literal.length() - 1);
                        if (!fieldName.trim().isEmpty()) {
                            builder.addSingleField(fieldName.trim());
                        }
                    } else if (literal.startsWith("{") && literal.endsWith("}")) {
                        // It's an array, split by commas
                        String content = literal.substring(1, literal.length() - 1);
                        if (!content.trim().isEmpty()) {
                            String[] items = content.split(",");
                            for (String item : items) {
                                String fieldName = item.trim();
                                // Remove quotes
                                if (fieldName.startsWith("\"") && fieldName.endsWith("\"")) {
                                    fieldName = fieldName.substring(1, fieldName.length() - 1);
                                }
                                if (!fieldName.isEmpty()) {
                                    builder.addSingleField(fieldName);
                                }
                            }
                        }
                    } else if (!literal.isEmpty() && !literal.equals("()")) {
                        // Fallback: just add as single field
                        builder.addSingleField(literal);
                    }
                }
            }

            // Parse groups attribute (composite check groups)
            Object groupsObject = annotation.getLiteralValue("groups");
            if (groupsObject != null) {
                String literal = groupsObject.toString().trim();
                // Handle both array form { @CheckGroup(...), ... } and single form @CheckGroup(...)
                List<String> groupTexts = new ArrayList<>();
                if (literal.startsWith("{") && literal.endsWith("}")) {
                    // It's an array
                    String content = literal.substring(1, literal.length() - 1).trim();
                    if (!content.isEmpty()) {
                        // Split by @CheckGroup to separate each group
                        for (String part : content.split("@CheckGroup")) {
                            part = part.trim();
                            if (!part.isEmpty()) {
                                groupTexts.add(part);
                            }
                        }
                    }
                } else {
                    // It's a single group (not wrapped in array braces)
                    if (!literal.isEmpty()) {
                        groupTexts.add(literal);
                    }
                }

                for (String groupText : groupTexts) {
                    // Look for fields = { ... }
                    int fieldsStart = groupText.indexOf("fields");
                    if (fieldsStart != -1) {
                        int equalSign = groupText.indexOf('=', fieldsStart);
                        int openBrace = groupText.indexOf('{', equalSign);
                        int closeBrace = groupText.lastIndexOf('}');
                        if (openBrace != -1 && closeBrace != -1 && openBrace < closeBrace) {
                            String fieldsContent = groupText.substring(openBrace + 1, closeBrace).trim();
                            if (!fieldsContent.isEmpty()) {
                                List<String> fieldNames = new ArrayList<>();
                                String[] items = fieldsContent.split(",");
                                for (String item : items) {
                                    String fieldName = item.trim();
                                    if (fieldName.startsWith("\"") && fieldName.endsWith("\"")) {
                                        fieldName = fieldName.substring(1, fieldName.length() - 1);
                                    }
                                    if (!fieldName.isEmpty()) {
                                        fieldNames.add(fieldName);
                                    }
                                }
                                if (!fieldNames.isEmpty()) {
                                    builder.addGroup(new CheckGroupConfig(fieldNames));
                                    log.debug("Parsed composite check group: " + fieldNames);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 如果既没有指定 value 也没有指定 groups，默认检查所有非静态实例字段
        CheckConfiguration config = builder.build();
        log.debug("config.hasChecks() = " + config.hasChecks() +
                ", singleFields.size() = " + config.getSingleFields().size() +
                ", groups.size() = " + config.getGroupConfigs().size());
        if (!config.hasChecks()) {
            // 收集所有非静态实例字段
            for (FieldSource<?> field : enumSource.getFields()) {
                if (!field.isStatic() && !field.getName().startsWith("$")) {
                    builder.addSingleField(field.getName());
                }
            }
            config = builder.build();
        }

        return config;
    }

    // -------------------------------------------------------------------------
    // 反射加载与检查
    // -------------------------------------------------------------------------

    /**
     * 通过反射加载已编译的枚举类，执行重复值检查。
     *
     * @param className 枚举全限定类名
     * @param config    检查配置
     * @return 检查结果，包含所有发现的重复
     * @throws ClassNotFoundException 枚举类无法加载
     */
    private CheckResult checkEnumByReflection(String className, CheckConfiguration config)
            throws ClassNotFoundException {

        List<DuplicateInfo> singleDuplicates = new ArrayList<>();
        List<CompositeDuplicateInfo> compositeDuplicates = new ArrayList<>();

        // 创建自定义类加载器从输出目录加载枚举类
        EnumCheckClassLoader classLoader = new EnumCheckClassLoader(
                outputDir, Thread.currentThread().getContextClassLoader());
        Class<?> enumClass = classLoader.loadClass(className);

        // 再次确认确实是枚举类
        if (!enumClass.isEnum()) {
            return new CheckResult(singleDuplicates, compositeDuplicates);
        }

        // 获取所有枚举常量实例
        Object[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null || enumConstants.length <= 1) {
            // 常量少于 2 个，不可能有重复
            return new CheckResult(singleDuplicates, compositeDuplicates);
        }

        // ---- 1. 检查单独字段 ----
        for (String fieldName : config.getSingleFields()) {
            try {
                Field reflectField = findField(enumClass, fieldName);
                if (reflectField == null) {
                    log.debug("枚举 " + className + " 中未找到字段: " + fieldName + "，跳过");
                    continue;
                }
                reflectField.setAccessible(true);

                // 按字段值分组，发现重复
                Map<Object, List<String>> valueToConstants = new LinkedHashMap<>();
                for (Object enumConstant : enumConstants) {
                    Object value = reflectField.get(enumConstant);
                    if (value == null) {
                        continue; // null 值不参与重复检测
                    }
                    String constantName = ((Enum<?>) enumConstant).name();
                    valueToConstants.computeIfAbsent(value, k -> new ArrayList<>())
                            .add(constantName);
                }

                // 收集重复结果
                for (Map.Entry<Object, List<String>> entry : valueToConstants.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        singleDuplicates.add(new DuplicateInfo(
                                className, fieldName, entry.getKey(), entry.getValue()));
                        log.debug("发现重复: " + className + "." + fieldName +
                                " = " + entry.getKey() + " in " + entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.warn("检查字段 " + className + "." + fieldName + " 失败: " + e.getMessage());
            }
        }

        // ---- 2. 检查组合字段 ----
        for (CheckGroupConfig group : config.getGroupConfigs()) {
            List<String> fieldNames = group.getFields();
            try {
                // 预先获取所有字段的反射对象
                List<Field> fields = new ArrayList<>();
                boolean allFound = true;
                for (String fieldName : fieldNames) {
                    Field field = findField(enumClass, fieldName);
                    if (field == null) {
                        log.debug("枚举 " + className + " 中未找到组合字段: " + fieldName + "，跳过该分组");
                        allFound = false;
                        break;
                    }
                    field.setAccessible(true);
                    fields.add(field);
                }
                if (!allFound) {
                    continue;
                }

                // 按组合值分组，发现重复
                // 使用 List 作为 HashMap 的 key 是安全的，因为 List 的 equals/hashCode 是基于元素的
                Map<List<Object>, List<String>> compositeMap = new LinkedHashMap<>();
                for (Object enumConstant : enumConstants) {
                    List<Object> values = new ArrayList<>();
                    for (Field field : fields) {
                        Object value = field.get(enumConstant);
                        values.add(value); // null 也参与组合，因为 null+xxx 重复也是重复
                    }
                    String constantName = ((Enum<?>) enumConstant).name();
                    compositeMap.computeIfAbsent(values, k -> new ArrayList<>())
                            .add(constantName);
                }

                // 收集重复结果
                for (Map.Entry<List<Object>, List<String>> entry : compositeMap.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        compositeDuplicates.add(new CompositeDuplicateInfo(
                                className, fieldNames, entry.getKey(), entry.getValue()));
                        log.debug("发现组合重复: " + className + "(" +
                                String.join(", ", fieldNames) + ") = " + entry.getKey() +
                                " in " + entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.warn("检查组合字段分组 " + className + "(" +
                        String.join(", ", fieldNames) + ") 失败: " + e.getMessage());
            }
        }

        return new CheckResult(singleDuplicates, compositeDuplicates);
    }

    /**
     * 在类继承树中查找指定名称的字段。
     * <p>
     * 会递归查找父类，但不越过 {@link Enum} 类本身（Enum 的字段不属于业务字段）。
     *
     * @param clazz     开始查找的类
     * @param fieldName 字段名称
     * @return 找到的字段对象；未找到返回 null
     */
    private Field findField(Class<?> clazz, String fieldName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        // 递归查找父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Enum.class) {
            return findField(superClass, fieldName);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // 自定义类加载器
    // -------------------------------------------------------------------------

    /**
     * 自定义类加载器，优先从编译输出目录加载类文件。
     * <p>
     * 委托模型：优先查找输出目录，找不到再交给父加载器。
     * 这样可以确保加载的是目标项目刚编译出来的最新版本。
     */
    private class EnumCheckClassLoader extends ClassLoader {
        private final Path baseDir;

        public EnumCheckClassLoader(Path baseDir, ClassLoader parent) {
            super(parent);
            this.baseDir = baseDir;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // 将全限定名转换为 .class 文件路径
            Path classPath = baseDir.resolve(name.replace('.', '/') + ".class");
            if (!Files.exists(classPath)) {
                log.debug("Class file not found: " + classPath);
                throw new ClassNotFoundException(name);
            }
            try {
                byte[] bytes = Files.readAllBytes(classPath);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException("读取 class 文件失败: " + classPath, e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 检查结果容器
    // -------------------------------------------------------------------------

    /**
     * 检查结果容器，包含单独字段重复和组合字段重复两类结果。
     */
    public static class CheckResult {
        private final List<DuplicateInfo> singleDuplicates;
        private final List<CompositeDuplicateInfo> compositeDuplicates;

        public CheckResult(List<DuplicateInfo> singleDuplicates,
                           List<CompositeDuplicateInfo> compositeDuplicates) {
            this.singleDuplicates = singleDuplicates;
            this.compositeDuplicates = compositeDuplicates;
        }

        public List<DuplicateInfo> getSingleDuplicates() {
            return Collections.unmodifiableList(singleDuplicates);
        }

        public List<CompositeDuplicateInfo> getCompositeDuplicates() {
            return Collections.unmodifiableList(compositeDuplicates);
        }

        /**
         * 判断是否有任何重复。
         *
         * @return true 表示至少有一处重复
         */
        public boolean hasDuplicates() {
            return !singleDuplicates.isEmpty() || !compositeDuplicates.isEmpty();
        }

        /**
         * 获取所有重复的总数。
         *
         * @return 重复总数
         */
        public int getTotalDuplicates() {
            return singleDuplicates.size() + compositeDuplicates.size();
        }
    }
}