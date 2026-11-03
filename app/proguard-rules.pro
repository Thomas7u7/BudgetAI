# Reglas de ofuscación para el build release.
# Firebase Firestore usa reflexión para deserializar modelos: conservar sus campos.
-keepclassmembers class com.example.budgetai.data.model.** {
    <fields>;
    <init>();
}
-keepattributes Signature
