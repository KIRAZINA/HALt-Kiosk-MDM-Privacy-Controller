-keep class com.kira_zina.haltprivacy.SecureServiceApplication { *; }

-keep class com.kira_zina.haltprivacy.security.EnterpriseSecurityReceiver { *; }
-keep class com.kira_zina.haltprivacy.security.BootCompletedReceiver { *; }

-keep class com.kira_zina.haltprivacy.ui.MainActivity { *; }
-keep class com.kira_zina.haltprivacy.ui.ProvisioningViewModel { *; }
-keep class com.kira_zina.haltprivacy.provisioning.ProvisioningActivity { *; }

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keepclassmembers class * extends androidx.lifecycle.LiveData {
    <init>(...);
}

-keep class * extends androidx.work.Worker { *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
