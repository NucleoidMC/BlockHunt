package com.github.voxxin.blockhunt.game.util.ext;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public interface WrittenBookItemExt {

    ArrayList<String> getPages(ItemStack book);

    String getTitle(ItemStack book);
}
