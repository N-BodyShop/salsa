//NotifyingHashtable.java

import java.util.Hashtable;
import java.util.Map;
import javax.swing.event.EventListenerList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** A NotifyingHashtable is a source of ChangeEvents, fired when the contents
 of the hashtable change.
 */
public class NotifyingHashtable extends Hashtable {
	
	protected EventListenerList listenerList = new EventListenerList();
	private ChangeEvent changeEvent;
	
	public NotifyingHashtable() {
		super();
	}
	
	public NotifyingHashtable(int capacity) {
		super(capacity);
	}

	public NotifyingHashtable(int capacity, float load) {
		super(capacity, load);
	}

	public NotifyingHashtable(Map t) {
		super(t);
	}
	
	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}

	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}
	
	private void fireChange() {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for(int i = listeners.length - 2; i >= 0; i -= 2) {
			if(listeners[i] == ChangeListener.class) {
				// Lazily create the event:
				if(changeEvent == null)
					changeEvent = new ChangeEvent(this);
				((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
			}
		}
	}
	
	public Object put(Object key, Object value) {
		Object result = super.put(key, value);
		fireChange();
		return result;
	}
	
	public void putAll(Map t) {
		super.putAll(t);
		fireChange();
	}
	
	public Object remove(Object key) {
		Object result = super.remove(key);
		fireChange();
		return result;
	}
}
