package kaba4cow.tracker.composition;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;

import kaba4cow.ascii.audio.AudioManager;

public class Sample {

	private static final ArrayList<Sample> library = new ArrayList<>();

	private final File file;
	private final byte[] bytes;

	private final String name;
	private final int buffer;

	private Sample(String name, File file) throws Exception {
		this.name = name;
		this.file = file;
		this.bytes = null;
		this.buffer = AudioManager.load(file);
		library.add(this);
	}

	public Sample(String name, byte[] bytes) throws Exception {
		this.name = name;
		this.file = null;
		this.bytes = bytes;
		this.buffer = AudioManager.load(new ByteArrayInputStream(bytes));
	}

	public static void loadLibrary(String location) {
		load(new File(location), new Stack<>());
	}

	private static void load(File dir, Stack<String> stack) {
		File[] list = dir.listFiles();
		for (File file : list) {
			if (file.isDirectory()) {
				stack.push(file.getName());
				load(file, stack);
				stack.pop();
			} else if (file.getName().endsWith(".wav")) {
				StringBuilder path = new StringBuilder();
				for (int i = 0; i < stack.size(); i++)
					path.append(stack.get(i)).append('/');
				path.append(file.getName());
				try {
					new Sample(path.toString(), file);
				} catch (Exception e) {
				}
			}
		}
	}

	public void export(File directory) throws IOException {
		File file = new File(directory.getAbsolutePath() + "/" + name);
		if (!file.exists())
			file.getParentFile().mkdirs();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		InputStream input = getStream();
		CompositionReader.writeSample(output, input, file);
	}

	public static Sample get(int index) {
		return library.get(index);
	}

	public static ArrayList<Sample> getLibrary() {
		return library;
	}

	public InputStream getStream() throws FileNotFoundException {
		if (file == null)
			return new ByteArrayInputStream(bytes);
		else
			return new BufferedInputStream(new FileInputStream(file));
	}

	public String getName() {
		return name;
	}

	public int getBuffer() {
		return buffer;
	}

}
