# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Scala CLI application for SHACL (Shapes Constraint Language) validation. 
It validates RDF data against SHACL shapes and generates validation reports. 
The application is containerized using Docker and built with SBT.

## Common Development Commands

### Building and Running

- `./sbtx compile` - Compile the Scala code
- `./sbtx run` - Run the application (shows usage)
- `./sbtx "run --help"` - Show detailed help
- `just usage` - Print usage via justfile
- `just help` - Print detailed help via justfile

### Testing

- `./sbtx test` - Run all tests using ZIO Test framework
- Tests are located in `src/test/scala/swiss/dasch/shacl/cli/`

### Docker Operations

- `just docker-build` - Build Docker image using sbt-native-packager
- `just docker-build-run-validate <shacl> <data>` - Build and run validation
- Manual Docker run: `docker run --rm -v $(pwd):/data daschswiss/shacl-cli:latest validate --shacl /data/shacl.ttl --data /data/data.ttl --report /data/report.ttl`

### SHACL Validation

- `just validate <shacl-file> <data-file>` - Validate using local build
- `./sbtx "run validate --shacl shapes.ttl --data data.ttl --report report.ttl"` - Direct SBT validation

## Architecture

### Core Components

- **Main.scala** (`src/main/scala/swiss/dasch/shacl/cli/Main.scala`): ZIO CLI application entry point with command-line parsing
- **ShaclValidator.scala** (`src/main/scala/swiss/dasch/shacl/cli/ShaclValidator.scala`): Core validation logic using TopBraid SHACL and Apache Jena

### Key Dependencies

- **ZIO**: Functional effects library for Scala 3
- **ZIO CLI**: Command-line interface framework
- **Apache Jena**: RDF processing (version 5.2.0)
- **TopBraid SHACL**: SHACL validation engine (version 1.4.4)
- **ZIO Test**: Testing framework

### Validation Flow

1. Parse CLI arguments for SHACL shapes file, data file, and report output
2. Load RDF models from Turtle files using Jena
3. Execute SHACL validation using TopBraid engine
4. Optionally add blank node references and validation details
5. Write validation report as Turtle format

### Build Configuration

- Uses SBT with Scala 3.3.5
- sbt-native-packager for Docker image creation
- Multi-platform Docker builds (linux/arm64, linux/amd64)
- Base image: eclipse-temurin:21-alpine

## File Structure Notes

- `sbtx`: Enhanced SBT wrapper script (sbt-extras)
- `justfile`: Task runner with common development commands
- Only supports Turtle format for RDF files
- Docker image published as `daschswiss/shacl-cli`