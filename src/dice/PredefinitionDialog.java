/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dice;

import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import jsonant.event.JSONListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class PredefinitionDialog implements JSONListener {
	private final JSONArray data;
	@FXML
	private TextField formula;
	@FXML
	private ListView<String> list;
	private MultipleSelectionModel<String> listModel = null;
	@FXML
	private TextField name;

	@FXML
	private BorderPane pane;

	@FXML
	private Button remove;

	private Stage window;

	/**
	 * Create the dialog.
	 *
	 * @param parentShell
	 */
	public PredefinitionDialog() {
		final JSONObject dice = ResourceManager.getResource("settings/Wuerfel");
		data = dice.getArr("Makros");
		data.addListener(this);
	}

	@FXML
	private void addEntry() {
		final JSONArray neu = new JSONArray(data);
		neu.add("Unbenannt");
		neu.add("");
		data.add(neu);
		list.getItems().add("Unbenannt");
		listModel.clearAndSelect(list.getItems().size() - 1);
		name.setText("Unbenannt");
		formula.setText("");
		remove.setDisable(false);
	}

	@FXML
	private void formulaChanged() {
		if (listModel.getSelectedIndex() > -1) {
			data.getArr(listModel.getSelectedIndex()).set(1, formula.getText());
			data.notifyListeners(this);
		}
	}

	@FXML
	private void nameChanged() {
		if (listModel.getSelectedIndex() > -1) {
			final int selected = listModel.getSelectedIndex();
			data.getArr(selected).set(0, name.getText());
			final int caret = name.getCaretPosition();
			final IndexRange selection = name.getSelection();
			list.getItems().set(selected, name.getText());
			listModel.clearAndSelect(selected);
			if (selection.getStart() == caret) {
				name.selectRange(selection.getEnd(), selection.getStart());
			} else {
				name.selectRange(selection.getStart(), selection.getEnd());
			}
			data.notifyListeners(this);

		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see resources.ResourceListener#notifyChanged()
	 */
	@Override
	public void notifyChanged(final JSONValue changed) {
		reload();
	}

	@FXML
	private void okClicked() {
		data.removeListener(this);
		window.close();
	}

	/**
	 * Reloads the data if it has changed
	 */
	private void reload() {
		list.getItems().clear();
		for (int i = 0; i < data.size(); ++i) {
			list.getItems().add(data.getArr(i).getString(0));
		}
		remove.setDisable(data.size() == 0);
		if (data.size() > 0) {
			name.setText(data.getArr(0).getString(0));
			formula.setText(data.getArr(0).getString(1));
			listModel.clearAndSelect(0);
		}
	}

	@FXML
	private void removeEntry() {
		final int index = listModel.getSelectedIndex();
		data.removeAt(index);
		data.notifyListeners(PredefinitionDialog.this);
		list.getItems().remove(index);
		if (data.size() == 0) {
			remove.setDisable(true);
			return;
		} else if (index == data.size()) {
			listModel.clearAndSelect(index - 1);
		} else {
			listModel.clearAndSelect(index);
		}
		name.setText(data.getArr(listModel.getSelectedIndex()).getString(0));
		formula.setText(data.getArr(listModel.getSelectedIndex()).getString(1));
	}

	public void show() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("PredefinitionDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		window = new Stage();
		window.setTitle("Vordefinierte Würfelformeln bearbeiten");

		listModel = list.getSelectionModel();

		listModel.selectedIndexProperty().addListener((final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) -> {
			if (listModel.getSelectedIndex() > -1) {
				name.setText(data.getArr(listModel.getSelectedIndex()).getString(0));
				formula.setText(data.getArr(listModel.getSelectedIndex()).getString(1));
				remove.setDisable(false);
			} else {
				remove.setDisable(true);
			}
		});

		reload();

		window.setScene(new Scene(pane, 400, 250));
		window.show();
	}
}
