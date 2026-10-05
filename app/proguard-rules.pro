# Gson reads these persisted models reflectively. Their field names form the local JSON schema
# and must remain stable across app updates.
-keepattributes Signature,*Annotation*
-keepclassmembers class com.projeto.egoodapp.data.model.AccountProfile { <fields>; }
-keepclassmembers class com.projeto.egoodapp.data.local.LocalState { <fields>; }
-keepclassmembers class com.projeto.egoodapp.data.model.Interest { <fields>; }
-keepclassmembers class com.projeto.egoodapp.data.model.VehicleView { <fields>; }
-keepclassmembers class com.projeto.egoodapp.data.model.DealerRating { <fields>; }
-keepclassmembers class com.projeto.egoodapp.data.model.SecurityAuditEvent { <fields>; }
-keepclassmembers class com.projeto.egoodapp.security.LoginAttemptPolicy { <fields>; }
-keepclassmembers class com.projeto.egoodapp.security.SessionDeadline { <fields>; }
-keep enum com.projeto.egoodapp.data.model.SecurityAuditEvent$Type { *; }
-keep enum com.projeto.egoodapp.data.model.SecurityAuditEvent$Result { *; }
-keepclassmembers class com.projeto.egoodapp.data.model.Vehicle { <fields>; }

# Release builds must not retain diagnostic logs or exception stacks.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
