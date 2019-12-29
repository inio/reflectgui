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

import java.awt.*;
import java.awt.dnd.*;
import javax.swing.tree.TreeModel;

import org.dotuseful.ui.tree.MouseAdaptedTree;

@SuppressWarnings("serial")
public class AutoScrollingJTree extends MouseAdaptedTree implements Autoscroll {
    private int margin = 12;
    
    int dropHiliteRow = -1;

    public AutoScrollingJTree(TreeModel model) {
      super(model);
    }

    public void autoscroll(Point p) {
      int realrow = getRowForLocation(p.x, p.y);
      Rectangle outer = getBounds();
      realrow = (p.y + outer.y <= margin ? realrow < 1 ? 0 : realrow - 1
          : realrow < getRowCount() - 1 ? realrow + 1 : realrow);
      scrollRowToVisible(realrow);
    }

    public Insets getAutoscrollInsets() {
      Rectangle outer = getBounds();
      Rectangle inner = getParent().getBounds();
      return new Insets(inner.y - outer.y + margin, inner.x - outer.x
          + margin, outer.height - inner.height - inner.y + outer.y
          + margin, outer.width - inner.width - inner.x + outer.x
          + margin);
    }

    // Use this method if you want to see the boundaries of the
    // autoscroll active region

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (dropHiliteRow >= 0) {
	      Rectangle droptarget = getRowBounds(dropHiliteRow);
	      g.setColor(new Color(0,0,0,.3f));
	      g.fillRect(droptarget.x, droptarget.y, droptarget.width, droptarget.height);
      }
    }

	public void setDropHilite(int row) {
		if (dropHiliteRow != -1)
			repaint(getRowBounds(dropHiliteRow));
		dropHiliteRow = row;
		if (dropHiliteRow != -1)
			repaint(getRowBounds(dropHiliteRow));
	}

  }