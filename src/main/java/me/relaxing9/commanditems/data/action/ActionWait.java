package me.relaxing9.commanditems.data.action;

import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;

import com.fasterxml.jackson.annotation.JsonProperty;

import me.relaxing9.commanditems.data.ItemDefinition;
import me.relaxing9.commanditems.interpreter.InterpretationContext;

/**
 * Created by Yamakaja on 26.05.18.
 */
public class ActionWait extends Action {

    @JsonProperty
    private int duration = 20;

    @JsonProperty(required = true)
    private Action[] actions;

    @Override
    public void init() {
        for (Action action : this.actions) action.init();
    }

    @Override
    public void trace(List<ItemDefinition.ExecutionTrace> trace, int depth) {
        String line = String.format("Wait for %d ticks:", this.duration);

        trace.add(new ItemDefinition.ExecutionTrace(depth, line));

        for (Action action : this.actions) action.trace(trace, depth + 1);
    }

    protected ActionWait() {
        super(ActionType.WAIT);
    }

    @Override
    public void process(InterpretationContext context) {
        InterpretationContext contextClone = context.copy();
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Action action : actions)
                    action.process(contextClone);

                contextClone.release();
            }

        }.runTaskLater(context.getPlugin(), duration);
    }

}
