package kaba4cow.tracker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import kaba4cow.ascii.MainProgram;
import kaba4cow.ascii.audio.Source;
import kaba4cow.ascii.core.Engine;
import kaba4cow.ascii.core.Input;
import kaba4cow.ascii.core.Renderer;
import kaba4cow.ascii.core.Window;
import kaba4cow.ascii.drawing.BoxDrawer;
import kaba4cow.ascii.drawing.Drawer;
import kaba4cow.ascii.drawing.Glyphs;
import kaba4cow.ascii.gui.GUIButton;
import kaba4cow.ascii.gui.GUIFrame;
import kaba4cow.ascii.gui.GUIRadioButton;
import kaba4cow.ascii.gui.GUIRadioPanel;
import kaba4cow.ascii.gui.GUISeparator;
import kaba4cow.ascii.gui.GUISlider;
import kaba4cow.ascii.gui.GUIText;
import kaba4cow.ascii.gui.GUITextField;
import kaba4cow.ascii.toolbox.Colors;
import kaba4cow.ascii.toolbox.Printer;
import kaba4cow.tracker.composition.Composition;
import kaba4cow.tracker.composition.CompositionReader;
import kaba4cow.tracker.composition.Music;
import kaba4cow.tracker.composition.Pattern;
import kaba4cow.tracker.composition.Sample;
import kaba4cow.tracker.composition.Track;

public class AsciiTracker implements MainProgram {

	private static final int COLOR = 0xC83000;

	private static final int OPTION_MENU = 0;
	private static final int OPTION_SONG_INFO = 1;
	private static final int OPTION_TRACK_LIST = 2;
	private static final int OPTION_TRACK_INFO = 3;
	private static final int OPTION_SAMPLE_LIST = 4;
	private static final int OPTION_SAMPLE_LIBRARY = 5;

	private int orderFrameX;
	private int patternFrameX;

	private Composition composition;

	private GUIFrame messageFrame;
	private GUIText messageText;

	private GUIFrame optionFrame;

	private GUIFrame menuFrame;

	private GUIFrame infoFrame;
	private GUITextField lengthField;
	private GUITextField tempoField;
	private GUISlider volumeSlider;
	private GUITextField nameField;
	private GUITextField authorField;
	private GUITextField commentField;

	private GUIFrame trackFrame;
	private GUIButton[] trackButtons;

	private GUIFrame[] trackFrames;
	private GUITextField[] trackNameFields;
	private GUIRadioPanel[] trackSamplePanels;
	private GUISlider[] trackVolumeSliders;

	private GUIFrame sampleFrame;

	private GUIFrame libraryFrame;

	private Source sampleSource;

	private int option;

	private int optionTrack;

	private int orderScroll;
	private int patternScrollX;
	private int patternScrollY;

	private int selectedBar;
	private int newSelectedBar;
	private int[] selectedNote;
	private int[] newSelectedNote;

	private boolean playButton;

	private Pattern.Data patternData;
	private File saveFile;

	private static int lastMidi = Music.INVALID_NOTE;
	private static int midiOctave = 1;
	private static boolean midiKeyboard = true;

	public AsciiTracker() {

	}

