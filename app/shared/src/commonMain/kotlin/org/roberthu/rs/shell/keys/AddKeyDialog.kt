package org.roberthu.rs.shell.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.presentation.AddKeyDialogState
import org.roberthu.rs.theme.color

private val creatableTypes = listOf(
    RedisKeyType.String,
    RedisKeyType.Hash,
    RedisKeyType.List,
    RedisKeyType.Set,
    RedisKeyType.ZSet,
    RedisKeyType.Stream,
    RedisKeyType.Json,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeyDialog(
    state: AddKeyDialogState,
    databases: List<RedisDatabaseSummary>,
    clusterMode: Boolean,
    onDismiss: () -> Unit,
    onKeyChange: (String) -> Unit,
    onDatabaseChange: (Int) -> Unit,
    onTypeChange: (RedisKeyType) -> Unit,
    onTtlTextChange: (String) -> Unit,
    onPermanentChange: (Boolean) -> Unit,
    onStringValueChange: (String) -> Unit,
    onHashFieldsChange: (List<Pair<String, String>>) -> Unit,
    onListValuesChange: (List<String>) -> Unit,
    onSetMembersChange: (List<String>) -> Unit,
    onZsetEntriesChange: (List<Pair<String, String>>) -> Unit,
    onStreamFieldsChange: (List<Pair<String, String>>) -> Unit,
    onJsonContentChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val canSubmit = state.key.isNotBlank() && !state.submitting && isPayloadValid(state)
    val typeExpanded = remember { mutableStateOf(false) }
    val dbExpanded = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 580.dp, max = 680.dp)
            .testTag("add_key_dialog"),
        title = { Text("添加键") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = state.key,
                    onValueChange = onKeyChange,
                    label = { Text("键名") },
                    singleLine = true,
                    isError = state.validationErrors.containsKey("key"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_key_name"),
                )

                if (clusterMode) {
                    OutlinedTextField(
                        value = "db0",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("数据库") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = dbExpanded.value,
                        onExpandedChange = { dbExpanded.value = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val dbLabel = databases.firstOrNull { it.index == state.database }
                            ?.let { "db${it.index} (${it.keyCount})" }
                            ?: "db${state.database}"
                        OutlinedTextField(
                            value = dbLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("数据库") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dbExpanded.value) },
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("add_key_database"),
                        )
                        ExposedDropdownMenu(
                            expanded = dbExpanded.value,
                            onDismissRequest = { dbExpanded.value = false },
                        ) {
                            databases.forEach { db ->
                                DropdownMenuItem(
                                    text = { Text("db${db.index} (${db.keyCount})") },
                                    onClick = {
                                        dbExpanded.value = false
                                        onDatabaseChange(db.index)
                                    },
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = typeExpanded.value,
                    onExpandedChange = { typeExpanded.value = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = state.type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类型") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(state.type.color()),
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded.value) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("add_key_type"),
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded.value,
                        onDismissRequest = { typeExpanded.value = false },
                    ) {
                        creatableTypes.forEach { type ->
                            val jsonDisabled = type == RedisKeyType.Json && state.redisJsonAvailable == false
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(type.color()),
                                        )
                                        Text(type.name)
                                    }
                                },
                                onClick = {
                                    if (!jsonDisabled) {
                                        typeExpanded.value = false
                                        onTypeChange(type)
                                    }
                                },
                                enabled = !jsonDisabled,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.ttlText,
                        onValueChange = onTtlTextChange,
                        label = { Text("TTL（秒）") },
                        enabled = !state.permanent,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_key_ttl"),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.permanent,
                            onCheckedChange = onPermanentChange,
                            modifier = Modifier.testTag("add_key_permanent"),
                        )
                        Text("永久")
                    }
                }

                PayloadEditor(
                    state = state,
                    onStringValueChange = onStringValueChange,
                    onHashFieldsChange = onHashFieldsChange,
                    onListValuesChange = onListValuesChange,
                    onSetMembersChange = onSetMembersChange,
                    onZsetEntriesChange = onZsetEntriesChange,
                    onStreamFieldsChange = onStreamFieldsChange,
                    onJsonContentChange = onJsonContentChange,
                )

                state.submitError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                state.validationErrors["global"]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (state.submitting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).testTag("add_key_submitting"))
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    modifier = Modifier.testTag("add_key_confirm"),
                ) {
                    Text("创建")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onImportClick,
                    modifier = Modifier.testTag("add_key_import"),
                ) {
                    Text("导入数据")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("add_key_cancel"),
                ) {
                    Text("取消")
                }
            }
        },
    )
}

