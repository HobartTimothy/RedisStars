package org.roberthu.rs.domain

/**
 * Named folder for organizing connection profiles in the connections panel.
 * Profiles with a null [ConnectionProfile.groupId] appear under an implicit "未分组" section.
 */
data class ConnectionGroup(
    val id: String,
    val name: String,
    val order: Int = 0,
    val expanded: Boolean = true,
) {
    fun validate(existingNames: Collection<String> = emptyList()): List<String> = buildList {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            add("组名称不能为空")
        }
        if (existingNames.any { it.equals(trimmed, ignoreCase = false) }) {
            add("同级已存在相同的组名称")
        }
    }
}
