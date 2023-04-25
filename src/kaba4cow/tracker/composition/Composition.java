package kaba4cow.tracker.composition;

import java.util.ArrayList;

public class Composition {

	private String author;
	private String name;
	private String comment;

	private int length;
	private int tempo;

	private Track[] tracks;

	private ArrayList<Sample> samples;

	private final int[] patternOrder;
	private final Pattern[] patternList;

	private boolean playing;
	private int bar;
	private int position;
	private float duration;

	private float volume;

	public Composition() {
		this.author = "";
		this.name = "";
		this.comment = "";
		this.length = 8;
		this.tempo = 100;
		this.volume = 1f;
		this.tracks = new Track[Music.TRACKS];
		this.samples = new ArrayList<>();
		this.patternOrder = new int[Music.SONG_LENGTH];
		for (int i = 0; i < patternOrder.length; i++)
			patternOrder[i] = Music.INVALID_NOTE;
		this.patternList = new Pattern[Music.PATTERNS];
		for (int i = 0; i < patternList.length; i++)
			patternList[i] = new Pattern(this, i);

		for (int i = 0; i < Music.TRACKS; i++)
			tracks[i] = new Track(this, i, "Track " + String.format("%02d", i + 1), 0);

		this.playing = false;
		this.bar = 0;
		this.position = 0;
		this.duration = 0f;
	}

	public void update(float dt) {
		if (!playing)
			return;
		duration += dt;
		if (duration >= Music.DELAY / tempo) {
			position++;
			if (position >= Music.BAR) {
				position = 0;
				bar++;
			}
			if (bar >= length)
				bar = 0;
			play();
		}
	}

	public void play() {
		playing = true;
		duration = 0f;
		if (patternOrder[bar] == Music.INVALID_NOTE)
			return;
		Pattern pattern = patternList[patternOrder[bar]];
		if (pattern != null)
			for (int i = 0; i < Music.TRACKS; i++)
				tracks[i].update(pattern, position);
	}

	public void stop() {
		for (int i = 0; i < Music.TRACKS; i++)
			tracks[i].stop();
		playing = false;
		position = 0;
		duration = 0f;
	}

	public boolean isPlaying() {
		return playing;
	}

	public int[] getPatternOrder() {
		return patternOrder;
	}

	public Pattern[] getPatternList() {
		return patternList;
	}

	public ArrayList<Sample> getSamples() {
		return samples;
	}

	public Track[] getTracks() {
		return tracks;
	}

	public void setPattern(int bar, Pattern pattern) {
		patternOrder[bar] = pattern.getIndex();
	}

	public void removePattern(int bar) {
		patternOrder[bar] = Music.INVALID_NOTE;
	}

	public Pattern getPattern(int bar) {
		if (patternOrder[bar] == Music.INVALID_NOTE)
			return null;
		return patternList[patternOrder[bar]];
	}

	public void prevPattern(int bar) {
		if (patternOrder[bar] == Music.INVALID_NOTE)
			patternOrder[bar] = patternList.length - 1;
		else if (patternOrder[bar] - 1 >= 0)
			patternOrder[bar]--;
		else
			patternOrder[bar] = Music.INVALID_NOTE;
	}

	public void nextPattern(int bar) {
		if (patternOrder[bar] == Music.INVALID_NOTE)
			patternOrder[bar] = 0;
		else if (patternOrder[bar] + 1 < patternList.length)
			patternOrder[bar]++;
		else
			patternOrder[bar] = Music.INVALID_NOTE;
	}

	public void prevBar() {
		if (bar == 0)
			bar = length - 1;
		else
			bar--;
	}

	public void nextBar() {
		if (bar == length - 1)
			bar = 0;
		else
			bar++;
	}

	public Sample getSample(int sample) {
		if (sample < 0 || sample >= samples.size())
			return null;
		return samples.get(sample);
	}

	public boolean addSample(Sample sample) {
		if (samples.size() >= Music.SAMPLES || samples.contains(sample))
			return false;
		samples.add(sample);
		return true;
	}

	public boolean removeSample(int sample) {
		if (sample < 0 || sample >= samples.size())
			return false;
		samples.remove(sample);
		return true;
	}

	public Track getTrack(int track) {
		return tracks[track];
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getTotalLength() {
		return Music.BAR * length;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		if (length < 1)
			length = 1;
		this.length = length;
	}

	public int getTempo() {
		return tempo;
	}

	public void setTempo(int tempo) {
		if (tempo < 10)
			tempo = 10;
		this.tempo = tempo;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

	public int getBar() {
		return bar;
	}

	public void setBar(int bar) {
		if (bar < 0)
			bar = 0;
		else if (bar >= length)
			bar = length - 1;
		this.bar = bar;
		this.position = 0;
	}

	public int getPosition() {
		return position;
	}

}
