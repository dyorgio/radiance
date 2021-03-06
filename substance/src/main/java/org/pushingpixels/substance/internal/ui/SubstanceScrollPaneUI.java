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

import org.pushingpixels.substance.api.*;
import org.pushingpixels.substance.internal.*;
import org.pushingpixels.substance.internal.painter.BackgroundPaintingUtils;
import org.pushingpixels.substance.internal.utils.*;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.callback.UIThreadTimelineCallbackAdapter;
import org.pushingpixels.trident.swing.SwingComponentTimeline;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.beans.*;
import java.util.*;

/**
 * UI for scroll panes in <b>Substance</b> look and feel.
 *
 * @author Kirill Grouchnikov
 */
public class SubstanceScrollPaneUI extends BasicScrollPaneUI {
    /**
     * Property change listener on {@link SubstanceSynapse#WATERMARK_VISIBLE} and
     * <code>layoutManager</code> properties.
     */
    private PropertyChangeListener substancePropertyChangeListener;

    /**
     * Listener on the vertical scroll bar. Installed for the smart tree scroll (see
     * {@link SubstanceSlices.AnimationFacet#TREE_SMART_SCROLL_ANIMATION}.
     */
    private ChangeListener substanceVerticalScrollbarChangeListener;

    /**
     * Timeline for the current horizontal scroll under smart tree scroll mode.
     */
    private Timeline horizontalScrollTimeline;

    private Set<SubstanceWidget> lafWidgets;

    /**
     * Creates new UI delegate.
     *
     * @param comp Component.
     * @return UI delegate for the component.
     */
    public static ComponentUI createUI(JComponent comp) {
        SubstanceCoreUtilities.testComponentCreationThreadingViolation(comp);
        return new SubstanceScrollPaneUI();
    }

    private SubstanceScrollPaneUI() {
    }

    @Override
    public void installUI(JComponent c) {
        this.lafWidgets = SubstanceWidgetRepository.getRepository().getMatchingWidgets(c);

        super.installUI(c);

        for (SubstanceWidget lafWidget : this.lafWidgets) {
            lafWidget.installUI();
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        for (SubstanceWidget lafWidget : this.lafWidgets) {
            lafWidget.uninstallUI();
        }
        super.uninstallUI(c);
    }

    @Override
    protected void installDefaults(final JScrollPane scrollpane) {
        super.installDefaults(scrollpane);
        if (SubstanceCoreUtilities.toDrawWatermark(scrollpane) && (SubstanceCortex.ComponentScope
                .getCurrentSkin(scrollpane).getWatermark() != null)) {
            scrollpane.setOpaque(false);
            scrollpane.getViewport().setOpaque(false);
        }

        SwingUtilities.invokeLater(() -> installTableHeaderCornerFiller(scrollpane));

        for (SubstanceWidget lafWidget : this.lafWidgets) {
            lafWidget.installDefaults();
        }
    }

    @Override
    protected void uninstallDefaults(JScrollPane c) {
        Component upperRight = c.getCorner(JScrollPane.UPPER_RIGHT_CORNER);
        if (upperRight instanceof UIResource) {
            c.setCorner(JScrollPane.UPPER_RIGHT_CORNER, null);
        }
        Component upperLeft = c.getCorner(JScrollPane.UPPER_LEFT_CORNER);
        if (upperLeft instanceof UIResource) {
            c.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);
        }

        for (SubstanceWidget lafWidget : this.lafWidgets) {
            lafWidget.uninstallDefaults();
        }
        super.uninstallDefaults(c);
    }

