package org.roberthu.rs.domain

enum class SidebarItemType {
    Connection,
    Group,
}

enum class SidebarMovePlacement {
    Before,
    After,
    Inside,
    End,
}

data class MoveSidebarItemRequest(
    val itemType: SidebarItemType,
    val itemId: String,
    val targetParentGroupId: String?,
    val targetIndex: Int,
    val placement: SidebarMovePlacement,
    val referenceItemType: SidebarItemType? = null,
    val referenceItemId: String? = null,
)

data class SidebarMoveOutcome(
    val profiles: List<ConnectionProfile>,
    val groups: List<ConnectionGroup>,
    val expandGroupId: String? = null,
    val noOp: Boolean = false,
)

sealed interface SidebarRootEntry {
    val id: String
    val sortOrder: Long
    val sortName: String

    data class UngroupedConnection(val profile: ConnectionProfile) : SidebarRootEntry {
        override val id: String = profile.id
        override val sortOrder: Long = profile.sortOrder
        override val sortName: String = profile.name
    }

    data class GroupHeader(val group: ConnectionGroup) : SidebarRootEntry {
        override val id: String = group.id
        override val sortOrder: Long = group.sortOrder
        override val sortName: String = group.name
    }
}

object SidebarTreeBuilder {
    fun rootEntries(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
    ): List<SidebarRootEntry> {
        val ungrouped = profiles.filter { it.groupId == null }
        return buildList {
            ungrouped.forEach { add(SidebarRootEntry.UngroupedConnection(it)) }
            groups.forEach { add(SidebarRootEntry.GroupHeader(it)) }
        }.sortedWith(
            compareBy<SidebarRootEntry>({ it.sortOrder }, { it.sortName }, { it.id }),
        )
    }

    fun profilesInGroup(
        profiles: List<ConnectionProfile>,
        groupId: String,
    ): List<ConnectionProfile> =
        profiles.filter { it.groupId == groupId }
            .sortedWith(compareBy({ it.sortOrder }, { it.name }, { it.id }))

    fun rootSortOrders(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
    ): List<Long> = rootEntries(profiles, groups).map { it.sortOrder }

    fun groupSortOrders(
        profiles: List<ConnectionProfile>,
        groupId: String,
    ): List<Long> = profilesInGroup(profiles, groupId).map { it.sortOrder }
}

object SidebarMoveEngine {
    fun applyMove(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
        request: MoveSidebarItemRequest,
    ): Result<SidebarMoveOutcome> {
        val validation = validate(request, profiles, groups)
        if (validation != null) {
            return Result.failure(validation)
        }

        return when (request.itemType) {
            SidebarItemType.Group -> moveGroup(profiles, groups, request)
            SidebarItemType.Connection -> moveConnection(profiles, groups, request)
        }
    }

    private fun validate(
        request: MoveSidebarItemRequest,
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
    ): IllegalArgumentException? {
        when (request.itemType) {
            SidebarItemType.Group -> {
                if (groups.none { it.id == request.itemId }) {
                    return IllegalArgumentException("Group '${request.itemId}' does not exist")
                }
                if (request.placement == SidebarMovePlacement.Inside) {
                    return IllegalArgumentException("Groups cannot be nested")
                }
                if (request.targetParentGroupId != null) {
                    return IllegalArgumentException("Groups must remain at root level")
                }
            }
            SidebarItemType.Connection -> {
                if (profiles.none { it.id == request.itemId }) {
                    return IllegalArgumentException("Connection '${request.itemId}' does not exist")
                }
            }
        }

        request.targetParentGroupId?.let { groupId ->
            if (groups.none { it.id == groupId }) {
                return IllegalArgumentException("Target group '$groupId' does not exist")
            }
        }

        request.referenceItemId?.let { referenceId ->
            when (request.referenceItemType) {
                SidebarItemType.Group -> {
                    if (groups.none { it.id == referenceId }) {
                        return IllegalArgumentException("Reference group '$referenceId' does not exist")
                    }
                }
                SidebarItemType.Connection -> {
                    if (profiles.none { it.id == referenceId }) {
                        return IllegalArgumentException("Reference connection '$referenceId' does not exist")
                    }
                }
                null -> Unit
            }
        }

        return null
    }

