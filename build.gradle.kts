import com.soywiz.korge.gradle.korge

buildscript {
	repositories {
		mavenLocal()
		maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }
		maven { url = uri("https://plugins.gradle.org/m2/") }
		mavenCentral()
	}
	dependencies {
		classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:1.5.6.0")
	}
}

plugins {
	kotlin("multiplatform") version "1.3.61"
	kotlin("plugin.serialization") version "1.3.61"
}

apply(plugin = "korge")

korge {
	id = "com.sample.demo"
}

kotlin {
	/* Targets declarations omitted */

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common"))
				implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.14.0")
				implementation("io.ktor:ktor-client-core:1.3.0")
			}
		}
		val jvmMain by getting {
			dependencies {
				implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
				implementation("io.ktor:ktor-client-cio:1.3.0")
			}
		}
		val mingwX64Main by getting {
			dependencies {
				implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:0.14.0")
				implementation("io.ktor:ktor-client-curl:1.3.0")
			}
		}
	}


}
