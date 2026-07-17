package org.roberthu.rs.presentation

import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.MoveSidebarItemRequest

/**
 * All user-initiated events dispatched from [ConnectionsPane] (and its sub-composables).
 *
 * Using a sealed interface collapses the 27-callback fan-out on `ConnectionsPane` into a single
 * `onAction: (ConnectionsUiAction) -> Unit` parameter. The [ConnectionsViewModel] handles all
 * events; composables must not perform business logic themselves.
 *
 * Action names describe user **intent**, not the button that was pressed.
 */
sealed interface ConnectionsUiAction {
    // ── Connection list ────────────────────────────────────────────────────────

    /** User clicked a connection profile row (selects it). */
    data class SelectProfile(val profile: ConnectionProfile) : ConnectionsUiAction

    /** User clicked a group header (selects it). */
    data class SelectGroup(val groupId: String?) : ConnectionsUiAction

    /** User asked to toggle a group's expanded state. */
    data class ToggleGroupExpanded(val groupId: String) : ConnectionsUiAction

    // ── Connection CRUD ────────────────────────────────────────────────────────

    /** User requested to start creating a new connection, optionally inside [groupId]. */
    data class BeginCreate(val groupId: String?) : ConnectionsUiAction

    /** User requested to connect with the given profile. */
    data class Connect(val profile: ConnectionProfile) : ConnectionsUiAction

    /** User requested to test the connection for [profile] without connecting. */
    data class TestProfile(val profile: ConnectionProfile) : ConnectionsUiAction

    /** User requested to open the editor for [profile]. */
    data class EditProfile(val profile: ConnectionProfile) : ConnectionsUiAction

    /** User requested to delete [profile] (shows confirmation). */
    data class RequestDeleteProfile(val profile: ConnectionProfile) : ConnectionsUiAction

    /** User confirmed the delete. */
    data object ConfirmDeleteProfile : ConnectionsUiAction

    /** User dismissed the delete confirmation. */
    data object DismissDeleteConfirmation : ConnectionsUiAction

    // ── Group CRUD ─────────────────────────────────────────────────────────────

    /** User opened the create-group dialog. */
    data object OpenCreateGroupDialog : ConnectionsUiAction

    /** User typed in the group-name field. */
    data class UpdateGroupName(val name: String) : ConnectionsUiAction

    /** User confirmed creating the group. */
    data object ConfirmCreateGroup : ConnectionsUiAction

    /** User dismissed the group dialog. */
    data object DismissGroupDialog : ConnectionsUiAction

    // ── Editor ─────────────────────────────────────────────────────────────────

    /** User switched to a different editor section. */
    data class SelectEditorSection(val section: ConnectionEditorSection) : ConnectionsUiAction

    /** User changed a field in the editor form. */
    data class UpdateEditorForm(
        val transform: (ConnectionFormState) -> ConnectionFormState,
    ) : ConnectionsUiAction

    /** User pressed the editor's close / X button (may show discard confirmation). */
    data object RequestCloseEditor : ConnectionsUiAction

    /** User confirmed discarding unsaved changes. */
    data object ConfirmDiscardEditor : ConnectionsUiAction

    /** User dismissed the discard confirmation (stay in editor). */
    data object DismissDiscardConfirmation : ConnectionsUiAction

    /** User pressed Save in the editor. */
    data object SaveEditor : ConnectionsUiAction

    /** User pressed Test Connection in the editor. */
    data object TestEditor : ConnectionsUiAction

    /** User pasted a Redis URL and requested parsing. */
    data class ParseClipboardUrl(val url: String) : ConnectionsUiAction

    // ── Drag reorder ───────────────────────────────────────────────────────────

    /** User completed a drag-and-drop reorder gesture. */
    data class MoveSidebarItem(val request: MoveSidebarItemRequest) : ConnectionsUiAction

    // ── Errors ─────────────────────────────────────────────────────────────────

    /** User dismissed the error snackbar. */
    data object DismissError : ConnectionsUiAction
}