	@Override
	public void init() {
		Renderer.setFont(1);

		sampleSource = new Source("");

		selectedNote = new int[2];
		newSelectedNote = new int[2];

		// MESSAGE
		messageFrame = new GUIFrame(COLOR, false, false);
		messageText = new GUIText(messageFrame, -1, "");
		new GUISeparator(messageFrame, -1, true);
		new GUIButton(messageFrame, -1, "OK", f -> messageText.setText(""));

		// OPTIONS
		optionFrame = new GUIFrame(COLOR, false, false);
		new GUIButton(optionFrame, -1, "Menu", f -> option = OPTION_MENU);
		new GUIButton(optionFrame, -1, "Info", f -> option = OPTION_SONG_INFO);
		new GUIButton(optionFrame, -1, "Tracks", f -> option = OPTION_TRACK_LIST);
		new GUIButton(optionFrame, -1, "Samples", f -> option = OPTION_SAMPLE_LIST);
		new GUIButton(optionFrame, -1, "Library", f -> option = OPTION_SAMPLE_LIBRARY);

		// MENU
		menuFrame = new GUIFrame(COLOR, false, false).setTitle("Menu");
		new GUIButton(menuFrame, -1, "New", f -> newComposition());
		new GUIButton(menuFrame, -1, "Load", f -> loadComposition());
		new GUISeparator(menuFrame, -1, false);
		new GUIButton(menuFrame, -1, "Save", f -> saveComposition());
		new GUIButton(menuFrame, -1, "Save as", f -> saveAsComposition());
		new GUISeparator(menuFrame, -1, false);
		new GUIButton(menuFrame, -1, "Export Samples", f -> exportSamples());
		new GUISeparator(menuFrame, -1, false);
		new GUIButton(menuFrame, -1, "Exit", f -> Engine.requestClose());

		// INFO
		infoFrame = new GUIFrame(COLOR, false, false).setTitle("Info");
		new GUIText(infoFrame, -1, "Length:");
		lengthField = new GUITextField(infoFrame, -1, "").setOnlyDigits().setMaxCharacters(3);
		new GUIText(infoFrame, -1, "Tempo:");
		tempoField = new GUITextField(infoFrame, -1, "").setOnlyDigits().setMaxCharacters(3);
		new GUIText(infoFrame, -1, "Volume:");
		volumeSlider = new GUISlider(infoFrame, -1, 1f);
		new GUIText(infoFrame, -1, "Name:");
		nameField = new GUITextField(infoFrame, -1, "").setMaxCharacters(Music.STRING_LENGTH);
		new GUIText(infoFrame, -1, "Author:");
		authorField = new GUITextField(infoFrame, -1, "").setMaxCharacters(Music.STRING_LENGTH);
		new GUIText(infoFrame, -1, "Comments:");
		commentField = new GUITextField(infoFrame, -1, "").setMaxCharacters(Music.STRING_LENGTH);

		trackFrame = new GUIFrame(COLOR, false, false).setTitle("Tracks");
		trackButtons = new GUIButton[Music.TRACKS];
		for (int i = 0; i < Music.TRACKS; i++) {
			Integer index = i;
			trackButtons[i] = new GUIButton(trackFrame, -1, "", f -> {
				option = OPTION_TRACK_INFO;
				optionTrack = index;
			});
		}

		// TRACKS
		trackFrames = new GUIFrame[Music.TRACKS];
		trackNameFields = new GUITextField[Music.TRACKS];
		trackSamplePanels = new GUIRadioPanel[Music.TRACKS];
		trackVolumeSliders = new GUISlider[Music.TRACKS];
		for (int i = 0; i < Music.TRACKS; i++) {
			trackFrames[i] = new GUIFrame(COLOR, false, false).setTitle("Track");
			trackNameFields[i] = new GUITextField(trackFrames[i], -1, "").setMaxCharacters(Music.TRACK_NAME);
			new GUIText(trackFrames[i], -1, "Volume:");
			trackVolumeSliders[i] = new GUISlider(trackFrames[i], -1, 1f);
			new GUISeparator(trackFrames[i], -1, true);
			trackSamplePanels[i] = new GUIRadioPanel(trackFrames[i], -1, "Default Sample:");
		}

		// SAMPLES
		sampleFrame = new GUIFrame(COLOR, false, false).setTitle("Samples");

		// LIBRARY
		libraryFrame = new GUIFrame(COLOR, false, false).setTitle("Library");
		ArrayList<Sample> samples = Sample.getLibrary();
		for (int i = 0; i < samples.size(); i++) {
			Integer index = i;
			new GUIButton(libraryFrame, -1, Glyphs.BEAMED_EIGHTH_NOTES + " " + samples.get(i).getName(), f -> {
				Sample sample = samples.get(index);
				sampleSource.stop();
				sampleSource.play(sample.getBuffer());
				if (Input.isKey(Input.KEY_SHIFT_LEFT) && composition.addSample(sample))
					updateSampleList();
			});
		}

		newComposition();
	}

