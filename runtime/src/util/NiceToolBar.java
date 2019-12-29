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

package util;

import java.awt.Insets;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;


@SuppressWarnings("serial")
public class NiceToolBar extends JToolBar {
	public NiceToolBar() {
		super();
	}

	public NiceToolBar(int orientation) {
		super(orientation);
	}

	public NiceToolBar(String name, int orientation) {
		super(name, orientation);
	}

	public NiceToolBar(String name) {
		super(name);
	}
	
	@Override
	public JButton add(Action a) {
		JButton b = super.add(a);
		b.setFocusPainted(false);
		b.setMargin(new Insets(0,0,0,0));
		return b;
	}
}
