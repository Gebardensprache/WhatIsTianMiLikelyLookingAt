# Jade

[Documentation](https://jademc.readthedocs.io/en/latest/)

Jade is a UI improvement mod which shows information about what you are looking at. Jade is a fork of [HWYLA](https://github.com/TehNut/HWYLA) by TehNut

## 1.12.2 backport (this branch)

This branch (`1.12.2`) is a backport of modern Jade (snapshot of `26.2-neoforge` @ `8808a49a`) to Minecraft 1.12.2 / Forge 14.23.5.2860.

### What works

- Looking-at tooltip for blocks and entities: object name, mod name, and a native Jade provider pipeline (server data sync over Forge `SimpleNetworkWrapper`, client-side component providers).
- Vanilla providers: container inventory (with locked-container indicator), furnace progress, brewing stand fuel/time, horse stats (jump/speed), animal growth and breeding cooldowns, crop growth, item frame rotation, active potion effects, and harvest tool display.
- Forge `Configuration` based config (in-game via the mod list GUI): overlay position/square style, per-provider enable/disable toggles, inventory slot cap (`minecraft.inventory.slots`).
- HWYLA compat layer: mods compiled against HWYLA's API (`mcp.mobius.waila.api`) work unmodified. Their `@WailaPlugin`-annotated plugins are discovered from Forge's annotation data and their providers are bridged into Jade's pipeline (head/body/tail strings, NBT server data, picked-stack override). Legacy `capability.*` config keys are mapped onto Jade's provider toggles.
- TheOneProbe compat layer: mods that send TOP's `getTheOneProbe` IMC function message receive a working `ITheOneProbe`. Their probe-info providers run server-side, the captured elements (text, item, progress, horizontal/vertical layouts) are serialized to NBT and rebuilt as native Jade elements on the client.

### Compat-layer guarantees and limits

- If the real HWYLA (`waila`) or TheOneProbe (`theoneprobe`) is installed, the corresponding shim disables itself with a loud log warning and stays inert — the real mod wins.
- HWYLA: `registerDecorator`, `registerTooltipRenderer`, and `registerFMP*` are logged-once no-ops. Jade renders everything with its own element model.
- TOP: overlay renderer, probe config providers, block/entity display overrides, and custom element factories beyond the captured core set are logged-once no-ops. TOP's probe item/helmet, probe GUI, TOP's own overlay style, and `/topcfg` are out of scope.
- ForgeMultipart, RF/Tesla integrations, REI/JEI bridges, ModMenu, and the theming editor GUI are out of scope.

### Building

- JDK 25 (Azul or any OpenJDK 25). This branch has no wrapper scripts; use a local Gradle 9.6.1 distribution or the IDE.
- `gradle setupDecompWorkspace build` (first run decompiles Minecraft; subsequent builds are incremental). The dev client is `gradle runClient`.
