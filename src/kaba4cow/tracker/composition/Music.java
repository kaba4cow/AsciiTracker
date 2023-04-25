package kaba4cow.tracker.composition;

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

	private Music() {

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

}
