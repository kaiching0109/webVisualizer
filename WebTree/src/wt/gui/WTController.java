package wt.gui;

import djf.components.AppDataComponent;
import djf.ui.AppMessageDialogSingleton;
import java.io.IOException;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebEngine;
import properties_manager.PropertiesManager;
import static wt.LanguageProperty.ADD_ELEMENT_ERROR_MESSAGE;
import static wt.LanguageProperty.ADD_ELEMENT_ERROR_TITLE;
import static wt.LanguageProperty.ATTRIBUTE_UPDATE_ERROR_MESSAGE;
import static wt.LanguageProperty.ATTRIBUTE_UPDATE_ERROR_TITLE;
import static wt.LanguageProperty.CSS_EXPORT_ERROR_MESSAGE;
import static wt.LanguageProperty.CSS_EXPORT_ERROR_TITLE;
import static wt.LanguageProperty.ILLEGAL_NODE_REMOVAL_ERROR_MESSAGE;
import static wt.LanguageProperty.ILLEGAL_NODE_REMOVAL_ERROR_TITLE;
import static wt.LanguageProperty.REMOVE_ELEMENT_ERROR_MESSAGE;
import static wt.LanguageProperty.REMOVE_ELEMENT_ERROR_TITLE;
import wt.WebTreeApp;
import wt.data.WTData;
import wt.data.HTMLTagPrototype;
import static wt.data.HTMLTagPrototype.TAG_BODY;
import static wt.data.HTMLTagPrototype.TAG_HEAD;
import static wt.data.HTMLTagPrototype.TAG_HTML;
import static wt.data.HTMLTagPrototype.TAG_LINK;
import static wt.data.HTMLTagPrototype.TAG_TITLE;
import wt.file.WTFiles;
import static wt.file.WTFiles.TEMP_HOME_CSS;
import static wt.file.WTFiles.TEMP_PAGE;

/**
 * This class provides event programmed responses to workspace interactions for
 * this application for things like adding elements, removing elements, and
 * editing them.
 *
 * @author Richard McKenna
 * @author ?
 * @version 1.0
 */
public class WTController {

    // HERE'S THE FULL APP, WHICH GIVES US ACCESS TO OTHER STUFF
    private final WebTreeApp app;

    // WE USE THIS TO MAKE SURE OUR PROGRAMMED UPDATES OF UI
    // VALUES DON'T THEMSELVES TRIGGER EVENTS
    private boolean enabled;

    /**
     * Constructor for initializing this object, it will keep the app for later.
     *
     * @param initApp The JavaFX application this controller is associated with.
     */
    public WTController(WebTreeApp initApp) {
	// KEEP IT FOR LATER
	app = initApp;
    }

    /**
     * This mutator method lets us enable or disable this controller.
     *
     * @param enableSetting If false, this controller will not respond to
     * workspace editing. If true, it will.
     */
    public void enable(boolean enableSetting) {
	enabled = enableSetting;
    }

    /**
     * This function responds live to the user typing changes into a text field
     * for updating element attributes. It will respond by updating the
     * appropriate data and then forcing an update of the temp site and its
     * display.
     *
     * @param selectedTag The element in the DOM (our tree) that's currently
     * selected and therefore is currently having its attribute updated.
     *
     * @param attributeName The name of the attribute for the element that is
     * currently being updated.
     *
     * @param attributeValue The new value for the attribute that is being
     * updated.
     */
    public void handleAttributeUpdate(HTMLTagPrototype selectedTag, String attributeName, String attributeValue) {
	if (enabled) {
	    try {
		// FIRST UPDATE THE ELEMENT'S DATA
		selectedTag.addAttribute(attributeName, attributeValue);

		// THEN FORCE THE CHANGES TO THE TEMP HTML PAGE
		WTFiles fileManager = (WTFiles) app.getFileComponent();
		fileManager.exportData(app.getDataComponent(), TEMP_PAGE);

		// AND FINALLY UPDATE THE WEB PAGE DISPLAY USING THE NEW VALUES
		WTWorkspace workspace = (WTWorkspace) app.getWorkspaceComponent();
		workspace.getHTMLEngine().reload();
	    } catch (IOException ioe) {
		// AN ERROR HAPPENED WRITING TO THE TEMP FILE, NOTIFY THE USER
		PropertiesManager props = PropertiesManager.getPropertiesManager();
		AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		dialog.show(props.getProperty(ATTRIBUTE_UPDATE_ERROR_TITLE), props.getProperty(ATTRIBUTE_UPDATE_ERROR_MESSAGE));
	    }
	}
    }

