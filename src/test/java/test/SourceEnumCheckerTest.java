package test;

import io.github.coderknock.maven.plugin.enumcheck.CompositeDuplicateInfo;
import io.github.coderknock.maven.plugin.enumcheck.DuplicateInfo;
import io.github.coderknock.maven.plugin.enumcheck.SourceEnumChecker;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SourceEnumChecker} 的单元测试。
 * <p>
 * 测试各种场景：
 * <ul>
 *   <li>无重复的枚举应该通过</li>
 *   <li>单个字段重复应该被检测</li>
 *   <li>组合字段重复应该被检测</li>
 *   <li>同时有单独和组合检查都能工作</li>
 *   <li>disabled=false 应该跳过检查</li>
 *   <li>字符串重复应该被检测</li>
 * </ul>
 */
class SourceEnumCheckerTest {

    /**
     * 测试源码目录：src/test/java/test/enums
     */
    private String testSourceDir;

    /**
     * 编译输出目录：target/test-classes
     */
    private String outputDir;

    /**
     * 日志，使用 Maven 的 SystemStreamLog 输出到控制台
     */
    private SystemStreamLog log;

    @BeforeEach
    void setUp() {
        // 获取当前项目的基础目录
        String baseDir = System.getProperty("user.dir");
        testSourceDir = baseDir + "/src/test/java/test/enums";
        outputDir = baseDir + "/target/test-classes";
        log = new SystemStreamLog();
    }

    /**
     * 获取单个枚举文件的路径。
     */
    private Path getEnumFile(String fileName) {
        return Paths.get(testSourceDir, fileName);
    }

