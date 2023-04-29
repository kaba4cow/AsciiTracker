package kaba4cow.tracker;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

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
import kaba4cow.ascii.gui.GUIColorText;
import kaba4cow.ascii.gui.GUIFileBrowser;
import kaba4cow.ascii.gui.GUIFileTree;
import kaba4cow.ascii.gui.GUIFrame;
import kaba4cow.ascii.gui.GUIRadioButton;
import kaba4cow.ascii.gui.GUIRadioPanel;
import kaba4cow.ascii.gui.GUISeparator;
import kaba4cow.ascii.gui.GUISlider;
import kaba4cow.ascii.gui.GUIText;
import kaba4cow.ascii.gui.GUITextField;
import kaba4cow.ascii.toolbox.Colors;
import kaba4cow.ascii.toolbox.Printer;
import kaba4cow.ascii.toolbox.utils.StringUtils;
import kaba4cow.ascii.tracker.Composition;
import kaba4cow.ascii.tracker.CompositionReader;
import kaba4cow.ascii.tracker.Pattern;
import kaba4cow.ascii.tracker.Sample;
import kaba4cow.ascii.tracker.Track;

public class AsciiTracker implements MainProgram {

	private static final String[] NOTES = { "C-", "C#", "D-", "D#", "E-", "F-", "F#", "G-", "G#", "A-", "A#", "B-" };

	// A# D F G F D D# D TODO

	public static final int NOTE_C = 60;
	public static final int NOTE_Cs = 61;
	public static final int NOTE_D = 62;
	public static final int NOTE_Ds = 63;
	public static final int NOTE_E = 64;
	public static final int NOTE_F = 65;
	public static final int NOTE_Fs = 66;
	public static final int NOTE_G = 67;
	public static final int NOTE_Gs = 68;
	public static final int NOTE_A = 69;
	public static final int NOTE_As = 70;
	public static final int NOTE_B = 71;

	private static final int COLOR = 0xFA6000;

	private static final int BROWSER_NONE = 0;
	private static final int BROWSER_SAVE = 1;
	private static final int BROWSER_OPEN = 2;

	private static final int OPTION_MENU = 0;
	private static final int OPTION_SONG_INFO = 1;
	private static final int OPTION_TRACK_LIST = 2;
	private static final int OPTION_TRACK_INFO = 3;
	private static final int OPTION_SAMPLE_LIST = 4;
	private static final int OPTION_SAMPLE_LIBRARY = 5;

	private final Preferences preferences;

	private Source sampleSource;

	private Composition composition;
	private Pattern.Data patternCopy;
	private File saveFile;

	private boolean library;
	private boolean help;
	private int browser;
	private int option;
	private int optionTrack;

	private int orderFrameX;
	private int patternFrameX;

	private int orderScroll;
	private int patternScrollX;
	private int patternScrollY;

	private int selectedBar;
	private int newSelectedBar;
	private int selectedTrack;
	private int newSelectedTrack;
	private int selectedNote;
	private int newSelectedNote;

	private HelpFrame helpFrame;
	private MessageFrame messageFrame;

	private GUIFrame libraryBrowserFrame;
	private GUIButton libraryBrowserButton;
	private GUIFileBrowser libraryBrowser;

	private GUIFrame fileBrowserFrame;
	private GUIButton fileBrowserButton;
	private GUIFileBrowser fileBrowser;

	private GUIFrame optionFrame;

	private GUIFrame menuFrame;

	private GUIFrame infoFrame;
	private GUITextField lengthField;
	private GUITextField tempoField;
	private GUISlider volumeSlider;
	private GUITextField nameField;
	private GUITextField authorField;

	private GUIFrame trackFrame;
	private GUIButton[] trackButtons;

	private GUIFrame[] trackFrames;
	private GUITextField[] trackNameFields;
	private GUIRadioPanel[] trackSamplePanels;
	private GUISlider[] trackVolumeSliders;

	private GUIFrame sampleFrame;

	private GUIFrame libraryFrame;
	private GUIFileTree libraryTree;

	private static int lastMidi = Composition.INVALID;
	private static int midiOctave = 1;
	private static boolean midiKeyboard = true;

	public AsciiTracker() {
		preferences = Preferences.userNodeForPackage(getClass());
	}

	private File getFileLocation() {
		File file = new File(preferences.get("proj-location", System.getProperty("user.dir")));
		if (!file.exists())
			file = new File(System.getProperty("user.dir"));
		return file;
	}