    /**
     * This function responds to when the user tries to add an element to the
     * tree being edited.
     *
     * @param element The element to add to the tree.
     */
    public void handleAddElementRequest(HTMLTagPrototype element) {
	if (enabled) {
	    WTWorkspace workspace = (WTWorkspace) app.getWorkspaceComponent();

	    // GET THE TREE TO SEE WHICH NODE IS CURRENTLY SELECTED
	    TreeView<HTMLTagPrototype> tree = workspace.getHTMLTree();
	    TreeItem<HTMLTagPrototype> selectedItem = tree.getSelectionModel().getSelectedItem();
	    HTMLTagPrototype selectedTag = selectedItem.getValue();

	    // MAKE A NEW HTMLTagPrototype AND PUT IT IN A NODE
	    HTMLTagPrototype newTag = element.clone();
	    TreeItem<HTMLTagPrototype> newNode = new TreeItem<>(newTag);

	    // ONLY ADD IT IF IT'S BEING ADDED TO A LEGAL NEIGHBOR
	    if (newTag.isLegalParent(selectedTag.getTagName())) {
		selectedItem.getChildren().add(newNode);

		// SELECT THE NEW NODE
		tree.getSelectionModel().select(newNode);
		selectedItem.setExpanded(true);

		// FORCE A RELOAD OF TAG EDITOR
                AppDataComponent data = app.getDataComponent();
		workspace.reloadWorkspace(data);

		workspace.refreshTagButtons();
		try {
		    WTFiles fileManager = (WTFiles) app.getFileComponent();
		    fileManager.exportData(data, TEMP_PAGE);
		} catch (IOException ioe) {
		    // AN ERROR HAPPENED WRITING TO THE TEMP FILE, NOTIFY THE USER
		    PropertiesManager props = PropertiesManager.getPropertiesManager();
		    AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		    dialog.show(props.getProperty(ADD_ELEMENT_ERROR_TITLE), props.getProperty(ADD_ELEMENT_ERROR_MESSAGE));
		}
	    }
	}
    }

    /**
     * This function responds to when the user requests to remove an element
     * from the tree. It responds by removing he currently selected node.
     */
    public void handleRemoveElementRequest() {
	PropertiesManager props = PropertiesManager.getPropertiesManager();

	if (enabled) {
	    WTWorkspace workspace = (WTWorkspace) app.getWorkspaceComponent();

	    // GET THE TREE TO SEE WHICH NODE IS CURRENTLY SELECTED
	    TreeView<HTMLTagPrototype> tree = workspace.getHTMLTree();
	    TreeItem<HTMLTagPrototype> selectedItem = tree.getSelectionModel().getSelectedItem();

	    // DON'T LET THE USER REMOVE THE HTML, HEAD,
	    // TITLE, LINK, OR BODY TAGS
	    HTMLTagPrototype selectedTag = selectedItem.getValue();
	    String tagName = selectedTag.getTagName();

	    // DON'T LET THE USER DELETE THESE ELEMENTS
	    if (tagName.equals(TAG_HTML)
		    || tagName.equals(TAG_HEAD)
		    || tagName.equals(TAG_TITLE)
		    || tagName.equals(TAG_LINK)
		    || tagName.equals(TAG_BODY)) {
		AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		dialog.show(props.getProperty(ILLEGAL_NODE_REMOVAL_ERROR_MESSAGE), props.getProperty(ILLEGAL_NODE_REMOVAL_ERROR_TITLE));
	    } else {
		TreeItem<HTMLTagPrototype> parentNode = selectedItem.getParent();

		parentNode.getChildren().remove(selectedItem);
		tree.getSelectionModel().select(parentNode);

		// FORCE A RELOAD OF TAG EDITOR
                AppDataComponent data = app.getDataComponent();
		workspace.reloadWorkspace(data);
                workspace.refreshTagButtons();
		try {
		    // NOW FORCE THE CHANGES TO OUR TEMP FILE
		    WTFiles fileManager = (WTFiles) app.getFileComponent();
		    fileManager.exportData(app.getDataComponent(), TEMP_PAGE);

		    // AND UPDATE THE PAGE
		    workspace.getHTMLEngine().reload();
		} catch (IOException ioe) {
		    AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		    dialog.show(props.getProperty(REMOVE_ELEMENT_ERROR_TITLE), props.getProperty(REMOVE_ELEMENT_ERROR_MESSAGE));
		}
	    }
	}
    }

    /**
     * This function provides a response to when the user changes the CSS
     * content. It responds but updating the data manager with the new CSS text,
     * and by exporting the CSS to the temp css file.
     *
     * @param cssContent The css content.
     *
     * @throws IOException Thrown should an error occur while writing out to the
     * CSS file.
     */
    public void handleCSSEditing(String cssContent) {
	if (enabled) {
	    try {
		// MAKE SURE THE DATA MANAGER GETS THE CSS TEXT
		WTData dataManager = (WTData) app.getDataComponent();
		dataManager.setCSSText(cssContent);

		// WRITE OUT THE TEXT TO THE CSS FILE
		WTFiles fileManager = (WTFiles) app.getFileComponent();
		fileManager.exportCSS(cssContent, TEMP_HOME_CSS);

		// REFRESH THE HTML VIEW VIA THE ENGINE
		WTWorkspace workspace = (WTWorkspace) app.getWorkspaceComponent();
		WebEngine htmlEngine = workspace.getHTMLEngine();
		htmlEngine.reload();
	    } catch (IOException ioe) {
		AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		PropertiesManager props = PropertiesManager.getPropertiesManager();
		dialog.show(props.getProperty(CSS_EXPORT_ERROR_TITLE), props.getProperty(CSS_EXPORT_ERROR_MESSAGE));
	    }
	}
    }
}
