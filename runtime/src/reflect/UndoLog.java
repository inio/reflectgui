/*
 * Copyright 2009 Ian Rickard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import util.REThrow;

public class UndoLog {

	protected static class Change {
		Object obj;
		Object arg;
		Object oldval;
		Object newval;
		public Change(Object obj, Object arg, Object oldval, Object newval) {
			this.obj = obj;
			this.arg = arg;
			this.oldval = oldval;
			this.newval = newval;
		}
		
		@SuppressWarnings("unchecked")
		public void undo() {
			if (obj instanceof UndoAware) {
				((UndoAware)obj).performUndo(arg, oldval);
			} else try {
				Field field = (Field)arg;
				PreChange hook = field.getAnnotation(PreChange.class);
				if (null != hook) {
					try {
						Method m = Mirror.getMethod(obj.getClass(), hook.value(), field.getType());
						m.setAccessible(true);
						m.invoke(obj, oldval);
					} catch (Exception e) {
						e.printStackTrace();
						System.err.println("exception running change hook for "+
								obj.getClass().getCanonicalName()+"."+field.getName()+": "+e);
					}
				}
				if (!(field.get(obj)==newval || field.get(obj).equals(newval))) {
					throw new REThrow("Current value doesn't match expected");
				}
				field.set(obj, oldval);
			} catch (Exception e) {
				throw new REThrow(e);
			}
		}
		
		@SuppressWarnings("unchecked")
		public void redo() {
			if (obj instanceof UndoAware) {
				((UndoAware)obj).performUndo(arg, newval);
			} else try {
				Field field = (Field)arg;
				PreChange hook = field.getAnnotation(PreChange.class);
				if (null != hook) {
					try {
						Method m = Mirror.getMethod(obj.getClass(), hook.value(), field.getType());
						m.setAccessible(true);
						m.invoke(obj, newval);
					} catch (Exception e) {
						e.printStackTrace();
						System.err.println("exception running change hook for "+
								obj.getClass().getCanonicalName()+"."+field.getName()+": "+e);
					}
				}
				if (!(field.get(obj)==oldval || field.get(obj).equals(oldval))) {
					throw new REThrow("Current value doesn't match expected");
				}
				field.set(obj, newval);
			} catch (Exception e) {
				throw new REThrow(e);
			}
		}
	}
	
	protected static class Action {
		ArrayList<Change> changes;
		String desc;
		public Action(ArrayList<Change> changes, String desc) {
			this.changes = changes;
			this.desc = desc;
		}

		@SuppressWarnings("unchecked")
		public void undo() {
			ArrayList<Change> rchanges = (ArrayList<Change>) changes.clone();
			Collections.reverse(rchanges);
			for(Change c : rchanges) c.undo();
		}
		
		public void redo() {
			for(Change c : changes) c.redo();
		}
	}
	
	LinkedList<Action> undo = new LinkedList<Action>(), redo = new LinkedList<Action>();
	ArrayList<Change> transactionChanges;
	String transactionName;
	int beginDepth;
	boolean reentry;
	LinkedList<UndoLogListener> listeners = new LinkedList<UndoLogListener>();
	
	public void begin(String name) {
		if (reentry) throw new REThrow("Undo Rentry");
		reentry = true;
		if (beginDepth == 0){
			if (transactionChanges != null)
				throw new REThrow("Began transaction while another was active");
			transactionChanges = new ArrayList<Change>();
			transactionName = name;
		}
		beginDepth++;
		reentry = false;
	}
	
	public void end() {
		if (reentry) throw new REThrow("Undo Rentry");
		reentry = true;
		if (beginDepth > 1) {
			// nothing
		} else if (beginDepth == 1) {
			if (transactionChanges == null)
				throw new REThrow("Ended transaction that didn't exist");
			if (transactionChanges.size() > 0) {
				redo.clear();
				undo.add(new Action(transactionChanges, transactionName));
			}
			transactionChanges = null;
			for (UndoLogListener l : listeners)
				l.undoLogUpdated();
		} else {
			throw new REThrow("popped too many transactions");
		}
		beginDepth--;
		reentry = false;
	}
	
	private void doLogChange(Object target, Object arg, Object oldval, Object newval) {
		if (reentry)
			throw new REThrow("Undo Rentry");
		boolean localTransaction = transactionChanges == null;
		if(localTransaction) {
			begin("");
		}
		reentry = true;
		transactionChanges.add(new Change(target, arg, oldval, newval));
		reentry = false;
		if (localTransaction) {
			end();
		}
	}
	
	public void logChange(Object target, Field field, Object oldval, Object newval) {
		doLogChange(target, field, oldval, newval);
	}
	
	public <K, T> void logChange(UndoAware<K, T> target, K arg, T oldval, T newval) {
		doLogChange(target, arg, oldval, newval);
	}
	
	public <K, T> void logChange(UndoAware<K, T> target, T oldval, T newval) {
		doLogChange(target, null, oldval, newval);
	}
	
	public void undo() {
		if (reentry) throw new REThrow("Undo Rentry");
		reentry = true;
		Action act = undo.removeLast();
		redo.add(act);
		act.undo();
		reentry = false;
		for (UndoLogListener l : listeners)
			l.undoLogUpdated();
	}

	public void redo() {
		if (reentry) throw new REThrow("Undo Rentry");
		reentry = true;
		Action act = redo.removeLast();
		undo.add(act);
		act.redo();
		reentry = false;
		for (UndoLogListener l : listeners)
			l.undoLogUpdated();
		
	}
	
	public String getUndo() {
		if (undo.size() == 0) return null;
		return undo.getLast().desc;
	}
	
	public String getRedo() {
		if (redo.size() == 0) return null;
		return redo.getLast().desc;
	}
	
	static UndoLog sThis = null; 
	
	public static UndoLog staticUndoLog() {
		if (sThis == null)
			sThis = new UndoLog();
		return sThis;
	}
	
	public void addLogListener(UndoLogListener l) {
		listeners.add(l);
	}
	
	public void removeLogListener(UndoLogListener l) {
		listeners.remove(l);
	}
}
