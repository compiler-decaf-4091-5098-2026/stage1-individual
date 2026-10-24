plugins {
    application
    antlr
}

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")
}

application {
    mainClass.set("decaf.DecafCompiler")
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-package", "decaf.parser")
}