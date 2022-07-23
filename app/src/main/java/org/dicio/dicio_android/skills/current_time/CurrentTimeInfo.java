package org.dicio.dicio_android.skills.current_time;

import static org.dicio.dicio_android.SectionsGenerated.current_time;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import org.dicio.dicio_android.R;
import org.dicio.dicio_android.Sections;
import org.dicio.skill.Skill;
import org.dicio.skill.SkillContext;
import org.dicio.skill.SkillInfo;
import org.dicio.skill.chain.ChainSkill;
import org.dicio.skill.standard.StandardRecognizer;

public class CurrentTimeInfo extends SkillInfo {

    public CurrentTimeInfo() {
        super("current_time",
            R.string.skill_name_current_time,
            R.string.skill_sentence_example_current_time,
            R.drawable.baseline_watch_24, false);
    }

    @Override
    public boolean isAvailable(final SkillContext context) {
        return Sections.isSectionAvailable(current_time);
    }

    @Override
    public Skill build(final SkillContext context) {
        return new ChainSkill.Builder()
                .recognize(new StandardRecognizer(Sections.getSection(current_time)))
                .process(new CurrentTimeStringProcessor())
                .output(new CurrentTimeOutput());
    }

    @Nullable
    @Override
    public PreferenceFragmentCompat getPreferenceFragment() {
        return null;
    }
}