	private void updateComposition() {
		option = OPTION_MENU;
		optionTrack = 0;

		patternData = null;
		saveFile = null;

		orderScroll = 0;
		patternScrollX = 0;
		patternScrollY = 0;

		selectedBar = 0;
		newSelectedBar = 0;
		selectedNote[0] = -1;
		selectedNote[1] = -1;
		newSelectedNote[0] = -1;
		newSelectedNote[1] = -1;

		lengthField.setText(Integer.toString(composition.getLength()));
		tempoField.setText(Integer.toString(composition.getTempo()));
		volumeSlider.setPosition(composition.getVolume());
		nameField.setText(composition.getName());
		authorField.setText(composition.getAuthor());
		commentField.setText(composition.getComment());

		for (int i = 0; i < Music.TRACKS; i++) {
			Track track = composition.getTrack(i);
			trackButtons[i].setText(track.getName());
			trackNameFields[i].setText(track.getName());
			trackVolumeSliders[i].setPosition(track.getVolume());
		}

		updateSampleList();
	}

	private void updateSampleList() {
		sampleFrame.clear();
		ArrayList<Sample> samples = composition.getSamples();
		for (int i = 0; i < samples.size(); i++) {
			Integer index = i;
			new GUIButton(sampleFrame, -1, Music.getSampleName(i + 1) + ":" + samples.get(i).getName(), f -> {
				sampleSource.stop();
				if (Input.isKey(Input.KEY_SHIFT_LEFT) && composition.removeSample(index))
					updateSampleList();
				else
					sampleSource.play(samples.get(index).getBuffer());
			});
		}
		for (int i = 0; i < Music.TRACKS; i++) {
			trackSamplePanels[i].clear();
			for (int j = 0; j < samples.size(); j++)
				new GUIRadioButton(trackSamplePanels[i], -1,
						Music.getSampleName(j + 1) + ":" + samples.get(j).getName());
		}
	}

	private void newComposition() {
		composition = new Composition();
		updateComposition();
	}

	private void loadComposition() {
		try {
			composition = CompositionReader.read(new File("song"));
			updateComposition();
		} catch (Exception e) {
			message("Error: Unable to load the file");
		}
	}

	private void saveAsComposition() {
		saveFile = null;
		saveComposition();
	}

	private void saveComposition() {
		if (saveFile == null) {
			// TODO
			saveFile = new File("song2");
		}
		try {
			CompositionReader.write(composition, saveFile);
			message("File saved as \"" + saveFile.getName() + "\"");
		} catch (Exception e) {
			message("Error: Unable to save the file");
		}
	}

	private void exportSamples() {
		ArrayList<Sample> samples = composition.getSamples();
		if (samples.isEmpty()) {
			message("No samples found");
			return;
		}
		File directory = new File(composition.getName() + "_samples");
		int total = 0;
		for (int i = 0; i < samples.size(); i++)
			try {
				samples.get(i).export(directory);
				total++;
			} catch (IOException e) {
			}
		message("Samples exported (" + total + "/" + samples.size() + ")");
	}

