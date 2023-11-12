package baritone.api.command.datatypes;

import java.util.stream.Stream;

import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

public enum EntityClassById implements IDatatypeFor<Class<? extends Entity>> {
    INSTANCE;

    @Override
    public Class<? extends Entity> get(IDatatypeContext ctx) throws CommandException {
        String id = ctx.getConsumer().getString();
        Class<? extends Entity> entityClass;
        if ((entityClass = (Class<? extends Entity>) EntityList.stringToClassMapping.get(id)) == null) {
            throw new IllegalArgumentException("no entity found by that id");
        }
        return entityClass;
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(EntityList.stringToClassMapping.keySet().stream())
                .filterPrefixNamespaced(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }
}