/*
 * Copyright (c) 2005-2019 Substance Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of Substance Kirill Grouchnikov nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.pushingpixels.substance.internal.ui;

import org.pushingpixels.substance.internal.painter.BackgroundPaintingUtils;
import org.pushingpixels.substance.internal.utils.*;
import org.pushingpixels.substance.internal.widget.animation.effects.GhostPaintingUtils;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicDesktopPaneUI;
import java.awt.*;

/**
 * UI for desktop panes in <b>Substance</b> look and feel.
 * 
 * @author Kirill Grouchnikov
 */
public class SubstanceDesktopPaneUI extends BasicDesktopPaneUI {
	public static ComponentUI createUI(JComponent comp) {
		SubstanceCoreUtilities.testComponentCreationThreadingViolation(comp);
		return new SubstanceDesktopPaneUI();
	}

	private SubstanceDesktopPaneUI() {
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
		Border curr = this.desktop.getBorder();
		if ((curr == null) || (curr instanceof UIResource)) {
			this.desktop.setBorder(new BorderUIResource(new EmptyBorder(0, 0, 0, 0)));
		}
	}

	@Override
	public void update(Graphics g, JComponent c) {
		if (!c.isShowing()) {
			return;
		}
		Graphics2D graphics = (Graphics2D) g.create();
		graphics.setComposite(WidgetUtilities.getAlphaComposite(c, g));
		if (SubstanceCoreUtilities.isOpaque(c)) {
			// hack for JLayeredPane.paint() and JDesktopPane.isOpaque()
			Color back = c.getBackground();
			if (back instanceof UIResource) {
				graphics.setColor(UIManager.getColor("Panel.background"));
				graphics.fillRect(0, 0, c.getWidth(), c.getHeight());
			}
			BackgroundPaintingUtils.updateIfOpaque(graphics, c);
		}
		super.paint(graphics, c);
		GhostPaintingUtils.paintGhostImages(c, graphics);
		graphics.dispose();
	}
}
