package org.edufosspage.data

object EduSession {
    @Volatile var schoolUrl:  String = ""
    @Volatile var subdomain:  String = ""
    @Volatile var cookies:    List<String> = emptyList()
    @Volatile var gsecHash:   String = ""
    @Volatile var userId:     String = ""

    @Volatile var userFullName:  String = ""
    @Volatile var userClass:     String = ""

    @Volatile var subjects:   Map<String, Pair<String, String>> = emptyMap()
    @Volatile var teachers:   Map<String, String> = emptyMap()
    @Volatile var classrooms: Map<String, String> = emptyMap()

    val isReady get() = schoolUrl.isNotEmpty() && gsecHash.isNotEmpty()

    fun clear() {
        schoolUrl    = ""
        subdomain    = ""
        cookies      = emptyList()
        gsecHash     = ""
        userId       = ""
        userFullName = ""
        userClass    = ""
        subjects     = emptyMap()
        teachers     = emptyMap()
        classrooms   = emptyMap()
    }

    fun cookieHeader(): String = cookies
        .map { it.split(";")[0].trim() }
        .filter { it.contains("=") }
        .associate { it.substringBefore("=") to it.substringAfter("=") }
        .map { "${it.key}=${it.value}" }
        .joinToString("; ")

    fun subjectName(id: String?)   = subjects[id]?.first  ?: "?"
    fun subjectShort(id: String?)  = subjects[id]?.second ?: "?"
    fun teacherName(id: String?)   = teachers[id]  ?: ""
    fun classroomName(id: String?) = classrooms[id] ?: ""
}
