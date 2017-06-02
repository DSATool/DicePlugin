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

import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;

import dsatool.resources.ResourceManager;
import dsatool.util.ReactiveSpinner;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextArea;
import javafx.util.StringConverter;
import jsonant.event.JSONListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class DiceController implements JSONListener {
	@FXML
	public ReactiveSpinner<Integer> dice;
	@FXML
	private ReactiveSpinner<Integer> count;
	private JSONArray data;
	@FXML
	private ReactiveSpinner<Integer> mod;
	private final Random r = new Random();
	@FXML
	private TextArea results;
	@FXML
	private ReactiveSpinner<Integer> rolls;
	@FXML
	private Label selectedFormula;
	@FXML
	ComboBox<String> formula;

	/**
	 * Rolls dice as specified by a formula
	 *
	 * @param formula
	 *            The formula for rolling the dice. Format: Individual rolls are
	 *            separated by '|' Repetition of the same roll is expressed by
	 *            the number of repetitions followed by 'x' followed by the roll
	 *            itself The number of identical dice and the count of their
	 *            sides is expressed by the number of dice followed by 'W'
	 *            followed by the number of sides, where the number of dice may
	 *            be omitted if it is 1 Constants may be added, subtracted,
	 *            multiplied or divided by stating them explicitly Arithmetic
	 *            operations are expressed by '+', '-', '*' and '/' respectively
	 *            Parentheses can be used inside a roll as usual Example:
	 *            "2xW6+5 | 2*2W20" starts by rolling one W6 and adding 5 twice,
	 *            then rolling 2 W20 and multiplying the sum by 2
	 */
	private void interpretFormula(final String formula) {
		final String[] rolls = formula.split("\\|");
		final StringBuilder resultText = new StringBuilder();
		for (String roll : rolls) {
			final int posRep = roll.indexOf('x');
			int numRep = 1;
			if (posRep != -1) {
				try {
					numRep = Integer.parseUnsignedInt(roll.substring(0, posRep).trim());
					roll = roll.substring(posRep + 1).trim();
				} catch (final NumberFormatException e) {
					final String repSpec = roll.substring(0, posRep).trim();
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Formel konnte nicht interpretiert werden");
					alert.setHeaderText("Formel konnte nicht interpretiert werden");
					alert.setContentText("Zahl von Wiederholungen muss positive Ganzzahl sein, war aber " + repSpec);
					alert.showAndWait();
					return;
				}
			}
			for (int i = 0; i < numRep; ++i) {
				if (i != 0) {
					resultText.append('\n');
				}
				final StringTokenizer t = new StringTokenizer(roll, "()+-*/W", true);
				final int result = interpretSimpleFormula(t, resultText);
				resultText.append(" = ");
				resultText.append(result);
			}
			resultText.append('\n');
		}
		resultText.append(results.getText());
		results.setText(resultText.toString());
	}

	/**
	 * Interprets the simple part of a dice rolling formula
	 *
	 * @param roll
	 *            The simple formula consisting of single rolls and arithmetic
	 *            operations only
	 * @param resultText
	 *            A StringBuilder to append the roll to
	 * @return The result of evaluating this formula
	 */
	private int interpretSimpleFormula(final StringTokenizer t, final StringBuilder resultText) {
		int result = 0;
		int curMul = 1;
		String token = "";
		int current = 1;
		boolean printCurrent = true;
		boolean pushBack = false;
		final Stack<Character> operators = new Stack<>();
		operators.push('+');
		while (t.hasMoreTokens()) {
			if (pushBack) {
				pushBack = false;
			} else {
				do {
					token = t.nextToken().trim();
				} while (token.isEmpty());
			}
			switch (token) {
			case "W":
				try {
					int sum = 0;
					int dice = 6;
					if (t.hasMoreTokens()) {
						do {
							token = t.nextToken().trim();
						} while (token.isEmpty());
						if ("+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token)) {
							pushBack = true;
						} else {
							dice = Integer.parseUnsignedInt(token);
						}
					}
					if (current > 1) {
						resultText.append('(');
					}
					int cur = r.nextInt(dice) + 1;
					resultText.append(cur);
					sum += cur;
					for (int j = 1; j < current; ++j) {
						resultText.append(" + ");
						cur = r.nextInt(dice) + 1;
						resultText.append(cur);
						sum += cur;
					}
					if (current > 1) {
						resultText.append(')');
					}
					current = sum;
				} catch (final NumberFormatException e) {
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Formel konnte nicht interpretiert werden");
					alert.setHeaderText("Formel konnte nicht interpretiert werden");
					alert.setContentText("Seitenzahl muss positive Ganzzahl sein, war aber " + token);
					alert.showAndWait();
					return -1;
				}
				printCurrent = false;
				break;
			case "(":
				if (current != 1) {
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Formel konnte nicht interpretiert werden");
					alert.setHeaderText("Formel konnte nicht interpretiert werden");
					alert.setContentText("Vor einer öffnenden Klammer muss ein Rechenzeichen stehen!");
					alert.showAndWait();
					return -1;
				}
				resultText.append('(');
				current = interpretSimpleFormula(t, resultText);
				printCurrent = false;
				break;
			case ")":
				switch (operators.pop()) {
				case '+':
					result += current;
					break;
				case '-':
					result -= current;
					break;
				case '*':
					curMul *= current;
					break;
				case '/':
					curMul = Math.round(curMul / (float) current);
					break;
				}
				if (!operators.empty()) {
					switch (operators.pop()) {
					case '+':
						result += curMul;
					case '-':
						result -= curMul;
					}
				}
				if (printCurrent) {
					resultText.append(current);
				}
				resultText.append(')');
				return result;
			case "+":
				operators.push('+');
				if (printCurrent) {
					resultText.append(current);
				}
				resultText.append(" + ");
				switch (operators.get(operators.size() - 2)) {
				case '+':
					result += current;
					break;
				case '-':
					result -= current;
					break;
				case '*':
					curMul *= current;
				case '/':
					curMul = Math.round(curMul / (float) current);
					switch (operators.get(operators.size() - 3)) {
					case '+':
						result += curMul;
						break;
					case '-':
						result -= curMul;
						break;
					}
					operators.remove(operators.size() - 3);
				}
				operators.remove(operators.size() - 2);
				current = 1;
				break;
			case "-":
				operators.push('-');
				if (printCurrent) {
					resultText.append(current);
				}
				resultText.append(" - ");
				switch (operators.get(operators.size() - 2)) {
				case '+':
					result += current;
					break;
				case '-':
					result -= current;
					break;
				case '*':
					curMul *= current;
				case '/':
					curMul = Math.round(curMul / (float) current);
					switch (operators.get(operators.size() - 3)) {
					case '+':
						result += curMul;
						break;
					case '-':
						result -= curMul;
						break;
					}
					operators.remove(operators.size() - 3);
				}
				operators.remove(operators.size() - 2);
				current = 1;
				break;
			case "*":
				operators.push('*');
				if (printCurrent) {
					resultText.append(current);
				}
				resultText.append(" * ");
				switch (operators.get(operators.size() - 2)) {
				case '+':
				case '-':
					curMul = current;
					break;
				case '*':
					curMul *= current;
					operators.remove(operators.size() - 2);
					break;
				case '/':
					curMul = Math.round(curMul / (float) current);
					operators.remove(operators.size() - 2);
					break;
				}
				current = 1;
				break;
			case "/":
				operators.push('/');
				resultText.append(" / ");
				switch (operators.get(operators.size() - 2)) {
				case '+':
				case '-':
					curMul = current;
					break;
				case '*':
					curMul *= current;
					operators.remove(operators.size() - 2);
					break;
				case '/':
					curMul = Math.round(curMul / (float) current);
					operators.remove(operators.size() - 2);
					break;
				}
				current = 1;
				break;
			default:
				try {
					current = Integer.parseUnsignedInt(token);
				} catch (final NumberFormatException e) {
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Formel konnte nicht interpretiert werden");
					alert.setHeaderText("Formel konnte nicht interpretiert werden");
					alert.setContentText("Positive Ganzzahl erwartet, war aber " + token);
					alert.showAndWait();
					return -1;
				}
				printCurrent = true;
			}
		}
		switch (operators.pop()) {
		case '+':
			result += current;
			break;
		case '-':
			result -= current;
			break;
		case '*':
			curMul *= current;
			break;
		case '/':
			curMul = Math.round(curMul / (float) current);
			break;
		}
		if (!operators.empty()) {
			switch (operators.pop()) {
			case '+':
				result += curMul;
				break;
			case '-':
				result -= curMul;
				break;
			}
		}
		if (printCurrent) {
			resultText.append(current);
		}
		return result;
	}

	public void load() {
		final JSONObject diceObject = ResourceManager.getResource("settings/Wuerfel");
		data = diceObject.getArr("Makros");
		if (data == null) {
			data = new JSONArray(diceObject);
			diceObject.put("Makros", data);
			diceObject.notifyListeners(this);
		}
		data.addListener(this);

		reload();
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

	public void prepare() {
		rolls.setConverter(new StringConverter<Integer>() {
			@Override
			public Integer fromString(final String string) {
				return Integer.parseInt(string.substring(0, string.length() - 2));
			}

			@Override
			public String toString(final Integer value) {
				if (value == null) return null;
				return value.toString() + " x";
			}
		});
		rolls.valueProperty().addListener(o -> {
			updateSelectedFormula();
		});

		count.valueProperty().addListener(o -> {
			updateSelectedFormula();
		});

		dice.valueProperty().addListener(o -> {
			updateSelectedFormula();
		});

		mod.setConverter(new StringConverter<Integer>() {
			@Override
			public Integer fromString(String string) {
				if (string.startsWith("±")) {
					string = string.substring(1);
				}
				return Integer.parseInt(string);
			}

			@Override
			public String toString(final Integer integer) {
				return (integer == 0 ? "±" : integer > 0 ? "+" : "") + integer;
			}
		});
		mod.valueProperty().addListener(o -> {
			updateSelectedFormula();
		});

		load();

		updateSelectedFormula();
	}

	/**
	 * Reloads the data if it has changed
	 */
	private void reload() {
		formula.getItems().clear();
		for (int i = 0; i < data.size(); ++i) {
			formula.getItems().add(data.getArr(i).getString(0));
		}
	}

	/**
	 * Rolls the given dice as specified by the spinners
	 *
	 * @param dice
	 *            The number of sides of the dice to roll
	 */
	public void roll(final int dice) {
		roll(rolls.getValue(), count.getValue(), dice, mod.getValue());
	}

	/**
	 * Rolls the given dice
	 *
	 * @param rolls
	 *            Number of individual rolls to perform
	 * @param count
	 *            Number of dice per roll
	 * @param dice
	 *            The number of sides of the dice to roll
	 * @param mod
	 *            An additive modifier to the sum
	 */
	private void roll(final int rolls, final int count, final int dice, final int mod) {
		final StringBuilder resultsText = new StringBuilder();
		for (int i = 0; i < rolls; ++i) {
			int sum = 0;
			final StringBuilder result = new StringBuilder();
			int current = r.nextInt(dice) + 1;
			result.append(current);
			sum += current;
			for (int j = 1; j < count; ++j) {
				result.append(" + ");
				current = r.nextInt(dice) + 1;
				result.append(current);
				sum += current;
			}
			if (mod != 0) {
				if (mod > 0) {
					result.append(" + ");
				} else {
					result.append("-");
				}
				result.append(Math.abs(mod));
				sum += mod;
			}
			if (count > 1 || mod != 0) {
				result.append(" = ");
				result.append(sum);
			}
			result.append('\n');
			resultsText.append(result);
		}
		resultsText.append(results.getText());
		results.setText(resultsText.toString());
	}

	@FXML
	private void rollFormula() {
		final SingleSelectionModel<String> formulaModel = formula.getSelectionModel();
		if (formulaModel.getSelectedIndex() > -1) {
			final JSONObject wuerfel = ResourceManager.getResource("settings/Wuerfel");
			final JSONArray daten = wuerfel.getArr("Makros").getArr(formulaModel.getSelectedIndex());
			if (daten.getString(0).equals(formulaModel.getSelectedItem())) {
				final String formulaString = daten.getString(1);
				interpretFormula(formulaString);
				results.setText(formulaModel.getSelectedItem() + " (" + formulaString + "):\n" + results.getText());
			} else {
				interpretFormula(formulaModel.getSelectedItem());
				results.setText(formulaModel.getSelectedItem() + ":\n" + results.getText());
			}
		} else if (formulaModel.getSelectedItem() != null) {
			interpretFormula(formulaModel.getSelectedItem());
			results.setText(formulaModel.getSelectedItem() + ":\n" + results.getText());
		}
	}

	@FXML
	private void showPredefinitionDialog() {
		new PredefinitionDialog().show();
	}

	/**
	 * Updates the label to show the formula currently selected with the
	 * spinners
	 */
	private void updateSelectedFormula() {
		final StringBuilder formula = new StringBuilder();
		formula.append(rolls.getValue());
		formula.append('x');
		formula.append(count.getValue());
		formula.append('W');
		formula.append(dice.getValue());
		if (mod.getValue() != 0) {
			if (mod.getValue() > 0) {
				formula.append('+');
			}
			formula.append(mod.getValue());
		}
		selectedFormula.setText(formula.toString());
	}

}
