package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.ext.WrittenBookItemExt;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;

@Mixin(WrittenBookItem.class)
public class WrittenBookItemMixin implements WrittenBookItemExt {
    @Override
    public ArrayList<String> getPages(ItemStack book) {
        ArrayList<String> pages = new ArrayList<>();

        DataComponentMap nbtCompound = book.getComponents();
        assert nbtCompound != null;
        WrittenBookContent rawPage = nbtCompound.get(DataComponents.WRITTEN_BOOK_CONTENT);

        for (int i = 0; i < rawPage.pages().size(); ++i) {
            pages.add(rawPage.pages().get(i).get(false).getString());
        }

        return pages;
    }

    @Override
    public String getTitle(ItemStack book) {
        DataComponentMap nbtCompound = book.getComponents();
        assert nbtCompound != null;
        WrittenBookContent rawContents = nbtCompound.get(DataComponents.WRITTEN_BOOK_CONTENT);
        return rawContents.title().raw();
    }
}
