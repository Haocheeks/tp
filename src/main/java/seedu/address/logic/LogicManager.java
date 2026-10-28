package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
        "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    public static final String READ_ONLY_ERROR_FORMAT = "Error: Local data file is corrupted at %s. "
        + "Displaying empty list. Please fix or delete the data file and restart.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final AddressBookParser addressBookParser;
    private boolean isReadOnlyMode = false;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this(model, storage, false);
    }

    /**
     * Constructs a {@code LogicManager} with an option to initialize in read-only mode.
     */
    public LogicManager(Model model, Storage storage, boolean isDataCorrupted) {
        this.model = model;
        this.storage = storage;
        addressBookParser = new AddressBookParser();
        this.isReadOnlyMode = isDataCorrupted;

        if (isReadOnlyMode) {
            logger.warning("Local storage corruption detected on launch sequence! Logic component locked.");
            this.model.setAddressBook(new AddressBook());
        }
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.fine("----------------[USER COMMAND][" + commandText + "]");

        CommandResult commandResult;
        Command command = addressBookParser.parseCommand(commandText);

        if (isReadOnlyMode && command.isMutating()) {
            throw new CommandException(String.format(READ_ONLY_ERROR_FORMAT, storage.getAddressBookFilePath()));
        }

        commandResult = command.execute(model);
        model.commitAddressBook();

        if (!isReadOnlyMode) {
            try {
                storage.saveAddressBook(model.getAddressBook());
            } catch (AccessDeniedException e) {
                throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
            } catch (IOException ioe) {
                throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
            }
        }

        return commandResult;
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }

    @Override
    public boolean isReadOnlyMode() {
        return isReadOnlyMode;
    }
}
