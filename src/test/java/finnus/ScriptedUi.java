package finnus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import finnus.ui.Ui;

/**
 * Supplies deterministic input and captures messages without changing global console streams.
 */
public class ScriptedUi extends Ui {
    private final Queue<String> commands;
    private final List<String> messages = new ArrayList<>();

    /** Creates a console stub that returns the supplied commands in order. */
    public ScriptedUi(String... commands) {
        this.commands = new ArrayDeque<>(List.of(commands));
    }

    @Override
    public String readCommand() {
        return commands.poll();
    }

    //@@author lololol8484
    @Override
    public String readCommand(String prompt) {
        return readCommand();
    }
    //@@author

    @Override
    public void showMessage(String... lines) {
        messages.addAll(List.of(lines));
    }

    @Override
    public void showMessage(List<String> lines) {
        messages.addAll(lines);
    }

    /** Returns all captured messages in display order. */
    public String output() {
        return String.join("\n", messages);
    }
}
