package org.roberthu.rs.util

object SensitiveRedactor {
    private val uriPassword = Regex("""(://[^/\s:@]*:)[^@\s]*@""")
    private val authorization = Regex(
        pattern = """\bAuthorization\s*:\s*[^\r\n]+""",
        option = RegexOption.IGNORE_CASE,
    )
    private val passwordAssignment = Regex(
        pattern = """(\bpassword\s*=\s*)[^\s&,;]+""",
        option = RegexOption.IGNORE_CASE,
    )

    fun redact(text: String): String =
        text
            .replace(uriPassword) { match -> "${match.groupValues[1]}***@" }
            .replace(authorization, "Authorization: ***")
            .replace(passwordAssignment) { match -> "${match.groupValues[1]}***" }
}
