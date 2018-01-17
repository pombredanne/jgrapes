/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.internal;

import java.util.concurrent.ExecutorService;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * This class provides the default implementation of an {@link EventPipeline}.
 */
public class EventProcessor implements InternalEventPipeline, Runnable {

	protected final static ThreadLocal<EventBase<?>> 
		currentlyHandling = new ThreadLocal<>();
	
	private final ExecutorService executorService;
	private final ComponentTree componentTree;
	private final EventPipeline asEventPipeline;
	protected final EventQueue queue = new EventQueue();
	private boolean isExecuting = false;
	
	EventProcessor(ComponentTree tree) {
		this(tree, Components.defaultExecutorService());
	}

	EventProcessor(ComponentTree tree, ExecutorService executorService) {
		this.componentTree = tree;
		this.executorService = executorService;
		asEventPipeline = new CheckingPipelineFilter(this);
	}

	@Override
	public <T extends Event<?>> T add(T event, Channel... channels) {
		((EventBase<?>)event).generatedBy(currentlyHandling.get());
		((EventBase<?>)event).processedBy(this);
		queue.add(event, channels);
		synchronized (this) {
			if (!isExecuting) {
				GeneratorRegistry.instance().add(this);
				isExecuting = true;
				executorService.execute(this);
			}
		}
		return event;
	}

	void add(EventQueue source) {
		while (true) {
			EventChannelsTuple entry = source.poll();
			if (entry == null) {
				break;
			}
			entry.event.processedBy(this);
			queue.add(entry);
		}
		synchronized (this) {
			if (!isExecuting) {
				GeneratorRegistry.instance().add(this);
				isExecuting = true;
				executorService.execute(this);
			}
		}
	}
	
	@Override
	public void merge(InternalEventPipeline other) {
		if (!(other instanceof BufferingEventPipeline)) {
			throw new IllegalArgumentException(
					"Can only merge events from an BufferingEventPipeline.");
		}
		add(((BufferingEventPipeline) other).retrieveEvents());
	}

	@Override
	public void run() {
		String origName = Thread.currentThread().getName();
		try {
			Thread.currentThread().setName(
					origName + " (P" + Components.objectId(this) + ")");
			FeedBackPipelineFilter.setAssociatedPipeline(this);
			while (true) {
				// No lock needed if queue is filled
				EventChannelsTuple next = queue.peek();
				if (next == null) {
					synchronized (this) {
						// Retry with lock for proper synchronization
						next = queue.peek();
						if (next == null) {
							GeneratorRegistry.instance().remove(this);
							isExecuting = false;
							break;
						}
					}
				}
				currentlyHandling.set(next.event);
				componentTree.dispatch(
						asEventPipeline, next.event, next.channels);
				currentlyHandling.get().decrementOpen();
				queue.remove();
			}
		} finally {
			Thread.currentThread().setName(origName);
			currentlyHandling.set(null);;
			FeedBackPipelineFilter.setAssociatedPipeline(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.InternalEventPipeline#executorService()
	 */
	@Override
	public ExecutorService executorService() {
		return executorService;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (queue != null) {
			builder.append("queue=");
			builder.append(queue);
		}
		builder.append("]");
		return builder.toString();
	}

}
