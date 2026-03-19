# Maven Enum Check Plugin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.coder-knock/enum-check-maven-plugin.svg)](https://search.maven.org/artifact/io.github.coder-knock/enum-check-maven-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[中文文档](README.md) | English

A Maven plugin that checks for duplicate values in specified fields of Java enums during the build process, helping you catch enum constant definition errors early.

## ✨ Features

- 🎯 **Annotation-Driven**: Only checks enums annotated with `@EnumCheck`, no impact on other code
- 🔍 **Single Field Check**: Ensures values of a specific field (e.g., `code`) are unique
- 🧩 **Composite Field Check**: Ensures combinations of multiple fields are unique
- 📦 **Multi-module Support**: Automatically scans current project and all submodules
- ⚙️ **Flexible Configuration**: Configure whether to fail the build on duplicates, supports command-line parameter overrides
- 🚀 **Source Code Parsing**: Uses JBoss Forge Roaster to parse source code, no dependency on compiled classes

## 📖 Usage

### 1. Configure the plugin in your pom.xml

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.coderknock</groupId>
      <artifactId>enum-check-maven-plugin</artifactId>
      <version>2.0.0</version>
      <executions>
        <execution>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <!-- Fail the build when duplicates are found (default: true) -->
        <failOnError>true</failOnError>
        <!-- Scan all submodules in a multi-module project (default: true) -->
        <scanSubmodules>true</scanSubmodules>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### 2. Add the `@EnumCheck` annotation to your enum class

First add the dependency:

```xml
<dependency>
  <groupId>io.github.coderknock</groupId>
  <artifactId>enum-check-maven-plugin</artifactId>
  <version>2.0.0</version>
  <scope>provided</scope>
</dependency>
```

#### Example 1: Single Field Check

Check that each value of the `code` field is unique:

```java
import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

@EnumCheck({"code"})
public enum Status {
    OK(200),
    NOT_FOUND(404),
    SERVER_ERROR(500),
    DUPLICATE_ERROR(500); // ⚠️ code=500 is duplicated, plugin will report error

    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

#### Example 2: Multiple Single Field Check

Check that both `code` and `name` fields are unique individually:

```java
@EnumCheck({"code", "name"})
public enum Role {
    ADMIN(1, "admin"),
    USER(2, "user"),
    GUEST(3, "admin"); // ⚠️ name="admin" is duplicated, plugin will report error

    private final int code;
    private final String name;
    // ...
}
```

#### Example 3: Composite Field Check

Check that the combination of multiple fields is unique (e.g., `type + code` combination must be unique):

```java
import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;
import io.github.coderknock.maven.plugin.enumcheck.annotation.CheckGroup;

@EnumCheck(
        groups = @CheckGroup(fields = {"type", "code"})
)
public enum Product {
    FOOD(1, 100),
    DRINK(1, 101),
    CLOTHING(2, 100),
    ELECTRONICS(1, 100); // ⚠️ type + code = (1, 100) is duplicated, plugin will report error

    private final int type;
    private final int code;
    // ...
}
```

#### Example 4: Multiple Check Groups

You can define multiple independent composite check groups:

```java
@EnumCheck(
    groups = {
        @CheckGroup(fields = {"type", "code"}),  // Group 1: type + code must be unique
        @CheckGroup(fields = {"category", "name"})  // Group 2: category + name must be unique
    }
)
public enum Goods {
    // ...
}
```

#### Example 5: Mix Single Field and Composite Checks

Perform both single field checks and composite checks simultaneously:

```java
@EnumCheck(
    value = "code",         // code must be unique by itself
    groups = @CheckGroup(fields = {"type", "name"})  // type + name combination must be unique
)
public enum MyEnum {
    // ...
}
```

#### Example 6: Automatic Check of All Fields

If you don't specify `value` and `groups`, the plugin automatically checks all non-static instance fields in the enum (each field is checked individually):

```java
@EnumCheck
public enum AutomaticCheck {
    // All non-static instance fields will be checked automatically
}
```

#### Example 7: Disable Check

You can temporarily disable checking for a specific enum with `enabled = false`:

```java
@EnumCheck(enabled = false)
public enum DisabledEnum {
    // This enum will not be checked
}
```

### 3. Run the Check

By default, the plugin is bound to the `process-classes` lifecycle phase, and will execute automatically with `mvn process-classes` or `mvn package`:

```bash
mvn process-classes
```

You can also execute it manually:

```bash
mvn enum-check:check
```

If you just want to compile and run the check:

```bash
mvn compile enum-check:check
```

### Execute Automatically at compile Phase

If you want the plugin to execute automatically when running `mvn compile`, you can explicitly bind it to the `compile` phase:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.coderknock</groupId>
      <artifactId>enum-check-maven-plugin</artifactId>
      <version>2.0.0</version>
      <executions>
        <execution>
          <phase>compile</phase>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <failOnError>true</failOnError>
        <scanSubmodules>true</scanSubmodules>
      </configuration>
    </plugin>
  </plugins>
</build>
```

