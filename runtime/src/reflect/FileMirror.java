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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import util.Utils;

public class FileMirror implements MagicMirror<File> {

	public boolean canReflect(Type type) {
		if (File.class.isAssignableFrom(Utils.typeClass(type))) return true;
		return false;
	}

	public void deserialize(Type t, Deserializer scanner, File autoValue, Property<File> prop)
			throws IOException, SyntaxError {
		if (scanner.nextToken() != '"') throw new SyntaxError("Expecting string");
		Mirror.dumbInstantiateInto(prop, t, false, scanner.sval);
	}

	public void serialize(File v, Serializer o) throws IOException {
		// serialize it as a string
		Mirror.serializeValue(((File) v).getCanonicalPath(), o);
	}
	
	public static File runEditor(AnnotatedElement target, File at) {
		FileEditorInfo tmpinfo;
		try {
			tmpinfo = target.getAnnotation(GUIInfo.class).editorInfo().file()[0];
		} catch (Exception e) {
			tmpinfo = null;
		}
		final FileEditorInfo info = tmpinfo;
		
		JFileChooser fc = new JFileChooser();
		if (at != null) fc.setCurrentDirectory(at.getParentFile());
		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (info != null) switch(info.mode()) {
				case openFolder:
					return f.isDirectory();
				default:
					if (f.isDirectory()) return true;
				
					if (info.extensions().length==0) return true;
					
					for(String ext : info.extensions())
						if (f.getName().endsWith(ext))
							return true;
					
					return false;
				} else {
					return true;
				}
			}

			@Override
			public String getDescription() {return null;}
			
		});
		if (info.prompt().length() > 0)
			fc.setDialogTitle(info.prompt());
		else if (info != null) switch (info.mode()) {
		case openFile:
			fc.setDialogTitle("select a file");
			break;
		case openFolder:
			fc.setDialogTitle("select a directory");
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			break;
		case saveFile:
			fc.setDialogTitle("save file");
			break;
		} else {
			fc.setDialogTitle("blargh! (no editor info!)");
		}
		fc.setSelectedFile(at);
		
		int result;
		
		if (info != null) switch(info.mode()) {
		case saveFile:
			result = fc.showSaveDialog(null);
			break;
		default:
			result = fc.showOpenDialog(null);
			break;
		} else {
			result = fc.showDialog(null, "OK");
		}
		switch(result) {
		case JFileChooser.APPROVE_OPTION:
			return fc.getSelectedFile();
		default:
			return null;
		}
	}
	
	@SuppressWarnings("serial")
	static class FileEditor extends Editor<File> implements ActionListener, FocusListener {
		JTextField field;
		JButton browse;
		FileEditorInfo info;
		protected FileEditor(Property<File> prop) {
			super(prop, BoxLayout.X_AXIS);
			add(new JLabel(label()));
			if (prop.getAnnotation(GUIInfo.class) != null)
				if (prop.getAnnotation(GUIInfo.class).editorInfo().file().length == 1)
					info = prop.getAnnotation(GUIInfo.class).editorInfo().file()[0];
			field = new JTextField("", 10);
			field.setMaximumSize(new Dimension(1000, field.getPreferredSize().height));
			field.addActionListener(this);
			field.addFocusListener(this);
			field.setActionCommand("field");
			add(field);
			browse = new JButton("Browse...");
			browse.addActionListener(this);
			browse.setActionCommand("browse");
			add(browse);
			reload();
		}
		
		private void reload() {
			String value;
			if (prop.get() != null) {
				try {
					value = ((File)prop.get()).getCanonicalPath();
				} catch (IOException e) {
					value = "EX: "+e.getMessage();
				}
			} else {
				value = "<null>";
			}
			field.setText(value);
		}

		@Override
		public void externallyUpdated() {
			reload();
			super.externallyUpdated();
		}
		
		private void commitText() {
			if (field.getText().equals("<null>")) {
				if (prop.get() != null) {
					UndoLog.staticUndoLog().begin("Change "+label());
					prop.set(null, true);
					UndoLog.staticUndoLog().end();
				}
				return;
			}
			try {
				File newval = new File(field.getText());
				if (info != null) switch(info.mode()) {
				case openFile:
					if (!newval.exists())
						throw new Exception("File not found.");
					if (!newval.isFile())
						throw new Exception("Path must point to a file.");
					if (!newval.canRead())
						throw new Exception("Path must be readable");
					break;
				case openFolder:
					if (!newval.exists())
						throw new Exception("File not found.");
					if (!newval.isDirectory())
						throw new Exception("Path must point to a folder.");
					if (!newval.canRead())
						throw new Exception("Path must be readable");
					break;
				case saveFile:
					if (!newval.canWrite())
						throw new Exception("Path must be writable");
					break;
				} else {
					if (!(newval.canRead() || newval.canWrite()))
						throw new Exception("Path not valid");
				}
				if (!newval.equals(prop.get())) {
					UndoLog.staticUndoLog().begin("Change "+label());
					prop.set(newval, true);
					UndoLog.staticUndoLog().end();
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				reload();
			}
			setDirty();
		}
		
		public void actionPerformed(ActionEvent arg0) {
			try {
				if (arg0.getActionCommand().equals("field")) {
					commitText();
				} else if (arg0.getActionCommand().equals("browse")) {
					File at = (File) prop.get();
					at = runEditor(prop, at);
					if (at != null && !at.equals(prop.get())) {
						File val = (File) Utils.typeClass(prop.getType()).
							getConstructor(String.class).newInstance(at.getCanonicalPath());
						UndoLog.staticUndoLog().begin("Change "+label());
						prop.set(val, true);
						UndoLog.staticUndoLog().end();
						setDirty();
						reload();
					}
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				reload();
			}
		}

		public void focusGained(FocusEvent arg0) {}

		public void focusLost(FocusEvent arg0) {
			commitText();
		}
	}

	public Editor<File> buildEditor(Property<File> prop) {
		return new FileEditor(prop);
	}

	public void instantiateInto(Property<File> prop, Type t, boolean log) {
		try {
			Mirror.dumbInstantiateInto(prop, t, log, null);
		} catch(Exception e) {
			Mirror.dumbInstantiateInto(prop, t, log, "");
		}
	}

	public boolean isStrongClass(Type t) {
		// TODO Auto-generated method stub
		return true;
	}
}
