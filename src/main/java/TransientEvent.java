import sx.blah.discord.api.events.Event;

public class TransientEvent<T extends Event> {
	private      boolean canceled = false;
	public final T       event;

	public TransientEvent(T event) {
		this.event = event;
	}

	public T getEvent() {
		return event;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public enum ReturnType {
		UNSUBSCRIBE,
		DONOTHING
	}
}
