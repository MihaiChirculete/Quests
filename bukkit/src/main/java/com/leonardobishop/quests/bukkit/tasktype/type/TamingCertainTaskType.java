package com.leonardobishop.quests.bukkit.tasktype.type;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.common.config.ConfigProblem;
import com.leonardobishop.quests.common.config.ConfigProblemDescriptions;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.quest.Task;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class TamingCertainTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;

    public TamingCertainTaskType(BukkitQuestsPlugin plugin) {
        super("tamingcertain", TaskUtils.TASK_ATTRIBUTION_STRING, "Tame a set amount of certain animals.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        if (TaskUtils.configValidateExists(root + ".mob", config.get("mob"), problems, "mob", super.getType())) {
            try {
                EntityType.valueOf(String.valueOf(config.get("mob")));
            } catch (IllegalArgumentException ex) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                        ConfigProblemDescriptions.UNKNOWN_ENTITY_TYPE.getDescription(String.valueOf(config.get("mob"))),
                        ConfigProblemDescriptions.UNKNOWN_ENTITY_TYPE.getExtendedDescription(String.valueOf(config.get("mob"))),
                        root + ".mob"));
            }
        }
        return problems;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getOwner();

        if (player.hasMetadata("NPC")) return;

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (qPlayer == null) {
            return;
        }

        for (Quest quest : super.getRegisteredQuests()) {
            if (qPlayer.hasStartedQuest(quest)) {
                QuestProgress questProgress = qPlayer.getQuestProgressFile().getQuestProgress(quest);

                for (Task task : quest.getTasksOfType(super.getType())) {
                    if (!TaskUtils.validateWorld(player, task)) continue;

                    TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

                    if (taskProgress.isCompleted()) {
                        continue;
                    }

                    EntityType entityType;
                    try {
                        entityType = EntityType.valueOf((String) task.getConfigValue("mob"));
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }

                    if (event.getEntity().getType() != entityType) {
                        continue;
                    }

                    int tamesNeeded = (int) task.getConfigValue("amount");

                    int progress;
                    if (taskProgress.getProgress() == null) {
                        progress = 0;
                    } else {
                        progress = (int) taskProgress.getProgress();
                    }

                    progress += 1;
                    taskProgress.setProgress(progress);

                    if (progress >= tamesNeeded) {
                        taskProgress.setCompleted(true);
                    }
                }
            }
        }
    }

}
