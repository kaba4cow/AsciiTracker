package kaba4cow.tracker.composition;

public class Pattern {

	private final Composition composition;
	private final int index;

	private final int[][] notes;
	private final int[][] samples;

	public Pattern(Composition composition, int index) {
		this(composition, index, new Data());
	}

	public Pattern(Composition composition, int index, Data data) {
		this.composition = composition;
		this.index = index;
		this.notes = data.notes;
		this.samples = data.samples;
	}

	public Data getData() {
		return new Data(notes, samples);
	}

	public void setData(Data data) {
		for (int i = 0; i < Composition.MAX_TRACKS; i++)
			for (int j = 0; j < Composition.PATTERN_LENGTH; j++) {
				this.notes[i][j] = data.notes[i][j];
				this.samples[i][j] = data.samples[i][j];
			}
	}

	public void clear() {
		for (int i = 0; i < Composition.MAX_TRACKS; i++)
			for (int j = 0; j < Composition.PATTERN_LENGTH; j++) {
				this.notes[i][j] = Composition.INVALID;
				this.samples[i][j] = 0;
			}
	}

	public int[][] getNotes() {
		return notes;
	}

	public int[][] getSamples() {
		return samples;
	}

	public void deleteNote(int track, int position) {
		notes[track][position] = Composition.INVALID;
		samples[track][position] = 0;
	}

	public void setBreak(int track, int position) {
		notes[track][position] = Composition.BREAK;
	}

	public void setNote(int track, int position, int note) {
		notes[track][position] = note;
	}

	public void changeNote(int track, int position, int delta) {
		if (notes[track][position] == Composition.INVALID || notes[track][position] == Composition.BREAK) {
			notes[track][position] = Music.NOTE_C;
			return;
		}
		if (notes[track][position] + delta >= 0 && notes[track][position] + delta < 128)
			notes[track][position] += delta;
	}

	public void setSample(int track, int position, int sample) {
		if (notes[track][position] == Composition.INVALID)
			notes[track][position] = Music.NOTE_C;
		samples[track][position] = sample;
	}

	public void prevSample(int track, int position) {
		if (notes[track][position] == Composition.INVALID)
			notes[track][position] = Music.NOTE_C;
		if (samples[track][position] - 1 >= 0)
			samples[track][position]--;
		else
			samples[track][position] = composition.getSamples().size();
	}

	public void nextSample(int track, int position) {
		if (notes[track][position] == Composition.INVALID)
			notes[track][position] = Music.NOTE_C;
		if (samples[track][position] + 1 <= composition.getSamples().size())
			samples[track][position]++;
		else
			samples[track][position] = 0;
	}

	public boolean contains(int track, int position) {
		return notes[track][position] != Composition.INVALID;
	}

	public int getNote(int track, int position) {
		return notes[track][position];
	}

	public int getSample(int track, int position) {
		return samples[track][position];
	}

	public int getIndex() {
		return index;
	}

	public static class Data {

		private final int[][] notes;
		private final int[][] samples;

		public Data(int[][] notes, int[][] samples) {
			this.notes = new int[Composition.MAX_TRACKS][Composition.PATTERN_LENGTH];
			this.samples = new int[Composition.MAX_TRACKS][Composition.PATTERN_LENGTH];
			for (int i = 0; i < Composition.MAX_TRACKS; i++)
				for (int j = 0; j < Composition.PATTERN_LENGTH; j++) {
					this.notes[i][j] = notes[i][j];
					this.samples[i][j] = samples[i][j];
				}
		}

		public Data() {
			this.notes = new int[Composition.MAX_TRACKS][Composition.PATTERN_LENGTH];
			this.samples = new int[Composition.MAX_TRACKS][Composition.PATTERN_LENGTH];
			for (int i = 0; i < Composition.MAX_TRACKS; i++)
				for (int j = 0; j < Composition.PATTERN_LENGTH; j++) {
					this.notes[i][j] = Composition.INVALID;
					this.samples[i][j] = 0;
				}
		}

	}

}
