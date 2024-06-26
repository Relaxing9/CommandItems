package me.relaxing9.commanditems.data.action;

import java.util.List;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonProperty;

import me.relaxing9.commanditems.CommandItems;
import me.relaxing9.commanditems.data.ItemDefinition;
import me.relaxing9.commanditems.interpreter.InterpretationContext;

/**
 * Created by Yamakaja on 27.05.18.
 */
public class ActionCalc extends Action {

    @JsonProperty(required = true, value = "op")
    private OperationType operationType;

    @JsonProperty(required = true)
    private String a;

    @JsonProperty(required = true)
    private String b;

    @JsonProperty
    private String target = "y";

    @JsonProperty(required = true)
    private Action[] actions;

    public ActionCalc() {
        super(ActionType.CALC);
    }

    @Override
    public void init() {
        for (Action action : this.actions) action.init();
    }

    @Override
    public void trace(List<ItemDefinition.ExecutionTrace> trace, int depth) {
        ItemDefinition.ExecutionTrace line = new ItemDefinition.ExecutionTrace(depth, String.format("%s = %s %c %s",
                target, a, operationType.op, b));

        trace.add(line);

        for (Action action : this.actions) action.trace(trace, depth + 1);
    }

    @Override
    public void process(InterpretationContext context) {
        try {
            int a = Integer.parseInt(context.resolveLocalsInString(this.a));
            int b = Integer.parseInt(context.resolveLocalsInString(this.b));

            context.pushFrame();
            context.pushLocal(target, String.valueOf(this.operationType.process(a, b)));

            for (Action action : actions)
                action.process(context);

            context.popFrame();
        } catch (NumberFormatException e) {
            CommandItems.logger.log(Level.SEVERE, "Parsing numbers failed: ", e);
        }
    }

    public enum OperationType {
        ADD('+') {
            public int process(int a, int b) {
                return a + b;
            }
        },
        SUB('-') {
            public int process(int a, int b) {
                return a - b;
            }
        },
        MUL('*') {
            public int process(int a, int b) {
                return a * b;
            }
        },
        DIV('/') {
            public int process(int a, int b) {
                return a / b;
            }
        };

        public final char op;

        OperationType(char op) {
            this.op = op;
        }

        public abstract int process(int a, int b);
    }

}
