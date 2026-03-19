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
 * Enum duplicate value checker based on source code parsing.
 * <p>
 * Uses <a href="https://github.com/forge/roaster">JBoss Forge Roaster</a> to parse
 * Java source files, and only processes enum classes annotated with
 * {@link EnumCheck @EnumCheck}.
 * <p>
 * Supports two checking modes:
 * <ol>
 *   <li><b>Single-field check</b>: Each specified field must have unique values
 *       across all enum constants</li>
 *   <li><b>Composite-field check</b>: The combination of values from a group
 *       of fields must be unique across all enum constants</li>
 * </ol>
 * <p>
 * Check flow:
 * <ol>
 *   <li>Recursively scan all {@code .java} source files in the source directory</li>
 *   <li>Use Roaster to parse source and determine if it's an enum class</li>
 *   <li>Check for {@link EnumCheck @EnumCheck} annotation, skip if none</li>
 *   <li>Parse check configuration from the annotation (single fields and composite groups)</li>
 *   <li>After compilation, load the enum class via reflection and read field values
 *       for each constant</li>
 *   <li>Perform duplicate detection according to configuration, collect and return
 *       all duplicate information</li>
 * </ol>
 */
public class SourceEnumChecker {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Maven plugin logger, used for debug and warning output. */
    private final Log log;

