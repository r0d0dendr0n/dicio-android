import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.dicio.sentences_compiler.compiler.CompilerBase
import org.dicio.sentences_compiler.compiler.CompilerToJava
import org.dicio.sentences_compiler.main.SentencesCompiler
import org.dicio.sentences_compiler.util.CompilerError
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Collections

buildscript {
    repositories {
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.dicio.sentences.compiler)
    }
}

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.dagger.hilt.android)
}

android {
    namespace = "org.stypox.dicio"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.stypox.dicio"
        minSdk = 21
        targetSdk = 34
        versionCode = 10
        versionName = "0.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        // add folders generated by sentencesCompiler task
        sourceSets["main"].java {
            srcDir("build/generated/source/sentences/main")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeKotlinCompilerExtension.get()
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Dicio own libraries
    implementation(libs.dicio.skill)
    implementation(libs.dicio.numbers)

    // Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.preference.ktx)

    // Compose (check out https://developer.android.com/jetpack/compose/bom/bom-mapping)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.android.compose.ui.test.junit4)
    debugImplementation(libs.debug.compose.ui.tooling)
    debugImplementation(libs.debug.compose.ui.test.manifest)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestAnnotationProcessor(libs.hilt.android.compiler)
    testImplementation(libs.hilt.android.testing)
    testAnnotationProcessor(libs.hilt.android.compiler)

    // Vosk
    implementation(libs.jna) { artifact { type = "aar" } }
    implementation(libs.vosk.android)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.picasso)

    // Miscellaneous
    implementation(libs.unbescape)
    implementation(libs.jsoup)

    // Used by skills
    implementation(libs.exp4j)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}

task("sentencesCompiler") {
    doFirst {
        val baseInputDirectory: File = file("src/main/sentences/")
        val outputDirectory: File = file("build/generated/source/sentences/main/org/stypox/dicio")
        outputDirectory.mkdirs()

        val locales = ArrayList<String>()
        val allSectionIds = HashSet<String>()
        for (localeName in baseInputDirectory.list { dir, name -> File(dir, name).isDirectory }!!) {

            val inputFiles = ArrayList<String>()
            for (file in File(baseInputDirectory, localeName).listFiles()!!) {
                try {
                    // first try to compile each file separately: if it fails, exclude it
                    val unusedOutput = OutputStreamWriter(
                        ByteArrayOutputStream(), StandardCharsets.UTF_8)
                    val fileString = "UTF-8:${file.absolutePath}"
                    SentencesCompiler.compile(
                        Collections.singletonList(fileString),
                        unusedOutput, unusedOutput, object : CompilerBase() {})
                    inputFiles.add(fileString)

                } catch (e: CompilerError) {
                    System.err.println(
                        "[ERROR] Ignoring invalid $localeName sentences file: ${e.message}")
                }
            }

            if (inputFiles.isEmpty()) {
                continue
            }
            // there is surely at least one file for this locale: we can add it
            locales.add(localeName)

            val outputStream = FileOutputStream(file("${outputDirectory}/Sentences_${localeName}.java"))
            val sectionIdsStream = ByteArrayOutputStream()
            SentencesCompiler.compile(
                inputFiles,
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                OutputStreamWriter(sectionIdsStream, StandardCharsets.UTF_8),
                CompilerToJava("", "org.stypox.dicio", "Sentences_${localeName}", "sections")
            )
            outputStream.close()
            sectionIdsStream.close()

            val sectionIds = String(sectionIdsStream.toByteArray(), StandardCharsets.UTF_8)
            allSectionIds.addAll(sectionIds.split(" "))
        }


        val fileOutputStream = FileOutputStream(file("${outputDirectory}/SectionsGenerated.java"))
        val outputStream = OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)
        outputStream.write("/*\n"
                + " * FILE AUTO-GENERATED BY GRADLE TASK sentencesCompiler.\n"
                + " * DO NOT MODIFY THIS FILE, AS EDITS WILL BE OVERWRITTEN.\n"
                + " */\n"
                + "\n"
                + "package org.stypox.dicio;\n"
                + "import org.dicio.skill.standard.StandardRecognizerData;\n"
                + "import java.util.HashMap;\n"
                + "import java.util.Map;\n")

        outputStream.write("public class SectionsGenerated {\n" +
                "public static final Map<String,Map<String,StandardRecognizerData>> localeSectionsMap=new HashMap<String,Map<String,StandardRecognizerData>>(){{")
        for (localeName in locales) {
            outputStream.write("put(\"${localeName.lowercase()}\",Sentences_${localeName}.sections);")
        }
        outputStream.write("}};\n")

        if (allSectionIds.isNotEmpty()) {
            outputStream.write("public static final String ")

            var first: Boolean = true
            for (sectionId in allSectionIds) {
                if (first) {
                    first = false
                } else {
                    outputStream.write(",")
                }
                outputStream.write("${sectionId}=\"${sectionId}\"")
            }
            outputStream.write(";\n")
        }
        outputStream.write("}\n")
        outputStream.flush()
        fileOutputStream.close()
    }
}
tasks.preBuild.dependsOn("sentencesCompiler")
