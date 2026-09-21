plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.projeto.egoodapp"

    // Ajustado para a API 34 conforme o requisito do projeto[cite: 1]
    compileSdk = 37

    defaultConfig {
        applicationId = "com.projeto.egoodapp"
        minSdk = 26 //[cite: 1]
        targetSdk = 37 //[cite: 1]
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        // Atualizado para usar o Java 21
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // Essencial para o FragmentLoginBinding funcionar
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Suas dependências existentes via Version Catalogs
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.preference)
    implementation(libs.transportation.consumer)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Firebase BoM e módulos[cite: 1]
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Navigation[cite: 1]
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Glide (carregamento de imagens)[cite: 1]
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // Gson (JSON serialization)
    implementation(libs.gson)

    // Google Maps e Localização[cite: 1]
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // OSMDroid (Maps Open Source)
    implementation(libs.osmdroid.android.v6113)

    // RecyclerView e CardView[cite: 1]
    implementation(libs.recyclerview)
    implementation(libs.cardview)

    // ViewModel e LiveData (Arquitetura MVVM)[cite: 1]
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)


    implementation(libs.osmdroid.android)

    }
