# Billing ships its own consumer rules; Room is handled by KSP-generated code.
# Remove verbose logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
