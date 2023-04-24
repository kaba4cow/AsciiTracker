package kaba4cow.tracker;

import kaba4cow.ascii.audio.Source;

public class Track {

	private final Composition composition;
	private final int index;

	private int defaultSample;
	private String name;

	private final Source source;

	private float volume;

	public Track(Composition composition, int index, String name, int defaultSample) {
		this.composition = composition;
		this.index = index;
		this.name = name;
		this.defaultSample = defaultSample;
		this.source = new Source("");
		this.volume = 1f;
	}

	public void update(Pattern pattern, int position) {
		play(pattern.getNote(index, position), pattern.getSample(index, position));
	}

	public void play(int note, int sample) {
		if (note == Music.INVALID_NOTE)
			return;
		if (note == Music.BREAK_NOTE) {
			stop();
			return;
		}
		if (sample == 0)
			sample = defaultSample;
		else
			sample--;
		Sample s = composition.getSample(sample);
		if (s != null)
			playNote(note).play(s.getBuffer());
	}

	private Source playNote(int note) {
		stop();
		return source.setGain(volume).setPitch(Music.getPitch(note));
	}

	public void stop() {
		source.stop();
	}

	public int getDefaultSample() {
		return defaultSample;
	}

	public void setDefaultSample(int defaultSample) {
		if (defaultSample < 0 || defaultSample >= Sample.getLibrary().size())
			return;
		this.defaultSample = defaultSample;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
