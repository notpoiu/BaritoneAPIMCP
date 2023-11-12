/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import baritone.api.utils.accessor.IItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IRegistry;
import net.minecraft.util.ResourceLocation;

public final class BlockOptionalMeta {

    private final Block block;
    private final Set<IBlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;
    private final ImmutableSet<Integer> stackHashes;
    private static final Pattern pattern = Pattern.compile("^(.+?)(?::(\\d+))?$");

    public BlockOptionalMeta(Block block) {
        this.block = block;
        this.blockstates = getStates(block);
        this.stateHashes = getStateHashes(blockstates);
        this.stackHashes = getStackHashes(blockstates);
    }

    public BlockOptionalMeta(String selector) {
        Matcher matcher = pattern.matcher(selector);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block selector");
        }

        MatchResult matchResult = matcher.toMatchResult();

        ResourceLocation id = new ResourceLocation(matchResult.group(1));

        if (!Block.blockRegistry.containsKey(id)) {
            throw new IllegalArgumentException("Invalid block ID");
        }

        block = (Block) Block.blockRegistry.getObject(id);
        blockstates = getStates(block);
        stateHashes = getStateHashes(blockstates);
        stackHashes = getStackHashes(blockstates);
    }

    private static Set<IBlockState> getStates(Block block) {
        return new HashSet<>(block.getBlockState().getValidStates());
    }

    private static ImmutableSet<Integer> getStateHashes(Set<IBlockState> blockstates) {
        return ImmutableSet.copyOf(
            blockstates.stream().map(IBlockState::hashCode).collect(Collectors.toSet())
        );
    }

    private static ImmutableSet<Integer> getStackHashes(Set<IBlockState> blockstates) {
        return ImmutableSet.copyOf(
            blockstates.stream().map(state -> state.getBlock().hashCode()).collect(Collectors.toSet())
        );
    }

    public Block getBlock() {
        return block;
    }

    public boolean matches(Block block) {
        return block == this.block;
    }

    public boolean matches(IBlockState blockstate) {
        Block block = blockstate.getBlock();
        return block == this.block && stateHashes.contains(blockstate.hashCode());
    }

    public boolean matches(ItemStack stack) {
        //noinspection ConstantConditions
        int hash = ((IItemStack) (Object) stack).getBaritoneHash();

        hash -= stack.getItemDamage();

        return stackHashes.contains(hash);
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s}", block);
    }

    public IBlockState getAnyBlockState() {
        if (blockstates.size() > 0) {
            return blockstates.iterator().next();
        }

        return null;
    }
}
