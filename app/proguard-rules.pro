# libxposed API 102 rules
-dontwarn io.github.libxposed.annotation.**
-dontwarn io.github.libxposed.api.**

# Keep the module entry class and its hookable methods.
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public <methods>;
    protected <methods>;
}

# DexKit ships its own native lib; suppress missing-class warnings on the JNI surface.
-dontwarn org.luckypray.dexkit.**
-keep class org.luckypray.dexkit.** { *; }
