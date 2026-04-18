# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Mapsforge uses internal structures, sometimes reflection or native code dependencies.
# Keep all mapsforge classes and their members from being stripped or renamed.
-keep class org.mapsforge.** { *; }
-dontwarn org.mapsforge.**

# BRouter uses extensive internal structures and reflection-like patterns for expressions.
# Keep all btools classes (routing engine, codec, util, mapaccess, expressions) from being stripped or renamed.
-keep class btools.** { *; }
-dontwarn btools.**
