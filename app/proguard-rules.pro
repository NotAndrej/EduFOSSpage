# The project intentionally ships without obfuscation (see the upstream app).
-dontobfuscate

# Compile-time-only annotations referenced by Tink (via androidx.security-crypto)
# and other libraries. They have no runtime implementation, so silence R8.
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