	private void inputScroll() {
		if (composition.isPlaying()) {
			selectedBar = composition.getBar();
			orderScroll = 2 * selectedBar - 4;
			patternScrollY = 2 * composition.getPosition() - 4;
		} else if (Input.getTileX() >= patternFrameX) {
			if (Input.isKey(Input.KEY_SHIFT_LEFT))
				patternScrollX -= 9 * Input.getScroll();
			else
				patternScrollY -= 2 * Input.getScroll();
		} else if (Input.getTileX() >= orderFrameX) {
			if (Input.isKey(Input.KEY_SHIFT_LEFT))
				orderScroll -= 20 * Input.getScroll();
			else
				orderScroll -= 2 * Input.getScroll();
		}

		int maxOrderScroll = 2 * composition.getLength() - Window.getHeight() + 5;
		maxOrderScroll += maxOrderScroll % 2;
		if (maxOrderScroll < 0)
			maxOrderScroll = 0;
		if (orderScroll < 0)
			orderScroll = 0;
		else if (orderScroll > maxOrderScroll)
			orderScroll = maxOrderScroll;

		int maxPatternScrollX = Music.TRACKS * 10 - Window.getWidth() + patternFrameX + 8;
		if (maxPatternScrollX < 0)
			maxPatternScrollX = 0;
		if (patternScrollX < 0)
			patternScrollX = 0;
		else if (patternScrollX > maxPatternScrollX)
			patternScrollX = maxPatternScrollX;
		int maxPatternScrollY = 2 * Music.BAR - Window.getHeight() + 5;
		maxPatternScrollY += maxPatternScrollY % 2;
		if (maxPatternScrollY < 0)
			maxPatternScrollY = 0;
		if (patternScrollY < 0)
			patternScrollY = 0;
		else if (patternScrollY > maxPatternScrollY)
			patternScrollY = maxPatternScrollY;
	}

	private int inputMidi() {
		if (Input.isKeyDown(Input.KEY_M))
			midiKeyboard = !midiKeyboard;
		if (lastMidi != Music.INVALID_NOTE) {
			int note = lastMidi;
			lastMidi = Music.INVALID_NOTE;
			return note;
		}
		if (!midiKeyboard)
			return Music.INVALID_NOTE;
		if (Input.isKeyDown(Input.KEY_Z) && Music.NOTE_C + 12 * midiOctave > 12)
			midiOctave--;
		if (Input.isKeyDown(Input.KEY_X) && Music.NOTE_C + 12 * midiOctave < 116)
			midiOctave++;
		int octave = 12 * midiOctave;
		if (Input.isKeyDown(Input.KEY_A))
			return Music.NOTE_C - 12 + octave;
		if (Input.isKeyDown(Input.KEY_W))
			return Music.NOTE_Cs - 12 + octave;
		if (Input.isKeyDown(Input.KEY_S))
			return Music.NOTE_D - 12 + octave;
		if (Input.isKeyDown(Input.KEY_E))
			return Music.NOTE_Ds - 12 + octave;
		if (Input.isKeyDown(Input.KEY_D))
			return Music.NOTE_E - 12 + octave;
		if (Input.isKeyDown(Input.KEY_F))
			return Music.NOTE_F - 12 + octave;
		if (Input.isKeyDown(Input.KEY_T))
			return Music.NOTE_Fs - 12 + octave;
		if (Input.isKeyDown(Input.KEY_G))
			return Music.NOTE_G - 12 + octave;
		if (Input.isKeyDown(Input.KEY_Y))
			return Music.NOTE_Gs - 12 + octave;
		if (Input.isKeyDown(Input.KEY_H))
			return Music.NOTE_A - 12 + octave;
		if (Input.isKeyDown(Input.KEY_U))
			return Music.NOTE_As - 12 + octave;
		if (Input.isKeyDown(Input.KEY_J))
			return Music.NOTE_B - 12 + octave;
		if (Input.isKeyDown(Input.KEY_K))
			return Music.NOTE_C + octave;
		if (Input.isKeyDown(Input.KEY_O))
			return Music.NOTE_Cs + octave;
		if (Input.isKeyDown(Input.KEY_L))
			return Music.NOTE_D + octave;
		return Music.INVALID_NOTE;
	}

