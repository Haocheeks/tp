package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.VALID_NAME_AMY;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Phone;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

@ExtendWith(ApplicationExtension.class)
public class MainWindowTest {

    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
    private static final PseudoClass EMPTY_SEARCH_PSEUDO_CLASS = PseudoClass.getPseudoClass("empty-search");

    @TempDir
    public Path temporaryFolder;

    private Stage stage;

    @Start
    public void start(Stage stage) {
        this.stage = stage;
        Path addressBookFilePath = temporaryFolder.resolve("addressBook.json");
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(addressBookFilePath),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        MainWindow mainWindow = new MainWindow(stage, new LogicManager(new ModelManager(), storage),
                addressBookFilePath);
        mainWindow.fillInnerParts();
        stage.show();
    }

    @Test
    public void executeCommand_validCommand_showsSuccess(FxRobot robot) {
        enterCommand(robot, ListCommand.COMMAND_WORD);

        assertTrue(getResultMessage().getText().startsWith(ListCommand.MESSAGE_SUCCESS));
        assertFalse(getResultDisplay().getPseudoClassStates().contains(ERROR_PSEUDO_CLASS));
    }

    @Test
    public void executeCommand_searchWithNoMatches_showsEmptySearch(FxRobot robot) {
        enterCommand(robot, FindCommand.COMMAND_WORD + " Amirah");

        assertTrue(getResultMessage().getText().startsWith(FindCommand.MESSAGE_NO_CLIENTS_FOUND));
        assertTrue(getResultDisplay().getPseudoClassStates().contains(EMPTY_SEARCH_PSEUDO_CLASS));
    }

    @Test
    public void executeCommand_errorWrapsAtMinimumWidth_shownInFullWithoutScrolling(FxRobot robot) {
        robot.interact(() -> stage.setWidth(stage.getMinWidth()));
        enterCommand(robot, "edit 1 p/123");

        Label resultMessage = getResultMessage();
        Node resultScrollPane = stage.getScene().lookup("#resultScrollPane");
        assertEquals(Phone.MESSAGE_CONSTRAINTS, ((Text) resultMessage.lookup(".text")).getText());
        assertEquals(resultMessage.getHeight(), resultScrollPane.getLayoutBounds().getHeight(), 0.5);
    }

    @Test
    public void pageDownInCommandBox_longResult_scrollsResult(FxRobot robot) {
        robot.interact(() -> stage.setWidth(stage.getMinWidth()));
        // Too long to show in full at the minimum width
        enterCommand(robot, "add n/Amirah Tan p/91234567 e/amirah@example.com a/" + "Blk 123 Yishun Ave 5 ".repeat(7)
                + "nt/" + "Weak at algebra ".repeat(12));

        ScrollPane resultScrollPane = (ScrollPane) stage.getScene().lookup("#resultScrollPane");
        TextField commandTextField = (TextField) stage.getScene().lookup("#commandTextField");
        robot.interact(() -> commandTextField.fireEvent(
                new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.PAGE_DOWN, false, false, false, false)));

        assertTrue(resultScrollPane.getVvalue() > resultScrollPane.getVmin());
    }

    @Test
    public void executeCommand_invalidCommand_showsError(FxRobot robot) {
        enterCommand(robot, "delete 0");

        assertTrue(getResultMessage().getText().startsWith("Invalid command format!"));
        assertTrue(getResultDisplay().getPseudoClassStates().contains(ERROR_PSEUDO_CLASS));
    }

    @Test
    public void executeCommand_clientDetails_loggedBelowInfoOnly(FxRobot robot) {
        List<LogRecord> records = new ArrayList<>();
        Handler recordCollector = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                records.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        // The parent of every LogsCenter logger; its level decides what reaches the log file
        Logger baseLogger = LogsCenter.getLogger(MainWindowTest.class).getParent();
        Level originalLevel = baseLogger.getLevel();
        baseLogger.setLevel(Level.ALL);
        baseLogger.addHandler(recordCollector);
        try {
            enterCommand(robot, AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY + EMAIL_DESC_AMY
                    + ADDRESS_DESC_AMY);
            enterCommand(robot, AddCommand.COMMAND_WORD + NAME_DESC_AMY);
        } finally {
            baseLogger.removeHandler(recordCollector);
            baseLogger.setLevel(originalLevel);
        }

        List<LogRecord> recordsWithName = records.stream()
                .filter(logRecord -> logRecord.getMessage().contains(VALID_NAME_AMY))
                .toList();
        assertFalse(recordsWithName.isEmpty());
        assertTrue(recordsWithName.stream()
                .allMatch(logRecord -> logRecord.getLevel().intValue() < Level.INFO.intValue()));
    }

    @Test
    public void toggleTheme_switchesBetweenDarkAndLightThemes(FxRobot robot) {
        Button themeToggleButton = (Button) stage.getScene().lookup("#themeToggleButton");

        assertTrue(stage.getScene().getStylesheets().stream()
                .noneMatch(stylesheet -> stylesheet.endsWith("DarkTheme.css")));
        assertTrue(themeToggleButton.getText().equals("Dark theme"));

        robot.interact(themeToggleButton::fire);

        assertTrue(stage.getScene().getStylesheets().stream()
                .anyMatch(stylesheet -> stylesheet.endsWith("DarkTheme.css")));
        assertTrue(themeToggleButton.getText().equals("Light theme"));

        robot.interact(themeToggleButton::fire);

        assertTrue(stage.getScene().getStylesheets().stream()
                .noneMatch(stylesheet -> stylesheet.endsWith("DarkTheme.css")));
        assertTrue(themeToggleButton.getText().equals("Dark theme"));
    }

    /**
     * Types {@code command} into the command box and presses Enter.
     */
    private void enterCommand(FxRobot robot, String command) {
        TextField commandTextField = (TextField) stage.getScene().lookup("#commandTextField");
        robot.interact(() -> {
            commandTextField.setText(command);
            commandTextField.fireEvent(new ActionEvent());
        });
    }

    private Label getResultMessage() {
        return (Label) stage.getScene().lookup("#resultMessage");
    }

    private Node getResultDisplay() {
        return stage.getScene().lookup(".result-display");
    }
}
