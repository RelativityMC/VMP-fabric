# Very Many Players

[![Github-CI](https://github.com/RelativityMC/VMP-fabric/workflows/build/badge.svg)](https://github.com/RelativityMC/VMP-fabric/actions/workflows/build.yml)
[![Build Status](https://ci.codemc.io/job/RelativityMC/job/VMP-fabric/job/ver%252F1.18/badge/icon)](https://ci.codemc.io/job/RelativityMC/job/VMP-fabric/job/ver%252F1.18/)
[![Discord](https://img.shields.io/discord/756715786747248641?logo=discord&logoColor=white)](https://discord.io/ishlandbukkit)

A Fabric mod designed to improve server performance at high playercounts. 

**VMP is still in early development and things may break. Please report any issues to our issue tracker.**

## So what is VMP? 
Very Many Players (VMP for short) is a Fabric mod designed to improve general server performance at a high player count 
**without sacrificing vanilla functionality or behavior**.  
For the best performance it is recommended to use VMP along with [Lithium](https://modrinth.com/mod/lithium).

## How does VMP achieve performance improvements?

*This list may contain features currently unreleased and only found in development builds*

**Server-side game logic performance improvements:**
- Uses area maps to optimize nearby packet sending and player lookups
- Uses cache to optimize entity trackers, fluid state lookups, ingredient matching and biome lookup
- Optimizes natural spawning with caches and other tricks to reduce the amount of load
- Optimizes entity tracking with area maps
- Optimizes entity iteration for collisions
- Optimizes ticket propagator using MCUtil from PaperMC (patch licensed under MIT)
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

Run the following commands:

```shell
git clone https://github.com/RelativityMC/VMP-fabric
cd VMP-fabric
chmod +x gradlew
./gradlew clean build
```

## License
```
The MIT License (MIT)

Copyright (c) 2021-2022 ishland

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
_It can also be read [here](/LICENSE)_
