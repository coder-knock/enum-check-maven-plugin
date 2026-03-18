# Maven Enum Check Plugin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.coderknock/enum-check-maven-plugin.svg)](https://search.maven.org/artifact/io.github.coderknock/enum-check-maven-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个 Maven 插件，用于在编译阶段检查 Java 枚举（Enum）中指定字段的重复值，帮助你提前发现业务代码中的枚举常量定义错误。

## ✨ 特性

- 🎯 **注解驱动**：只检查你标记了 `@EnumCheck` 的枚举，不影响其他代码
- 🔍 **支持单字段检查**：确保枚举中某个字段（如 `code`）的值全局唯一
- 🧩 **支持组合字段检查**：确保多个字段组合后的值全局唯一
- 📦 **多模块项目支持**：自动扫描当前项目及所有子模块
- ⚙️ **灵活配置**：可配置发现重复后是否中断构建，支持命令行覆盖参数
- 🚀 **源码级别解析**：使用 JBoss Forge Roaster 解析源码，不依赖编译产物

## 📖 使用方法

### 1. 在你的 pom.xml 中配置插件

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
        <!-- 发现重复时让构建失败（默认 true） -->
        <failOnError>true</failOnError>
        <!-- 是否扫描所有子模块（默认 true） -->
        <scanSubmodules>true</scanSubmodules>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### 2. 在你的枚举类上添加 `@EnumCheck` 注解

首先导入依赖：

```xml
<dependency>
  <groupId>io.github.coderknock</groupId>
  <artifactId>enum-check-maven-plugin</artifactId>
  <version>2.0.0</version>
  <scope>provided</scope>
</dependency>
```

#### 示例 1：单字段检查

检查 `code` 字段的每个值必须唯一：

```java
import io.github.coderknock.maven.plugin.enumcheck.annotation.EnumCheck;

@EnumCheck({"code"})
public enum Status {
    OK(200),
    NOT_FOUND(404),
    SERVER_ERROR(500),
    DUPLICATE_ERROR(500); // ⚠️ code=500 重复，插件会报错

    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

#### 示例 2：多个单字段检查

检查 `code` 和 `name` 两个字段各自唯一：

```java
@EnumCheck({"code", "name"})
public enum Role {
    ADMIN(1, "admin"),
    USER(2, "user"),
    GUEST(3, "admin"); // ⚠️ name="admin" 重复，插件会报错

    private final int code;
    private final String name;
    // ...
}
```

#### 示例 3：组合字段检查

检查多个字段的组合必须唯一（例如 `type + code` 组合唯一）：

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
    ELECTRONICS(1, 100); // ⚠️ type + code = (1, 100) 重复，插件会报错

    private final int type;
    private final int code;
    // ...
}
```

#### 示例 4：多个组合分组

可以定义多个独立的组合检查分组：

```java
@EnumCheck(
    groups = {
        @CheckGroup(fields = {"type", "code"}),  // 第一组：type + code 组合唯一
        @CheckGroup(fields = {"category", "name"})  // 第二组：category + name 组合唯一
    }
)
public enum Goods {
    // ...
}
```

#### 示例 5：混合使用单字段和组合检查

同时进行单字段检查和组合检查：

```java
@EnumCheck(
    value = "code",         // code 单独必须唯一
    groups = @CheckGroup(fields = {"type", "name"})  // type + name 组合必须唯一
)
public enum MyEnum {
    // ...
}
```

#### 示例 6：自动检查所有字段

如果不指定 `value` 和 `groups`，默认自动检查枚举类中所有非静态实例字段（每个字段单独验重）：

```java
@EnumCheck
public enum AutomaticCheck {
    // 所有非静态实例字段都会被检查
}
```

#### 示例 7：禁用检查

可以通过 `enabled = false` 临时禁用某个枚举的检查：

```java
@EnumCheck(enabled = false)
public enum DisabledEnum {
    // 这个枚举不会被检查
}
```

### 3. 运行检查

插件默认绑定到 `process-classes` 生命周期阶段，在 `mvn compile` 或 `mvn package` 时会自动执行：

```bash
mvn compile
```

也可以手动执行：

```bash
mvn process-classes enum-check:check
```

## ⚙️ 配置参数

| 参数 | 说明 | 默认值 | 命令行覆盖 |
|------|------|--------|-----------|
| `failOnError` | 发现重复值时是否让构建失败。`true` 则中止构建，`false` 仅打印警告 | `true` | `-Denumcheck.failOnError=false` |
| `scanSubmodules` | 多模块项目中是否扫描所有子模块 | `true` | `-Denumcheck.scanSubmodules=false` |

## 📋 输出示例

当发现重复值时，插件会输出详细报告：

```
[ERROR] 发现枚举重复值：
[ERROR] ============================================
[ERROR]
[ERROR] 1. 枚举类: test.enums.BadEnumSingleDuplicate
[ERROR]    字段:    code
[ERROR]    重复值:  100
[ERROR]    出现于:  FIRST, SECOND
[ERROR]
[ERROR] 2. 枚举类: test.enums.BadEnumCompositeDuplicate
[ERROR]    字段组合: type + code
[ERROR]    重复值:  (1, 100)
[ERROR]    出现于:  FOOD, DRINK
[ERROR]
[ERROR] 合计: 2 处重复值。
[INFO] 共发现 2 处重复值。
[ERROR] Failed to execute goal io.github.coderknock:enum-check-maven-plugin:2.0.0:check (default) on project demo: 发现 2 处枚举重复值，构建失败。
```

## 🏗️ 项目结构

```
maven-enum-check-plugin/
├── src/
│   ├── main/
│   │   └── java/io/github/coderknock/maven/plugin/enumcheck/
│   │       ├── annotation/          # 注解定义
│   │       │   ├── CheckGroup.java
│   │       │   └── EnumCheck.java
│   │       ├── CompositeDuplicateInfo.java
│   │       ├── DuplicateInfo.java
│   │       ├── EnumCheckMojo.java      # Maven 插件入口
│   │       └── SourceEnumChecker.java   # 核心检查逻辑
│   └── test/
│       └── java/
│           ├── test/SourceEnumCheckerTest.java  # 单元测试
│           └── test/enums/                 # 测试用枚举示例
└── pom.xml
```

## 🔧 本地开发构建

```bash
# 克隆项目
git clone https://github.com/coder-knock/maven-enum-check-plugin.git
cd maven-enum-check-plugin

# 编译打包
mvn clean package -DskipTests

# 运行所有测试
mvn clean test
```

## 📝 版本历史

### 2.0.0
- ✨ 完全重写为**注解驱动**版本，使用源码解析
- ✨ 新增组合字段检查支持（通过 `CheckGroup`）
- ✨ 支持多模块项目递归扫描
- ✨ 移除 ASM 依赖，改用 JBoss Forge Roaster
- ✨ 更好的错误报告格式

### 1.0.0
- 初始版本，基于 ASM 扫描 class 文件

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

## 📄 许可证

MIT License - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👨‍💻 作者

[coderknock](https://github.com/coder-knock)

- 技术博客：https://coderknock.blog.csdn.net
- GitHub：https://github.com/coder-knock

## ⭐ Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=coderknock/maven-enum-check-plugin&type=Date)](https://star-history.com/#coderknock/maven-enum-check-plugin&Date)