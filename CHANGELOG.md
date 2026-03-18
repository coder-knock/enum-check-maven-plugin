# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-03-19

### Added
- Complete rewrite to annotation-driven architecture
- Added composite field check support via `@CheckGroup` annotation
- Added recursive scanning for multi-module projects
- Added automatic checking of all non-static instance fields when no fields are specified
- Added `enabled` attribute to temporarily disable checking on specific enums
- Added command-line parameter override support
- Added English documentation README-EN.md

### Changed
- Replaced ASM with JBoss Forge Roaster for source code parsing
- Improved error report formatting with clearer output
- Changed default lifecycle phase from `compile` to `process-classes`
- Improved documentation with instructions for binding to compile phase

### Removed
- Removed ASM dependency
- Removed legacy class-file scanning approach

## [1.0.0] - 2026-03-08

### Added
- Initial release
- Basic duplicate value checking for enum fields based on ASM class file scanning
