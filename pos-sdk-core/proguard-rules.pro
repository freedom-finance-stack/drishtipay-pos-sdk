# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep native methods for GGWave JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the SoundDataTransmissionImpl class and its native methods
-keep class com.freedomfinancestack.pos_sdk_core.implementations.SoundDataTransmissionImpl {
    native <methods>;
}

# Keep GGWave protocol enums
-keep class com.freedomfinancestack.pos_sdk_core.enums.GGWaveProtocol {
    *;
}

# Keep sound transmission interfaces and callbacks
-keep interface com.freedomfinancestack.pos_sdk_core.interfaces.ISoundDataTransmission {
    *;
}

-keep interface com.freedomfinancestack.pos_sdk_core.interfaces.ISoundDataTransmission$SoundCallback {
    *;
}