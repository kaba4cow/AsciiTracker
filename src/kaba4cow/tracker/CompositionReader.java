package kaba4cow.tracker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class CompositionReader {

	private static final int INFO = 0x00;
	private static final int TRACK = 0x01;
	private static final int ORDER = 0x02;
	private static final int PATTERN = 0x03;
	private static final int SAMPLE = 0x04;

	private static final int INVALID = 0x80;
	private static final int BREAK = 0x81;

	public CompositionReader() {

	}

	public static Composition read(File file) throws Exception {
		FileInputStream input = new FileInputStream(file);
		return read(input);
	}

	public static Composition read(String path) throws Exception {
		InputStream input = CompositionReader.class.getClassLoader().getResourceAsStream(path);
		return read(input);
	}

	public static Composition read(InputStream input) throws Exception {
		Composition composition = new Composition();

		int[] patternOrder = composition.getPatternOrder();
		Pattern[] patternList = composition.getPatternList();

		int b;
		while ((b = input.read()) != -1) {
			if (b == INFO) {
				composition.setName(readString(input));
				composition.setAuthor(readString(input));
				composition.setComment(readString(input));
				composition.setLength(readByte(input));
				composition.setTempo(readByte(input));
			} else if (b == TRACK) {
				int index = readByte(input);
				int sample = readByte(input);
				float volume = (float) readByte(input) / (float) 0xFF;
				String name = readString(input);
				Track track = composition.getTrack(index);
				track.setName(name);
				track.setDefaultSample(sample);
				track.setVolume(volume);
			} else if (b == ORDER) {
				for (int i = 0; i < Music.PATTERNS; i++) {
					int pattern = readByte(input);
					if (pattern == INVALID)
						patternOrder[i] = Music.INVALID_NOTE;
					else
						patternOrder[i] = pattern;
				}
			} else if (b == PATTERN) {
				int index = readByte(input);
				Pattern pattern = patternList[index];
				for (int track = 0; track < Music.TRACKS; track++) {
					int notes = readByte(input);
					for (int i = 0; i < notes; i++) {
						int position = readByte(input);
						int note = readByte(input);
						if (note == BREAK)
							pattern.setNote(track, position, Music.BREAK_NOTE);
						else {
							int sample = readByte(input);
							pattern.setNote(track, position, note);
							pattern.setSample(track, position, sample);
						}
					}
				}
			} else if (b == SAMPLE) {
				String name = readString(input);
				byte[] bytes = readBytes(input);
				Sample sample = new Sample(name, bytes);
				composition.addSample(sample);
			}
		}

		input.close();
		return composition;
	}

	private static String readString(InputStream input) throws IOException {
		StringBuilder builder = new StringBuilder();
		int length = input.read();
		for (int i = 0; i < length; i++)
			builder.append((char) input.read());
		return builder.toString();
	}

	private static int readByte(InputStream input) throws IOException {
		return input.read();
	}

	private static byte[] readBytes(InputStream input) throws IOException {
		int length = 0;
		length |= input.read() << 24;
		length |= input.read() << 16;
		length |= input.read() << 8;
		length |= input.read() << 0;

		byte[] bytes = new byte[length];
		input.read(bytes, 0, length);
		return bytes;
	}

	public static void write(Composition composition, File file) throws IOException {
		FileOutputStream stream = new FileOutputStream(file);
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		write(output, INFO);
		write(output, composition.getName());
		write(output, composition.getAuthor());
		write(output, composition.getComment());
		write(output, composition.getLength());
		write(output, composition.getTempo());

		Track[] tracks = composition.getTracks();
		for (int i = 0; i < tracks.length; i++) {
			Track track = tracks[i];
			write(output, TRACK);
			write(output, i);
			write(output, track.getDefaultSample());
			write(output, (int) (0xFF * track.getVolume()));
			write(output, track.getName());
		}

		int[] patternOrder = composition.getPatternOrder();
		write(output, ORDER);
		for (int i = 0; i < Music.PATTERNS; i++) {
			if (patternOrder[i] == Music.INVALID_NOTE)
				write(output, INVALID);
			else
				write(output, patternOrder[i]);
		}

		Pattern[] patternList = composition.getPatternList();
		for (int i = 0; i < patternList.length; i++) {
			Pattern pattern = patternList[i];
			write(output, PATTERN);
			write(output, pattern.getIndex());
			for (int track = 0; track < Music.TRACKS; track++) {
				int notes = 0;
				for (int position = 0; position < Music.BAR; position++)
					if (pattern.getNote(track, position) != Music.INVALID_NOTE)
						notes++;
				write(output, notes);
				for (int position = 0; position < Music.BAR; position++) {
					int note = pattern.getNote(track, position);
					if (note != Music.INVALID_NOTE) {
						write(output, position);
						if (note == Music.BREAK_NOTE)
							write(output, BREAK);
						else {
							int sample = pattern.getSample(track, position);
							write(output, note);
							write(output, sample);
						}
					}
				}
			}
		}

		ArrayList<Sample> samples = composition.getSamples();
		for (int i = 0; i < samples.size(); i++) {
			Sample sample = samples.get(i);
			write(output, SAMPLE);
			write(output, sample.getName());
			write(output, getBytes(sample.getStream()));
		}

		output.writeTo(stream);
		output.close();
	}

	private static byte[] getBytes(InputStream stream) throws IOException {
		byte[] buffer = new byte[1024];
		int bytesRead;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		while ((bytesRead = stream.read(buffer)) != -1)
			output.write(buffer, 0, bytesRead);
		stream.close();
		return output.toByteArray();
	}

	private static void write(ByteArrayOutputStream output, String string) throws IOException {
		output.write(string.length());
		output.write(string.getBytes());
	}

	private static void write(ByteArrayOutputStream output, int value) {
		output.write(value);
	}

	private static void write(ByteArrayOutputStream output, byte[] bytes) throws IOException {
		output.write((bytes.length >> 24) & 0xFF);
		output.write((bytes.length >> 16) & 0xFF);
		output.write((bytes.length >> 8) & 0xFF);
		output.write((bytes.length >> 0) & 0xFF);
		output.write(bytes);
	}

}