	private void inputSelectedNote(Pattern pattern) {
		int note = inputMidi();
		if (pattern != null && !composition.isPlaying() && selectedNote[0] != -1) {
			Track track = composition.getTrack(selectedNote[0]);
			if (note != Music.INVALID_NOTE) {
				pattern.setNote(selectedNote[0], selectedNote[1], note);
				track.update(pattern, selectedNote[1]);
			}
			if (Input.isKeyDown(Input.KEY_BACKSPACE))
				pattern.deleteNote(selectedNote[0], selectedNote[1]);
			else if (Input.isKeyDown(Input.KEY_B))
				pattern.setBreak(selectedNote[0], selectedNote[1]);
			else if (Input.isKey(Input.KEY_SHIFT_LEFT)) {
				if (Input.isKeyDown(Input.KEY_UP)) {
					pattern.changeNote(selectedNote[0], selectedNote[1], +1);
					track.update(pattern, selectedNote[1]);
				} else if (Input.isKeyDown(Input.KEY_DOWN)) {
					pattern.changeNote(selectedNote[0], selectedNote[1], -1);
					track.update(pattern, selectedNote[1]);
				} else if (Input.isKeyDown(Input.KEY_LEFT)) {
					pattern.prevSample(selectedNote[0], selectedNote[1]);
					track.update(pattern, selectedNote[1]);
				} else if (Input.isKeyDown(Input.KEY_RIGHT)) {
					pattern.nextSample(selectedNote[0], selectedNote[1]);
					track.update(pattern, selectedNote[1]);
				}
			} else if (Input.isKeyDown(Input.KEY_1)) {
				pattern.changeNote(selectedNote[0], selectedNote[1], -12);
				track.update(pattern, selectedNote[1]);
			} else if (Input.isKeyDown(Input.KEY_2)) {
				pattern.changeNote(selectedNote[0], selectedNote[1], +12);
				track.update(pattern, selectedNote[1]);
			}
		}
	}

	private void inputSelectedPattern(Pattern pattern) {
		if (Input.isKeyDown(Input.KEY_LEFT))
			composition.prevPattern(composition.getBar());
		else if (Input.isKeyDown(Input.KEY_RIGHT))
			composition.nextPattern(composition.getBar());
		else if (pattern != null) {
			if (Input.isKeyDown(Input.KEY_C))
				patternData = pattern.getData();
			else if (patternData != null && Input.isKeyDown(Input.KEY_V))
				pattern.setData(patternData);
			else if (Input.isKeyDown(Input.KEY_BACKSPACE))
				pattern.clear();
		}
	}

	private void inputMoveNote() {
		if (selectedNote[0] == -1 || Input.isKey(Input.KEY_SHIFT_LEFT))
			return;
		if (Input.isKeyDown(Input.KEY_UP)) {
			selectedNote[1]--;
			if (selectedNote[1] < 0)
				selectedNote[1] = 0;
		} else if (Input.isKeyDown(Input.KEY_DOWN)) {
			selectedNote[1]++;
			if (selectedNote[1] >= composition.getTotalLength())
				selectedNote[1] = composition.getTotalLength() - 1;
		} else if (Input.isKeyDown(Input.KEY_LEFT)) {
			selectedNote[0]--;
			if (selectedNote[0] < 0)
				selectedNote[0] = 0;
		} else if (Input.isKeyDown(Input.KEY_RIGHT)) {
			selectedNote[0]++;
			if (selectedNote[0] >= Music.TRACKS)
				selectedNote[0] = Music.TRACKS - 1;
		}
	}

	private void inputMovePattern() {
		if (Input.isKeyDown(Input.KEY_UP))
			composition.prevBar();
		else if (Input.isKeyDown(Input.KEY_DOWN))
			composition.nextBar();
	}