@Composable
private fun PayloadEditor(
    state: AddKeyDialogState,
    onStringValueChange: (String) -> Unit,
    onHashFieldsChange: (List<Pair<String, String>>) -> Unit,
    onListValuesChange: (List<String>) -> Unit,
    onSetMembersChange: (List<String>) -> Unit,
    onZsetEntriesChange: (List<Pair<String, String>>) -> Unit,
    onStreamFieldsChange: (List<Pair<String, String>>) -> Unit,
    onJsonContentChange: (String) -> Unit,
) {
    when (state.type) {
        RedisKeyType.String -> {
            OutlinedTextField(
                value = state.stringValue,
                onValueChange = onStringValueChange,
                label = { Text("值") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
                    .testTag("add_key_string_value"),
            )
        }
        RedisKeyType.Hash -> {
            PairListEditor(
                pairs = state.hashFields,
                firstLabel = "字段",
                secondLabel = "值",
                onChange = onHashFieldsChange,
                testTagPrefix = "add_key_hash",
            )
        }
        RedisKeyType.List -> {
            StringListEditor(
                values = state.listValues,
                label = "元素",
                onChange = onListValuesChange,
                testTagPrefix = "add_key_list",
            )
        }
        RedisKeyType.Set -> {
            StringListEditor(
                values = state.setMembers,
                label = "成员",
                onChange = onSetMembersChange,
                testTagPrefix = "add_key_set",
            )
        }
        RedisKeyType.ZSet -> {
            PairListEditor(
                pairs = state.zsetEntries,
                firstLabel = "分数",
                secondLabel = "成员",
                onChange = onZsetEntriesChange,
                testTagPrefix = "add_key_zset",
            )
        }
        RedisKeyType.Stream -> {
            PairListEditor(
                pairs = state.streamFields,
                firstLabel = "字段",
                secondLabel = "值",
                onChange = onStreamFieldsChange,
                testTagPrefix = "add_key_stream",
            )
        }
        RedisKeyType.Json -> {
            OutlinedTextField(
                value = state.jsonContent,
                onValueChange = onJsonContentChange,
                label = { Text("JSON 内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("add_key_json_content"),
            )
        }
        else -> Unit
    }
}

@Composable
private fun StringListEditor(
    values: List<String>,
    label: String,
    onChange: (List<String>) -> Unit,
    testTagPrefix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEachIndexed { index, value ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        onChange(values.toMutableList().also { it[index] = newValue })
                    },
                    label = { Text("$label $index") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_$index"),
                )
                if (values.size > 1) {
                    IconButton(
                        onClick = { onChange(values.filterIndexed { i, _ -> i != index }) },
                    ) {
                        Text("×")
                    }
                }
            }
        }
        TextButton(onClick = { onChange(values + "") }) { Text("添加行") }
    }
}

@Composable
private fun PairListEditor(
    pairs: List<Pair<String, String>>,
    firstLabel: String,
    secondLabel: String,
    onChange: (List<Pair<String, String>>) -> Unit,
    testTagPrefix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        pairs.forEachIndexed { index, (first, second) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = first,
                    onValueChange = { newFirst ->
                        onChange(pairs.toMutableList().also { it[index] = newFirst to second })
                    },
                    label = { Text(firstLabel) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("${testTagPrefix}_${index}_first"),
                )
                OutlinedTextField(
                    value = second,
                    onValueChange = { newSecond ->
                        onChange(pairs.toMutableList().also { it[index] = first to newSecond })
                    },
                    label = { Text(secondLabel) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("${testTagPrefix}_${index}_second"),
                )
                if (pairs.size > 1) {
                    IconButton(
                        onClick = { onChange(pairs.filterIndexed { i, _ -> i != index }) },
                    ) {
                        Text("×")
                    }
                }
            }
        }
        TextButton(onClick = { onChange(pairs + ("" to "")) }) { Text("添加行") }
    }
}

private fun isPayloadValid(state: AddKeyDialogState): Boolean = when (state.type) {
    RedisKeyType.String -> true
    RedisKeyType.Hash -> state.hashFields.any { it.first.isNotBlank() }
    RedisKeyType.List -> state.listValues.any { it.isNotBlank() }
    RedisKeyType.Set -> state.setMembers.any { it.isNotBlank() }
    RedisKeyType.ZSet -> state.zsetEntries.any { it.first.isNotBlank() && it.second.isNotBlank() }
    RedisKeyType.Stream -> state.streamFields.any { it.first.isNotBlank() }
    RedisKeyType.Json -> state.jsonContent.isNotBlank() && state.redisJsonAvailable != false
    else -> false
}
