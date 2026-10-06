import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "me.uc.hussein"
version = "v1"

/*
 * ============================================================
 *  ULTRAS CLANS - Build Configuration
 * ============================================================
 * ملاحظة توافق الإصدارات:
 * السيرفرات الحديثة (Paper 1.21.x فما فوق) تتطلب Java 21 كحد أدنى
 * لتشغيل الـPaper API نفسه. تم ضبط toolchain هنا على Java 21 لأن
 * هذا هو أحدث LTS مدعوم رسميًا من PaperMC وقت كتابة هذا المشروع.
 * عند توفر دعم رسمي لـ Java 25 من PaperMC، يكفي تغيير الرقم أدناه
 * دون الحاجة لتعديل أي كود مصدري، لأن المشروع لا يستخدم أي ميزة
 * حصرية بإصدار جافا معين.
 */
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    // نطاق التوافق: نبني ضد أحدث Paper API متاح (1.21.x) وهو متوافق ثنائيًا
    // (ABI-compatible) مع الإصدارات الأحدث طالما لم تُحذف API مستخدمة هنا.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Vault - اقتصاد اختياري (soft-depend)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { exclude(group = "org.bukkit", module = "bukkit") }

    // PlaceholderAPI - اختياري (soft-depend)
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("com.mysql:mysql-connector-j:9.1.0")

    // Adventure API - متوفر أساسًا داخل Paper، لكنه يوضع هنا صراحة للأمان
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveBaseName.set("Ultras_clans")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // إعادة تحديد مسار المكتبات المُدمجة (relocation) لتفادي أي تعارض
    // مع بلجنات أخرى تستخدم نفس المكتبات بإصدارات مختلفة.
    relocate("com.zaxxer.hikari", "me.uc.hussein.ultrasclans.libs.hikari")
    relocate("org.sqlite", "me.uc.hussein.ultrasclans.libs.sqlite")
    relocate("com.mysql", "me.uc.hussein.ultrasclans.libs.mysql")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
