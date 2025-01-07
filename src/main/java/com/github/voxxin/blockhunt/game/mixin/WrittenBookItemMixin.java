package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.ext.WrittenBookItemExt;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;

@Mixin(WrittenBookItem.class)
public class WrittenBookItemMixin implements WrittenBookItemExt {
    @Override
    public ArrayList<String> getPages(ItemStack book) {
        ArrayList<String> pages = new ArrayList<>();

        ComponentMap nbtCompound = book.getComponents();
        assert nbtCompound != null;
        WrittenBookContentComponent rawPage = nbtCompound.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);

        for (int i = 0; i < rawPage.pages().size(); ++i) {
            pages.add(rawPage.pages().get(i).get(false).getString());
        }

        return pages;
    }

    @Override
    public String getTitle(ItemStack book) {
        ComponentMap nbtCompound = book.getComponents();
        assert nbtCompound != null;
        WrittenBookContentComponent rawContents = nbtCompound.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        return rawContents.title().raw();
    }
}