    private fun moveGroup(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
        request: MoveSidebarItemRequest,
    ): Result<SidebarMoveOutcome> {
        val moving = groups.first { it.id == request.itemId }
        val rootEntries = SidebarTreeBuilder.rootEntries(profiles, groups)
        val currentIndex = rootEntries.indexOfFirst { it.id == moving.id }
        val filtered = rootEntries.filterNot { it.id == request.itemId }

        val insertIndex = resolveRootInsertIndex(
            rootEntries = filtered,
            request = request,
        )
        if (currentIndex >= 0 && currentIndex == insertIndex) {
            return Result.success(SidebarMoveOutcome(profiles, groups, noOp = true))
        }

        val existingOrders = filtered.map { it.sortOrder }
        val insertResult = SidebarOrder.computeInsertSortOrder(existingOrders, insertIndex)
        val updatedGroups = if (insertResult.needsNormalization && insertResult.normalizedOrders != null) {
            val entriesWithMoving = filtered.toMutableList().apply {
                add(insertIndex.coerceIn(0, size), SidebarRootEntry.GroupHeader(moving))
            }
            normalizeRootGroups(groups, entriesWithMoving, insertResult.normalizedOrders, moving.id)
        } else {
            groups.map { group ->
                if (group.id == moving.id) group.copy(sortOrder = insertResult.sortOrder) else group
            }
        }

        return Result.success(SidebarMoveOutcome(profiles, updatedGroups))
    }

    private fun moveConnection(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
        request: MoveSidebarItemRequest,
    ): Result<SidebarMoveOutcome> {
        val moving = profiles.first { it.id == request.itemId }

        return when (request.placement) {
            SidebarMovePlacement.Inside -> {
                val targetGroupId = request.referenceItemId
                    ?: request.targetParentGroupId
                    ?: return Result.failure(IllegalArgumentException("Inside placement requires a target group"))
                if (groups.none { it.id == targetGroupId }) {
                    return Result.failure(IllegalArgumentException("Target group '$targetGroupId' does not exist"))
                }
                moveConnectionIntoGroup(profiles, groups, moving, targetGroupId, request.targetIndex)
            }
            else -> {
                if (request.targetParentGroupId == null) {
                    moveConnectionAtRoot(profiles, groups, moving, request)
                } else {
                    moveConnectionWithinOrIntoGroup(profiles, groups, moving, request)
                }
            }
        }
    }

    private fun moveConnectionAtRoot(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
        moving: ConnectionProfile,
        request: MoveSidebarItemRequest,
    ): Result<SidebarMoveOutcome> {
        val rootEntries = SidebarTreeBuilder.rootEntries(profiles, groups)
        val currentIndex = rootEntries.indexOfFirst { it.id == moving.id }
        val filtered = rootEntries.filterNot { it is SidebarRootEntry.UngroupedConnection && it.id == moving.id }

        val insertIndex = resolveRootInsertIndex(filtered, request)
        val targetGroupId: String? = null
        if (moving.groupId == targetGroupId && currentIndex >= 0 && currentIndex == insertIndex) {
            return Result.success(SidebarMoveOutcome(profiles, groups, noOp = true))
        }

        val existingOrders = filtered.map { it.sortOrder }
        val insertResult = SidebarOrder.computeInsertSortOrder(existingOrders, insertIndex)
        val (updatedProfiles, updatedGroups) = if (
            insertResult.needsNormalization && insertResult.normalizedOrders != null
        ) {
            val entriesWithMoving = filtered.toMutableList().apply {
                add(
                    insertIndex.coerceIn(0, size),
                    SidebarRootEntry.UngroupedConnection(moving.copy(groupId = targetGroupId)),
                )
            }
            normalizeRootScope(
                profiles = profiles,
                groups = groups,
                orderedEntries = entriesWithMoving,
                normalizedOrders = insertResult.normalizedOrders,
                movingId = moving.id,
                movingGroupId = targetGroupId,
            )
        } else {
            profiles.map { profile ->
                if (profile.id == moving.id) {
                    profile.copy(groupId = targetGroupId, sortOrder = insertResult.sortOrder)
                } else {
                    profile
                }
            } to groups
        }

        return Result.success(SidebarMoveOutcome(updatedProfiles, updatedGroups))
    }

