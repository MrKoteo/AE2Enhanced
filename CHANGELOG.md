## AE2Enhanced 1.2.0

**For**: Minecraft 1.12.2 / Forge 14.23.5.2768+ / AE2-UEL v0.56.7+

---

### ✨ New: Hyperdimensional Storage Nexus (Phase 2)

The centerpiece of this update — the **Hyperdimensional Storage Nexus**, a 21-block flat multiblock storage structure designed to eliminate late-game AE2 network capacity bottlenecks.

- **BigInteger-level capacity**: Single-structure storage with no theoretical upper bound, completely bypassing `int`/`long` limits
- **Multi-type support**: Native support for items and fluids; optional support for Mekanism gases and Thaumcraft essentia (auto-enabled when corresponding mods are installed)
- **External file persistence**: Data stored in `<world>/ae2enhanced/storage/<uuid>.dat` using compressed NBT + atomic writes, fundamentally preventing NBT overflow and save lag
- **Safe mode**: Automatically enters read-only state on file exceptions to prevent data corruption; supports safe mod version updates — items are not lost when updating
- **Third-party extension API**: Other mods can register custom storage types via `registerExternalAdapter()`
- **Holographic projection**: TESR above the structure (dual rotating cubes + rings + pulsing core); render toggle and max distance are configurable
- **Energy particles**: Enchantment-table-style particles converge toward the multiblock center while the structure is online

### ⚙️ New: Configuration System

Located at `config/ae2enhanced.cfg`:

| Parameter | Description |
|---|---|
| `flushIntervalSeconds` | Auto-flush interval for storage files (seconds, default: 5) |
| `enableHyperdimensionalRenderer` | Enable effect rendering (default: true) |
| `renderDistance` | Maximum render distance in blocks (default: 64) |
| `damageMode` | Black hole damage mode: ALL / NON_CREATIVE / NONE (default: ALL) |

### 🔧 Improvements & Fixes

- **Recipes**: Added crafting recipes for all 4 Hyperdimensional Storage Nexus blocks
- **Localization**: Completed en_us / zh_cn / ru_ru language files; fixed missing Speed Upgrade Card display name
- **Black hole**: Removed the "banish to random coordinates after 16 failed kills" fallback from the Assembly Hub event horizon; added `damageMode` config so creative players can be made immune
- **Rendering fixes**: Corrected the TESR render-distance calculation error in the hyperdimensional controller; particles now spawn around the multiblock geometric center instead of the controller block

---

### 📥 Dependencies

**Required**: Minecraft 1.12.2 · Forge 14.23.5.2768+ · AE2-UEL v0.56.7+ · MixinBooter 8.9+

**Optional**: Mekanism + MekanismEnergistics (gas storage) · Thaumcraft + Thaumic Energistics (essentia storage)
