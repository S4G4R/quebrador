## Quebrador

A zip file password cracker.

Reference: [Coding Challenge 66](https://codingchallenges.substack.com/p/coding-challenge-66-zip-file-cracker)

### Prerequisites

[unzip](https://linux.die.net/man/1/unzip)
[Babashka](https://github.com/babashka/babashka)

### Usage

#### Help Menu

```bash
bb -m quebrador.main -h
```

#### Dictionary Attack

```bash
bb -m quebrador.main -d dictionary.txt -f file.zip
```
