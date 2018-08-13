import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.obj.IRole;

public class main {
	static final IDiscordClient client;
	static final Config         config;

	static {
		config = Config.getConfig();
		client = ClientFactory.getClient();
	}

	public static void main(String[] args) {
		client.getDispatcher().registerListener(MessageHandler.class);
		client.getDispatcher().registerListener(main.class);
		MessageHandler.read();
	}

	@EventSubscriber
	public static void handleJoin(GuildCreateEvent event) {
		for (IRole role : event.getGuild().getRoles())
			if (role.getLongID() == Long.valueOf(config.get(Config.Property.MUTEROLE)))
				MessageHandler.muteRole = role;
	}
}