	private File getLibraryLocation() {
		File file = new File(preferences.get("lib-location", System.getProperty("user.dir")));
		if (!file.exists())
			file = new File(System.getProperty("user.dir"));
		return file;
	}

	@Override
	public void init() {
		Renderer.setFont(1);

		sampleSource = new Source();

		// HELP
		helpFrame = new HelpFrame();

		// MESSAGE
		messageFrame = new MessageFrame();

		// LIBRARY BROWSER
		libraryBrowserFrame = new GUIFrame(COLOR, false, false);
		libraryBrowserButton = new GUIButton(libraryBrowserFrame, -1, "", f -> selectLibraryLocation());
		libraryBrowser = new GUIFileBrowser(libraryBrowserFrame, -1, getLibraryLocation());
		libraryBrowser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory();
			}
		});
		Sample.loadLibrary(getLibraryLocation());

		// FILE BROWSER
		fileBrowserFrame = new GUIFrame(COLOR, false, false);
		fileBrowserButton = new GUIButton(fileBrowserFrame, -1, "", f -> selectBrowserFile());
		fileBrowser = new GUIFileBrowser(fileBrowserFrame, -1, getFileLocation())
				.setFileGlyph(Glyphs.BEAMED_EIGHTH_NOTES);
		fileBrowser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".atc");
			}
		});

		// OPTIONS
		optionFrame = new GUIFrame(COLOR, false, false).setTitle("Options");
		new GUIButton(optionFrame, -1, "Menu", f -> option = OPTION_MENU);
		new GUIButton(optionFrame, -1, "Info", f -> option = OPTION_SONG_INFO);
		new GUIButton(optionFrame, -1, "Tracks", f -> option = OPTION_TRACK_LIST);
		new GUIButton(optionFrame, -1, "Samples", f -> option = OPTION_SAMPLE_LIST);
		new GUIButton(optionFrame, -1, "Library", f -> option = OPTION_SAMPLE_LIBRARY);

		// MENU
		menuFrame = new GUIFrame(COLOR, false, false).setTitle("Menu");
		new GUIButton(menuFrame, -1, "New", f -> newComposition());
		new GUIButton(menuFrame, -1, "Open", f -> openComposition());
		new GUISeparator(menuFrame, -1, false);
		new GUIButton(menuFrame, -1, "Save", f -> saveComposition(false));
		new GUIButton(menuFrame, -1, "Save as", f -> saveComposition(true));
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
		nameField = new GUITextField(infoFrame, -1, "").setMaxCharacters(Composition.STRING_LENGTH);
		new GUIText(infoFrame, -1, "Author:");
		authorField = new GUITextField(infoFrame, -1, "").setMaxCharacters(Composition.STRING_LENGTH);

		trackFrame = new GUIFrame(COLOR, false, false).setTitle("Tracks");
		trackButtons = new GUIButton[Composition.MAX_TRACKS];
		for (int i = 0; i < Composition.MAX_TRACKS; i++) {
			Integer index = i;
			trackButtons[i] = new GUIButton(trackFrame, -1, "", f -> {
				option = OPTION_TRACK_INFO;
				optionTrack = index;
			});
		}

		// MAX_TRACKS
		trackFrames = new GUIFrame[Composition.MAX_TRACKS];
		trackNameFields = new GUITextField[Composition.MAX_TRACKS];
		trackSamplePanels = new GUIRadioPanel[Composition.MAX_TRACKS];
		trackVolumeSliders = new GUISlider[Composition.MAX_TRACKS];
		for (int i = 0; i < Composition.MAX_TRACKS; i++) {
			trackFrames[i] = new GUIFrame(COLOR, false, false).setTitle("Track");
			trackNameFields[i] = new GUITextField(trackFrames[i], -1, "")
					.setMaxCharacters(Composition.TRACK_NAME_LENGTH);
			new GUIText(trackFrames[i], -1, "Volume:");
			trackVolumeSliders[i] = new GUISlider(trackFrames[i], -1, 1f);
			new GUISeparator(trackFrames[i], -1, true);
			trackSamplePanels[i] = new GUIRadioPanel(trackFrames[i], -1, "Default Sample:");
		}

		// MAX_SAMPLES
		sampleFrame = new GUIFrame(COLOR, false, false).setTitle("Samples");

		// LIBRARY
		libraryFrame = new GUIFrame(COLOR, false, false).setTitle("Library");
		new GUIButton(libraryFrame, -1, "Change location", f -> changeLibraryLocation());
		new GUIButton(libraryFrame, -1, "Add sample", f -> addSample());
		libraryTree = new GUIFileTree(libraryFrame, -1, getLibraryLocation()) {
			@Override
			public void onNewFileSelected(File file) {
				Sample sample = Sample.get(file.getAbsolutePath());
				if (sample != null)
					sampleSource.play(sample.getBuffer());
			}
		}.setFileGlyph(Glyphs.BEAMED_EIGHTH_NOTES);
		libraryTree.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".wav");
			}
		});
		libraryTree.refresh();
		new GUISeparator(libraryFrame, -1, false);

		newComposition();
	}

	private void updateComposition(File file) {
		option = OPTION_MENU;
		optionTrack = 0;

		patternCopy = null;
		saveFile = file;

		orderScroll = 0;
		patternScrollX = 0;
		patternScrollY = 0;

		selectedBar = 0;
		newSelectedBar = 0;
		selectedTrack = 0;
		newSelectedTrack = 0;
		selectedNote = -1;
		newSelectedNote = -1;

		lengthField.setText(Integer.toString(composition.getLength()));
		tempoField.setText(Integer.toString(composition.getTempo()));
		volumeSlider.setPosition(composition.getVolume());
		nameField.setText(composition.getName());
		authorField.setText(composition.getAuthor());

		for (int i = 0; i < Composition.MAX_TRACKS; i++) {
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
			new GUIButton(sampleFrame, -1, getSampleName(i + 1) + ":" + samples.get(i).getName(), f -> {
				sampleSource.stop();
				if (Input.isKey(Input.KEY_SHIFT_LEFT) && composition.removeSample(index))
					updateSampleList();
				else
					sampleSource.play(samples.get(index).getBuffer());
			});
		}
		for (int i = 0; i < Composition.MAX_TRACKS; i++) {
			trackSamplePanels[i].clear();
			for (int j = 0; j < samples.size(); j++)
				new GUIRadioButton(trackSamplePanels[i], -1, getSampleName(j + 1) + ":" + samples.get(j).getName());
			trackSamplePanels[i].setIndex(composition.getTrack(i).getDefaultSample());
		}
	}

	private void addSample() {
		if (libraryTree.getSelectedFile() == null)
			return;
		Sample sample = Sample.get(libraryTree.getSelectedFile().getAbsolutePath());
		if (composition.addSample(sample))
			updateSampleList();
	}

	private void changeLibraryLocation() {
		library = true;
		libraryBrowser.setDirectory(getLibraryLocation());
	}

	private void selectLibraryLocation() {
		File directory = libraryBrowser.getSelectedFile();
		libraryBrowser.setDirectory(directory);
		Sample.loadLibrary(directory);
		preferences.put("lib-location", directory.getAbsolutePath());
		libraryTree.setDirectory(getLibraryLocation());
		library = false;
	}

	private void selectBrowserFile() {
		File file = fileBrowser.getSelectedFile();
		if (file == null)
			return;
		if (browser == BROWSER_OPEN && file.isFile())
			openComposition(file);
		else if (browser == BROWSER_SAVE)
			saveComposition(file);
		preferences.put("proj-location", fileBrowser.getDirectory().getAbsolutePath());
	}

	private void newComposition() {
		composition = new Composition();
		updateComposition(null);
	}

	private void openComposition() {
		fileBrowser.refresh();
		browser = BROWSER_OPEN;
	}

	private void openComposition(File file) {
		Composition newComposition = null;
		try {
			newComposition = CompositionReader.read(file);
		} catch (Exception e) {
		}
		if (newComposition == null)
			messageFrame.spawn("Error: Unable to load the file");
		else {
			composition = newComposition;
			updateComposition(file);
			browser = BROWSER_NONE;
		}
	}

	private void saveComposition(boolean newFile) {
		fileBrowser.refresh();
		if (newFile)
			saveFile = null;
		if (saveFile == null)
			browser = BROWSER_SAVE;
		else
			saveComposition(saveFile);
	}

	private void saveComposition(File file) {
		if (file.isDirectory())
			file = new File(file.getAbsolutePath() + "/" + composition.getName() + ".atc");
		saveFile = file;
		try {
			CompositionReader.write(composition, file);
			messageFrame.spawn("File saved as \"" + file.getName() + "\"");
			browser = BROWSER_NONE;
		} catch (Exception e) {
			messageFrame.spawn("Error: Unable to save the file");
		}
		fileBrowser.refresh();
	}

	private void exportSamples() {
		ArrayList<Sample> samples = composition.getSamples();
		if (samples.isEmpty()) {
			messageFrame.spawn("No samples found");
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
		messageFrame.spawn("Samples exported (" + total + "/" + samples.size() + ")");
	}

	@Override
	public void update(float dt) {
		if (Input.isKeyDown(Input.KEY_F11))
			if (Window.isFullscreen())
				Window.createWindowed(120, 80);
			else
				Window.createFullscreen();

		if (messageFrame.isActive()) {
			messageFrame.update();
			return;
		}
		if (Input.isKeyDown(Input.KEY_ESCAPE)) {
			library = false;
			help = false;
			browser = BROWSER_NONE;
		}
		if (library) {
			File file = libraryBrowser.getSelectedFile();
			String string = "Set to: ";
			if (libraryBrowser.getSelectedFile() != null)
				string += file.getParentFile() == null ? file.getAbsolutePath() : file.getName();
			libraryBrowserButton.setText(string);
			libraryBrowserFrame.update();
			if (Input.isKeyDown(Input.KEY_ENTER))
				selectLibraryLocation();
			return;
		}
		if (browser != BROWSER_NONE) {
			File file = fileBrowser.getSelectedFile();
			String string = browser == BROWSER_OPEN ? "Open: " : "Save to: ";
			if (fileBrowser.getSelectedFile() != null)
				string += file.getParentFile() == null ? file.getAbsolutePath() : file.getName();
			fileBrowserButton.setText(string);
			fileBrowserFrame.update();
			if (Input.isKeyDown(Input.KEY_ENTER))
				selectBrowserFile();
			return;
		}
		if (Input.isKey(Input.KEY_CONTROL_LEFT) && Input.isKeyDown(Input.KEY_H))
			help = !help;
		if (help) {
			helpFrame.update();
			return;
		}

		if (Input.isKey(Input.KEY_CONTROL_LEFT)) {
			if (Input.isKeyDown(Input.KEY_N)) {
				newComposition();
				return;
			} else if (Input.isKeyDown(Input.KEY_O)) {
				openComposition();
				return;
			} else if (Input.isKeyDown(Input.KEY_S)) {
				saveComposition(Input.isKey(Input.KEY_SHIFT_LEFT));
				return;
			} else if (Input.isKeyDown(Input.KEY_E)) {
				exportSamples();
				return;
			} else if (Input.isKeyDown(Input.KEY_W)) {
				Engine.requestClose();
				return;
			}
		}

		inputOptions();

		orderFrameX = Window.getWidth() / 4;
		patternFrameX = orderFrameX + 5;

		Pattern pattern = composition.getTrack(selectedTrack).getPattern(composition.getBar());

		if (Input.isButtonDown(Input.LEFT)) {
			if (newSelectedBar != -1)
				composition.setBar(newSelectedBar);
			if (newSelectedTrack != -1)
				selectedTrack = newSelectedTrack;
			selectedNote = newSelectedNote;
		} else if (Input.isKeyDown(Input.KEY_ENTER))
			selectedNote = -1;
		selectedBar = composition.getBar();

		if (Input.isKeyDown(Input.KEY_SPACE)) {
			if (sampleSource.isPlaying())
				sampleSource.stop();
			else {
				if (Input.isKey(Input.KEY_CONTROL_LEFT))
					composition.setBar(0);
				if (composition.isPlaying())
					composition.stop();
				else
					composition.play();
			}
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

	private void inputOptions() {
		if (Input.isKey(Input.KEY_CONTROL_LEFT)) {
			if (Input.isKeyDown(Input.KEY_1))
				option = OPTION_MENU;
			else if (Input.isKeyDown(Input.KEY_2))
				option = OPTION_SONG_INFO;
			else if (Input.isKeyDown(Input.KEY_3))
				option = OPTION_TRACK_LIST;
			else if (Input.isKeyDown(Input.KEY_4))
				option = OPTION_SAMPLE_LIST;
			else if (Input.isKeyDown(Input.KEY_5))
				option = OPTION_SAMPLE_LIBRARY;
		}
	}

	private void inputScroll() {
		if (composition.isPlaying()) {
			selectedBar = composition.getBar();
			orderScroll = 2 * selectedBar - 4;
			patternScrollY = 2 * composition.getPosition() - 4;
		} else if (Input.getTileX() >= patternFrameX) {
			if (Input.isKey(Input.KEY_SHIFT_LEFT))
				patternScrollX -= 8 * Input.getScroll();
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

		int maxPatternScrollX = Composition.MAX_TRACKS * 9 - Window.getWidth() + patternFrameX + 4;
		if (maxPatternScrollX < 0)
			maxPatternScrollX = 0;
		if (patternScrollX < 0)
			patternScrollX = 0;
		else if (patternScrollX > maxPatternScrollX)
			patternScrollX = maxPatternScrollX;
		int maxPatternScrollY = 2 * Composition.PATTERN_LENGTH - Window.getHeight() + 5;
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
		if (lastMidi != Composition.INVALID) {
			int note = lastMidi;
			lastMidi = Composition.INVALID;
			return note;
		}
		if (!midiKeyboard)
			return Composition.INVALID;
		if (!Input.isKey(Input.KEY_SHIFT_LEFT)) {
			if (Input.isKeyDown(Input.KEY_Z) && NOTE_C + 12 * midiOctave > 12)
				midiOctave--;
			if (Input.isKeyDown(Input.KEY_X) && NOTE_C + 12 * midiOctave < 116)
				midiOctave++;
		}
		int octave = 12 * midiOctave;
		if (Input.isKeyDown(Input.KEY_A))
			return NOTE_C - 12 + octave;
		if (Input.isKeyDown(Input.KEY_W))
			return NOTE_Cs - 12 + octave;
		if (Input.isKeyDown(Input.KEY_S))
			return NOTE_D - 12 + octave;
		if (Input.isKeyDown(Input.KEY_E))
			return NOTE_Ds - 12 + octave;
		if (Input.isKeyDown(Input.KEY_D))
			return NOTE_E - 12 + octave;
		if (Input.isKeyDown(Input.KEY_F))
			return NOTE_F - 12 + octave;
		if (Input.isKeyDown(Input.KEY_T))
			return NOTE_Fs - 12 + octave;
		if (Input.isKeyDown(Input.KEY_G))
			return NOTE_G - 12 + octave;
		if (Input.isKeyDown(Input.KEY_Y))
			return NOTE_Gs - 12 + octave;
		if (Input.isKeyDown(Input.KEY_H))
			return NOTE_A - 12 + octave;
		if (Input.isKeyDown(Input.KEY_U))
			return NOTE_As - 12 + octave;
		if (Input.isKeyDown(Input.KEY_J))
			return NOTE_B - 12 + octave;
		if (Input.isKeyDown(Input.KEY_K))
			return NOTE_C + octave;
		if (Input.isKeyDown(Input.KEY_O))
			return NOTE_Cs + octave;
		if (Input.isKeyDown(Input.KEY_L))
			return NOTE_D + octave;
		return Composition.INVALID;
	}

	private void inputSelectedNote(Pattern pattern) {
		int note = inputMidi();
		if (pattern != null && !composition.isPlaying() && selectedTrack != -1 && selectedNote != -1) {
			Track track = composition.getTrack(selectedTrack);
			if (note != Composition.INVALID) {
				pattern.setNote(selectedNote, note);
				track.play(pattern, selectedNote);
			}
			if (Input.isKeyDown(Input.KEY_BACKSPACE))
				pattern.deleteNote(selectedNote);
			else if (Input.isKeyDown(Input.KEY_B))
				pattern.setBreak(selectedNote);
			else if (Input.isKey(Input.KEY_SHIFT_LEFT)) {
				if (Input.isKeyDown(Input.KEY_DOWN)) {
					pattern.prevNote(selectedNote);
					track.play(pattern, selectedNote);
				} else if (Input.isKeyDown(Input.KEY_UP)) {
					pattern.nextNote(selectedNote);
					track.play(pattern, selectedNote);
				} else if (Input.isKeyDown(Input.KEY_LEFT)) {
					pattern.prevSample(selectedNote);
					track.play(pattern, selectedNote);
				} else if (Input.isKeyDown(Input.KEY_RIGHT)) {
					pattern.nextSample(selectedNote);
					track.play(pattern, selectedNote);
				} else if (Input.isKeyDown(Input.KEY_Z)) {
					pattern.prevOctave(selectedNote);
					track.play(pattern, selectedNote);
				} else if (Input.isKeyDown(Input.KEY_X)) {
					pattern.nextOctave(selectedNote);
					track.play(pattern, selectedNote);
				}
			}
		}
	}

	private void inputSelectedPattern(Pattern pattern) {
		if (selectedTrack == -1)
			return;
		Track track = composition.getTrack(selectedTrack);
		if (Input.isKeyDown(Input.KEY_LEFT))
			track.prevPattern(composition.getBar());
		else if (Input.isKeyDown(Input.KEY_RIGHT))
			track.nextPattern(composition.getBar());
		else if (pattern != null) {
			if (Input.isKeyDown(Input.KEY_C))
				patternCopy = pattern.getData();
			else if (patternCopy != null && Input.isKeyDown(Input.KEY_V))
				pattern.setData(patternCopy);
			else if (Input.isKeyDown(Input.KEY_BACKSPACE))
				pattern.clear();
		}
	}

	private void inputMoveNote() {
		if (selectedTrack == -1 || Input.isKey(Input.KEY_SHIFT_LEFT))
			return;
		if (Input.isKeyDown(Input.KEY_UP)) {
			selectedNote--;
			if (selectedNote < 0)
				selectedNote = 0;
		} else if (Input.isKeyDown(Input.KEY_DOWN)) {
			selectedNote++;
			if (selectedNote >= composition.getTotalLength())
				selectedNote = composition.getTotalLength() - 1;
		} else if (Input.isKeyDown(Input.KEY_LEFT)) {
			selectedTrack--;
			if (selectedTrack < 0)
				selectedTrack = 0;
		} else if (Input.isKeyDown(Input.KEY_RIGHT)) {
			selectedTrack++;
			if (selectedTrack >= Composition.MAX_TRACKS)
				selectedTrack = Composition.MAX_TRACKS - 1;
		}
	}

	private void inputMovePattern() {
		if (Input.isKeyDown(Input.KEY_UP))
			composition.prevBar();
		else if (Input.isKeyDown(Input.KEY_DOWN))
			composition.nextBar();
	}

	@Override
	public void render() {
		optionFrame.render(0, 0, orderFrameX, Window.getHeight() / 3, false);
		currentFrame().render(0, Window.getHeight() / 3, orderFrameX, Window.getHeight() - Window.getHeight() / 3,
				false);

		newSelectedBar = -1;
		newSelectedTrack = -1;
		newSelectedNote = -1;

		int mX = Input.getTileX();
		int mY = Input.getTileY();
		int color = Colors.swap(COLOR);

		renderOrder(mX, mY, color);
		renderPattern(mX, mY, color);

		Drawer.drawString(orderFrameX, 0, false,
				"FILE:" + (saveFile == null ? "NONE" : saveFile.getName()) + " BAR:" + getBarName(composition.getBar())
						+ " OCTAVE:" + String.format("%+d", midiOctave) + " KEYBOARD:" + (midiKeyboard ? "ON" : "OFF"),
				color);

		BoxDrawer.disableCollision();
		if (help)
			helpFrame.render(orderFrameX, 0, Window.getWidth() - orderFrameX, Window.getHeight(), false);
		if (library)
			libraryBrowserFrame.render(Window.getWidth() / 2, Window.getHeight() / 2, Window.getWidth() / 2,
					Window.getHeight() / 2, true);
		else if (browser != BROWSER_NONE)
			fileBrowserFrame.render(Window.getWidth() / 2, Window.getHeight() / 2, Window.getWidth() / 2,
					Window.getHeight() / 2, true);
		if (messageFrame.isActive())
			messageFrame.render(Window.getWidth() / 2, Window.getHeight() / 2, Window.getWidth() / 3,
					Window.getHeight() / 5, true);
		BoxDrawer.enableCollision();
	}

	private void renderOrder(int mX, int mY, int color) {
		// ORDER
		BoxDrawer.drawBox(orderFrameX, 1, 4, 2, false, color);
		Drawer.drawString(orderFrameX + 1, 2, false, "BAR", color);

		// MAX_PATTERNS
		Drawer.enableClipping(orderFrameX, 4, Window.getWidth(), Window.getHeight());
		for (int i = 0; i < composition.getLength(); i++) {
			int y = i * 2 + 4 - orderScroll;

			if (mY == y + 1 && mX >= orderFrameX && mX <= orderFrameX + 4) {
				newSelectedBar = i;
			}

			String name = getBarName(i);
			BoxDrawer.drawBox(orderFrameX, y, 4, 2, false, color);
			Drawer.drawString(orderFrameX + 1, y + 1, false, name, i == selectedBar ? COLOR : color);
		}
		Drawer.disableClipping();
	}

	private void renderPattern(int mX, int mY, int color) {
		int position = composition.isPlaying() ? composition.getPosition() : selectedNote;

		// STOP/PLAY
		BoxDrawer.drawBox(patternFrameX, 1, 3, 2, false, color);
		Drawer.drawString(patternFrameX + 1, 2, false,
				StringUtils.repeat(composition.isPlaying() ? Glyphs.BLACK_RIGHT_POINTER : Glyphs.MEDIUM_SHADE, 2),
				color);

		// POSITIONS
		Drawer.enableClipping(patternFrameX, 4, Window.getWidth(), Window.getHeight());
		for (int j = 0; j < Composition.PATTERN_LENGTH; j++) {
			int y = j * 2 + 4 - patternScrollY;
			BoxDrawer.drawBox(patternFrameX, y, 3, 2, false, color);
			Drawer.drawString(patternFrameX + 1, y + 1, false, getPositionName(j), position == j ? COLOR : color);
		}

		// NOTES
		for (int i = 0; i < Composition.MAX_TRACKS; i++) {
			int x = i * 9 + patternFrameX + 4 - patternScrollX;
			Track track = composition.getTrack(i);
			Pattern pattern = track.getPattern(selectedBar);

			Drawer.enableClipping(patternFrameX + 4, 0, Window.getWidth(), Window.getHeight());
			BoxDrawer.drawBox(x, 1, 8, 2, false, color);
			Drawer.drawString(x + 1, 2, false, getTrackPatternName(i, pattern), i == selectedTrack ? COLOR : color);

			if (mY == 2 && mX >= x && mX <= x + 8)
				newSelectedTrack = i;

			Drawer.enableClipping(patternFrameX + 4, 4, Window.getWidth(), Window.getHeight());
			for (int j = 0; j < Composition.PATTERN_LENGTH; j++) {
				int y = j * 2 + 4 - patternScrollY;
				BoxDrawer.drawBox(x, y, 8, 2, true, color);

				if (mY == y + 1 && mX >= x && mX <= x + 8) {
					newSelectedTrack = i;
					newSelectedNote = j;
				}

				int currentColor = (i == selectedTrack && j == selectedNote && Engine.getElapsedTime() % 0.6f < 0.3f)
						? COLOR
						: color;

				String name = getNoteSampleName(pattern, j);
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

	private static String getNoteName(int note) {
		return String.format("%s%01X", NOTES[note % 12], note / 12);
	}

	private static String getNoteSampleName(Pattern pattern, int position) {
		if (pattern == null)
			return "       ";
		int note = pattern.getNote(position);
		if (note == Composition.INVALID)
			return "       ";
		else if (note == Composition.BREAK)
			return "^^^^^^^";
		int sample = pattern.getSample(position);
		return getNoteName(note) + ":" + getSampleName(sample);
	}

	private static String getBarName(int bar) {
		return String.format("%03d", bar + 1);
	}

	private static String getPositionName(int position) {
		return String.format("%02d", position + 1);
	}

	private static String getSampleName(int sample) {
		return String.format("%03d", sample);
	}

	private static String getTrackPatternName(int track, Pattern pattern) {
		return String.format("T%02d:%s", track + 1, getPatternName(pattern));
	}

	private static String getPatternName(Pattern pattern) {
		if (pattern == null)
			return "Pxx";
		else
			return String.format("P%02X", pattern.getIndex());
	}

	@Override
	public void onClose() {
		MidiReceiver.closeDevices();
	}

	public static void main(String[] args) throws Exception {
		Engine.init("Ascii Tracker", 8, 60);
		MidiReceiver.init();
		Window.createWindowed(120, 80);
		Engine.start(new AsciiTracker());
	}

	private static class MessageFrame extends GUIFrame {

		private final GUIText text;

		public MessageFrame() {
			super(COLOR, false, false);
			text = new GUIText(this, -1, "");
			new GUIButton(this, -1, "OK", f -> text.setText(""));
		}

		@Override
		public void update() {
			if (Input.isKeyDown(Input.KEY_ESCAPE) || Input.isKeyDown(Input.KEY_ENTER))
				text.setText("");
			super.update();
		}

		public void spawn(String message) {
			text.setText(message);
		}

		public boolean isActive() {
			return !text.getText().isEmpty();
		}

	}

	private static class HelpFrame extends GUIFrame {

		public HelpFrame() {
			super(Colors.swap(COLOR), false, false);
			setTitle("Help");
			add("Menu tab", Input.KEY_CONTROL_LEFT, Input.KEY_1);
			add("Info tab", Input.KEY_CONTROL_LEFT, Input.KEY_2);
			add("Tracks tab", Input.KEY_CONTROL_LEFT, Input.KEY_3);
			add("Samples tab", Input.KEY_CONTROL_LEFT, Input.KEY_4);
			add("Library tab", Input.KEY_CONTROL_LEFT, Input.KEY_5);
			new GUISeparator(this, -1, true);
			add("Quit", Input.KEY_CONTROL_LEFT, Input.KEY_W);
			add("Help", Input.KEY_CONTROL_LEFT, Input.KEY_H);
			add("Window/fullscreen", Input.KEY_F11);
			new GUISeparator(this, -1, true);
			add("New composition", Input.KEY_CONTROL_LEFT, Input.KEY_N);
			add("Open composition", Input.KEY_CONTROL_LEFT, Input.KEY_O);
			add("Save composition", Input.KEY_CONTROL_LEFT, Input.KEY_S);
			add("Save as", Input.KEY_CONTROL_LEFT, Input.KEY_SHIFT_LEFT, Input.KEY_S);
			add("Export samples", Input.KEY_CONTROL_LEFT, Input.KEY_E);
			new GUISeparator(this, -1, true);
			add("Play/stop", Input.KEY_SPACE);
			add("Reset to start", Input.KEY_CONTROL_LEFT, Input.KEY_SPACE);
			new GUISeparator(this, -1, true);
			add("Keyboard on/off", Input.KEY_M);
			add("Keyboard octave up", Input.KEY_Z);
			add("Keyboard octave down", Input.KEY_X);
			new GUISeparator(this, -1, true);
			add("Previous bar", Input.KEY_CONTROL_LEFT, Input.KEY_UP);
			add("Next bar", Input.KEY_CONTROL_LEFT, Input.KEY_DOWN);
			add("Previous pattern", Input.KEY_CONTROL_LEFT, Input.KEY_LEFT);
			add("Next pattern", Input.KEY_CONTROL_LEFT, Input.KEY_RIGHT);
			add("Copy pattern", Input.KEY_CONTROL_LEFT, Input.KEY_C);
			add("Paste pattern", Input.KEY_CONTROL_LEFT, Input.KEY_V);
			add("Clear pattern", Input.KEY_CONTROL_LEFT, Input.KEY_BACKSPACE);
			new GUISeparator(this, -1, true);
			add("Insert break", Input.KEY_B);
			add("Clear note", Input.KEY_BACKSPACE);
			add("Note up", Input.KEY_SHIFT_LEFT, Input.KEY_UP);
			add("Note down", Input.KEY_SHIFT_LEFT, Input.KEY_DOWN);
			add("Octave up", Input.KEY_SHIFT_LEFT, Input.KEY_Z);
			add("Octave down", Input.KEY_SHIFT_LEFT, Input.KEY_X);
			add("Previous sample", Input.KEY_SHIFT_LEFT, Input.KEY_LEFT);
			add("Next sample", Input.KEY_SHIFT_LEFT, Input.KEY_RIGHT);
			new GUISeparator(this, -1, true);
			add("Previous note", Input.KEY_UP);
			add("Next note", Input.KEY_DOWN);
			add("Previous track", Input.KEY_LEFT);
			add("Next track", Input.KEY_RIGHT);
		}

		public HelpFrame add(String name, int... keys) {
			new GUISeparator(this, -1, true);
			GUIColorText text = new GUIColorText(this);
			text.addText(name + ": ", -1);
			for (int i = 0; i < keys.length; i++) {
				if (i > 0)
					text.addText(" + ", -1);
				text.addText(" " + Input.nameKeyboard(keys[i]) + " ", Colors.swap(getColor()));
			}
			return this;
		}

	}

	private static class MidiReceiver implements Receiver {

		private static ArrayList<MidiReceiver> midiReceivers = new ArrayList<>();

		private final MidiDevice device;

		public MidiReceiver(MidiDevice device) throws MidiUnavailableException {
			this.device = device;
			List<Transmitter> transmitters = device.getTransmitters();
			for (int j = 0; j < transmitters.size(); j++)
				transmitters.get(j).setReceiver(this);
			Transmitter transmitter = device.getTransmitter();
			transmitter.setReceiver(this);
			device.open();
			Printer.println("Opened MIDI device: " + device.getDeviceInfo());
		}

		public static void init() {
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			for (int i = 0; i < infos.length; i++)
				try {
					MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
					midiReceivers.add(new MidiReceiver(device));
				} catch (MidiUnavailableException e) {
				}
		}

		public static void closeDevices() {
			for (int i = 0; i < midiReceivers.size(); i++)
				midiReceivers.get(i).close();
		}

		@Override
		public void send(MidiMessage msg, long timeStamp) {
			byte[] bytes = msg.getMessage();
			if (bytes[0] == -112)
				lastMidi = bytes[1];
		}

		@Override
		public void close() {
			device.close();
		}
	}

}
