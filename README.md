# Very Many Players

[![Github-CI](https://github.com/RelativityMC/VMP-fabric/workflows/build/badge.svg)](https://github.com/RelativityMC/VMP-fabric/actions/workflows/build.yml)
[![Build Status](https://ci.codemc.io/job/RelativityMC/job/VMP-fabric/job/ver%252F1.18/badge/icon)](https://ci.codemc.io/job/RelativityMC/job/VMP-fabric/job/ver%252F1.18/)
[![Discord](https://img.shields.io/discord/756715786747248641?logo=discord&logoColor=white)](https://discord.io/ishlandbukkit)

A Fabric mod designed to improve server performance at high playercounts. 

**VMP is still in early development and things are expected to break. Please report any issues to our issue tracker.**

## So what is VMP? 
Very Many Players, or VMP for short, is a Fabric mod designed to improve general server performance at high playercount 
**without sacrificing vanilla functionality or behavior**.  
For the best performance it is recommended to use VMP with [Lithium](https://modrinth.com/mod/lithium).

## How VMP achieves its performance improvements?
- Uses area maps to optimize nearby packet sending and player lookups
- Uses cache to optimize entity trackers, fluid state lookups and ingredient matching for mob AIs
- Optimizes entity iteration for collisions
- Optimizes ticket propagator using MCUtil from Paper project (patch licensed under MIT)
- Makes clientside time source to use built-in Java time source instead of JNI calls
- Uses our own chunk sending mechanism (optionally with packet-level rate-limiting)
- Adds per-player render distance (packet-level)
- Makes vanilla tcp connections more responsive using packet priority  
  (works best when the server is connected **without reverse proxies such as velocity and ssh port forwarding**)
- ... and more

## Support
Our issue tracker: [link](https://github.com/RelativityMC/VMP-fabric/issues)  
Our discord server: [link](https://discord.io/ishlandbukkit)

## Building and setting up
JDK 17+ is required to build and use VMP  
Run the following commands in the root directory:

```shell
./gradlew clean build
```

## License
License information can be found [here](/LICENSE).