    /**
     * 测试：没有重复的带注解枚举，应该返回空结果。
     */
    @Test
    void testGoodEnum_NoDuplicates() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("GoodEnumWithAnnotation.java"));

        // GoodEnumWithAnnotation 没有重复，应该检查通过
        assertTrue(result.getSingleDuplicates().isEmpty());
        assertTrue(result.getCompositeDuplicates().isEmpty());
        assertFalse(result.hasDuplicates());
        assertEquals(0, result.getTotalDuplicates());
    }

    /**
     * 测试：单个字段有重复，应该正确检测。
     */
    @Test
    void testBadEnumSingleDuplicate_DetectsDuplicate() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumSingleDuplicate.java"));

        // 应该检测到一处单独字段重复（code=100）
        assertFalse(result.getSingleDuplicates().isEmpty());
        assertTrue(result.getCompositeDuplicates().isEmpty());
        assertTrue(result.hasDuplicates());
        assertEquals(1, result.getTotalDuplicates());

        DuplicateInfo duplicate = result.getSingleDuplicates().get(0);
        assertEquals("test.enums.BadEnumSingleDuplicate", duplicate.getEnumClassName());
        assertEquals("code", duplicate.getFieldName());
        assertEquals(100, duplicate.getValue());
        assertEquals(2, duplicate.getEnumConstants().size());
        assertTrue(duplicate.getEnumConstants().contains("FIRST"));
        assertTrue(duplicate.getEnumConstants().contains("SECOND"));
    }

    /**
     * 测试：组合字段有重复，应该正确检测。
     */
    @Test
    void testBadEnumCompositeDuplicate_DetectsCompositeDuplicate() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumCompositeDuplicate.java"));

        // BadEnumCompositeDuplicate 有一处组合重复
        int totalSingle = result.getSingleDuplicates().size();
        int totalComposite = result.getCompositeDuplicates().size();

        assertTrue(result.hasDuplicates());
        assertEquals(0, totalSingle);
        assertEquals(1, totalComposite);
        assertEquals(1, result.getTotalDuplicates());

        CompositeDuplicateInfo duplicate = result.getCompositeDuplicates().get(0);
        assertEquals("test.enums.BadEnumCompositeDuplicate", duplicate.getEnumClassName());
        assertEquals(List.of("type", "code"), duplicate.getFieldNames());
        assertEquals(List.of(1, 100), duplicate.getValues());
        assertEquals(2, duplicate.getEnumConstants().size());
        assertTrue(duplicate.getEnumConstants().contains("FOOD"));
        assertTrue(duplicate.getEnumConstants().contains("DRINK"));
        assertEquals("type + code", duplicate.formatFieldNames());
        assertEquals("(1, 100)", duplicate.formatValues());
    }

    /**
     * 测试：同时使用单独检查和组合检查，都能正常工作。
     */
    @Test
    void testMixedCheck_BothWork() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumMixedCheck.java"));

        // BadEnumMixedCheck 中：
        // - code=101 重复（单独检查）出现在 PRODUCT_A, PRODUCT_C, PRODUCT_D 三个常量 → 1 处重复
        // - (type, category) = (1, electronics) → 重复在 PRODUCT_A 和 PRODUCT_B → 1 处
        // - (type, category) = (2, clothing) → 重复在 PRODUCT_C 和 PRODUCT_D → 1 处
        // 所以总共有 1 个单独重复 + 2 个组合重复 = 3 处重复
        assertTrue(result.hasDuplicates());
        assertEquals(1, result.getSingleDuplicates().size());
        assertEquals(2, result.getCompositeDuplicates().size());
        assertEquals(3, result.getTotalDuplicates());

        DuplicateInfo singleDuplicate = result.getSingleDuplicates().get(0);
        assertEquals("test.enums.BadEnumMixedCheck", singleDuplicate.getEnumClassName());
        assertEquals("code", singleDuplicate.getFieldName());
        assertEquals(101, singleDuplicate.getValue());
        assertEquals(3, singleDuplicate.getEnumConstants().size());

        // 验证第一个组合重复 (1, electronics)
        boolean foundFirst = result.getCompositeDuplicates().stream()
                .anyMatch(d -> d.getValues().equals(List.of(1, "electronics")));
        assertTrue(foundFirst);

        // 验证第二个组合重复 (2, clothing)
        boolean foundSecond = result.getCompositeDuplicates().stream()
                .anyMatch(d -> d.getValues().equals(List.of(2, "clothing")));
        assertTrue(foundSecond);
    }

    /**
     * 测试：enabled=false 的枚举，即使有重复也应该跳过。
     */
    @Test
    void testDisabledEnum_SkipsCheck() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);

        // EnumDisabledByAnnotation 有重复但是 disabled，不应该出现在结果中
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("EnumDisabledByAnnotation.java"));

        // 因为禁用了检查，所以不应该有任何重复被报告
        assertFalse(result.hasDuplicates());
        assertEquals(0, result.getTotalDuplicates());
        assertTrue(result.getSingleDuplicates().isEmpty());
        assertTrue(result.getCompositeDuplicates().isEmpty());
    }

    /**
     * 测试：字符串类型字段的重复，应该被正确检测。
     */
    @Test
    void testStringDuplicate_DetectsCorrectly() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumStringDuplicate.java"));

        // 应该检测到 "active" 重复
        List<DuplicateInfo> singles = result.getSingleDuplicates();
        boolean found = singles.stream()
                .anyMatch(d ->
                        d.getEnumClassName().equals("test.enums.BadEnumStringDuplicate") &&
                        d.getFieldName().equals("status") &&
                        d.getValue().equals("active") &&
                        d.getEnumConstants().size() == 2);
        assertTrue(found);
        assertEquals(1, result.getTotalDuplicates());
    }

    /**
     * 测试：扫描整个目录时，总重复计数正确。
     * <p>
     * 测试目录中有多个重复，验证总数是否正确：
     * <ul>
     *   <li>BadEnumSingleDuplicate: 1 个单独重复</li>
     *   <li>BadEnumCompositeDuplicate: 1 个组合重复</li>
     *   <li>BadEnumMixedCheck: 1 个单独 + 2 个组合 = 3</li>
     *   <li>BadEnumStringDuplicate: 1 个单独重复</li>
     *   <li>Good...: 0</li>
     *   <li>Disabled: 0</li>
     * </ul>
     * 总共应该是 1+1+3+1 = 6 个重复。
     */
    @Test
    void testTotalCount_Correct() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkDirectory(Paths.get(testSourceDir));

        // 计算预期：
        // BadEnumSingleDuplicate (1) +
        // BadEnumCompositeDuplicate (1) +
        // BadEnumMixedCheck (3) +
        // BadEnumStringDuplicate (1) = 总计 6
        // 单独重复：BadEnumSingleDuplicate(1) + BadEnumMixedCheck(1) + BadEnumStringDuplicate(1) = 3
        // 组合重复：BadEnumCompositeDuplicate(1) + BadEnumMixedCheck(2) = 3
        assertEquals(3, result.getSingleDuplicates().size());
        assertEquals(3, result.getCompositeDuplicates().size());
        assertEquals(6, result.getTotalDuplicates());
        assertTrue(result.hasDuplicates());
    }
}
