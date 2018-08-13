
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

public class ClientFactory {
	private static IDiscordClient createClient(String token, boolean login) {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(token);
		try {
			if (login) {
				return clientBuilder.login();
			} else {
				return clientBuilder.build();
			}
		} catch (DiscordException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static IDiscordClient getClient() {
		if (main.client == null) {
			String token = main.config.get(Config.Property.DISCORD_TOKEN);
			if (token == null || token.isEmpty() || token.equals("undefined")){
				System.out.println();
				System.out.println("MISSING DISCORD TOKEN");
				System.out.println("EDIT PROPERTIES AND RELAUNCH");
				System.out.println();
				System.exit(-1);
			}
			return createClient(token,true);
		}
		return main.client;
	}
}
