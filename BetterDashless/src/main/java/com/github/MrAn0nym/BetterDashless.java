package com.github.MrAn0nym;

import android.content.Context;
import android.widget.TextView;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.ReflectUtils;
import com.discord.databinding.WidgetChannelsListItemChannelBinding;
import com.discord.databinding.WidgetHomeBinding;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter.ItemChannelText;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.home.WidgetHome;
import com.discord.widgets.home.WidgetHomeHeaderManager;
import com.discord.widgets.home.WidgetHomeModel;

import java.lang.reflect.Method;

@AliucordPlugin
public class BetterDashless extends Plugin {
    
    @Override
    public void start(Context context) {
        // Create a logger instance
        Logger logger = new Logger("BetterDashless");
        
        // Patch channel list item to replace dashes with spaces
        patcher.patch(ItemChannelText.class, "onConfigure",
                new Class<?>[]{ int.class, ChannelListItem.class }, 
                new Hook(callFrame -> {
                    try {
                        ItemChannelText itemChannelText = (ItemChannelText) callFrame.thisObject;
                        WidgetChannelsListItemChannelBinding binding = 
                            (WidgetChannelsListItemChannelBinding) ReflectUtils
                                .getField(itemChannelText, "binding");
                        
                        TextView channelNameView = binding.getRoot()
                            .findViewById(com.aliucord.Utils.getResId("channels_item_channel_name", "id"));
                        
                        if (channelNameView != null) {
                            String originalText = channelNameView.getText().toString();
                            channelNameView.setText(originalText.replace("-", " "));
                        }
                    } catch (Exception e) {
                        logger.error("Error in channel name patching", e);
                    }
                }));
        
        // Patch home header to replace dashes with spaces
        patcher.patch(WidgetHomeHeaderManager.class, "configure",
                new Class<?>[]{ WidgetHome.class, WidgetHomeModel.class, WidgetHomeBinding.class },
                new Hook(callFrame -> {
                    try {
                        WidgetHome widgetHome = (WidgetHome) callFrame.args[0];
                        WidgetHomeModel homeModel = (WidgetHomeModel) callFrame.args[1];
                        
                        // Get the channel object
                        Object channel = homeModel.getChannel();
                        
                        // Use reflection to find a method that returns the channel name
                        String channelName = null;
                        for (Method method : channel.getClass().getDeclaredMethods()) {
                            if (method.getReturnType() == String.class && method.getParameterCount() == 0) {
                                method.setAccessible(true);
                                try {
                                    String potentialName = (String) method.invoke(channel);
                                    if (potentialName != null && potentialName.contains("-")) {
                                        channelName = potentialName.replace("-", " ");
                                        break;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        
                        // If we found a name, set it
                        if (channelName != null) {
                            widgetHome.setActionBarTitle(channelName);
                        }
                    } catch (Exception e) {
                        logger.error("Error in home header patching", e);
                    }
                }));
    }
    
    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
