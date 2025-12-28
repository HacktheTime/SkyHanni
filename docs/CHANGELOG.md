# Bingo Net

The following List does not include Bug Fixes. If empty may be only Bug Fixes.

## Bingo Net Changes:

- Removed 1.8.9 support due to SH dropping it.
    - Note that while it remains possibly to use the older 1.8.9 version published that there are likely going to be breaking changes at
      some point and msot certainly breaking changes in some Features meaning you will loose more and more of the existing and wont get new
      features.

- Added BSC Splash Support
  - You can find it in the Bingo Networks Config
  - Note that this was not tested yet.

- Improved Third Party Warnings (They are now in a state how I would consider them acceptable for sh merge)
  - Added Dependency Reference Support for Features.
  - There are still some things that need some adjustments but it works fine for now.

- Added support for Splashers to open the BB Splash Channel in a browser and have a splash message prepared in clipboard.

- fixed an issue with the chest coords detection for storage api and search.
## Static Info for modrinth:

Bingo Net is based on SkyHanni as if it were included. This means that it currently has to replace the normal SkyHanni mod to work. It is
not a standalone mod and does not work with the official SkyHanni mod. You also have to enable the Third Party Networks in the config
manually, as it is planned to be disabled by default, since from sh perspective it's a third-party network.

To enable do:
`/sh` → Events → Bingo → Third Party Networks →

1) Bingo Net → Use Bingo Net = Off → ON
2) Use Bingo Brewers = OFF → ON
3) Use Bingo Splash Community = OFF → ON

It is recommended to also enable the widen config option for most users in the /sh start screen.
