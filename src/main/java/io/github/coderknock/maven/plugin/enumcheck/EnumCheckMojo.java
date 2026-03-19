package io.github.coderknock.maven.plugin.enumcheck;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core Mojo for Maven enum duplicate value check plugin (annotation-driven version).
 * <p>
 * Binds to the {@code process-classes} phase by default (after {@code compile}),
 * when all {@code .class} files have been generated and compilation is complete.
 * The plugin uses JBoss Forge Roaster to parse source code, and only checks
 * enum classes annotated with {@code @EnumCheck}.
 *
 * <h3>Usage Example (configure in target project's pom.xml)</h3>
 * <pre>{@code
 * <plugin>
 *   <groupId>io.github.coderknock</groupId>
 *   <artifactId>enum-check-maven-plugin</artifactId>
 *   <version>2.0.0</version>
 *   <executions>
 *     <execution>
 *       <goals><goal>check</goal></goals>
 *     </execution>
 *   </executions>
 *   <configuration>
 *     <!-- Fail the build when duplicates are found (default: true) -->
 *     <failOnError>true</failOnError>
 *     <!-- Whether to scan all submodules (default: true) -->
 *     <scanSubmodules>true</scanSubmodules>
 *   </configuration>
 * </plugin>
 *
 * Then add the annotation configuration to your enum:
 *
 * import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;
 *
 * @EnumCheck({"code"})
 * public enum Status {
 *     OK(200), ERROR(500);
 *     private final int code;
 *     // ...
 * }
 * }</pre>
 *
 * <p>Composite field checking is also supported:
 * <pre>{@code
 * import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;
 * import io.github.coderknock.maven.plugin.enumcheck.annotation.CheckGroup;
 *
 * @EnumCheck(
 *     groups = @CheckGroup(fields = {"type", "code"})
 * )
 * public enum Product {
 *     FOOD(1, 100),
 *     DRINK(1, 101);
 *     private final int type;
 *     private final int code;
 *     // ...
 * }
 * }</pre>
 *
 * <p>Can also be executed from command line:
 * {@code mvn process-classes enum-check:check}
 *
 * @author coderknock
 * @since 2.0.0
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class EnumCheckMojo extends AbstractMojo {

    /**
     * Current Maven project (injected automatically by Maven).
     * <p>Used to collect source directories of the current project
     * and all submodules.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Whether to fail the build when duplicate values are found.
     * <ul>
     *   <li>{@code true} (default): Throws {@link MojoFailureException} and aborts the build</li>
     *   <li>{@code false}: Only prints warnings and allows the build to continue</li>
     * </ul>
     * <p>Can be overridden from command line: {@code -Denumcheck.failOnError=false}
     */
    @Parameter(defaultValue = "true", property = "enumcheck.failOnError")
    private boolean failOnError;

    /**
     * Whether to scan all submodules.
     * <p>If the project is multi-module, whether to recursively scan
     * source code in all submodules.
     * <ul>
     *   <li>{@code true} (default): Scan the main project plus all submodules</li>
     *   <li>{@code false}: Only scan the current project</li>
     * </ul>
     * <p>Can be overridden from command line: {@code -Denumcheck.scanSubmodules=false}
     */
    @Parameter(defaultValue = "true", property = "enumcheck.scanSubmodules")
    private boolean scanSubmodules;

    // -------------------------------------------------------------------------
    // Mojo Entry Point
    // -------------------------------------------------------------------------

    /**
     * Plugin execution entry point, automatically called by Maven
     * during the {@code process-classes} phase.
     * <p>Execution flow:
     * <ol>
     *   <li>Collect source directories to scan (current project + all submodules)</li>
     *   <li>Use {@link SourceEnumChecker} to scan and check each source directory</li>
     *   <li>Collect all duplicates and print a detailed report</li>
     *   <li>Determine build result based on {@code failOnError}</li>
     * </ol>
     *
     * @throws MojoExecutionException System-level problems like I/O errors
     * @throws MojoFailureException   Duplicates found and failOnError=true
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("============================================");
        getLog().info("Starting annotation-driven enum duplicate check...");

        // Collect all source directories to scan
        List<File> sourceRoots = collectSourceRoots();
        getLog().info("Will scan " + sourceRoots.size() + " source director" +
                (sourceRoots.size() > 1 ? "ies" : "y"));

        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        if (!outputDirectory.exists()) {
            getLog().warn("Compiled output directory does not exist: " + outputDirectory +
                    " - please ensure the project has been compiled.");
        } else {
            getLog().debug("Using compiled output directory: " + outputDirectory);
        }

        try {
            SourceEnumChecker checker = new SourceEnumChecker(
                    outputDirectory.toPath(), getLog());

            List<DuplicateInfo> allSingleDuplicates = new ArrayList<>();
            List<CompositeDuplicateInfo> allCompositeDuplicates = new ArrayList<>();

            // Scan all collected source directories
            for (File sourceRoot : sourceRoots) {
                if (!sourceRoot.exists()) {
                    getLog().debug("Source directory does not exist, skipping: " + sourceRoot);
                    continue;
                }
                getLog().debug("Scanning source directory: " + sourceRoot);
                SourceEnumChecker.CheckResult result = checker.checkDirectory(sourceRoot.toPath());
                allSingleDuplicates.addAll(result.getSingleDuplicates());
                allCompositeDuplicates.addAll(result.getCompositeDuplicates());
            }

            // Process check results
            if (allSingleDuplicates.isEmpty() && allCompositeDuplicates.isEmpty()) {
                getLog().info("Check complete, no duplicate enum values found.");
                getLog().info("============================================");
                return;
            }

            // Print detailed error report
            String errorMessage = formatErrorMessage(allSingleDuplicates, allCompositeDuplicates);
            getLog().error(errorMessage);

            int total = allSingleDuplicates.size() + allCompositeDuplicates.size();
            getLog().info("Found " + total + " duplicate entr" +
                    (total > 1 ? "ies" : "y") + " in total.");

            if (failOnError) {
                throw new MojoFailureException(
                        "Found " + total + " duplicate enum value" +
                                (total > 1 ? "s" : "") + ", build failed.");
            } else {
                getLog().warn("Since failOnError=false, build continues despite duplicates.");
            }

            getLog().info("============================================");

        } catch (IOException e) {
            throw new MojoExecutionException("I/O error occurred while scanning source code", e);
        }
    }

    // -------------------------------------------------------------------------
    // Collect Source Directories (supports multi-module projects)
    // -------------------------------------------------------------------------

    /**
     * Holds a pairing of source root directory and corresponding compiled output directory.
     */
    private static class SourceRootWithOutput {
        private final File sourceRoot;
        private final File outputDir;

        /**
         * Create a new SourceRootWithOutput pairing.
         *
         * @param sourceRoot Source root directory
         * @param outputDir  Compiled output directory
         */
        public SourceRootWithOutput(File sourceRoot, File outputDir) {
            this.sourceRoot = sourceRoot;
            this.outputDir = outputDir;
        }

        /**
         * Get the source root directory.
         *
         * @return Source root directory
         */
        public File getSourceRoot() {
            return sourceRoot;
        }

        /**
         * Get the compiled output directory.
         *
         * @return Compiled output directory
         */
        public File getOutputDir() {
            return outputDir;
        }
    }

    /**
     * Collect all source directories that need to be scanned along with their
     * corresponding compiled output directories.
     * <p>If {@link #scanSubmodules} is true, recursively collects source roots
     * from the current project and all its submodules.
     *
     * @return List of (source directory, output directory) pairs to scan
     */
    private List<SourceRootWithOutput> collectSourceRootsWithOutput() {
        List<SourceRootWithOutput> result = new ArrayList<>();

        // Add main source roots of the current project
        addMainSourceRootsWithOutput(project, result);

        // If submodule scanning is enabled, add all submodules
        if (scanSubmodules) {
            collectSubmoduleSourceRootsWithOutput(project, result);
        }

        getLog().debug("Collected " + result.size() + " source director" +
                (result.size() > 1 ? "ies" : "y") + " in total");
        return result;
    }

    /**
     * Add the main source roots of the given project to the result list,
     * while recording the corresponding output directory.
     *
     * @param proj   The Maven project
     * @param result Result list to add to
     */
    private void addMainSourceRootsWithOutput(MavenProject proj, List<SourceRootWithOutput> result) {
        @SuppressWarnings("unchecked")
        List<String> compileSourceRoots = proj.getCompileSourceRoots();
        File outputDir = new File(proj.getBuild().getOutputDirectory());
        for (String root : compileSourceRoots) {
            File rootFile = new File(root);
            if (rootFile.exists()) {
                result.add(new SourceRootWithOutput(rootFile, outputDir));
                getLog().debug("Added source directory: " + rootFile.getAbsolutePath() +
                        " -> " + outputDir.getAbsolutePath());
            }
        }
    }

    /**
     * Recursively collect source directories and output directories from all submodules.
     *
     * @param proj   Current project
     * @param result Result list to add to
     */
    private void collectSubmoduleSourceRootsWithOutput(MavenProject proj, List<SourceRootWithOutput> result) {
        @SuppressWarnings("unchecked")
        List<MavenProject> modules = proj.getCollectedProjects();
        if (modules != null && !modules.isEmpty()) {
            for (MavenProject module : modules) {
                addMainSourceRootsWithOutput(module, result);
                // Recursively process submodules of submodules (supports nested multi-level)
                collectSubmoduleSourceRootsWithOutput(module, result);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Error Report Formatting
    // -------------------------------------------------------------------------

    /**
     * Format the error report, organizing all duplicate information into
     * a human-readable format.
     *
     * @param singleDuplicates    List of single-field duplicates
     * @param compositeDuplicates List of composite-field duplicates
     * @return Formatted multi-line report
     */
    private String formatErrorMessage(List<DuplicateInfo> singleDuplicates,
            List<CompositeDuplicateInfo> compositeDuplicates) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Found duplicate enum values:\n");
        sb.append("============================================\n");
        sb.append("\n");

        int count = 1;

        // Output single-field duplicates
        for (DuplicateInfo dup : singleDuplicates) {
            sb.append(String.format("%d. Enum class: %s%n", count++, dup.getEnumClassName()));
            sb.append(String.format("   Field:     %s%n", dup.getFieldName()));
            sb.append(String.format("   Value:     %s%n", dup.getValue()));
            sb.append(String.format("   Occurs in: %s%n",
                    dup.getEnumConstants().stream().collect(Collectors.joining(", "))));
            sb.append("\n");
        }

        // Output composite-field duplicates
        for (CompositeDuplicateInfo dup : compositeDuplicates) {
            sb.append(String.format("%d. Enum class: %s%n", count++, dup.getEnumClassName()));
            sb.append(String.format("   Fields:    %s%n", dup.formatFieldNames()));
            sb.append(String.format("   Value:     %s%n", dup.formatValues()));
            sb.append(String.format("   Occurs in: %s%n",
                    dup.getEnumConstants().stream().collect(Collectors.joining(", "))));
            sb.append("\n");
        }

        int total = singleDuplicates.size() + compositeDuplicates.size();
        sb.append("Total: ").append(total).append(" duplicate entr").append(total > 1 ? "ies" : "y").append(".\n");
        return sb.toString();
    }

    /**
     * Collect all source roots from the current project.
     * <p>This is a simplified version that just returns the main compile
     * source roots from the current project. For multi-module projects with
     * submodule scanning enabled, use {@link #collectSourceRootsWithOutput()} instead.
     *
     * @return List of source root directories to scan
     */
    private List<File> collectSourceRoots() {
        List<File> result = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<String> compileSourceRoots = project.getCompileSourceRoots();
        for (String root : compileSourceRoots) {
            File rootFile = new File(root);
            if (rootFile.exists()) {
                result.add(rootFile);
            }
        }
        return result;
    }
}