    private fun moveConnectionWithinOrIntoGroup(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
        moving: ConnectionProfile,
        request: MoveSidebarItemRequest,
    ): Result<SidebarMoveOutcome> {
        val targetGroupId = request.targetParentGroupId!!
        val allGroupProfiles = SidebarTreeBuilder.profilesInGroup(profiles, targetGroupId)
        val currentIndex = allGroupProfiles.indexOfFirst { it.id == moving.id }
        val groupProfiles = allGroupProfiles.filterNot { it.id == moving.id }

        val insertIndex = resolveGroupInsertIndex(groupProfiles, request)
        if (moving.groupId == targetGroupId && currentIndex >= 0 && currentIndex == insertIndex) {
            return Result.success(SidebarMoveOutcome(profiles, groups, noOp = true))
        }

        val existingOrders = groupProfiles.map { it.sortOrder }
        val insertResult = SidebarOrder.computeInsertSortOrder(existingOrders, insertIndex)
        val updatedProfiles = if (insertResult.needsNormalization && insertResult.normalizedOrders != null) {
            applyGroupNormalization(
                profiles = profiles,
                groupId = targetGroupId,
                orderedProfiles = groupProfiles,
                normalizedOrders = insertResult.normalizedOrders,
                movingId = moving.id,
                movingSortOrder = insertResult.sortOrder,
            )
        } else {
            profiles.map { profile ->
                if (profile.id == moving.id) {
                    profile.copy(groupId = targetGroupId, sortOrder = insertResult.sortOrder)
                } else {
                    profile
                }
            }
        }

        return Result.success(
            SidebarMoveOutcome(
                profiles = updatedProfiles,
                groups = groups,
                expandGroupId = targetGroupId,
            ),
        )
    }

    private fun moveConnectionIntoGroup(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
        moving: ConnectionProfile,
        targetGroupId: String,
        targetIndex: Int,
    ): Result<SidebarMoveOutcome> {
        val allGroupProfiles = SidebarTreeBuilder.profilesInGroup(profiles, targetGroupId)
        val currentIndex = allGroupProfiles.indexOfFirst { it.id == moving.id }
        val groupProfiles = allGroupProfiles.filterNot { it.id == moving.id }
        val insertIndex = targetIndex.coerceIn(0, groupProfiles.size)
        if (moving.groupId == targetGroupId && currentIndex >= 0 && currentIndex == insertIndex) {
            return Result.success(SidebarMoveOutcome(profiles, groups, expandGroupId = targetGroupId, noOp = true))
        }

        val existingOrders = groupProfiles.map { it.sortOrder }
        val insertResult = SidebarOrder.computeInsertSortOrder(existingOrders, insertIndex)
        val updatedProfiles = if (insertResult.needsNormalization && insertResult.normalizedOrders != null) {
            applyGroupNormalization(
                profiles = profiles,
                groupId = targetGroupId,
                orderedProfiles = groupProfiles,
                normalizedOrders = insertResult.normalizedOrders,
                movingId = moving.id,
                movingSortOrder = insertResult.sortOrder,
            )
        } else {
            profiles.map { profile ->
                if (profile.id == moving.id) {
                    profile.copy(groupId = targetGroupId, sortOrder = insertResult.sortOrder)
                } else {
                    profile
                }
            }
        }

        return Result.success(
            SidebarMoveOutcome(
                profiles = updatedProfiles,
                groups = groups,
                expandGroupId = targetGroupId,
            ),
        )
    }

    private fun resolveRootInsertIndex(
        rootEntries: List<SidebarRootEntry>,
        request: MoveSidebarItemRequest,
    ): Int = when (request.placement) {
        SidebarMovePlacement.End -> rootEntries.size
        SidebarMovePlacement.Before -> {
            val referenceId = request.referenceItemId ?: return request.targetIndex.coerceIn(0, rootEntries.size)
            rootEntries.indexOfFirst { it.id == referenceId }.takeIf { it >= 0 }
                ?: request.targetIndex.coerceIn(0, rootEntries.size)
        }
        SidebarMovePlacement.After -> {
            val referenceId = request.referenceItemId ?: return request.targetIndex.coerceIn(0, rootEntries.size)
            val index = rootEntries.indexOfFirst { it.id == referenceId }
            if (index < 0) request.targetIndex.coerceIn(0, rootEntries.size) else index + 1
        }
        SidebarMovePlacement.Inside -> request.targetIndex.coerceIn(0, rootEntries.size)
    }.coerceIn(0, rootEntries.size)

    private fun resolveGroupInsertIndex(
        groupProfiles: List<ConnectionProfile>,
        request: MoveSidebarItemRequest,
    ): Int = when (request.placement) {
        SidebarMovePlacement.End -> groupProfiles.size
        SidebarMovePlacement.Before -> {
            val referenceId = request.referenceItemId ?: return request.targetIndex.coerceIn(0, groupProfiles.size)
            groupProfiles.indexOfFirst { it.id == referenceId }.takeIf { it >= 0 }
                ?: request.targetIndex.coerceIn(0, groupProfiles.size)
        }
        SidebarMovePlacement.After -> {
            val referenceId = request.referenceItemId ?: return request.targetIndex.coerceIn(0, groupProfiles.size)
            val index = groupProfiles.indexOfFirst { it.id == referenceId }
            if (index < 0) request.targetIndex.coerceIn(0, groupProfiles.size) else index + 1
        }
        SidebarMovePlacement.Inside -> request.targetIndex.coerceIn(0, groupProfiles.size)
    }.coerceIn(0, groupProfiles.size)

