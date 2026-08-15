# CC UTF-8 Compat

CC UTF-8 Compat adds UTF-8 compatibility patches for CC:Tweaked on Minecraft 1.21.1 NeoForge.

The mod improves UTF-8 text handling in CC:Tweaked terminals and related peripherals. It is mainly intended for modpacks or servers that need non-ASCII text support, such as Russian, Chinese, Spanish accents, and other Unicode characters.

## Features

* UTF-8 text rendering in computer terminals
* UTF-8 input and paste support
* UTF-8 support for computer labels
* UTF-8 terminal synchronization between server and client
* UTF-8 rendering fixes for monitors
* UTF-8 support for pocket computers
* UTF-8 support for printers and printed pages
* Patched CraftOS Lua files for better UTF-8 behavior in selected programs and APIs

## Tested with

* Minecraft 1.21.1
* NeoForge 21.1.247
* CC:Tweaked 1.120.0

## Requirements

* Minecraft 1.21.1
* NeoForge
* CC:Tweaked 1.120.0 (NeoForge)

This mod is version-specific and depends on CC:Tweaked internals. Other CC:Tweaked versions are not guaranteed to work.

## Installation

Install the mod on both client and server.

Required files:

```text
mods/
  cc-tweaked-1.21.1-forge-1.120.0.jar
  cc_utf8_compat-1.0.1-beta.1.jar
```

For singleplayer, place both mods in the client `mods` folder.

For multiplayer, the compat mod must be installed on both sides.

## Building

The project uses NeoForge ModDevGradle (Java 21 required):

```text
gradlew build
```

The resulting jar is placed in `build/libs/`.

## Configuration

After the first launch, the config file will be created:

```text
config/cc_utf8_compat-common.toml
```

The UTF-8 compatibility layer can be enabled or disabled:

```toml
ccUtf8Compat = true
```

Set it to `false` to disable the patches without removing the mod.

## Known limitations

This mod patches internal CC:Tweaked classes using mixins. Because of that, it is only tested against CC:Tweaked 1.120.2.

Newer CC:Tweaked versions may change internal class names, method names, or behavior, which can break compatibility.

## License

This project is licensed under the MIT License.
