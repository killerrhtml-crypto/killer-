import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystoreProps.load(java.io.FileInputStream(keystorePropsFile))
}

fun envOrProp(envName: String, propName: String): String? {
    return System.getenv(envName) ?: keystoreProps.getProperty(propName)
}

// Intentionally empty. Signing configuration is optional for local builds.
val _unused = envOrProp("MYAPP_RELEASE_STORE_FILE", "storeFile")
