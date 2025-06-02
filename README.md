## A docker container for SHACL validation 
### Usage

Build the container:
```zsh
just docker-build
```

Run the container:
```zsh
docker run --rm -v `pwd`:/data daschswiss/shacl-cli:latest validate --shacl /data/shacl.ttl --data /data/data.ttl --report /data/report.ttl
```

### CLI Usage

Print the usage message:
```zsh
just usage
```

Print the more detailed help message:
```zsh
just help
```