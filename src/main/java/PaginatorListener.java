import com.google.common.collect.Streams;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class PaginatorListener {
	private final List<EmbedObject> pages;
	private       int               index = 0;
	private       IMessage          message;
	private       ReactionListener  reactionListener;

	public PaginatorListener(IChannel channel, @Nullable IUser user, List<EmbedBuilder> pages) {
		this.pages = Streams.mapWithIndex(pages.stream(), (v, i) -> v.withFooterText("Page " + (i + 1) + " of " + pages.size()).build()).collect(Collectors.toList());
		RequestBuffer.request(() -> {
			this.message = channel.sendMessage(this.pages.get(0));
			new RequestBuilder(main.client)
					.shouldBufferRequests(true)
					.doAction(() -> {
						message.addReaction(PageControl.LEFT.emoji);
						return true;
					})
					.andThen(() -> {
						message.addReaction(PageControl.RIGHT.emoji);
						return true;
					})
					.andThen(() -> {
						message.addReaction(PageControl.STOP.emoji);
						return true;
					})
					.execute();
			reactionListener = new ReactionListener(new ReactionListener.Builder(message).setAuthor(user).setOnAdd(this::handle).setOnRemove(this::handle));
		});
	}

	private TransientEvent.ReturnType handle(ReactionEvent event) {
		switch (PageControl.fromEmoji(event.getReaction().getEmoji())) {
			case LEFT:
				index = --index < 0 ? pages.size() - 1 : index;
				displayPage();
				break;
			case RIGHT:
				index = ++index >= pages.size() ? 0 : index;
				displayPage();
				break;
			case STOP:
				RequestBuffer.request(message::removeAllReactions);
				reactionListener.dispose();
				break;
		}
		return TransientEvent.ReturnType.DONOTHING;
	}

	private void displayPage() {
		RequestBuffer.request(() -> message.edit(pages.get(index)));
	}

	private enum PageControl {
		LEFT(ReactionEmoji.of("⬅")),
		RIGHT(ReactionEmoji.of("➡")),
		STOP(ReactionEmoji.of("❌")),
		UNKNOWN(null);
		final ReactionEmoji emoji;

		PageControl(ReactionEmoji emoji) {
			this.emoji = emoji;
		}

		static PageControl fromEmoji(ReactionEmoji emoji) {
			for (PageControl control : values())
				if (emoji.equals(control.emoji))
					return control;
			return UNKNOWN;
		}
	}
}
