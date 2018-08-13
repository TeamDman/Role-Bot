import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
	private final String     name;
	private final Properties props = new Properties();

	private Config(String name) {
		this.name = name;
		load();
		save();
	}

	private void load() {
		try (FileInputStream in = new FileInputStream(name + ".properties")) {
			props.load(in);
			for (Property p : Property.values()) {
				props.computeIfAbsent(p.name(), k -> p.fallback);
			}
		} catch (FileNotFoundException e) { // Create config from defaults
			for (Property p : Property.values())
				props.put(p.name(), p.fallback);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void save() {
		try (FileOutputStream out = new FileOutputStream(name + ".properties")) {
			props.store(out, "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static Config getConfig() {
		if (main.config == null) {
			return new Config("bot");
		}
		return main.config;
	}

	String get(Property key) {
		return props.get(key.name()).toString(); // Not good, but good enough for now.
	}

	void set(Property key, String value) {
		props.put(key.name(), value);
		save();
	}

	public enum Property {
		DISCORD_TOKEN("undefined"),
		COOLDOWN("10000");
		final Object fallback;

		Property(Object fallback) {
			this.fallback = fallback;
		}
	}
}
