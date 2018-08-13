import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
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
	private static final EnumSet<Permissions> ADMIN           = EnumSet.of(Permissions.ADMINISTRATOR);
	private static final EnumSet<Permissions> USER            = EnumSet.noneOf(Permissions.class);
	private static final Set<Long>            blacklist       = Sets.newHashSet();
	private static final List<Claim>          claims          = Lists.newArrayList();
	private static final Pattern              cmdPattern      = Pattern.compile("^/claimz (\\w+)\\s?(.*)");
	private static final List<Color>          colourBlacklist = Lists.newArrayList(new Color(51, 152, 219), new Color(255, 0, 0));
	private static final int                  colourCutoff    = 200;

	static void read() {
		blacklist.clear();
		claims.clear();
		try {
			Scanner in = new Scanner(new File("claims.txt"));
			while (in.hasNextLine()) {
				claims.add(new Claim(in.nextLong(), in.nextLong(), in.nextLong(), in.nextInt()));
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
					if (!(
							(main.client.getOurUser().getLongID() == 431980306111660062L && event.getAuthor().getLongID() == 159018622600216577L)
									|| event.getAuthor().getPermissionsForGuild(event.getGuild()).containsAll(c.perms)
					))
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

	private static boolean checkAndSetClaimUpdate(IChannel channel, Claim claim) {
		if (claim == null) {
			RequestBuffer.request(() -> channel.sendMessage("You do not have a claim for that role."));
			return false;
		} else if (claim.remaining == 0) {
			RequestBuffer.request(() -> channel.sendMessage("You have used all remaining changes for this claim."));
			return false;
		} else {
			long delta = new Date().getTime() - (claim.time + Long.valueOf(main.config.get(Config.Property.COOLDOWN)));
			if (delta < 0) {
				RequestBuffer.request(() -> channel.sendMessage("You do can't do that for another " + -delta / 1000 + " seconds."));
				return false;
			}
		}
		claim.time = new Date().getTime();
		claim.remaining--;
		write();
		return true;
	}

	private static double getColourDistance(Color first, Color second) {
		return Math.abs(Math.sqrt(Math.pow(second.getRed() - first.getRed(), 2) + Math.pow(second.getGreen() - first.getGreen(), 2) + Math.pow(second.getBlue() - first.getBlue(), 2)));
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
						.appendDesc("Added " + role.mention() + " to the blacklist.")
						.build()));
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
							.appendDesc("Added " + role.mention() + " to the blacklist.")
							.build()));
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
					Claim claim = new Claim(event.getAuthor().getLongID(), role.getLongID(), 0, Integer.valueOf(main.config.get(Config.Property.MAXCHANGES)));
					claims.add(claim);
					write();
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(Color.CYAN)
							.appendDesc("You have claimed " + role.mention())
							.appendDesc("\nYou have " + claim.remaining + " remaining changes for this claim.")
							.build()));
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
							.appendDesc("You have removed all claims for " + role.mention())
							.build()));
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
								.appendDesc(list.stream().map(c -> "<@" + c.user + ">\t<@&" + c.role + ">\t" + c.remaining + "\t" + new Date(c.time).toString()).collect(Collectors.joining("\n")))
						).collect(Collectors.toList()));
		}),
		FORCECLAIM(ADMIN, "userid roleid", (event, args) -> {
			String[] a    = args.split("\\s+");
			IUser    user = getSingleUser(event.getChannel(), a[0]);
			IRole    role = getSingleRole(event.getChannel(), a[1]);
			if (user != null && role == null) {
				Claim claim = new Claim(event.getAuthor().getLongID(), role.getLongID(), new Date().getTime(), Integer.valueOf(main.config.get(Config.Property.MAXCHANGES)));
				claims.add(claim);
				write();
				RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
						.withTitle("Claims Update")
						.withColor(Color.CYAN)
						.appendDesc("You have given " + user.mention() + " a claim to " + role.mention())
						.appendDesc("\nThey have " + claim.remaining + " remaining changes for this claim.")
						.build()));
			}
		}),
		RENAMECLAIM(USER, "roleid name", (event, args) -> {
			String[] a     = args.split("\\s+", 2);
			IRole    role  = getSingleRole(event.getChannel(), a[0]);
			Claim    claim = getClaim(event.getAuthor(), role);
			if (role != null)
				if (checkAndSetClaimUpdate(event.getChannel(), claim)) {
					role.changeName(a[1]);
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(role.getColor())
							.appendDesc("You have renamed " + role.mention())
							.appendDesc("\nYou have " + claim.remaining + " remaining changes for this claim.")
							.build()));
				}
		}),
		RECOLOURCLAIM(USER, "roleid r g b", (event, args) -> {
			String[] a      = args.split("\\s+", 4);
			IRole    role   = getSingleRole(event.getChannel(), a[0]);
			Claim    claim  = getClaim(event.getAuthor(), role);
			Color    target = new Color(Integer.valueOf(a[1]), Integer.valueOf(a[2]), Integer.valueOf(a[3]));
			if (colourBlacklist.stream().anyMatch(c -> getColourDistance(target, c) <= colourCutoff))
				RequestBuffer.request(() -> event.getChannel().sendMessage("That colour is too similar to one in the blacklist."));
			else if (role != null)
				if (checkAndSetClaimUpdate(event.getChannel(), claim)) {
					role.changeColor(target);
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(role.getColor())
							.appendDesc("You have recoloured " + role.mention())
							.appendDesc("\nYou have " + claim.remaining + " remaining changes for this claim.")
							.build()));
				}
		}),
		CHECKCOLOUR(USER, "r g b", (event, args) -> {
			String[] a      = args.split("\\s+", 3);
			Color    target = new Color(Integer.valueOf(a[0]), Integer.valueOf(a[1]), Integer.valueOf(a[2]));
			RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
					.withTitle("Colour Distance Info")
					.withColor(target)
					.appendField("Distance from " + colourBlacklist.get(0).toString(), Double.toString(getColourDistance(colourBlacklist.get(0), target)), true)
					.appendField("Distance from " + colourBlacklist.get(1).toString(), Double.toString(getColourDistance(colourBlacklist.get(1), target)), true)
					.build()));
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
						.appendDesc("You have reset " + user.mention() + "'s cooldown.")
						.build()));
			}
		}),
		PREGENBLACKLIST(ADMIN, "", (event, args) -> {
			List<IRole> noncustoms = event.getGuild().getRoles().stream()
					.filter(r -> event.getGuild().getUsersByRole(r).size() > 1)
					.collect(Collectors.toList());
			blacklist.addAll(noncustoms.stream().map(IIDLinkedObject::getLongID).collect(Collectors.toList()));
			write();
			RequestBuffer.request(() -> event.getChannel().sendMessage("The blacklist has been appended with the following roles"));
			new PaginatorListener(event.getChannel(), event.getAuthor(), Lists.partition(noncustoms, 10).stream()
					.map(list -> new EmbedBuilder()
							.withTitle("Non-Custom Roles")
							.appendDesc(list.stream().map(r -> r.mention() + "\t" + r.getLongID()).collect(Collectors.joining("\n"))))
					.collect(Collectors.toList()));
		}),
		LISTUNUSED(USER, "", (event, args) -> {
			List<IRole> noncustoms = event.getGuild().getRoles().stream()
					.filter(r -> event.getGuild().getUsersByRole(r).size() == 0)
					.collect(Collectors.toList());
			new PaginatorListener(event.getChannel(), event.getAuthor(), Lists.partition(noncustoms, 10).stream()
					.map(list -> new EmbedBuilder()
							.withTitle("Unused Roles")
							.appendDesc(list.stream().map(r -> r.mention() + "\t" + r.getLongID()).collect(Collectors.joining("\n"))))
					.collect(Collectors.toList()));
		}),
		SETGLOBALCOOLDOWN(ADMIN, "seconds", (event, args) -> {
			main.config.set(Config.Property.COOLDOWN, Long.toString(Long.valueOf(args) * 1000));
			write();
			RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
					.withTitle("Claims Update")
					.withColor(Color.ORANGE)
					.appendDesc("The cooldown is now " + Long.valueOf(main.config.get(Config.Property.COOLDOWN)) / 1000 + " seconds.")
					.build()));
		}),
		SETGLOBALCHANGELIMIT(ADMIN, "count", (event, args) -> {
			main.config.set(Config.Property.MAXCHANGES, Integer.toString(Integer.valueOf(args)));
			write();
			RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
					.withTitle("Claims Update")
					.withColor(Color.ORANGE)
					.appendDesc("The maximum change count is now " + main.config.get(Config.Property.MAXCHANGES) + ".")
					.build()));
		}),
		GRANTCHANGES(ADMIN, "userid roleid amount", (event, args) -> {
			String[] a     = args.split("\\s+");
			IUser    user  = getSingleUser(event.getChannel(), a[0]);
			IRole    role  = getSingleRole(event.getChannel(), a[1]);
			int      toAdd = Integer.valueOf(a[2]);
			if (user != null && role != null) {
				Optional<Claim> claim = claims.stream()
						.filter(c -> c.user == user.getLongID() && c.role == role.getLongID())
						.findFirst();
				if (claim.isPresent()) {
					claim.get().remaining += toAdd;
					RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
							.withTitle("Claims Update")
							.withColor(role.getColor())
							.appendDesc(user.mention() + " now has " + claim.get().remaining + " remaining changes for this claim.")
							.build()));
				} else {
					RequestBuffer.request(() -> event.getChannel().sendMessage("No matching claim found."));
				}
			}
		}),
		INFO(USER, "", (event, args) -> {
			RequestBuffer.request(() -> event.getChannel().sendMessage(new EmbedBuilder()
					.withTitle("Claims Information")
					.withColor(Color.magenta)
					.appendField("Getting Started","Before anything else, you must claim your custom role." +
							"\nClaim roles with `/claims claim @role`." +
							"\nFrom there, you can either change the name of the claimed role, or change the colour." +
							"\nName change: `/claims renameclaim @role name`" +
							"\nColour change: `/claims recolourclaim @role r g b`",true)
					.appendField("Restrictions","You can only claim roles you have. " +
							"\nYou may not claim roles that are in the blacklist." +
							"\nThere is a cooldown between name/colour changes." +
							"\nThere is a limit to the total number of changes to your claim." +
							"\nColour changes must not be similar to admin/owner colours." +
							"\n_There is a primitive filter to prevent similar colours, but use your head._",true)
					.appendField("Cooldown",Long.valueOf(main.config.get(Config.Property.COOLDOWN))/1000+"",true)
					.appendField("Maximum Changes",main.config.get(Config.Property.MAXCHANGES), true)
					.build()));
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
		int  remaining;
		long user, role, time;

		public Claim(long user, long role, long time, int remaining) {
			this.user = user;
			this.role = role;
			this.time = time;
			this.remaining = remaining;
		}

		@Override
		public String toString() {
			return user + " " + role + " " + time + " " + remaining;
		}
	}
}
