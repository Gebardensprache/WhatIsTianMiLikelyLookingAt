 - https://github.com/TehNut/HWYLA/issues/199
 - https://github.com/TehNut/HWYLA/issues/230
 - https://github.com/TehNut/HWYLA/issues/237
 - https://github.com/TehNut/HWYLA/issues/240
 - https://github.com/TehNut/HWYLA/issues/247
 - https://github.com/TehNut/HWYLA/issues/248
 - https://github.com/TehNut/HWYLA/issues/249
 - https://github.com/TehNut/HWYLA/issues/250
 - https://github.com/TehNut/HWYLA/issues/257
 - https://github.com/TehNut/HWYLA/issues/262
 - https://github.com/TehNut/HWYLA/issues/264
 - https://github.com/TehNut/HWYLA/issues/312
 - https://github.com/TehNut/HWYLA/pull/289
 - https://github.com/TehNut/HWYLA/pull/298
 - https://github.com/TehNut/HWYLA/pull/300

Backport scope note (1.12.2 branch): this branch ports the modern Jade codebase (snapshot of `26.2-neoforge` @ `8808a49a`) to Minecraft 1.12.2 Forge, so the fixes above — upstream HWYLA issues Jade already resolved — remain fixed here. Anything resolved upstream *after* that snapshot, or modern-Jade features relying on post-1.12.2 APIs (component system, fluid display, registries beyond 1.12.2), is out of scope for this branch; see README.md for the compat-layer guarantees and limits.