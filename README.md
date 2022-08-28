# Very Many Players

[![Github-CI](https://github.com/RelativityMC/VMP-fabric/workflows/build/badge.svg)](https://github.com/RelativityMC/VMP-fabric/actions/workflows/build.yml)
[![Build Status](https://ci.codemc.io/job/RelativityMC/job/VMP-fabric/job/ver%252F1.19.2/badge/icon)](https://ci.codemc.io/job/RelativityMC/job/VMP-fabric/job/ver%252F1.18/)
[![Discord](https://img.shields.io/discord/756715786747248641?logo=discord&logoColor=white)](https://discord.io/ishlandbukkit)

A Fabric mod designed to improve server performance at high playercounts. 

**VMP is still in early development and things may break. Please report any issues to our issue tracker.**

## So what is VMP? 
Very Many Players, or VMP for short, is a Fabric mod designed to improve general server performance at high playercount 
**without sacrificing vanilla functionality or behavior**.  
For the best performance it is recommended to use VMP along with [Lithium](https://modrinth.com/mod/lithium).

## How VMP achieves its performance improvements?

*This list may contain features currently unreleased and only found in development builds*

**Server-side game logic performance improvements:**  
- Uses area maps to optimize nearby packet sending and player lookups
- Uses cache to optimize entity trackers, fluid state lookups, ingredient matching and biome lookup
- Optimizes natural spawning with caches and other tricks
- Optimizes entity tracking with area maps
- Optimizes entity iteration for collisions
- Optimizes ticket propagator using MCUtil from the Paper project (patch licensed under MIT)
- Makes initial chunk loading async on player login
- Makes several commands run async **only when issued by a player**

**Client-side game logic performance improvements:**  
- Makes time source to use built-in Java time source instead of GLFW via JNI calls

**Networking performance & responsiveness improvements:**
- Uses our own chunk sending mechanism (optionally with packet-level rate-limiting)
- Adds packet-level per-player render distance
- Makes vanilla tcp connections more responsive using packet priority from raknetify  
  (works best when the server is connected **without reverse proxies such as Velocity and SSH port forwarding**)
- Mitigates several kinds of bot attacks with split event loops and optimizations

**Other improvements:**
- Uses AsyncAppender to improve logging performance and keep logging IO off the main thread

**... and more**

## Support
[Issue tracker](https://github.com/RelativityMC/VMP-fabric/issues)  
[Discord server](https://discord.gg/Kdy8NM5HW4)

## Building and setting up
_Requires Java 17 or later, both to build it and to use it._  
Use Git to clone this repository, **do NOT download zip**  
Run the following commands in the project directory:

```shell
./gradlew clean build
```

## License
License information can be found [here](/LICENSE).


