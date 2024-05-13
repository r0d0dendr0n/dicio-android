package org.stypox.dicio.skills.telephone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.Skill
import org.dicio.skill.SkillContext
import org.dicio.skill.output.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.output.graphical.Headline
import org.stypox.dicio.util.getString

class TelephoneOutput(
    private val contacts: List<Pair<String, List<String>>>,
) : SkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String = if (contacts.isEmpty()) {
        ctx.getString(R.string.skill_telephone_unknown_contact)
    } else {
        ctx.getString(R.string.skill_telephone_found_contacts, contacts.size)
    }

    override fun getNextSkills(ctx: SkillContext): List<Skill> {
        val result = mutableListOf<Skill>(
            ContactChooserName(
                // when saying the name, there is no way to distinguish between
                // different numbers, so just use the first one
                contacts.map { Pair(it.first, it.second[0]) }
            )
        )

        if (ctx.parserFormatter != null) {
            result.add(
                ContactChooserIndex(
                    contacts.flatMap { contact ->
                        contact.second.map { number ->
                            Pair(contact.first, number)
                        }
                    }
                )
            )
        }

        return result
    }

    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        if (contacts.isEmpty()) {
            Headline(text = getSpeechOutput(ctx))
        } else {
            Column {
                for (i in contacts.indices) {
                    if (i != 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = contacts[i].first,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    for (number in contacts[i].second) {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