    /** Compiled output root directory, used by the class loader to load
     *  the compiled enum classes. */
    private final Path outputDir;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Create a new source-based enum checker instance.
     *
     * @param outputDir Compiled output directory (usually target/classes),
     *                  used to load compiled .class files
     * @param log       Maven logger object
     */
    public SourceEnumChecker(Path outputDir, Log log) {
        this.outputDir = outputDir;
        this.log = log;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Scan all Java sources in the given source directory and perform
     * enum duplicate checking.
     * <p>
     * Only processes enum classes annotated with {@link EnumCheck @EnumCheck}.
     *
     * @param sourceDir Source root directory (usually src/main/java)
     * @return Check result containing all detected duplicates;
     *         returns empty list if no duplicates found
     * @throws IOException I/O error occurred during file traversal or reading
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
                        // Failure parsing a single file doesn't abort the entire scan,
                        // just downgrade to a warning
                        log.warn("Failed to check source file: " + file +
                                ", reason: " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return new CheckResult(singleDuplicates, compositeDuplicates);
    }

    // -------------------------------------------------------------------------
    // Single File Check
    // -------------------------------------------------------------------------

    /**
     * Check a single Java source file, automatically infer the source root
     * directory based on the package declaration in the fully qualified class name.
     * <p>
     * This method is mainly used for unit testing, making it convenient to
     * test a single enum file.
     *
     * @param sourceFile Source file path
     * @return Check result, returns empty result if not an enum or no annotation
     * @throws IOException Error reading the file
     */
    public CheckResult checkFile(Path sourceFile) throws IOException {
        // Infer source root directory: traverse upward from file until the
        // first package level directory matches the path
        // For example, if file is .../test/enums/GoodEnum.java and package is test.enums,
        // then root is ...
        Path parent = sourceFile.getParent();
        String packageName = null;
        try {
            JavaEnumSource enumSource = Roaster.parse(JavaEnumSource.class, sourceFile.toFile());
            packageName = enumSource.getPackage();
        } catch (ParserException e) {
            // Not an enum, return empty result
            return new CheckResult(new ArrayList<>(), new ArrayList<>());
        }

        // If we can't get the package, use the file's parent directory as baseDir
        if (packageName == null || packageName.isEmpty()) {
            return checkSourceFile(sourceFile, sourceFile.getParent());
        }

        // Calculate how many levels to go upward from packageName
        int packageLevels = packageName.split("\\.").length;
        Path baseDir = sourceFile;
        for (int i = 0; i < packageLevels; i++) {
            baseDir = baseDir.getParent();
        }

        CheckResult result = checkSourceFile(sourceFile, baseDir);
        return result != null ? result : new CheckResult(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Check a single Java source file.
     *
     * @param sourceFile Source file path
     * @param baseDir    Source root directory, used to calculate
     *                   the fully qualified class name
     * @return Check result, returns null if not an enum or no annotation
     * @throws IOException Error reading the file
     */
    private CheckResult checkSourceFile(Path sourceFile, Path baseDir) throws IOException {
        // Use Roaster to parse Java source
        JavaEnumSource enumSource;
        try {
            enumSource = Roaster.parse(JavaEnumSource.class, sourceFile.toFile());
        } catch (ParserException e) {
            // This is not an enum class, skip
            return null;
        }

        // Check if it has @EnumCheck annotation
        if (!enumSource.hasAnnotation(EnumCheck.class)) {
            // No annotation, skip
            return null;
        }

        // Get fully qualified class name
        String className = getClassName(sourceFile, baseDir, enumSource);
        log.debug("Found annotated enum: " + className);

        // Parse check configuration from annotation
        CheckConfiguration config = parseAnnotation(enumSource);
        if (!config.isEnabled() || !config.hasChecks()) {
            log.debug("Enum " + className + " check is disabled or has no checks configured, skipping");
            return null;
        }

        // Load compiled enum class and perform duplicate detection
        try {
            return checkEnumByReflection(className, config);
        } catch (ClassNotFoundException e) {
            log.warn("Cannot load enum class " + className +
                    " (please ensure the project has been compiled), skipping. Reason: " +
                    e.getMessage());
            return new CheckResult(new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Calculate fully qualified class name based on source file path.
     * <p>
     * If the package is declared in the enum source, use package + class name
     * directly to avoid path calculation errors.
     *
     * @param sourceFile Source file path
     * @param baseDir    Source root directory
     * @return Fully qualified class name
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
        // Remove .java suffix
        if (className.endsWith(".java")) {
            className = className.substring(0, className.length() - 5);
        }
        return className;
    }

    // -------------------------------------------------------------------------
    // Annotation Parsing
    // -------------------------------------------------------------------------

    /**
     * Parse check configuration from the {@link EnumCheck} annotation
     * in the enum source.
     *
     * @param enumSource Enum source object
     * @return Parsed check configuration
     */
    private CheckConfiguration parseAnnotation(JavaEnumSource enumSource) {
        CheckConfiguration.Builder builder = CheckConfiguration.builder();

        // Get @EnumCheck annotation
        AnnotationSource<JavaEnumSource> annotation = enumSource.getAnnotation(EnumCheck.class);

        // enabled attribute, default true
        boolean enabled = true;
        if (annotation != null) {
            Object enabledValue = annotation.getLiteralValue("enabled");
            if ("false".equals(enabledValue)) {
                enabled = false;
            }
        }
        builder.enabled(enabled);

        // value attribute: single check fields
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

        // If neither value nor groups is specified, default to checking
        // all non-static instance fields
        CheckConfiguration config = builder.build();
        log.debug("config.hasChecks() = " + config.hasChecks() +
                ", singleFields.size() = " + config.getSingleFields().size() +
                ", groups.size() = " + config.getGroupConfigs().size());
        if (!config.hasChecks()) {
            // Collect all non-static instance fields
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
    // Reflection Loading and Checking
    // -------------------------------------------------------------------------

    /**
     * Load the compiled enum class via reflection and perform duplicate checking.
     *
     * @param className Fully qualified name of the enum
     * @param config    Check configuration
     * @return Check result containing all found duplicates
     * @throws ClassNotFoundException The enum class could not be loaded
     */
    private CheckResult checkEnumByReflection(String className, CheckConfiguration config)
            throws ClassNotFoundException {

        List<DuplicateInfo> singleDuplicates = new ArrayList<>();
        List<CompositeDuplicateInfo> compositeDuplicates = new ArrayList<>();

        // Create custom class loader to load enum from output directory
        EnumCheckClassLoader classLoader = new EnumCheckClassLoader(
                outputDir, Thread.currentThread().getContextClassLoader());
        Class<?> enumClass = classLoader.loadClass(className);

        // Double-check it really is an enum class
        if (!enumClass.isEnum()) {
            return new CheckResult(singleDuplicates, compositeDuplicates);
        }

        // Get all enum constant instances
        Object[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null || enumConstants.length <= 1) {
            // Less than 2 constants, can't have duplicates
            return new CheckResult(singleDuplicates, compositeDuplicates);
        }

        // ---- 1. Check single fields ----
        for (String fieldName : config.getSingleFields()) {
            try {
                Field reflectField = findField(enumClass, fieldName);
                if (reflectField == null) {
                    log.debug("Field " + fieldName + " not found in enum " +
                            className + ", skipping");
                    continue;
                }
                reflectField.setAccessible(true);

                // Group by field value to find duplicates
                Map<Object, List<String>> valueToConstants = new LinkedHashMap<>();
                for (Object enumConstant : enumConstants) {
                    Object value = reflectField.get(enumConstant);
                    if (value == null) {
                        continue; // null values are not checked for duplicates
                    }
                    String constantName = ((Enum<?>) enumConstant).name();
                    valueToConstants.computeIfAbsent(value, k -> new ArrayList<>())
                            .add(constantName);
                }

                // Collect duplicate results
                for (Map.Entry<Object, List<String>> entry : valueToConstants.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        singleDuplicates.add(new DuplicateInfo(
                                className, fieldName, entry.getKey(), entry.getValue()));
                        log.debug("Found duplicate: " + className + "." + fieldName +
                                " = " + entry.getKey() + " in " + entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to check field " + className + "." + fieldName +
                        ": " + e.getMessage());
            }
        }

        // ---- 2. Check composite fields ----
        for (CheckGroupConfig group : config.getGroupConfigs()) {
            List<String> fieldNames = group.getFields();
            try {
                // Pre-fetch reflection objects for all fields
                List<Field> fields = new ArrayList<>();
                boolean allFound = true;
                for (String fieldName : fieldNames) {
                    Field field = findField(enumClass, fieldName);
                    if (field == null) {
                        log.debug("Composite field " + fieldName + " not found in enum " +
                                className + ", skipping this group");
                        allFound = false;
                        break;
                    }
                    field.setAccessible(true);
                    fields.add(field);
                }
                if (!allFound) {
                    continue;
                }

                // Group by composite value to find duplicates
                // Using List as HashMap key is safe because List equals/hashCode
                // is based on the elements
                Map<List<Object>, List<String>> compositeMap = new LinkedHashMap<>();
                for (Object enumConstant : enumConstants) {
                    List<Object> values = new ArrayList<>();
                    for (Field field : fields) {
                        Object value = field.get(enumConstant);
                        values.add(value); // null also participates in combination
                                          // because null+xxx duplicate is still a duplicate
                    }
                    String constantName = ((Enum<?>) enumConstant).name();
                    compositeMap.computeIfAbsent(values, k -> new ArrayList<>())
                            .add(constantName);
                }

                // Collect duplicate results
                for (Map.Entry<List<Object>, List<String>> entry : compositeMap.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        compositeDuplicates.add(new CompositeDuplicateInfo(
                                className, fieldNames, entry.getKey(), entry.getValue()));
                        log.debug("Found composite duplicate: " + className + "(" +
                                String.join(", ", fieldNames) + ") = " + entry.getKey() +
                                " in " + entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to check composite field group " + className + "(" +
                        String.join(", ", fieldNames) + "): " + e.getMessage());
            }
        }

        return new CheckResult(singleDuplicates, compositeDuplicates);
    }

    /**
     * Find a field with the given name in the class inheritance hierarchy.
     * <p>
     * Searches recursively in superclasses, but does not cross {@link Enum}
     * itself (fields in Enum are not business fields).
     *
     * @param clazz     Class to start searching from
     * @param fieldName Field name
     * @return The found field object; returns null if not found
     */
    private Field findField(Class<?> clazz, String fieldName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        // Recursively search superclass
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Enum.class) {
            return findField(superClass, fieldName);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Custom Class Loader
    // -------------------------------------------------------------------------

    /**
     * Custom class loader that prioritizes loading class files from the
     * compiled output directory.
     * <p>
     * Delegation model: search output directory first, delegate to parent
     * class loader if not found. This ensures we load the freshly compiled
     * version from the target project.
     */
    private class EnumCheckClassLoader extends ClassLoader {
        /** Compiled output root directory */
        private final Path baseDir;

        /**
         * Create a new custom class loader.
         *
         * @param baseDir Compiled output root directory
         * @param parent  Parent class loader (delegates to parent for system classes)
         */
        public EnumCheckClassLoader(Path baseDir, ClassLoader parent) {
            super(parent);
            this.baseDir = baseDir;
        }

        /**
         * Find and load the class, reading .class file from the compiled
         * output directory first.
         *
         * @param name Fully qualified class name
         * @return The defined class object
         * @throws ClassNotFoundException Class file does not exist or
         *         reading failed
         */
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // Convert fully qualified name to .class file path
            Path classPath = baseDir.resolve(name.replace('.', '/') + ".class");
            if (!Files.exists(classPath)) {
                log.debug("Class file not found: " + classPath);
                throw new ClassNotFoundException(name);
            }
            try {
                byte[] bytes = Files.readAllBytes(classPath);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to read class file: " +
                        classPath, e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Check Result Container
    // -------------------------------------------------------------------------

    /**
     * Check result container, containing both single-field duplicates and
     * composite-field duplicates.
     */
    public static class CheckResult {
        private final List<DuplicateInfo> singleDuplicates;
        private final List<CompositeDuplicateInfo> compositeDuplicates;

        /**
         * Create a new check result instance.
         *
         * @param singleDuplicates    List of single-field duplicate information
         * @param compositeDuplicates List of composite-field duplicate information
         */
        public CheckResult(List<DuplicateInfo> singleDuplicates,
                           List<CompositeDuplicateInfo> compositeDuplicates) {
            this.singleDuplicates = singleDuplicates;
            this.compositeDuplicates = compositeDuplicates;
        }

        /**
         * Get all single-field duplicate information.
         *
         * @return Unmodifiable list of single-field duplicate information
         */
        public List<DuplicateInfo> getSingleDuplicates() {
            return Collections.unmodifiableList(singleDuplicates);
        }

        /**
         * Get all composite-field duplicate information.
         *
         * @return Unmodifiable list of composite-field duplicate information
         */
        public List<CompositeDuplicateInfo> getCompositeDuplicates() {
            return Collections.unmodifiableList(compositeDuplicates);
        }

        /**
         * Check if there are any duplicates.
         *
         * @return true means at least one duplicate was found
         */
        public boolean hasDuplicates() {
            return !singleDuplicates.isEmpty() || !compositeDuplicates.isEmpty();
        }

        /**
         * Get the total number of all duplicates.
         *
         * @return Total duplicate count
         */
        public int getTotalDuplicates() {
            return singleDuplicates.size() + compositeDuplicates.size();
        }
    }
}