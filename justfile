# List all recipies
default:
    @just --list

# Build the Docker image for SHACL validation
docker-build:
    @echo "Building the Docker image"
    ./sbtx "root/Docker/publishLocal"
    @echo "Docker image built"
    @echo "To run the image, use:"
    @echo "docker run --rm -v `pwd`:/data daschswiss/shacl-cli:latest validate --shacl /data/shacl.ttl --data /data/data.ttl --report /data/report.ttl"

# Build the Docker image for SHACL validation and run it
docker-build-run-validate shacl data: docker-build
    docker run --rm -v `pwd`:/data daschswiss/shacl-cli:latest validate --shacl /data/shacl.ttl --data /data/data.ttl --report /data/report.ttl

# Validate SHACL data in the current directory, produces report.ttl
validate shacl data:
    @echo "Validating SHACL shapes"
    sbt "run validate --shacl {{shacl}} --data {{data}} --report report.ttl"
    @echo "Validation complete. Check report.ttl for results."

# Prints the usage of the SHACL validation command
usage:
    ./sbtx run

# Prints the detailed help of the SHACL validation command
help:
    ./sbtx "run --help"