    private fun normalizeRootGroups(
        groups: List<ConnectionGroup>,
        orderedEntries: List<SidebarRootEntry>,
        normalizedOrders: List<Long>,
        movingGroupId: String,
    ): List<ConnectionGroup> {
        val orderById = orderedEntries.mapIndexed { index, entry ->
            entry.id to normalizedOrders[index]
        }.toMap()
        return groups.map { group ->
            group.copy(sortOrder = orderById[group.id] ?: group.sortOrder)
        }
    }

    private fun normalizeRootScope(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
        orderedEntries: List<SidebarRootEntry>,
        normalizedOrders: List<Long>,
        movingId: String,
        movingGroupId: String?,
    ): Pair<List<ConnectionProfile>, List<ConnectionGroup>> {
        val orderById = orderedEntries.mapIndexed { index, entry ->
            entry.id to normalizedOrders[index]
        }.toMap()
        val updatedProfiles = profiles.map { profile ->
            when {
                profile.id == movingId ->
                    profile.copy(groupId = movingGroupId, sortOrder = orderById[movingId] ?: profile.sortOrder)
                profile.groupId == null && profile.id in orderById ->
                    profile.copy(sortOrder = orderById.getValue(profile.id))
                else -> profile
            }
        }
        val updatedGroups = groups.map { group ->
            orderById[group.id]?.let { order -> group.copy(sortOrder = order) } ?: group
        }
        return updatedProfiles to updatedGroups
    }

    private fun applyGroupNormalization(
        profiles: List<ConnectionProfile>,
        groupId: String,
        orderedProfiles: List<ConnectionProfile>,
        normalizedOrders: List<Long>,
        movingId: String,
        movingSortOrder: Long,
    ): List<ConnectionProfile> {
        val orderById = orderedProfiles.mapIndexed { index, profile ->
            profile.id to normalizedOrders[index]
        }.toMap() + (movingId to movingSortOrder)
        return profiles.map { profile ->
            if (profile.groupId == groupId || profile.id == movingId) {
                val order = orderById[profile.id]
                if (order != null) {
                    profile.copy(groupId = groupId, sortOrder = order)
                } else if (profile.id == movingId) {
                    profile.copy(groupId = groupId, sortOrder = movingSortOrder)
                } else {
                    profile
                }
            } else {
                profile
            }
        }
    }
}

object SidebarSortMigration {
    /**
     * Assigns deterministic sort orders matching the pre-upgrade sidebar: root items (ungrouped
     * connections and groups) interleaved alphabetically by name; group children alphabetically.
     */
    fun migrate(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
    ): Pair<List<ConnectionProfile>, List<ConnectionGroup>> {
        val needsMigration = profiles.any { it.sortOrder == 0L } ||
            groups.any { it.sortOrder == 0L }
        if (!needsMigration) {
            return profiles to groups
        }

        val rootItems = buildList<Pair<String, SidebarItemType>> {
            profiles.filter { it.groupId == null }.forEach { add(it.id to SidebarItemType.Connection) }
            groups.forEach { add(it.id to SidebarItemType.Group) }
        }.sortedWith(
            compareBy(
                { (id, type) ->
                    when (type) {
                        SidebarItemType.Connection -> profiles.first { it.id == id }.name
                        SidebarItemType.Group -> groups.first { it.id == id }.name
                    }
                },
                { (id, _) -> id },
            ),
        )

        var rootStep = SidebarOrder.STEP
        val rootOrderById = rootItems.associate { (id, _) ->
            id to rootStep.also { rootStep += SidebarOrder.STEP }
        }

        val updatedGroups = groups.map { group ->
            group.copy(sortOrder = rootOrderById[group.id] ?: group.sortOrder)
        }

        val profilesByGroup = profiles.filter { it.groupId != null }.groupBy { it.groupId!! }
        val groupOrderById = buildMap<String, Long> {
            profilesByGroup.forEach { (groupId, groupedProfiles) ->
                var step = SidebarOrder.STEP
                groupedProfiles.sortedWith(compareBy({ it.name }, { it.id })).forEach { profile ->
                    put(profile.id, step)
                    step += SidebarOrder.STEP
                }
            }
        }

        val updatedProfiles = profiles.map { profile ->
            when {
                profile.groupId == null ->
                    profile.copy(sortOrder = rootOrderById[profile.id] ?: profile.sortOrder)
                else ->
                    profile.copy(sortOrder = groupOrderById[profile.id] ?: profile.sortOrder)
            }
        }

        return updatedProfiles to updatedGroups
    }
}
