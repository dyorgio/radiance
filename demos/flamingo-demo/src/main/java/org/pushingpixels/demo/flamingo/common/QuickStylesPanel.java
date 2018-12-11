/**
 *
 */
package org.pushingpixels.demo.flamingo.common;

import org.pushingpixels.demo.flamingo.svg.tango.transcoded.Font_x_generic;
import org.pushingpixels.flamingo.api.common.*;
import org.pushingpixels.flamingo.api.common.icon.DecoratedResizableIcon;
import org.pushingpixels.flamingo.api.common.model.*;
import org.pushingpixels.neon.icon.ResizableIcon;

import java.awt.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

public class QuickStylesPanel {
    public static CommandPanelContentModel getQuickStylesContentModel(
            ResourceBundle resourceBundle, Locale locale) {
        List<CommandGroupModel> commandGroups = new ArrayList<>();

        MessageFormat mf = new MessageFormat(resourceBundle.getString("PanelStyles.text"));
        mf.setLocale(locale);

        for (int groupIndex = 0; groupIndex < 4; groupIndex++) {
            String commandGroupName = mf.format(new Object[] { groupIndex });
            List<Command> commands = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                final String deco = groupIndex + "/" + i;
                ResizableIcon fontIcon = new Font_x_generic();
                ResizableIcon finalIcon = new DecoratedResizableIcon(fontIcon,
                        (Component c, Graphics g, int x, int y,
                                int width, int height) -> {
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setColor(Color.black);
                            if (c.getComponentOrientation().isLeftToRight()) {
                                g2d.drawString(deco, x + 2, y + height - 2);
                            } else {
                                g2d.drawString(deco,
                                        x + width - g2d.getFontMetrics().stringWidth(deco) - 2,
                                        y + height - 2);
                            }
                            g2d.dispose();
                        });
                Command command = Command.builder()
                        .setIcon(finalIcon).setToggle().setAction(
                                (CommandActionEvent e) -> System.out.println(
                                        "Invoked action on " + deco)).build();
                commands.add(command);
            }

            commandGroups.add(new CommandGroupModel(commandGroupName, commands));
        }

        CommandPanelContentModel commandPanelContentModel =
                new CommandPanelContentModel(commandGroups);
        commandPanelContentModel.setSingleSelectionMode(true);

        return commandPanelContentModel;
    }

    public static void updatePanelContentModel(CommandPanelContentModel model,
            ResourceBundle resourceBundle, Locale locale) {
        MessageFormat mf = new MessageFormat(resourceBundle.getString("PanelStyles.text"));
        mf.setLocale(locale);

        List<CommandGroupModel> commandGroups = model.getCommandGroups();
        for (int groupIndex = 0; groupIndex < commandGroups.size(); groupIndex++) {
            CommandGroupModel commandGroup = commandGroups.get(groupIndex);
            String commandGroupName = mf.format(new Object[] { groupIndex });
            commandGroup.setTitle(commandGroupName);
        }
    }

    public static CommandPanelPresentationModel getQuickStylesPresentationModel() {
        return CommandPanelPresentationModel.builder()
                .setToShowGroupLabels(false)
                .setCommandPresentationState(CommandButtonPresentationState.FIT_TO_ICON)
                .setCommandIconDimension(48).build();
    }
}