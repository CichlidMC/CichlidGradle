package fish.cichlidmc.cichlid_gradle.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;

public final class TransformingIterable<A, B> implements Iterable<B> {
	private final Iterable<A> wrapped;
	private final Function<A, B> function;

	public TransformingIterable(Iterable<A> wrapped, Function<A, B> function) {
		this.wrapped = wrapped;
		this.function = function;
	}

	@NotNull
	@Override
	public Iterator<B> iterator() {
		return new TransformingIterator(this.wrapped.iterator());
	}

	private final class TransformingIterator implements Iterator<B> {
		private final Iterator<A> wrapped;

		private TransformingIterator(Iterator<A> wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public boolean hasNext() {
			return this.wrapped.hasNext();
		}

		@Override
		public B next() {
			return TransformingIterable.this.function.apply(this.wrapped.next());
		}

		@Override
		public void remove() {
			this.wrapped.remove();
		}
	}
}
