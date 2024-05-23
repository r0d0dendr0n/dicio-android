package org.stypox.dicio.settings

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.datastore.dataStore
import androidx.hilt.navigation.compose.hiltViewModel
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.di.SkillContextImpl
import org.stypox.dicio.settings.datastore.UserSettingsSerializer
import org.stypox.dicio.skills.SkillHandler2
import org.stypox.dicio.ui.theme.AppTheme
import org.stypox.dicio.ui.util.SkillInfoPreviews
import org.stypox.dicio.util.PermissionUtils

@Composable
fun SkillSettingsScreen(viewModel: SkillSettingsViewModel = hiltViewModel()) {
    val skills = viewModel.skills
    val enabledSkills by viewModel.enabledSkills
        .collectAsState(initial = mapOf())

    LazyColumn {
        items(skills) { skill ->
            SkillSettingsItem(
                skill = skill,
                isAvailable = skill.isAvailable(viewModel.skillContext),
                enabled = enabledSkills.getOrDefault(skill.id, true),
                setEnabled = { enabled -> viewModel.setSkillEnabled(skill.id, enabled) }
            )
        }
    }
}

@Composable
fun SkillSettingsItem(
    skill: SkillInfo,
    isAvailable: Boolean,
    enabled: Boolean,
    setEnabled: (Boolean) -> Unit
) {
    val canExpand = isAvailable && (skill.hasPreferences || skill.neededPermissions.isNotEmpty())
    var expanded by rememberSaveable { mutableStateOf(false) }
    val expandedAnimation by animateFloatAsState(
        label = "skill ${skill.id} card expanded",
        targetValue = if (expanded) 180f else 0f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .let { if (canExpand) it.clickable { expanded = !expanded } else it }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(skill.iconResource),
                contentDescription = stringResource(skill.nameResource),
                modifier = Modifier.size(32.dp),
            )
            Checkbox(
                checked = enabled,
                onCheckedChange = setEnabled,
                enabled = isAvailable,
            )
            Text(
                text = stringResource(skill.nameResource),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.weight(1.0f),
            )
            if (canExpand) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        modifier = Modifier.rotate(expandedAnimation),
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(
                            if (expanded) R.string.reduce else R.string.expand
                        )
                    )
                }
            }
        }


        if (!isAvailable) {
            Text(
                text = stringResource(R.string.pref_skill_not_available),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )

        } else if (expanded) {
            if (skill.neededPermissions.isNotEmpty()) {
                var allPermissionsGranted by remember { mutableStateOf(true) }
                val context = LocalContext.current
                LaunchedEffect(skill.neededPermissions) {
                    allPermissionsGranted = PermissionUtils
                        .checkPermissions(context, *skill.neededPermissions.toTypedArray())
                }

                val needingPermissionsString = if (LocalInspectionMode.current) {
                    "Requires these permissions: directly call contacts, whatever 123, test"
                } else {
                    stringResource(
                        R.string.pref_skill_missing_permissions,
                        PermissionUtils.getCommaJoinedPermissions(LocalContext.current, skill)
                    )
                }

                if (allPermissionsGranted) {
                    Text(
                        text = needingPermissionsString,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    )

                } else {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { isGranted ->
                        allPermissionsGranted = isGranted.values.all { it }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = needingPermissionsString,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .weight(1.0f)
                                .padding(end = 8.dp),
                        )

                        TextButton(
                            onClick = { launcher.launch(skill.neededPermissions.toTypedArray()) },
                        ) {
                            Text(text = stringResource(R.string.pref_skill_grant_permissions))
                        }
                    }
                }
            }

            if (skill.hasPreferences) {
                // TODO
                Text(text = "This skill has preferences TODO")
            }
        }
    }
}

@Preview
@Composable
private fun SkillSettingsItemPreview(@PreviewParameter(SkillInfoPreviews::class) skill: SkillInfo) {
    SkillSettingsItem(
        skill = skill,
        isAvailable = true,
        enabled = false,
        setEnabled = {},
    )
}

@Preview
@Composable
private fun SkillSettingsScreenPreview() {
    AppTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            SkillSettingsScreen(
                SkillSettingsViewModel(
                    application = Application(),
                    dataStore = dataStore("pre", UserSettingsSerializer)
                        .getValue(LocalContext.current, MainSettingsViewModel::settingsFlow),
                    skillContext = SkillContextImpl.newForPreviews(),
                    skillHandler = SkillHandler2(SkillContextImpl.newForPreviews()),
                )
            )
        }
    }
}