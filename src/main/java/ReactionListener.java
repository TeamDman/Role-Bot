import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.function.Function;

public class ReactionListener {
	private final IUser                                                    author;
	private final ReactionEmoji                                            emoji;
	private final IMessage                                                 message;
	private final Function<ReactionAddEvent, TransientEvent.ReturnType>    onAdd;
	private final Function<ReactionRemoveEvent, TransientEvent.ReturnType> onRemove;
	private final Runnable                                                 onStop;
	private       IListener                                                listenerAdd, listenerRemove;


	public ReactionListener(Builder builder) {
		this.message = builder.getMessage();
		this.onAdd = builder.getOnAdd();
		this.onRemove = builder.getOnRemove();
		this.onStop = builder.getOnStop();
		this.author = builder.getAuthor();
		this.emoji = builder.getEmoji();
		if (onAdd != null)
			main.client.getDispatcher().registerListener(listenerAdd = new AddListener());
		if (onRemove != null)
			main.client.getDispatcher().registerListener(listenerRemove = new RemoveListener());
	}

	public void dispose() {
		if (onStop != null)
			onStop.run();
		if (listenerAdd != null)
			main.client.getDispatcher().unregisterListener(listenerAdd);
		if (listenerRemove != null)
			main.client.getDispatcher().unregisterListener(listenerRemove);
	}

	public static class Builder {
		private IUser                                                    author;
		private ReactionEmoji                                            emoji;
		private IMessage                                                 message;
		private Function<ReactionAddEvent, TransientEvent.ReturnType>    onAdd;
		private Function<ReactionRemoveEvent, TransientEvent.ReturnType> onRemove;
		private Runnable                                                 onStop;

		public Builder(IMessage message) {
			this.message = message;
			this.author = message.getAuthor();
		}

		public IUser getAuthor() {
			return author;
		}

		public Builder setAuthor(IUser author) {
			this.author = author;
			return this;
		}

		private ReactionEmoji getEmoji() {
			return emoji;
		}

		public Builder setEmoji(ReactionEmoji emoji) {
			this.emoji = emoji;
			return this;
		}

		private Function<ReactionAddEvent, TransientEvent.ReturnType> getOnAdd() {
			return onAdd;
		}

		public Builder setOnAdd(Function<ReactionAddEvent, TransientEvent.ReturnType> onAdd) {
			this.onAdd = onAdd;
			return this;
		}

		private Function<ReactionRemoveEvent, TransientEvent.ReturnType> getOnRemove() {
			return onRemove;
		}

		public Builder setOnRemove(Function<ReactionRemoveEvent, TransientEvent.ReturnType> onRemove) {
			this.onRemove = onRemove;
			return this;
		}

		private Runnable getOnStop() {
			return onStop;
		}

		public Builder setOnStop(Runnable onStop) {
			this.onStop = onStop;
			return this;
		}

		private IMessage getMessage() {
			return message;
		}

		public Builder setMessage(IMessage message) {
			this.message = message;
			return this;
		}

	}

	@SuppressWarnings("ConstantConditions")
	private class AddListener implements IListener<ReactionAddEvent> {
		@Override
		public void handle(ReactionAddEvent event) {
			if (message != null && !event.getMessage().equals(message))
				return;
			if (author != null && !event.getUser().equals(author))
				return;
			if (emoji != null && !event.getReaction().getEmoji().equals(emoji))
				return;
			if (onAdd.apply(event) == TransientEvent.ReturnType.UNSUBSCRIBE)
				dispose();
		}
	}

	@SuppressWarnings("ConstantConditions")
	private class RemoveListener implements IListener<ReactionRemoveEvent> {
		@Override
		public void handle(ReactionRemoveEvent event) {
			if (message != null && !event.getMessage().equals(message))
				return;
			if (author != null && !event.getUser().equals(author))
				return;
			if (emoji != null && !event.getReaction().getEmoji().equals(emoji))
				return;
			if (onRemove.apply(event) == TransientEvent.ReturnType.UNSUBSCRIBE)
				dispose();
		}
	}
}
