## A docker container for SHACL validation 
### Usage

Build the container:
```zsh
just docker-build
```

Run the container:
```zsh
docker run --rm -v `pwd`:/data daschswiss/shacl-cli:latest validate /data/shacl.ttl /data/data.ttl /data/report.ttl
```

### CLI Usage

```
❯ docker run --rm daschswiss/shacl-cli:latest
   _____ __  _____   ________      ________    ____
  / ___// / / /   | / ____/ /     / ____/ /   /  _/
  \__ \/ /_/ / /| |/ /   / /     / /   / /    / /
 ___/ / __  / ___ / /___/ /___  / /___/ /____/ /
/____/_/ /_/_/  |_\____/_____/  \____/_____/___/



SHACL CLI 0.0.1 -- Validate SHACL shapes against data files

USAGE

  $ shacl validate [--validate-shapes] [--report-details] [--add-blank-nodes] <shacl.ttl> <data.ttl> <report.ttl>

COMMANDS

  - validate [--validate-shapes] [--report-details] [--add-blank-nodes] <shacl.ttl> <data.ttl> <report.ttl>  Validate a SHACL shape against a data file.

```