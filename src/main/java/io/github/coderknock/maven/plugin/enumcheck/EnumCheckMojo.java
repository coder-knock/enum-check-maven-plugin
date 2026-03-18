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
 * Maven 枚举重复值检查插件的核心 Mojo（注解驱动版本）。
 * <p>
 * 默认绑定到 {@code process-classes} 阶段（即 {@code compile} 之后），
 * 此时 .class 文件已全部生成，源码也编译完成。
 * 插件使用 JBoss Forge Roaster 解析源码，只检查带有 {@code @EnumCheck}
 * 注解的枚举类。
 *
 * <h3>使用示例（在目标项目的 pom.xml 中配置）</h3>
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
 *     <!-- 发现重复时让构建失败（默认 true） -->
 *     <failOnError>true</failOnError>
 *     <!-- 是否扫描所有子模块（默认 true） -->
 *     <scanSubmodules>true</scanSubmodules>
 *   </configuration>
 * </plugin>
 *
 * 然后在你的枚举上添加注解配置：
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
 * <p>支持组合字段检查：
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
 * <p>也可以通过命令行执行：
 * {@code mvn process-classes enum-check:check}
 *
 * @author coderknock
 * @since 2.0.0
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class EnumCheckMojo extends AbstractMojo {

    /**
     * 当前 Maven 项目（由 Maven 自动注入）。
     * <p>用于收集当前项目和所有子模块的源码目录。
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * 发现重复值时是否让构建失败。
     * <ul>
     *   <li>{@code true}（默认）：发现重复则抛出 {@link MojoFailureException}，构建中止</li>
     *   <li>{@code false}：仅打印警告，构建继续</li>
     * </ul>
     * <p>命令行覆盖：{@code -Denumcheck.failOnError=false}
     */
    @Parameter(defaultValue = "true", property = "enumcheck.failOnError")
    private boolean failOnError;

    /**
     * 是否扫描所有子模块。
     * <p>如果项目是多模块项目，是否递归扫描所有子模块的源码。
     * <ul>
     *   <li>{@code true}（默认）：扫描主项目加上所有子模块</li>
     *   <li>{@code false}：只扫描当前项目</li>
     * </ul>
     * <p>命令行覆盖：{@code -Denumcheck.scanSubmodules=false}
     */
    @Parameter(defaultValue = "true", property = "enumcheck.scanSubmodules")
    private boolean scanSubmodules;

    // -------------------------------------------------------------------------
    // Mojo 入口
    // -------------------------------------------------------------------------

    /**
     * 插件执行入口，由 Maven 在 {@code process-classes} 阶段自动调用。
     * <p>执行流程：
     * <ol>
     *   <li>收集需要扫描的源码目录（当前项目 + 所有子模块）</li>
     *   <li>对每个源码目录，使用 {@link SourceEnumChecker} 扫描检查</li>
     *   <li>收集所有重复值，打印详细报告</li>
     *   <li>根据 {@code failOnError} 决定构建结果</li>
     * </ol>
     *
     * @throws MojoExecutionException I/O 错误等系统级问题
     * @throws MojoFailureException   发现重复且 failOnError=true
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("============================================");
        getLog().info("开始注解驱动枚举重复值检查...");

        // 收集所有需要扫描的源码目录
        List<File> sourceRoots = collectSourceRoots();
        getLog().info("将扫描 " + sourceRoots.size() + " 个源码目录");

        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        if (!outputDirectory.exists()) {
            getLog().warn("编译输出目录不存在: " + outputDirectory +
                    " 请确保项目已编译。");
        } else {
            getLog().debug("使用编译输出目录: " + outputDirectory);
        }

        try {
            SourceEnumChecker checker = new SourceEnumChecker(
                    outputDirectory.toPath(), getLog());

            List<DuplicateInfo> allSingleDuplicates = new ArrayList<>();
            List<CompositeDuplicateInfo> allCompositeDuplicates = new ArrayList<>();

            // 扫描所有收集到的源码目录
            for (File sourceRoot : sourceRoots) {
                if (!sourceRoot.exists()) {
                    getLog().debug("源码目录不存在，跳过: " + sourceRoot);
                    continue;
                }
                getLog().debug("扫描源码目录: " + sourceRoot);
                SourceEnumChecker.CheckResult result = checker.checkDirectory(sourceRoot.toPath());
                allSingleDuplicates.addAll(result.getSingleDuplicates());
                allCompositeDuplicates.addAll(result.getCompositeDuplicates());
            }

            // 检查结果处理
            if (allSingleDuplicates.isEmpty() && allCompositeDuplicates.isEmpty()) {
                getLog().info("检查完成，未发现重复枚举值。");
                getLog().info("============================================");
                return;
            }

            // 打印详细错误报告
            String errorMessage = formatErrorMessage(allSingleDuplicates, allCompositeDuplicates);
            getLog().error(errorMessage);

            int total = allSingleDuplicates.size() + allCompositeDuplicates.size();
            getLog().info("共发现 " + total + " 处重复值。");

            if (failOnError) {
                throw new MojoFailureException(
                        "发现 " + total + " 处枚举重复值，构建失败。");
            } else {
                getLog().warn("由于 failOnError=false，尽管存在重复，构建继续。");
            }

            getLog().info("============================================");

        } catch (IOException e) {
            throw new MojoExecutionException("扫描源码时发生 I/O 错误", e);
        }
    }

    // -------------------------------------------------------------------------
    // 收集源码目录（支持多模块）
    // -------------------------------------------------------------------------

    /**
     * 保存源码目录和对应的编译输出目录的配对。
     */
    private static class SourceRootWithOutput {
        private final File sourceRoot;
        private final File outputDir;

        public SourceRootWithOutput(File sourceRoot, File outputDir) {
            this.sourceRoot = sourceRoot;
            this.outputDir = outputDir;
        }

        public File getSourceRoot() {
            return sourceRoot;
        }

        public File getOutputDir() {
            return outputDir;
        }
    }

    /**
     * 收集所有需要扫描的源码目录及其对应的编译输出目录。
     * <p>如果 {@link #scanSubmodules} 为 true，则递归收集当前项目
     * 以及所有子模块的编译源码根目录。
     *
     * @return 所有需要扫描的(源码目录, 输出目录)配对列表
     */
    private List<SourceRootWithOutput> collectSourceRootsWithOutput() {
        List<SourceRootWithOutput> result = new ArrayList<>();

        // 添加当前项目的主源码目录
        addMainSourceRootsWithOutput(project, result);

        // 如果启用了扫描子模块，则添加所有子模块
        if (scanSubmodules) {
            collectSubmoduleSourceRootsWithOutput(project, result);
        }

        getLog().debug("共收集到 " + result.size() + " 个源码目录");
        return result;
    }

    /**
     * 将给定项目的主源码目录添加到列表中，同时记录对应的输出目录。
     *
     * @param proj   项目
     * @param result 结果列表
     */
    private void addMainSourceRootsWithOutput(MavenProject proj, List<SourceRootWithOutput> result) {
        @SuppressWarnings("unchecked")
        List<String> compileSourceRoots = proj.getCompileSourceRoots();
        File outputDir = new File(proj.getBuild().getOutputDirectory());
        for (String root : compileSourceRoots) {
            File rootFile = new File(root);
            if (rootFile.exists()) {
                result.add(new SourceRootWithOutput(rootFile, outputDir));
                getLog().debug("添加源码目录: " + rootFile.getAbsolutePath() + " -> " + outputDir.getAbsolutePath());
            }
        }
    }

    /**
     * 递归收集所有子模块的源码目录和输出目录。
     *
     * @param proj   当前项目
     * @param result 结果列表
     */
    private void collectSubmoduleSourceRootsWithOutput(MavenProject proj, List<SourceRootWithOutput> result) {
        @SuppressWarnings("unchecked")
        List<MavenProject> modules = proj.getCollectedProjects();
        if (modules != null && !modules.isEmpty()) {
            for (MavenModule module : modules) {
                addMainSourceRootsWithOutput(module, result);
                // 递归处理子模块的子模块（支持多级嵌套）
                collectSubmoduleSourceRootsWithOutput(module, result);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 错误报告格式化
    // -------------------------------------------------------------------------

    /**
     * 格式化错误报告，将所有重复信息整理为可读格式。
     *
     * @param singleDuplicates    单独字段重复列表
     * @param compositeDuplicates 组合字段重复列表
     * @return 格式化的多行报告
     */
    private String formatErrorMessage(List<DuplicateInfo> singleDuplicates,
            List<CompositeDuplicateInfo> compositeDuplicates) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("发现枚举重复值：\n");
        sb.append("============================================\n");
        sb.append("\n");

        int count = 1;

        // 输出单独字段重复
        for (DuplicateInfo dup : singleDuplicates) {
            sb.append(String.format("%d. 枚举类: %s%n", count++, dup.getEnumClassName()));
            sb.append(String.format("   字段:    %s%n", dup.getFieldName()));
            sb.append(String.format("   重复值:  %s%n", dup.getValue()));
            sb.append(String.format("   出现于:  %s%n",
                    dup.getEnumConstants().stream().collect(Collectors.joining(", "))));
            sb.append("\n");
        }

        // 输出组合字段重复
        for (CompositeDuplicateInfo dup : compositeDuplicates) {
            sb.append(String.format("%d. 枚举类: %s%n", count++, dup.getEnumClassName()));
            sb.append(String.format("   字段组合: %s%n", dup.formatFieldNames()));
            sb.append(String.format("   重复值:  %s%n", dup.formatValues()));
            sb.append(String.format("   出现于:  %s%n",
                    dup.getEnumConstants().stream().collect(Collectors.joining(", "))));
            sb.append("\n");
        }

        int total = singleDuplicates.size() + compositeDuplicates.size();
        sb.append("合计: ").append(total).append(" 处重复值。\n");
        return sb.toString();
    }
}