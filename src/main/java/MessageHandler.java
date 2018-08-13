import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuilder;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageHandler {
	static final Pattern       cmdPattern = Pattern.compile("^/filter (\\w+)\\s?(.*)");
	static final List<Pattern> patterns   = new ArrayList<>();
	static       IRole         muteRole   = null;
	static       boolean       useRole    = main.config.get(Config.Property.MUTETYPE).toLowerCase().equals("role");

	static void read() {
		patterns.clear();
		try {
			Scanner in = new Scanner(new File("patterns.txt"));
			while (in.hasNextLine())
				patterns.add(Pattern.compile(in.nextLine()));
		} catch (IOException e) {
			System.out.println("There was an exception loading the patterns.txt file. If this is the first time run, then this can be ignored.");
			e.printStackTrace();
			write();
		}
	}

	static void write() {
		try (FileWriter writer = new FileWriter(new File("patterns.txt"))) {
			writer.flush();
			for (Pattern pattern : patterns) {
				writer.write(pattern.pattern());
				writer.write('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@EventSubscriber
	public static void handle(MessageReceivedEvent event) {
		Matcher m = cmdPattern.matcher(event.getMessage().getContent());
		if (m.find()) {
			for (Command c : Command.values()) {
				if (c.name().toLowerCase().equals(m.group(1)))
					if (!event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.ADMINISTRATOR))
						RequestBuffer.request(() -> event.getChannel().sendMessage("You do not have permission for that."));
					else
						c.action.accept(event, m.group((2)));
			}
		}
	}

	private static IUser getSingleUser(IChannel channel, String arg) {
		Pattern p    = Pattern.compile("<@!?(\\d+)>");
		Matcher m    = p.matcher(arg);
		IUser   user = null;
		if (!m.find() || (user = channel.getGuild().getUserByID(Long.valueOf(m.group(1)))) == null)
			RequestBuffer.request(() -> channel.sendMessage("No users matched the provided selector."));
		return user;
	}

	enum Command {
		HELP((event, args) -> {
			EmbedBuilder embed = new EmbedBuilder().withTitle("Filter Commands");
			for (Command c : Command.values())
				embed.appendDesc("/" + c.name().toLowerCase() + " \n");
			RequestBuffer.request(() -> event.getChannel().sendMessage(embed.build()));
		})
		,

		public BiConsumer<MessageReceivedEvent, String> action;

		Command(BiConsumer<MessageReceivedEvent, String> action) {
			this.action = action;
		}
	}
}
