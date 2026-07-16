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
    private val accessToken = Regex(
        pattern = """(\b(?:access[_-]?token|token|session)\s*[=:]\s*)[^\s&,;]+""",
        option = RegexOption.IGNORE_CASE,
    )
    private val privateKeyBlock = Regex(
        pattern = """-----BEGIN [A-Z ]+PRIVATE KEY-----[\s\S]*?-----END [A-Z ]+PRIVATE KEY-----""",
    )

    fun redact(text: String): String =
        text
            .replace(uriPassword) { match -> "${match.groupValues[1]}******@" }
            .replace(authorization, "Authorization: ***")
            .replace(passwordAssignment) { match -> "${match.groupValues[1]}***" }
            .replace(accessToken) { match -> "${match.groupValues[1]}***" }
            .replace(privateKeyBlock, "-----BEGIN PRIVATE KEY-----***-----END PRIVATE KEY-----")
}