	@Override
	public void update(float dt) {
		if (Input.isKeyDown(Input.KEY_F11))
			if (Window.isFullscreen())
				Window.createWindowed(120, 80);
			else
				Window.createFullscreen();

		if (!messageText.getText().isEmpty()) {
			messageFrame.update();
			if (Input.isKeyDown(Input.KEY_ENTER))
				messageText.setText("");
			return;
		}

		if (Input.isKey(Input.KEY_CONTROL_LEFT) && Input.isKeyDown(Input.KEY_S)) {
			saveComposition();
			return;
		}

		orderFrameX = Window.getWidth() / 4;
		patternFrameX = orderFrameX + 9;

		Pattern pattern = composition.getPattern(composition.getBar());

		if (Input.isButtonDown(Input.LEFT)) {
			if (newSelectedBar != -1)
				composition.setBar(newSelectedBar);
			selectedNote[0] = newSelectedNote[0];
			selectedNote[1] = newSelectedNote[1];
		} else if (Input.isKeyDown(Input.KEY_ENTER))
			selectedNote[0] = -1;
		selectedBar = composition.getBar();

		if (Input.isKeyDown(Input.KEY_SPACE) || playButton && Input.isButtonDown(Input.LEFT)) {
			if (Input.isKey(Input.KEY_CONTROL_LEFT))
				composition.setBar(0);
			if (composition.isPlaying())
				composition.stop();
			else
				composition.play();
			playButton = false;
		}

		if (Input.isKey(Input.KEY_CONTROL_LEFT)) {
			inputSelectedPattern(pattern);
			inputMovePattern();
		} else {
			inputSelectedNote(pattern);
			inputMoveNote();
		}
		inputScroll();

		composition.update(dt);

		optionFrame.update();
		currentFrame().update();

		composition.setName(nameField.getText());
		composition.setAuthor(authorField.getText());
		composition.setComment(commentField.getText());
		if (!tempoField.getText().isEmpty())
			composition.setTempo(Integer.parseInt(tempoField.getText()));
		if (!lengthField.getText().isEmpty())
			composition.setLength(Integer.parseInt(lengthField.getText()));
		composition.setVolume(volumeSlider.getPosition());

		trackButtons[optionTrack].setText(trackNameFields[optionTrack].getText());
		composition.getTrack(optionTrack).setName(trackNameFields[optionTrack].getText());
		composition.getTrack(optionTrack).setDefaultSample(trackSamplePanels[optionTrack].getIndex());
		composition.getTrack(optionTrack).setVolume(trackVolumeSliders[optionTrack].getPosition());
	}

	@Override
	public void render() {
		optionFrame.render(0, 0, orderFrameX, Window.getHeight() / 3, false);
		currentFrame().render(0, Window.getHeight() / 3, orderFrameX, Window.getHeight() - Window.getHeight() / 3,
				false);

		newSelectedBar = -1;
		newSelectedNote[0] = -1;

		int mX = Input.getTileX();
		int mY = Input.getTileY();
		int color = Colors.swap(COLOR);

		renderOrder(mX, mY, color);
		renderPattern(mX, mY, color);

		Drawer.drawString(orderFrameX, 0, false,
				"BAR:" + Music.getBarName(composition.getBar()) + " PATTERN:"
						+ Music.getPatternName(composition.getPattern(selectedBar)) + " OCTAVE:"
						+ String.format("%+d", midiOctave) + " KEYBOARD:" + (midiKeyboard ? "ON" : "OFF"),
				color);

		if (!messageText.getText().isEmpty()) {
			BoxDrawer.disableCollision();
			messageFrame.render(Window.getWidth() / 2, Window.getHeight() / 2, Window.getWidth() / 2,
					Window.getHeight() / 5, true);
			BoxDrawer.enableCollision();
		}
	}

	private void renderOrder(int mX, int mY, int color) {
		// ORDER
		BoxDrawer.drawBox(orderFrameX, 1, 8, 2, false, color);
		Drawer.drawString(orderFrameX + 1, 2, false, "-ORDER-", color);

		// PATTERNS
		Drawer.enableClipping(orderFrameX, 4, Window.getWidth(), Window.getHeight());
		for (int i = 0; i < composition.getLength(); i++) {
			int y = i * 2 + 4 - orderScroll;
			Pattern pattern = composition.getPattern(i);

			if (mY == y + 1 && mX >= orderFrameX && mX <= orderFrameX + 8)
				newSelectedBar = i;

			String name = Music.getBarName(i) + "::" + Music.getPatternName(pattern);
			BoxDrawer.drawBox(orderFrameX, y, 8, 2, false, color);
			Drawer.drawString(orderFrameX + 1, y + 1, false, name, i == selectedBar ? COLOR : color);
		}
		Drawer.disableClipping();
	}

