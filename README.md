## A docker container for SHACL validation 

This project provides a Docker container for validating RDF data against [SHACL](https://www.w3.org/TR/shacl/) (Shapes Constraint Language) shapes. 
It uses Scala 3 and ZIO for the CLI application, and Apache Jena with TopBraid SHACL for the validation logic.

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