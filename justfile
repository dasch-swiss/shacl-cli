# List all recipies
default:
    @just --list

docker-build:
    @echo "Building the Docker image"
    ./sbtx "root/Docker/publishLocal"
    @echo "Docker image built"
    @echo "To run the image, use:"
    @echo "docker run --rm -v `pwadd docker buildd`:/data daschswiss/shacl-cli:latest validate /data/shacl.ttl /data/data.ttl /data/report.ttl"