After this configuration, running `mvn compile` will automatically run the enum check.

## ⚙️ Configuration Parameters

| Parameter | Description | Default | Command-line Override |
|-----------|-------------|---------|------------------------|
| `failOnError` | Whether to fail the build when duplicate values are found. `true` aborts the build, `false` only prints warnings | `true` | `-Denumcheck.failOnError=false` |
| `scanSubmodules` | Whether to scan all submodules in a multi-module project | `true` | `-Denumcheck.scanSubmodules=false` |

## 📋 Output Example

When duplicate values are found, the plugin outputs a detailed report:

```
[ERROR] Found duplicate enum values:
[ERROR] ============================================
[ERROR]
[ERROR] 1. Enum class: test.enums.BadEnumSingleDuplicate
[ERROR]    Field:    code
[ERROR]    Duplicate value:  100
[ERROR]    Found in:  FIRST, SECOND
[ERROR]
[ERROR] 2. Enum class: test.enums.BadEnumCompositeDuplicate
[ERROR]    Field combination: type + code
[ERROR]    Duplicate value:  (1, 100)
[ERROR]    Found in:  FOOD, DRINK
[ERROR]
[ERROR] Total: 2 duplicate values found.
[INFO] Found 2 duplicate values in total.
[ERROR] Failed to execute goal io.github.coderknock:enum-check-maven-plugin:2.0.0:check (default) on project demo: Found 2 duplicate enum values, build failed.
```

## 🏗️ Project Structure

```
maven-enum-check-plugin/
├── src/
│   ├── main/
│   │   └── java/io/github/coderknock/maven/plugin/enumcheck/
│   │       ├── annotation/          # Annotation definitions
│   │       │   ├── CheckGroup.java
│   │       │   └── EnumCheck.java
│   │       ├── CompositeDuplicateInfo.java
│   │       ├── DuplicateInfo.java
│   │       ├── EnumCheckMojo.java      # Maven plugin entry point
│   │       └── SourceEnumChecker.java   # Core checking logic
│   └── test/
│       └── java/
│           ├── test/SourceEnumCheckerTest.java  # Unit tests
│           └── test/enums/                 # Test enum examples
└── pom.xml
```

## 🔧 Local Development Build

```bash
# Clone the project
git clone https://github.com/coder-knock/enum-check-maven-plugin.git
cd maven-enum-check-plugin

# Build package
mvn clean package -DskipTests

# Run all tests
mvn clean test
```

## 📝 Change Log

### 2.0.0
- ✨ Complete rewrite to **annotation-driven** version, uses source code parsing
- ✨ Added composite field check support (via `CheckGroup`)
- ✨ Added recursive scanning for multi-module projects
- ✨ Removed ASM dependency, switched to JBoss Forge Roaster
- ✨ Better error report format

### 1.0.0
- Initial release, based on ASM scanning of class files

## 🤝 Contributing

Issues and Pull Requests are welcome!

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed contributing guidelines.

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).

## 📄 License

MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

[coderknock](https://github.com/coder-knock)

- Technical Blog: https://coderknock.blog.csdn.net
- GitHub: https://github.com/coder-knock

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=coder-knock/maven-enum-check-plugin&type=Date)](https://star-history.com/#coder-knock/maven-enum-check-plugin&Date)