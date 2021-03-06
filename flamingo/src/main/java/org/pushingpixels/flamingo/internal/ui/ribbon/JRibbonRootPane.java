/*
 * Copyright (c) 2005-2019 Flamingo Kirill Grouchnikov. All Rights Reserved.
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
 *  o Neither the name of Flamingo Kirill Grouchnikov nor the names of
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
package org.pushingpixels.flamingo.internal.ui.ribbon;

import org.pushingpixels.flamingo.api.ribbon.*;
import org.pushingpixels.flamingo.internal.substance.ribbon.ui.SubstanceRibbonRootPaneUI;
import org.pushingpixels.neon.NeonCortex;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Root pane for the {@link JRibbonFrame}. This class is for internal use only and should not be
 * directly used by the applications.
 *
 * @author Kirill Grouchnikov
 */
public class JRibbonRootPane extends JRootPane {
    /**
     * The UI class ID string.
     */
    public static final String uiClassID = "RibbonRootPaneUI";

    public JRibbonRootPane() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        actionMap.put("toggleMinimized", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JRibbonFrame ribbonFrame = (JRibbonFrame) SwingUtilities
                        .getWindowAncestor(JRibbonRootPane.this);
                JRibbon ribbon = ribbonFrame.getRibbon();
                ribbon.setMinimized(!ribbon.isMinimized());
            }
        });
        KeyStroke keyStroke = (NeonCortex.getPlatform() == NeonCortex
                .Platform.MACOS) ? KeyStroke.getKeyStroke("meta alt R") :
                KeyStroke.getKeyStroke("ctrl F1");
        inputMap.put(keyStroke, "toggleMinimized");
    }

    @Override
    public void updateUI() {
        setUI(SubstanceRibbonRootPaneUI.createUI(this));
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }
}
