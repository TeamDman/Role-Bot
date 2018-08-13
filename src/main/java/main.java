import sx.blah.discord.api.IDiscordClient;

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
}
