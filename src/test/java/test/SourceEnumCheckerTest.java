package test;

import com.google.inject.internal.util.Lists;
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
 * Unit tests for {@link SourceEnumChecker}.
 * <p>
 * Tests various scenarios:
 * <ul>
 *   <li>Enums without duplicates should pass</li>
 *   <li>Single-field duplicates should be detected</li>
 *   <li>Composite-field duplicates should be detected</li>
 *   <li>Both individual and composite checks work together</li>
 *   <li>enabled=false should skip the check</li>
 *   <li>String duplicates should be detected</li>
 * </ul>
 */
class SourceEnumCheckerTest {

    /**
     * Test source directory: src/test/java/test/enums
     */
    private String testSourceDir;

    /**
     * Compiled output directory: target/test-classes
     */
    private String outputDir;

    /**
     * Logger, uses Maven's SystemStreamLog to output to console
     */
    private SystemStreamLog log;

    @BeforeEach
    void setUp() {
        // Get the base directory of the current project
        String baseDir = System.getProperty("user.dir");
        testSourceDir = baseDir + "/src/test/java/test/enums";
        outputDir = baseDir + "/target/test-classes";
        log = new SystemStreamLog();
    }

    /**
     * Get the path for a single enum test file.
     *
     * @param fileName Name of the enum file
     * @return Path to the enum file
     */
    private Path getEnumFile(String fileName) {
        return Paths.get(testSourceDir, fileName);
    }

    /**
     * Test: Annotated enum without duplicates should return an empty result.
     */
    @Test
    void testGoodEnum_NoDuplicates() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("GoodEnumWithAnnotation.java"));

        // GoodEnumWithAnnotation has no duplicates, should pass the check
        assertTrue(result.getSingleDuplicates().isEmpty());
        assertTrue(result.getCompositeDuplicates().isEmpty());
        assertFalse(result.hasDuplicates());
        assertEquals(0, result.getTotalDuplicates());
    }

    /**
     * Test: When a single field has duplicates, it should be detected correctly.
     */
    @Test
    void testBadEnumSingleDuplicate_DetectsDuplicate() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumSingleDuplicate.java"));

        // Should detect one single-field duplicate (code=100)
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
     * Test: When a combination of fields has duplicates, it should be detected correctly.
     */
    @Test
    void testBadEnumCompositeDuplicate_DetectsCompositeDuplicate() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumCompositeDuplicate.java"));

        // BadEnumCompositeDuplicate has one composite duplicate
        int totalSingle = result.getSingleDuplicates().size();
        int totalComposite = result.getCompositeDuplicates().size();

        assertTrue(result.hasDuplicates());
        assertEquals(0, totalSingle);
        assertEquals(1, totalComposite);
        assertEquals(1, result.getTotalDuplicates());

        CompositeDuplicateInfo duplicate = result.getCompositeDuplicates().get(0);
        assertEquals("test.enums.BadEnumCompositeDuplicate", duplicate.getEnumClassName());
        assertEquals(Lists.newArrayList("type", "code"), duplicate.getFieldNames());
        assertEquals(Lists.newArrayList(1, 100), duplicate.getValues());
        assertEquals(2, duplicate.getEnumConstants().size());
        assertTrue(duplicate.getEnumConstants().contains("FOOD"));
        assertTrue(duplicate.getEnumConstants().contains("DRINK"));
        assertEquals("type + code", duplicate.formatFieldNames());
        assertEquals("(1, 100)", duplicate.formatValues());
    }

    /**
     * Test: Using both individual and composite checks together works correctly.
     */
    @Test
    void testMixedCheck_BothWork() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumMixedCheck.java"));

        // In BadEnumMixedCheck:
        // - code=101 is duplicated (individual check) among PRODUCT_A, PRODUCT_C, PRODUCT_D → 1 duplicate
        // - (type, category) = (1, electronics) → duplicated between PRODUCT_A and PRODUCT_B → 1
        // - (type, category) = (2, clothing) → duplicated between PRODUCT_C and PRODUCT_D → 1
        // So total is 1 individual + 2 composite = 3 duplicates in total
        assertTrue(result.hasDuplicates());
        assertEquals(1, result.getSingleDuplicates().size());
        assertEquals(2, result.getCompositeDuplicates().size());
        assertEquals(3, result.getTotalDuplicates());

        DuplicateInfo singleDuplicate = result.getSingleDuplicates().get(0);
        assertEquals("test.enums.BadEnumMixedCheck", singleDuplicate.getEnumClassName());
        assertEquals("code", singleDuplicate.getFieldName());
        assertEquals(101, singleDuplicate.getValue());
        assertEquals(3, singleDuplicate.getEnumConstants().size());

        // Verify first composite duplicate (1, electronics)
        boolean foundFirst = result.getCompositeDuplicates().stream()
                .anyMatch(d -> d.getValues().equals(Lists.newArrayList(1, "electronics")));
        assertTrue(foundFirst);

        // Verify second composite duplicate (2, clothing)
        boolean foundSecond = result.getCompositeDuplicates().stream()
                .anyMatch(d -> d.getValues().equals(Lists.newArrayList(2, "clothing")));
        assertTrue(foundSecond);
    }

    /**
     * Test: When enabled=false, the enum should be skipped even if it has duplicates.
     */
    @Test
    void testDisabledEnum_SkipsCheck() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);

        // EnumDisabledByAnnotation has duplicates but is disabled, should not appear in results
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("EnumDisabledByAnnotation.java"));

        // Since checking is disabled, no duplicates should be reported
        assertFalse(result.hasDuplicates());
        assertEquals(0, result.getTotalDuplicates());
        assertTrue(result.getSingleDuplicates().isEmpty());
        assertTrue(result.getCompositeDuplicates().isEmpty());
    }

    /**
     * Test: Duplicates in String-type fields should be detected correctly.
     */
    @Test
    void testStringDuplicate_DetectsCorrectly() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkFile(getEnumFile("BadEnumStringDuplicate.java"));

        // Should detect "active" duplicate
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
     * Test: When scanning an entire directory, the total duplicate count is correct.
     * <p>
     * The test directory contains multiple duplicates, verify the total count is correct:
     * <ul>
     *   <li>BadEnumSingleDuplicate: 1 individual duplicate</li>
     *   <li>BadEnumCompositeDuplicate: 1 composite duplicate</li>
     *   <li>BadEnumMixedCheck: 1 individual + 2 composite = 3</li>
     *   <li>BadEnumStringDuplicate: 1 individual duplicate</li>
     *   <li>Good...: 0</li>
     *   <li>Disabled: 0</li>
     * </ul>
     * Total should be 1+1+3+1 = 6 duplicates.
     */
    @Test
    void testTotalCount_Correct() throws IOException {
        SourceEnumChecker checker = new SourceEnumChecker(Paths.get(outputDir), log);
        SourceEnumChecker.CheckResult result = checker.checkDirectory(Paths.get(testSourceDir));

        // Expected calculation:
        // BadEnumSingleDuplicate (1) +
        // BadEnumCompositeDuplicate (1) +
        // BadEnumMixedCheck (3) +
        // BadEnumStringDuplicate (1) = Total 6
        // Individual: BadEnumSingleDuplicate(1) + BadEnumMixedCheck(1) + BadEnumStringDuplicate(1) = 3
        // Composite: BadEnumCompositeDuplicate(1) + BadEnumMixedCheck(2) = 3
        assertEquals(3, result.getSingleDuplicates().size());
        assertEquals(3, result.getCompositeDuplicates().size());
        assertEquals(6, result.getTotalDuplicates());
        assertTrue(result.hasDuplicates());
    }
}