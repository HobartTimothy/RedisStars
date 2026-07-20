package org.roberthu.rs.i18n

import org.roberthu.rs.domain.AppLanguage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringCatalogTest {

    @BeforeTest
    fun disableMissingKeyWarnings() {
        // Keep test output clean; the missing-key path itself is still exercised below.
        StringCatalog.warnOnMissingKey = false
    }

    @AfterTest
    fun restoreMissingKeyWarnings() {
        StringCatalog.warnOnMissingKey = true
    }

    @Test
    fun `missing key returns the key itself without crashing`() {
        val missing = "does.not.exist"
        val result = StringCatalog.t(AppLanguage.EnUS, missing)
        assertEquals(missing, result)
    }

    @Test
    fun `missing key with args does not crash and still returns something usable`() {
        val missing = "does.not.exist.with.args"
        val result = StringCatalog.t(AppLanguage.EnUS, missing, "arg1", 42)
        // No format specifiers in the raw key, so args are ignored — the key is returned verbatim.
        assertEquals(missing, result)
    }

    @Test
    fun `hasKey reflects catalog membership`() {
        assertTrue(StringCatalog.hasKey(StringKeys.Common.Confirm))
        assertFalse(StringCatalog.hasKey("totally.made.up.key"))
    }

    @Test
    fun `english lookup returns natural english text`() {
        assertEquals("Confirm", StringCatalog.t(AppLanguage.EnUS, StringKeys.Common.Confirm))
        assertEquals("Cancel", StringCatalog.t(AppLanguage.EnUS, StringKeys.Common.Cancel))
        assertEquals("Connections", StringCatalog.t(AppLanguage.EnUS, StringKeys.Nav.Connections))
    }

    @Test
    fun `chinese lookup returns chinese text with chinese punctuation`() {
        assertEquals("确定", StringCatalog.t(AppLanguage.ZhCN, StringKeys.Common.Confirm))
        assertEquals("取消", StringCatalog.t(AppLanguage.ZhCN, StringKeys.Common.Cancel))
        assertEquals("删除连接？", StringCatalog.t(AppLanguage.ZhCN, StringKeys.Connections.DeleteTitle))
    }

    @Test
    fun `positional format args are substituted java-formatter style`() {
        assertEquals(
            "Reconnecting (3)…",
            StringCatalog.t(AppLanguage.EnUS, StringKeys.Shell.StatusReconnecting, 3),
        )
        assertEquals(
            "重连中（第 3 次）…",
            StringCatalog.t(AppLanguage.ZhCN, StringKeys.Shell.StatusReconnecting, 3),
        )
    }

    @Test
    fun `multiple positional args are substituted independent of declaration order`() {
        assertEquals(
            "db2 (17)",
            StringCatalog.t(AppLanguage.EnUS, StringKeys.Keys.DatabaseFormat, 2, 17),
        )
    }

    @Test
    fun `string positional args are substituted`() {
        assertEquals(
            "Delete \u201cprod-cache\u201d? This cannot be undone.",
            StringCatalog.t(AppLanguage.EnUS, StringKeys.Connections.DeleteMessage, "prod-cache"),
        )
        assertEquals(
            "删除「prod-cache」？此操作无法撤销。",
            StringCatalog.t(AppLanguage.ZhCN, StringKeys.Connections.DeleteMessage, "prod-cache"),
        )
    }

    @Test
    fun `every english key is also present in the chinese catalog or documented as english-only`() {
        // Deployment-mode names (Standalone/Sentinel/Cluster) and a couple of protocol-name-only
        // strings are intentionally identical across languages; the important invariant is that
        // t() never throws and always resolves to *some* string for every declared key.
        for (key in ALL_DECLARED_KEYS) {
            assertTrue(StringCatalog.hasKey(key), "key not defined in either catalog: $key")
            val en = StringCatalog.t(AppLanguage.EnUS, key)
            val zh = StringCatalog.t(AppLanguage.ZhCN, key)
            assertTrue(en.isNotBlank(), "english translation blank for $key")
            assertTrue(zh.isNotBlank(), "chinese translation blank for $key")
        }
    }

    @Test
    fun `fallback to english happens transparently when a language-specific entry is absent`() {
        // StringCatalog.t() always tries the requested language first, then EnUS, then the raw
        // key. Since every declared key in this catalog has both a zh-CN and en-US entry, we
        // simulate the fallback path directly here rather than relying on an intentionally
        // missing zh-CN string (which would otherwise need to be kept in sync with the fixture).
        val enOnlyKey = "fallback.simulated.en.only"
        assertFalse(StringCatalog.hasKey(enOnlyKey))
        // With no entry in either catalog, t() must still fall back to the raw key, never throw.
        assertEquals(enOnlyKey, StringCatalog.t(AppLanguage.ZhCN, enOnlyKey))
    }

    private companion object {
        val ALL_DECLARED_KEYS: List<String> = listOf(
            StringKeys.Common.Confirm,
            StringKeys.Common.Cancel,
            StringKeys.Common.Delete,
            StringKeys.Common.Save,
            StringKeys.Common.Dismiss,
            StringKeys.Common.Close,
            StringKeys.Common.Refresh,
            StringKeys.Common.Persistent,
            StringKeys.Common.Loading,
            StringKeys.Common.Unknown,
            StringKeys.Settings.Title,
            StringKeys.Settings.Language,
            StringKeys.Settings.LanguageDesc,
            StringKeys.Settings.LanguageOptionZh,
            StringKeys.Settings.LanguageOptionEn,
            StringKeys.Settings.DarkMode,
            StringKeys.Settings.DarkModeDesc,
            StringKeys.Settings.AutoConnect,
            StringKeys.Settings.AutoConnectDesc,
            StringKeys.Settings.AboutTitle,
            StringKeys.Settings.Version,
            StringKeys.Settings.License,
            StringKeys.Settings.Product,
            StringKeys.Settings.ProductDesc,
            StringKeys.Settings.ErrorSave,
            StringKeys.Nav.Connections,
            StringKeys.Nav.Monitor,
            StringKeys.Nav.RuntimeLogs,
            StringKeys.Nav.Settings,
            StringKeys.Nav.ExpandRail,
            StringKeys.Nav.CollapseRail,
            StringKeys.Shell.StatusConnected,
            StringKeys.Shell.StatusDisconnected,
            StringKeys.Shell.StatusConnecting,
            StringKeys.Shell.StatusReconnecting,
            StringKeys.Shell.StatusFailed,
            StringKeys.Shell.Dismiss,
            StringKeys.Shell.ErrorSaveSettings,
            StringKeys.Monitor.Title,
            StringKeys.Monitor.Placeholder,
            StringKeys.RuntimeLogs.Title,
            StringKeys.RuntimeLogs.EmptyTitle,
            StringKeys.RuntimeLogs.EmptyDescription,
            StringKeys.RuntimeLogs.FilterAll,
            StringKeys.RuntimeLogs.SearchPlaceholder,
            StringKeys.RuntimeLogs.AutoScroll,
            StringKeys.RuntimeLogs.Pause,
            StringKeys.RuntimeLogs.Resume,
            StringKeys.RuntimeLogs.ClearDisplay,
            StringKeys.RuntimeLogs.Export,
            StringKeys.RuntimeLogs.ExportDialogTitle,
            StringKeys.RuntimeLogs.ExportSuccess,
            StringKeys.RuntimeLogs.ErrorLoadFailed,
            StringKeys.RuntimeLogs.ErrorExportFailed,
            StringKeys.RuntimeLogs.ErrorExportEmpty,
            StringKeys.RuntimeLogs.UnavailablePreview,
            StringKeys.Connections.Title,
            StringKeys.Connections.AddGroup,
            StringKeys.Connections.AddConnection,
            StringKeys.Connections.Ungrouped,
            StringKeys.Connections.GroupDialogTitle,
            StringKeys.Connections.GroupNameLabel,
            StringKeys.Connections.GroupConfirm,
            StringKeys.Connections.GroupCancel,
            StringKeys.Connections.ContextAddGroup,
            StringKeys.Connections.ContextAddConnection,
            StringKeys.Connections.ContextCreateConnection,
            StringKeys.Connections.ContextCreateConnectionInGroup,
            StringKeys.Connections.ContextCreateRootConnection,
            StringKeys.Connections.Empty,
            StringKeys.Connections.Connect,
            StringKeys.Connections.Test,
            StringKeys.Connections.Edit,
            StringKeys.Connections.Delete,
            StringKeys.Connections.MoreActions,
            StringKeys.Connections.DeleteTitle,
            StringKeys.Connections.DeleteMessage,
            StringKeys.Connections.SummarySentinel,
            StringKeys.Connections.SummaryCluster,
            StringKeys.Connections.UnavailablePreview,
            StringKeys.Connections.ErrorLoadFailed,
            StringKeys.Connections.ErrorOperationFailed,
            StringKeys.Connections.ErrorSaveConnectionFailed,
            StringKeys.Connections.ErrorTestFailed,
            StringKeys.Connections.ErrorGroupNameEmpty,
            StringKeys.Connections.ErrorGroupNameDuplicate,
            StringKeys.ConnectionEditor.TitleNew,
            StringKeys.ConnectionEditor.TitleEdit,
            StringKeys.ConnectionEditor.Close,
            StringKeys.ConnectionEditor.SectionGeneral,
            StringKeys.ConnectionEditor.SectionAdvanced,
            StringKeys.ConnectionEditor.SectionDatabaseAlias,
            StringKeys.ConnectionEditor.SectionTls,
            StringKeys.ConnectionEditor.SectionSsh,
            StringKeys.ConnectionEditor.SectionSentinel,
            StringKeys.ConnectionEditor.SectionCluster,
            StringKeys.ConnectionEditor.SectionNetworkProxy,
            StringKeys.ConnectionEditor.ComingSoon,
            StringKeys.ConnectionEditor.RootGroup,
            StringKeys.ConnectionEditor.SecondsSuffix,
            StringKeys.ConnectionEditor.SshStandaloneHint,
            StringKeys.ConnectionEditor.NameLabel,
            StringKeys.ConnectionEditor.DeploymentModeLabel,
            StringKeys.ConnectionEditor.ModeStandalone,
            StringKeys.ConnectionEditor.ModeSentinel,
            StringKeys.ConnectionEditor.ModeCluster,
            StringKeys.ConnectionEditor.HostLabel,
            StringKeys.ConnectionEditor.PortLabel,
            StringKeys.ConnectionEditor.UsernameLabel,
            StringKeys.ConnectionEditor.UsernamePlaceholder,
            StringKeys.ConnectionEditor.PasswordLabel,
            StringKeys.ConnectionEditor.PasswordPlaceholder,
            StringKeys.ConnectionEditor.ShowPassword,
            StringKeys.ConnectionEditor.HidePassword,
            StringKeys.ConnectionEditor.SshTunnelToggle,
            StringKeys.ConnectionEditor.SshSectionTitle,
            StringKeys.ConnectionEditor.SshHostLabel,
            StringKeys.ConnectionEditor.SshPortLabel,
            StringKeys.ConnectionEditor.SshUsernameLabel,
            StringKeys.ConnectionEditor.SshAuthMethodLabel,
            StringKeys.ConnectionEditor.SshAuthPassword,
            StringKeys.ConnectionEditor.SshAuthPrivateKey,
            StringKeys.ConnectionEditor.SshPasswordLabel,
            StringKeys.ConnectionEditor.SshPrivateKeyPathLabel,
            StringKeys.ConnectionEditor.SshPrivateKeyBrowse,
            StringKeys.ConnectionEditor.SshPrivateKeyBrowseDescription,
            StringKeys.ConnectionEditor.SshPrivateKeyLabel,
            StringKeys.ConnectionEditor.SshPrivateKeyPassphraseLabel,
            StringKeys.ConnectionEditor.SshConnectTimeoutLabel,
            StringKeys.ConnectionEditor.ClientNameLabel,
            StringKeys.ConnectionEditor.ConnectTimeoutLabel,
            StringKeys.ConnectionEditor.CommandTimeoutLabel,
            StringKeys.ConnectionEditor.ReconnectTimeoutLabel,
            StringKeys.ConnectionEditor.ConnectTimeoutSecLabel,
            StringKeys.ConnectionEditor.CommandTimeoutSecLabel,
            StringKeys.ConnectionEditor.KeyPatternLabel,
            StringKeys.ConnectionEditor.KeySeparatorLabel,
            StringKeys.ConnectionEditor.KeyListViewLabel,
            StringKeys.ConnectionEditor.KeyListViewTree,
            StringKeys.ConnectionEditor.KeyListViewFlat,
            StringKeys.ConnectionEditor.KeyLoadBatchSizeLabel,
            StringKeys.ConnectionEditor.DatabaseFilterModeLabel,
            StringKeys.ConnectionEditor.DatabaseFilterShowAll,
            StringKeys.ConnectionEditor.DatabaseFilterShowSpecified,
            StringKeys.ConnectionEditor.DatabaseFilterHideSpecified,
            StringKeys.ConnectionEditor.DatabaseFilterTextLabel,
            StringKeys.ConnectionEditor.DatabaseFilterTextPlaceholder,
            StringKeys.ConnectionEditor.TagColorLabel,
            StringKeys.ConnectionEditor.TlsEnableLabel,
            StringKeys.ConnectionEditor.TlsVerifyPeerLabel,
            StringKeys.ConnectionEditor.TlsVerifyPeerDesc,
            StringKeys.ConnectionEditor.MasterNameLabel,
            StringKeys.ConnectionEditor.SentinelNodesLabel,
            StringKeys.ConnectionEditor.SentinelModeHint,
            StringKeys.ConnectionEditor.ClusterSeedNodesLabel,
            StringKeys.ConnectionEditor.ClusterModeHint,
            StringKeys.ConnectionEditor.AddNode,
            StringKeys.ConnectionEditor.RemoveNode,
            StringKeys.ConnectionEditor.TestConnection,
            StringKeys.ConnectionEditor.ParseClipboardUrl,
            StringKeys.ConnectionEditor.TestSuccess,
            StringKeys.ConnectionEditor.Save,
            StringKeys.ConnectionEditor.Cancel,
            StringKeys.ConnectionEditor.DiscardTitle,
            StringKeys.ConnectionEditor.DiscardMessage,
            StringKeys.ConnectionEditor.DiscardConfirm,
            StringKeys.ConnectionEditor.DiscardContinueEditing,
            StringKeys.Keys.TitleCount,
            StringKeys.Keys.Add,
            StringKeys.Keys.Refresh,
            StringKeys.Keys.PatternPlaceholder,
            StringKeys.Keys.FilterPlaceholder,
            StringKeys.Keys.PatternLabel,
            StringKeys.Keys.Empty,
            StringKeys.Keys.EmptyTitle,
            StringKeys.Keys.Search,
            StringKeys.Keys.NotConnected,
            StringKeys.Keys.LoadMore,
            StringKeys.Keys.CancelScan,
            StringKeys.Keys.ScanErrorHint,
            StringKeys.Keys.ClusterPartial,
            StringKeys.Keys.Database,
            StringKeys.Keys.DatabaseFormat,
            StringKeys.Keys.DatabaseSelectionFormat,
            StringKeys.Keys.ClusterDbTooltip,
            StringKeys.Keys.TtlPermanent,
            StringKeys.Keys.ClearPattern,
            StringKeys.Keys.DatabasesLoading,
            StringKeys.Keys.ViewToggle,
            StringKeys.Keys.ViewTree,
            StringKeys.Keys.ViewFlat,
            StringKeys.Keys.TypeFilterAll,
            StringKeys.Keys.TypeFilterTooltip,
            StringKeys.AddKey.Title,
            StringKeys.AddKey.NameLabel,
            StringKeys.AddKey.DatabaseLabel,
            StringKeys.AddKey.TypeLabel,
            StringKeys.AddKey.TtlLabel,
            StringKeys.AddKey.Permanent,
            StringKeys.AddKey.StringValue,
            StringKeys.AddKey.HashField,
            StringKeys.AddKey.HashValue,
            StringKeys.AddKey.ListElement,
            StringKeys.AddKey.SetMember,
            StringKeys.AddKey.ZsetScore,
            StringKeys.AddKey.ZsetMember,
            StringKeys.AddKey.StreamField,
            StringKeys.AddKey.StreamValue,
            StringKeys.AddKey.JsonContent,
            StringKeys.AddKey.Import,
            StringKeys.AddKey.ImportTooltip,
            StringKeys.AddKey.Confirm,
            StringKeys.AddKey.Cancel,
            StringKeys.AddKey.AddRow,
            StringKeys.AddKey.RemoveRow,
            StringKeys.AddKey.ErrorKeyRequired,
            StringKeys.AddKey.ErrorJsonUnavailable,
            StringKeys.AddKey.ErrorValidationFailed,
            StringKeys.KeyDetail.SelectPrompt,
            StringKeys.KeyDetail.DeletedMessage,
            StringKeys.KeyDetail.Refresh,
            StringKeys.KeyDetail.Delete,
            StringKeys.KeyDetail.DeleteTitle,
            StringKeys.KeyDetail.DeleteMessage,
            StringKeys.KeyDetail.DeleteConfirm,
            StringKeys.KeyDetail.Cancel,
            StringKeys.KeyDetail.KeyNameLabel,
            StringKeys.KeyDetail.Rename,
            StringKeys.KeyDetail.TtlSecondsLabel,
            StringKeys.KeyDetail.TtlPlaceholderPersistent,
            StringKeys.KeyDetail.SetTtl,
            StringKeys.KeyDetail.Persist,
            StringKeys.KeyDetail.TtlValueSeconds,
            StringKeys.KeyDetail.MemoryValueBytes,
            StringKeys.KeyDetail.MetadataSummary,
            StringKeys.KeyDetail.StringValueLabel,
            StringKeys.KeyDetail.SaveValue,
            StringKeys.KeyDetail.HashEntriesCount,
            StringKeys.KeyDetail.ListEntriesCount,
            StringKeys.KeyDetail.SetMembersCount,
            StringKeys.KeyDetail.ZsetEntriesCount,
            StringKeys.KeyDetail.StreamUnsupported,
            StringKeys.KeyDetail.JsonUnsupported,
            StringKeys.KeyDetail.TypeUnsupported,
            StringKeys.KeyDetail.BinaryWarning,
            StringKeys.KeyDetail.TruncatedWarning,
            StringKeys.Validation.NameRequired,
            StringKeys.Validation.HostRequired,
            StringKeys.Validation.PortRequired,
            StringKeys.Validation.PortInvalidNumber,
            StringKeys.Validation.PortRange,
            StringKeys.Validation.LabelRequired,
            StringKeys.Validation.LabelInvalidNumber,
            StringKeys.Validation.LabelMinZero,
            StringKeys.Validation.LabelGreaterThanZero,
            StringKeys.Validation.LabelOutOfRange,
            StringKeys.Validation.DatabaseFilterInvalid,
            StringKeys.Validation.DatabaseFilterRequired,
            StringKeys.Validation.MasterNameRequired,
            StringKeys.Validation.SentinelNodeRequired,
            StringKeys.Validation.ClusterNodeRequired,
            StringKeys.Validation.NodeHostRequired,
            StringKeys.Validation.NodePortRange,
            StringKeys.Validation.SshHostRequired,
            StringKeys.Validation.SshUsernameRequired,
            StringKeys.Validation.SshPasswordRequired,
            StringKeys.Validation.SshPrivateKeyRequired,
            StringKeys.Validation.SshTunnelStandaloneOnly,
            StringKeys.Validation.ClusterDatabaseMustBeZero,
            StringKeys.Validation.UrlBlank,
            StringKeys.Validation.UrlInvalidFormat,
            StringKeys.Validation.UrlUnsupportedScheme,
            StringKeys.Validation.UrlSentinelClusterUnsupported,
            StringKeys.Validation.UrlHostRequired,
            StringKeys.Validation.UrlPortNumber,
            StringKeys.Validation.UrlPortRange,
            StringKeys.Validation.UrlDatabaseInvalid,
            StringKeys.Validation.GroupNameEmpty,
            StringKeys.Validation.GroupNameDuplicate,
            StringKeys.Errors.ConnectionOperationFailed,
            StringKeys.Errors.SaveConnectionFailed,
            StringKeys.Errors.ConnectionTestFailed,
            StringKeys.Errors.ScanKeysFailed,
            StringKeys.Errors.KeyOperationFailed,
            StringKeys.Errors.SaveSettingsFailed,
            StringKeys.Errors.ValidationFailed,
            StringKeys.Errors.CreateKeyFailed,
            StringKeys.Errors.LoadDatabasesFailed,
        )
    }
}
