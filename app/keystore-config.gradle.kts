import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystoreProps.load(java.io.FileInputStream(keystorePropsFile))
}

fun envOrProp(envName: String, propName: String): String? {
    return System.getenv(envName) ?: keystoreProps.getProperty(propName)
}

android {
    signingConfigs {
        create("release") {
            // storeFile: prefer env var, then keystore.properties
            envOrProp("MYAPP_RELEASE_STORE_FILE", "storeFile")?.let { path ->
                storeFile = file(path)
            }

            // passwords / alias: prefer env var, then keystore.properties
            envOrProp("MYAPP_RELEASE_STORE_PASSWORD", "storePassword")?.let { pwd ->
                storePassword = pwd
            }

            envOrProp("MYAPP_RELEASE_KEY_ALIAS", "keyAlias")?.let { alias ->
                keyAlias = alias
            }

            envOrProp("MYAPP_RELEASE_KEY_PASSWORD", "keyPassword")?.let { keyPwd ->
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
