# List all recipies
default:
    @just --list

docker-build:
    @echo "Building the Docker image"
    ./sbtx "root/Docker/publishLocal"
    @echo "Docker image built"
    @echo "To run the image, use:"
    @echo "docker run daschswiss/shacl-cli:latest"
