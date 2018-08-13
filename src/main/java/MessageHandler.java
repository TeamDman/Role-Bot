import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageHandler {
	private static final EnumSet<Permissions> ADMIN      = EnumSet.of(Permissions.ADMINISTRATOR);
	private static final EnumSet<Permissions> USER       = EnumSet.noneOf(Permissions.class);
	private static final Set<Long>            blacklist  = Sets.newHashSet();
	private static final List<Claim>          claims     = Lists.newArrayList();
	private static final Pattern              cmdPattern = Pattern.compile("^/claims (\\w+)\\s?(.*)");

	static void read() {
		blacklist.clear();
		claims.clear();
		try {
			Scanner in = new Scanner(new File("claims.txt"));
			while (in.hasNextLine()) {
				claims.add(new Claim(in.nextLong(), in.nextLong(), in.nextLong()));
				in.nextLine();
			}
			in = new Scanner(new File("blacklist.txt"));
			while (in.hasNextLine()) {
				blacklist.add(Long.valueOf(in.nextLine()));
			}
		} catch (IOException e) {
			write();
		}
	}

	private static void write() {
		try (FileWriter writer = new FileWriter(new File("claims.txt"))) {
			writer.flush();
			for (Claim claim : claims) {
				writer.write(claim.toString());
				writer.write('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (FileWriter writer = new FileWriter(new File("blacklist.txt"))) {
			writer.flush();
			for (long role : blacklist) {
				writer.write(Long.toString(role));
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
					if (!event.getAuthor().getPermissionsForGuild(event.getGuild()).containsAll(c.perms))
						RequestBuffer.request(() -> event.getChannel().sendMessage("You do not have permission for that."));
					else
						try {
							c.action.accept(event, m.group((2)));
						} catch (ArrayIndexOutOfBoundsException e) {
							RequestBuffer.request(() -> event.getChannel().sendMessage("Incorrect arguments provided."));
						}
			}
		}
	}

	private static IUser getSingleUser(IChannel channel, String arg) {
		Pattern p    = Pattern.compile("(\\d+)");
		Matcher m    = p.matcher(arg);
		IUser   user = null;
		if (!m.find() || (user = channel.getGuild().getUserByID(Long.valueOf(m.group(1)))) == null)
			RequestBuffer.request(() -> channel.sendMessage("No users matched the provided selector."));
		return user;
	}

	private static IRole getSingleRole(IChannel channel, String arg) {
		Pattern p    = Pattern.compile("(\\d+)");
		Matcher m    = p.matcher(arg);
		IRole   role = null;
		if (!m.find() || (role = channel.getGuild().getRoleByID(Long.valueOf(m.group(1)))) == null)
			RequestBuffer.request(() -> channel.sendMessage("No roles matched the provided selector."));
		return role;
	}

	private static Claim getClaim(IUser user, IRole role) {
		return claims.stream().filter(c -> c.user == user.getLongID() && c.role == role.getLongID()).findFirst().orElse(null);
	}

	private static boolean checkAndSetUpdate(IChannel channel, Claim claim) {
		if (claim == null) {
			RequestBuffer.request(() -> channel.sendMessage("You do not have a claim for that role."));
			return false;
		} else {
			long delta = new Date().getTime() - (claim.time + Long.valueOf(main.config.get(Config.Property.COOLDOWN)));
			if (delta < 0) {
				RequestBuffer.request(() -> channel.sendMessage("You do can't do that for another " + -delta / 1000 + " seconds."));
				return false;
			}
		}
		claim.time = new Date().getTime();
		write();
		return true;
	}

	enum Command {
		HELP(USER, "", (event, args) -> {
			EmbedBuilder embed = new EmbedBuilder().withTitle("Filter Commands");
			for (Command c : Command.values())
				embed.appendDesc("/roles " + c.name().toLowerCase() + " " + c.usage + "\n");
			RequestBuffer.request(() -> event.getChannel().sendMessage(embed.build()));
		}),
		LISTROLES(USER, "", (event, args) -> {
			new PaginatorListener(event.getChannel(), event.getAuthor(), Lists.partition(event.getGuild().getRoles(), 10).stream()
					.map(list -> new EmbedBuilder()
							.withTitle("Role Information")
							.withColor(Color.orange)
							.withAuthorIcon(event.getAuthor().getAvatarURL())
							.withAuthorName(event.getAuthor().getName())
							.appendDesc(list.stream().map(r -> "<@&" + r.getStringID() + ">\t" + r.getStringID()).collect(Collectors.joining("\n")))
					).collect(Collectors.toList()));
		}),
		LISTBLACKLIST(USER, "", (event, args) -> {
			new PaginatorListener(event.getChannel(), event.getAuthor(), Lists.partition(event.getGuild().getRoles().stream()
					.filter(r -> blacklist.contains(r.getLongID()))
					.collect(Collectors.toList()), 10).stream()
					.map(list -> new EmbedBuilder()
							.withTitle("Blacklist Information")
							.withColor(Color.BLACK)
							.withAuthorIcon(event.getAuthor().getAvatarURL())
							.withAuthorName(event.getAuthor().getName())
							.appendDesc(list.stream().map(r -> "<@&" + r.getStringID() + ">\t" + r.getStringID()).collect(Collectors.joining("\n")))
					).collect(Collectors.toList()));
		}),
		BLACKLISTADD(ADMIN, "roleid", (event, args) -> {
			IRole role = getSingleRole(event.getChannel(), args);
			if (role != null) {
				blacklist.add(role.getLongID());
				write();
				RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
						.withTitle("Blacklist Update")
						.withColor(Color.BLACK)
						.appendDesc("Added " + role.mention() + " to the blacklist.").build()));
			}
		}),
		BLACKLISTREMOVE(ADMIN, "roleid", (event, args) -> {
			IRole role;
			if ((role = getSingleRole(event.getChannel(), args)) != null)
				if (blacklist.remove(role.getLongID())) {
					write();
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Blacklist Update")
							.withColor(Color.BLACK)
							.appendDesc("Added " + role.mention() + " to the blacklist.").build()));
				} else
					RequestBuffer.request(() -> event.getChannel().sendMessage("That role is not in the blacklist."));
		}),
		CLAIM(USER, "roleid", (event, args) -> {
			IRole role = getSingleRole(event.getChannel(), args);
			if (role != null) {
				if (blacklist.contains(role.getLongID()))
					RequestBuffer.request(() -> event.getChannel().sendMessage("You may not claim that role."));
				else if (!event.getAuthor().getRolesForGuild(event.getGuild()).contains(role))
					RequestBuffer.request(() -> event.getChannel().sendMessage("You may only claim roles you have."));
				else if (claims.stream().anyMatch(c -> c.role == role.getLongID() && c.user == event.getAuthor().getLongID()))
					RequestBuffer.request(() -> event.getChannel().sendMessage("You have already claimed that role."));
				else {
					claims.add(new Claim(event.getAuthor().getLongID(), role.getLongID(), new Date().getTime()));
					write();
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(Color.CYAN)
							.appendDesc("You have claimed " + role.mention()).build()));
				}
			}
		}),
		REMOVECLAIMS(ADMIN, "roleid", (event, args) -> {
			IRole role = getSingleRole(event.getChannel(), args);
			if (role != null) {
				if (claims.removeIf(c -> c.role == role.getLongID())) {
					write();
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(Color.ORANGE)
							.appendDesc("You have removed all claims for " + role.mention()).build()));
				} else
					RequestBuffer.request(() -> event.getChannel().sendMessage("No claims for that role were found."));
			}
		}),
		LISTCLAIMS(USER, "", (event, args) -> {
			if (claims.size() == 0)
				RequestBuffer.request(() -> event.getChannel().sendMessage("There are no claims currently."));
			else
				new PaginatorListener(event.getChannel(), event.getAuthor(), Lists.partition(claims, 10).stream()
						.map(list -> new EmbedBuilder()
								.withTitle("Claims Information")
								.withColor(Color.CYAN)
								.appendDesc(list.stream().map(c -> "<@" + c.user + ">\t<@&" + c.role + ">\t" + new Date(c.time).toString()).collect(Collectors.joining("\n")))
						).collect(Collectors.toList()));
		}),
		FORCECLAIM(ADMIN, "userid roleid", (event, args) -> {
			String[] a    = args.split("\\s+");
			IUser    user = getSingleUser(event.getChannel(), a[0]);
			IRole    role = getSingleRole(event.getChannel(), a[1]);
			if (user != null && role == null) {
				claims.add(new Claim(event.getAuthor().getLongID(), role.getLongID(), new Date().getTime()));
				write();
				RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
						.withTitle("Claims Update")
						.withColor(Color.CYAN)
						.appendDesc("You have given " + user.mention() + " a claim to " + role.mention()).build()));
			}
		}),
		RENAMECLAIM(USER, "roleid name", (event, args) -> {
			String[] a     = args.split("\\s+", 2);
			IRole    role  = getSingleRole(event.getChannel(), a[0]);
			Claim    claim = getClaim(event.getAuthor(), role);
			if (role != null)
				if (checkAndSetUpdate(event.getChannel(), claim)) {
					role.changeName(a[1]);
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(role.getColor())
							.appendDesc("You have renamed " + role.mention()).build()));
				}
		}),
		RECOLOURCLAIM(USER, "roleid r g b", (event, args) -> {
			String[] a     = args.split("\\s+", 4);
			IRole    role  = getSingleRole(event.getChannel(), a[0]);
			Claim    claim = getClaim(event.getAuthor(), role);
			if (role != null)
				if (checkAndSetUpdate(event.getChannel(), claim)) {
					role.changeColor(new Color(Integer.valueOf(a[1]), Integer.valueOf(a[2]), Integer.valueOf(a[3])));
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(role.getColor())
							.appendDesc("You have recoloured " + role.mention()).build()));
				}
		}),
		RESETCOOLDOWN(ADMIN, "userid", (event, args) -> {
			IUser user = getSingleUser(event.getChannel(), args);
			if (user != null) {
				claims.stream()
						.filter(c -> c.user == user.getLongID())
						.forEach(c -> c.time = 0);
				write();
				RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
						.withTitle("Claims Update")
						.withColor(Color.ORANGE)
						.appendDesc("You have reset " + user.mention() + "'s COOLDOWN.").build()));
			}
		}),
		SETCOOLDOWN(ADMIN, "seconds", (event, args) -> {
			main.config.set(Config.Property.COOLDOWN, Long.toString(Long.valueOf(args) * 1000));

			RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
					.withTitle("Claims Update")
					.withColor(Color.ORANGE)
					.appendDesc("The cooldown is now " + Long.valueOf(main.config.get(Config.Property.COOLDOWN)) / 1000 + " seconds.").build()));
		});

		public final BiConsumer<MessageReceivedEvent, String> action;
		public final EnumSet<Permissions>                     perms;
		public final String                                   usage;

		Command(EnumSet<Permissions> perms, String usage, BiConsumer<MessageReceivedEvent, String> action) {
			this.perms = perms;
			this.usage = usage;
			this.action = action;
		}
	}

	private static class Claim {
		long user, role, time;

		public Claim(long user, long role, long time) {
			this.user = user;
			this.role = role;
			this.time = time;
		}

		@Override
		public String toString() {
			return user + " " + role + " " + time;
		}
	}
}