	private void renderPattern(int mX, int mY, int color) {
		int position = composition.getPosition();

		playButton = mY == 2 && mX >= patternFrameX && mX <= patternFrameX + 7;

		// STOP/PLAY
		BoxDrawer.drawBox(patternFrameX, 1, 7, 2, false, color);
		Drawer.drawString(patternFrameX + 1, 2, false,
				composition.isPlaying() ? (Glyphs.MEDIUM_SHADE + " STOP") : (Glyphs.BLACK_RIGHT_POINTER + " PLAY"),
				playButton ? COLOR : color);

		// POSITIONS
		Drawer.enableClipping(patternFrameX, 4, Window.getWidth(), Window.getHeight());
		for (int j = 0; j < Music.BAR; j++) {
			int y = j * 2 + 4 - patternScrollY;
			BoxDrawer.drawBox(patternFrameX, y, 7, 2, false, color);
			Drawer.drawString(patternFrameX + 1, y + 1, false, Music.getPositionName(selectedBar, j),
					position == j ? COLOR : color);
		}

		// NOTES
		Pattern pattern = composition.getPattern(selectedBar);
		for (int i = 0; i < Music.TRACKS; i++) {
			int x = i * 10 + patternFrameX + 8 - patternScrollX;
			Track track = composition.getTrack(i);

			Drawer.enableClipping(patternFrameX + 8, 0, Window.getWidth(), Window.getHeight());
			BoxDrawer.drawBox(x, 1, 9, 2, false, color);
			Drawer.drawString(x + 1, 2, false, track.getName(), color);

			Drawer.enableClipping(patternFrameX + 8, 4, Window.getWidth(), Window.getHeight());
			for (int j = 0; j < Music.BAR; j++) {
				int y = j * 2 + 4 - patternScrollY;
				BoxDrawer.drawBox(x, y, 9, 2, true, color);

				if (mY == y + 1 && mX >= x && mX <= x + 9) {
					newSelectedNote[0] = i;
					newSelectedNote[1] = j;
				}

				int currentColor = (i == selectedNote[0] && j == selectedNote[1]
						&& Engine.getElapsedTime() % 0.6f < 0.3f) ? COLOR : color;

				String name = Music.getNoteSampleName(pattern, i, j);
				Drawer.drawString(x + 1, y + 1, false, name, currentColor);
			}
		}
		Drawer.disableClipping();
	}

	private GUIFrame currentFrame() {
		if (option == OPTION_MENU)
			return menuFrame;
		if (option == OPTION_TRACK_LIST)
			return trackFrame;
		if (option == OPTION_SAMPLE_LIST)
			return sampleFrame;
		if (option == OPTION_SAMPLE_LIBRARY)
			return libraryFrame;
		if (option == OPTION_TRACK_INFO)
			return trackFrames[optionTrack];
		return infoFrame;
	}

	private void message(String message) {
		messageText.setText(message);
	}

	public static void main(String[] args) throws Exception {
		MidiDevice device;
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < infos.length; i++)
			try {
				device = MidiSystem.getMidiDevice(infos[i]);
				List<Transmitter> transmitters = device.getTransmitters();
				for (int j = 0; j < transmitters.size(); j++)
					transmitters.get(j).setReceiver(new MidiInputReceiver());
				Transmitter transmitter = device.getTransmitter();
				transmitter.setReceiver(new MidiInputReceiver());
				device.open();
				Printer.println("MIDI device: " + device.getDeviceInfo());

			} catch (MidiUnavailableException e) {
			}

		Engine.init("Ascii Tracker", 8, 60);
		Sample.loadLibrary("library");
		Window.createWindowed(120, 80);
		Engine.start(new AsciiTracker());
	}

	private static class MidiInputReceiver implements Receiver {

		public MidiInputReceiver() {

		}

		@Override
		public void send(MidiMessage msg, long timeStamp) {
			byte[] bytes = msg.getMessage();
			if (bytes[0] == -112)
				lastMidi = bytes[1];
		}

		@Override
		public void close() {
		}
	}

}