    @Override
    protected void installListeners(final JScrollPane c) {
        super.installListeners(c);
        this.substancePropertyChangeListener = (PropertyChangeEvent evt) -> {
            if (SubstanceSynapse.WATERMARK_VISIBLE.equals(evt.getPropertyName())) {
                boolean toBleed = SubstanceCoreUtilities.toDrawWatermark(c);
                c.setOpaque(!toBleed);
                c.getViewport().setOpaque(!toBleed);
                Component view = c.getViewport().getView();
                if (view instanceof JComponent)
                    ((JComponent) view).setOpaque(!toBleed);
            }
            if ("background".equals(evt.getPropertyName())) {
                // propagate application-specific background color to the
                // scroll bars.
                Color newBackgr = (Color) evt.getNewValue();
                if (!(newBackgr instanceof UIResource)) {
                    JScrollBar vertical = scrollpane.getVerticalScrollBar();
                    if (vertical != null) {
                        if (vertical.getBackground() instanceof UIResource) {
                            vertical.setBackground(newBackgr);
                        }
                    }
                    JScrollBar horizontal = scrollpane.getHorizontalScrollBar();
                    if (horizontal != null) {
                        if (horizontal.getBackground() instanceof UIResource) {
                            horizontal.setBackground(newBackgr);
                        }
                    }
                }
            }
            if ("columnHeader".equals(evt.getPropertyName())
                    || "componentOrientation".equals(evt.getPropertyName())
                    || "ancestor".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    // need to switch the corner filler based on the
                    // current scroll pane state.
                    if (scrollpane != null) {
                        installTableHeaderCornerFiller(scrollpane);
                    }
                });
            }
        };
        c.addPropertyChangeListener(this.substancePropertyChangeListener);

        this.substanceVerticalScrollbarChangeListener = (ChangeEvent e) -> {
            // check if it's a horizontally scrollable tree inside
            if ((c.getHorizontalScrollBar() != null) && c.getHorizontalScrollBar().isVisible()
                    && (c.getViewport().getView() instanceof JTree)) {
                JTree tree = (JTree) c.getViewport().getView();
                // check if the smart scroll is enabled
                if (AnimationConfigurationManager.getInstance().isAnimationAllowed(
                        SubstanceSlices.AnimationFacet.TREE_SMART_SCROLL_ANIMATION, tree)) {
                    SubstanceTreeUI treeUI = (SubstanceTreeUI) tree.getUI();
                    final Rectangle viewportRect = c.getViewport().getViewRect();
                    int pivotX = treeUI.getPivotRendererX(viewportRect);
                    int currPivotX = viewportRect.x;// + viewportRect.width
                    // / 2;
                    int delta = pivotX - currPivotX;
                    int finalX = viewportRect.x + delta;
                    if (finalX < 0) {
                        delta -= finalX;
                    }
                    final int finalDelta = delta;
                    if (Math.abs(finalDelta) > viewportRect.width / 6) {
                        if (horizontalScrollTimeline != null) {
                            // abort previous horizontal scroll
                            horizontalScrollTimeline.abort();
                        }
                        horizontalScrollTimeline = SwingComponentTimeline.componentBuilder(tree)
                                .addCallback(new UIThreadTimelineCallbackAdapter() {
                                    @Override
                                    public void onTimelinePulse(float durationFraction,
                                            float timelinePosition) {
                                        if (timelinePosition >= 0.5) {
                                            int nudge =
                                                    (int) (finalDelta * (timelinePosition - 0.5));
                                            c.getViewport().setViewPosition(
                                                    new Point(viewportRect.x + nudge,
                                                            viewportRect.y));
                                        }
                                    }
                                })
                                .setEase((float durationFraction) -> {
                                    if (durationFraction < 0.5) {
                                        return 0.5f * durationFraction;
                                    }
                                    return 0.25f + (durationFraction - 0.5f) * 0.75f / 0.5f;
                                }).setDuration(2 * AnimationConfigurationManager.getInstance()
                                        .getTimelineDuration())
                                .build();
                        horizontalScrollTimeline.play();
                    }
                }
            }
        };
        c.getVerticalScrollBar().getModel()
                .addChangeListener(this.substanceVerticalScrollbarChangeListener);

        for (SubstanceWidget lafWidget : this.lafWidgets) {
            lafWidget.installListeners();
        }
    }

    @Override
    protected void uninstallListeners(JComponent c) {
        c.removePropertyChangeListener(this.substancePropertyChangeListener);
        this.substancePropertyChangeListener = null;

        JScrollPane jsp = (JScrollPane) c;
        jsp.getVerticalScrollBar().getModel()
                .removeChangeListener(this.substanceVerticalScrollbarChangeListener);
        this.substanceVerticalScrollbarChangeListener = null;

        for (SubstanceWidget lafWidget : this.lafWidgets) {
            lafWidget.uninstallListeners();
        }

        super.uninstallListeners(c);
    }

    @Override
    public void update(Graphics g, JComponent c) {
        BackgroundPaintingUtils.updateIfOpaque(g, c);
        JScrollPane jsp = (JScrollPane) c;

        LayoutManager lm = jsp.getLayout();
        ScrollPaneLayout scrollLm = null;
        if (lm instanceof ScrollPaneLayout) {
            scrollLm = (ScrollPaneLayout) lm;
        }

        if (scrollLm != null) {
            Set<Component> corners = new HashSet<>();
            if (scrollLm.getCorner(ScrollPaneLayout.LOWER_LEFT_CORNER) != null) {
                corners.add(scrollLm.getCorner(ScrollPaneLayout.LOWER_LEFT_CORNER));
            }
            if (scrollLm.getCorner(ScrollPaneLayout.LOWER_RIGHT_CORNER) != null) {
                corners.add(scrollLm.getCorner(ScrollPaneLayout.LOWER_RIGHT_CORNER));
            }
            if (scrollLm.getCorner(ScrollPaneLayout.UPPER_LEFT_CORNER) != null) {
                corners.add(scrollLm.getCorner(ScrollPaneLayout.UPPER_LEFT_CORNER));
            }
            if (scrollLm.getCorner(ScrollPaneLayout.UPPER_RIGHT_CORNER) != null) {
                corners.add(scrollLm.getCorner(ScrollPaneLayout.UPPER_RIGHT_CORNER));
            }

            if (SubstanceCoreUtilities.isOpaque(c)) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(SubstanceColorUtilities
                        .getBackgroundFillColorScrollBar(this.scrollpane.getVerticalScrollBar()));
                for (Component corner : corners) {
                    g2d.fill(corner.getBounds());
                    // BackgroundPaintingUtils.fillAndWatermark(g, c, c
                    // .getBackground(), corner.getBounds());
                }

                JScrollBar horizontal = this.scrollpane.getHorizontalScrollBar();
                JScrollBar vertical = this.scrollpane.getVerticalScrollBar();
                if ((horizontal != null) && (vertical != null)) {
                    if (this.scrollpane.getComponentOrientation().isLeftToRight()) {
                        // Bottom right corner
                        if (scrollLm.getCorner(ScrollPaneLayout.LOWER_RIGHT_CORNER) == null) {
                            g2d.fillRect(horizontal.getX() + horizontal.getWidth(),
                                    horizontal.getY(), vertical.getWidth(), horizontal.getHeight());
                        }
                    } else {
                        // Bottom left corner
                        if (scrollLm.getCorner(ScrollPaneLayout.LOWER_LEFT_CORNER) == null) {
                            g2d.fillRect(0, horizontal.getY(), vertical.getWidth(),
                                    horizontal.getHeight());
                        }
                    }
                }
            }
        }

        super.paint(g, c);
    }

    /**
     * Installs a corner filler that matches the table header. This is done to provide a continuous
     * appearance for tables with table headers placed in scroll panes.
     *
     * @param scrollpane Scroll pane.
     */
    private void installTableHeaderCornerFiller(JScrollPane scrollpane) {
        // install custom scroll pane corner filler
        // for continuous painting of table headers
        JViewport columnHeader = scrollpane.getColumnHeader();
        // System.out.println("Column header " + columnHeader);
        if (columnHeader == null) {
            return;
        }
        Component columnHeaderComp = columnHeader.getView();
        // System.out.println("Column header comp " + columnHeaderComp);
        if (!(columnHeaderComp instanceof JTableHeader)) {
            return;
        }
        JTableHeader tableHeader = (JTableHeader) columnHeaderComp;
        TableHeaderUI tableHeaderUI = tableHeader.getUI();
        if (!(tableHeaderUI instanceof SubstanceTableHeaderUI)) {
            return;
        }
        SubstanceTableHeaderUI ui = (SubstanceTableHeaderUI) tableHeaderUI;
        JComponent scrollPaneCornerFiller = ui.getScrollPaneCornerFiller();
        String cornerKey = scrollpane.getComponentOrientation().isLeftToRight()
                ? JScrollPane.UPPER_RIGHT_CORNER
                : JScrollPane.UPPER_LEFT_CORNER;
        Component cornerComp = scrollpane.getCorner(cornerKey);
        // Corner component can be replaced when the current one is null or
        // UIResource
        boolean canReplace = (cornerComp == null) || (cornerComp instanceof UIResource);
        // System.out.println(canReplace + ":" + cornerComp);
        if (canReplace) {
            scrollpane.setCorner(cornerKey, scrollPaneCornerFiller);
        }
    }
}
