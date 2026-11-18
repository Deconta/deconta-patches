group = "de.deconta.patches"

patches {
    about {
        name = "DecontaPatches"
        description = "Morphe patches by Deconta"
        source = "https://github.com/Deconta/deconta-patches"
        author = "Deconta"
        contact = "na"
        website = "na"
        license = "GPLv3"
    }
}

val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}
