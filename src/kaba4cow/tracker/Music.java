package kaba4cow.tracker;

import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import kaba4cow.ascii.core.Input;
import kaba4cow.ascii.toolbox.maths.Maths;

public class Music {

	public static final int TRACKS = 16;
	public static final int PATTERNS = 128;
	public static final int SAMPLES = 128;

	public static final int STRING_LENGTH = 256;

	public static final int INVALID_NOTE = -1;
	public static final int BREAK_NOTE = 128;

	public static final int BAR = 32;

	public static final float DELAY = 240f / (float) BAR;

	public static final int TRACK_NAME = 8;
	public static final int SONG_LENGTH = 999;

	public static final String[] NOTES = { "C-", "C#", "D-", "D#", "E-", "F-", "F#", "G-", "G#", "A-", "A#", "B-" };

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

	private static int lastMidi = INVALID_NOTE;
	private static int midiOctave = 1;
	private static boolean midiKeyboard = true;

	private Music() {

	}

	public static void init() {
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
				System.out.println(device.getDeviceInfo());

			} catch (MidiUnavailableException e) {
			}
	}

	public static int readInput() {
		if (Input.isKeyDown(Input.KEY_M))
			midiKeyboard = !midiKeyboard;
		if (lastMidi != INVALID_NOTE) {
			int note = lastMidi;
			lastMidi = INVALID_NOTE;
			return note;
		}
		if (!midiKeyboard)
			return INVALID_NOTE;
		if (Input.isKeyDown(Input.KEY_Z) && NOTE_C + 12 * midiOctave > 12)
			midiOctave--;
		if (Input.isKeyDown(Input.KEY_X) && NOTE_C + 12 * midiOctave < 116)
			midiOctave++;
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
		return INVALID_NOTE;
	}

	public static float getPitch(int note) {
		float pow = (note - NOTE_A) / 12f;
		return Maths.pow(2f, pow);
	}

	public static int getOctave(int note) {
		return note / 12;
	}

	public static String getNoteName(int note) {
		return String.format("%s%01X", NOTES[note % 12], getOctave(note));
	}

	public static String getNoteSampleName(Pattern pattern, int track, int position) {
		if (pattern == null)
			return "        ";
		int note = pattern.getNote(track, position);
		if (note == INVALID_NOTE)
			return "        ";
		else if (note == BREAK_NOTE)
			return "^^^^^^^^";
		int sample = pattern.getSample(track, position);
		return getNoteName(note) + "::" + getSampleName(sample);
	}

	public static String getBarName(int bar) {
		return String.format("%03d", bar + 1);
	}

	public static String getPositionName(int bar, int position) {
		return String.format("%03d-%02d", bar + 1, position);
	}

	public static String getSampleName(int sample) {
		return String.format("%03d", sample);
	}

	public static String getPatternName(Pattern pattern) {
		if (pattern == null)
			return "xx";
		else
			return getPatternName(pattern.getIndex());
	}

	public static String getPatternName(int pattern) {
		return String.format("%02X", pattern);
	}

	public static int getOctave() {
		return midiOctave;
	}

	public static boolean isKeyboardEnabled() {
		return midiKeyboard;
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
