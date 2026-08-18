# Bingo Net

The following List may not include Bug Fixes. If empty may be only Bug Fixes.

## Bingo Net Changes:

- Party chat commands (prefixes `!`, `.`, `?`) with trust-based permissions:
    - `!pt`, `!ptme`, `!transfer` — transfer the party leader
    - `!pw`, `!warp`, `!warpus` — warp the party
    - `!allinv`, `!allinvite` — enable all invites
    - `!inv [name...]`, `!invite [name...]` — invite players (no name invites the caller)
    - `!inviteme`, `!request` — request to join the party
    - `!kick <name> [name...]` — kick players
    - `!kickoff`, `!kickoffline` — kick offline players
    - `!stream [limit]` — open the party to everyone, with an optional member limit
    - `!limit [amount]` — show the party member limit, set it, or remove it with `!limit 0`
    - `!poll <question> <option1>/<option2>[/...]` — create a party poll (options are split by `/`)
    - `!ping`, `!tps` — current ping/TPS (config toggles)
    - Remote control via private messages: `!accept`, `!accept force` (leaves the current party first), `!leave`, `!disband`
    - Per-user permission overrides editable via the config button or `/shtrustedpartyusers`
- Improved Dependency and its markings in sh config.
- Sh numpad and sh keybinds now support tab event completion from sh itself too.
- Added an option that after a configurable amount of time or when pressing shift the currently hovered over options name and description
  gets shown as a larger tooltip for easier readability
- Improved all GUIs designed by the Bingo Net Team Visually.

## Static Info for modrinth:

Bingo Net is based on SkyHanni as if it were included. This means that it currently has to replace the normal SkyHanni mod to work. It is
not a standalone mod and does not work with the official SkyHanni mod. Due to the long term plan being to merge into official SkyHanni at
some point there are the Third Party Warnings. This is since out of official sh these are third party Servers controlled by me.

You can find most settings under:
`/sh` → Events → Bingo → (Third Party Networks)

It is recommended to also enable the widen config option for most users in the /sh start screen.
