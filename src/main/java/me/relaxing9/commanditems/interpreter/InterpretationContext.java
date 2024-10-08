package me.relaxing9.commanditems.interpreter;

import java.lang.ref.Cleaner;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import me.relaxing9.commanditems.CommandItems;

/**
 * Created by Yamakaja on 26.05.18.
 * Modified by Relaxing9 on 19.09.24.
 */
public class InterpretationContext {

    private static Lock cacheLock = new ReentrantLock();
    private static Deque<InterpretationStackFrame> stackFrameCache = new ArrayDeque<>();

    private Deque<InterpretationStackFrame> interpretationStack = new ArrayDeque<>();
    private CommandItems plugin;
    private Player player;
    private static final Cleaner cleaner = Cleaner.create();
    private final Cleaner.Cleanable cleanable;

    public InterpretationContext(CommandItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        cleanable = cleaner.register(this, () -> {
            close();
        });
    }

    public InterpretationContext(InterpretationContext context) {
        this.plugin = context.plugin;
        this.player = context.player;
        cleanable = cleaner.register(this, () -> {
            close();
        });

        for (InterpretationStackFrame frame : context.interpretationStack)
            this.interpretationStack.add(frame.copy(getNewFrame()));
    }

    private static void addToCache(InterpretationStackFrame frame) {
        try {
            cacheLock.lock();
            stackFrameCache.push(frame);
        } finally {
            cacheLock.unlock();
        }
    }

    public CommandItems getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    private InterpretationStackFrame getNewFrame() {
        if (stackFrameCache.size() < 1)
            return new InterpretationStackFrame();

        return stackFrameCache.remove();
    }

    public void pushFrame() {
        this.interpretationStack.addFirst(new InterpretationStackFrame());
    }

    public void popFrame() {
        InterpretationStackFrame stackFrame = this.interpretationStack.removeFirst();
        stackFrame.reset();
        stackFrameCache.push(stackFrame);
    }

    public void pushLocal(String key, String value) {
        this.interpretationStack.getFirst().pushLocal(key, value);
    }

    public String resolveLocal(String key) {
        Iterator<InterpretationStackFrame> iterator = this.interpretationStack.iterator();
        while (iterator.hasNext()) {
            InterpretationStackFrame next = iterator.next();
            String result = next.getLocal(key);
            if (result != null)
                return result;
        }

        return null;
    }

    public void forEachNumericLocal(BiConsumer<String, Double> consumer) {
        Iterator<InterpretationStackFrame> iterator = this.interpretationStack.descendingIterator();

        while (iterator.hasNext()) {
            InterpretationStackFrame next = iterator.next();
            for (Map.Entry<String, String> entry : next.getLocals().entrySet()) {
                try {
                    consumer.accept(entry.getKey(), Double.parseDouble(entry.getValue()));
                } catch(Exception ignored) {
                }
            }
        }
    }

    public String resolveLocalsInString(String input) {
        char[] chars = input.toCharArray();
        StringBuilder outputBuilder = new StringBuilder();

        boolean escaped = false;

        for (int i = 0; i < chars.length; i++) {
            if (escaped) {
                outputBuilder.append(chars[i]);
                escaped = false;
                continue;
            } else if (chars[i] == '\\') {
                escaped = true;
                continue;
            }

            if (chars[i] != '{') {
                outputBuilder.append(chars[i]);
                continue;
            }

            int end = input.indexOf('}', i);
            if (end == -1)
                CommandItems.logger.log(Level.WARNING,"Unterminated curly braces!");

            String localName = input.substring(i + 1, end);
            String local = this.resolveLocal(localName);

            if (local == null)
                CommandItems.logger.log(Level.SEVERE, ("Attempt to access undefined local '" + localName + "'!"));

            outputBuilder.append(local);
            i = end;
        }

        return outputBuilder.toString();
    }

    public InterpretationContext copy() {
        return new InterpretationContext(this);
    }

    protected void close() {
        release();
    }

    public void release() {
        for (InterpretationStackFrame frame : this.interpretationStack) {
            frame.reset();
            addToCache(frame);
        }

        this.interpretationStack.clear();
        this.cleanable.clean();
    }
}
