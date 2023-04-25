package kaba4cow.tracker.composition;

public class Music {

	private static final String[] NOTES = { "C-", "C#", "D-", "D#", "E-", "F-", "F#", "G-", "G#", "A-", "A#", "B-" };

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

	public static int getOctave(int note) {
		return note / 12;
	}

	public static String getNote(int note) {
		return NOTES[note % 12];
	}

}
