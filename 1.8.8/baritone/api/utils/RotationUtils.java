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

import java.util.Optional;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * @author Brady
 * @since 9/25/2018
 */
public final class RotationUtils {

    /**
     * Constant that a degree value is multiplied by to get the equivalent radian value
     */
    public static final double DEG_TO_RAD = Math.PI / 180.0;

    /**
     * Constant that a radian value is multiplied by to get the equivalent degree value
     */
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    /**
     * Offsets from the root block position to the center of each side.
     */
    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.5, 0, 0.5), // Down
            new Vec3(0.5, 1, 0.5), // Up
            new Vec3(0.5, 0.5, 0), // North
            new Vec3(0.5, 0.5, 1), // South
            new Vec3(0, 0.5, 0.5), // West
            new Vec3(1, 0.5, 0.5)  // East
    };

    private RotationUtils() {}

    /**
     * Calculates the rotation from BlockPos<sub>dest</sub> to BlockPos<sub>orig</sub>
     *
     * @param orig The origin position
     * @param dest The destination position
     * @return The rotation from the origin to the destination
     */
    public static Rotation calcRotationFromCoords(BlockPos orig, BlockPos dest) {
        return calcRotationFromVec3(new Vec3(orig), new Vec3(dest));
    }

    /**
     * Wraps the target angles to a relative value from the current angles. This is done by
     * subtracting the current from the target, normalizing it, and then adding the current
     * angles back to it.
     *
     * @param current The current angles
     * @param target  The target angles
     * @return The wrapped angles
     */
    public static Rotation wrapAnglesToRelative(Rotation current, Rotation target) {
        if (current.yawIsReallyClose(target)) {
            return new Rotation(current.getYaw(), target.getPitch());
        }
        return target.subtract(current).normalize().add(current);
    }

    /**
     * Calculates the rotation from Vec<sub>dest</sub> to Vec<sub>orig</sub> and makes the
     * return value relative to the specified current rotations.
     *
     * @param orig    The origin position
     * @param dest    The destination position
     * @param current The current rotations
     * @return The rotation from the origin to the destination
     * @see #wrapAnglesToRelative(Rotation, Rotation)
     */
    public static Rotation calcRotationFromVec3(Vec3 orig, Vec3 dest, Rotation current) {
        return wrapAnglesToRelative(current, calcRotationFromVec3(orig, dest));
    }

    /**
     * Calculates the rotation from Vec<sub>dest</sub> to Vec<sub>orig</sub>
     *
     * @param orig The origin position
     * @param dest The destination position
     * @return The rotation from the origin to the destination
     */
    private static Rotation calcRotationFromVec3(Vec3 orig, Vec3 dest) {
        double[] delta = {orig.xCoord - dest.xCoord, orig.yCoord - dest.yCoord, orig.zCoord - dest.zCoord};
        double yaw = Math.atan2(delta[0], -delta[2]);
        double dist = Math.sqrt(delta[0] * delta[0] + delta[2] * delta[2]);
        double pitch = Math.atan2(delta[1], dist);
        return new Rotation(
                (float) (yaw * RAD_TO_DEG),
                (float) (pitch * RAD_TO_DEG)
        );
    }

    /**
     * Calculates the look vector for the specified yaw/pitch rotations.
     *
     * @param rotation The input rotation
     * @return Look vector for the rotation
     */
    public static Vec3 calcVec3FromRotation(Rotation rotation) {
        float f = MathHelper.cos(-rotation.getYaw() * (float) DEG_TO_RAD - (float) Math.PI);
        float f1 = MathHelper.sin(-rotation.getYaw() * (float) DEG_TO_RAD - (float) Math.PI);
        float f2 = -MathHelper.cos(-rotation.getPitch() * (float) DEG_TO_RAD);
        float f3 = MathHelper.sin(-rotation.getPitch() * (float) DEG_TO_RAD);
        return new Vec3((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    /**
     * @param ctx Context for the viewing entity
     * @param pos The target block position
     * @return The optional rotation
     * @see #reachable(EntityPlayerSP, BlockPos, double)
     */
    public static Optional<Rotation> reachable(IPlayerContext ctx, BlockPos pos) {
        return reachable(ctx.player(), pos, ctx.playerController().getBlockReachDistance());
    }

    public static Optional<Rotation> reachable(IPlayerContext ctx, BlockPos pos, boolean wouldSneak) {
        return reachable(ctx.player(), pos, ctx.playerController().getBlockReachDistance(), wouldSneak);
    }

    /**
     * Determines if the specified entity is able to reach the center of any of the sides
     * of the specified block. It first checks if the block center is reachable, and if so,
     * that rotation will be returned. If not, it will return the first center of a given
     * side that is reachable. The return type will be {@link Optional#empty()} if the entity is
     * unable to reach any of the sides of the block.
     *
     * @param entity             The viewing entity
     * @param pos                The target block position
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachable(EntityPlayerSP entity, BlockPos pos, double blockReachDistance) {
        return reachable(entity, pos, blockReachDistance, false);
    }

    public static Optional<Rotation> reachable(EntityPlayerSP entity, BlockPos pos, double blockReachDistance, boolean wouldSneak) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(entity);
        if (baritone.getPlayerContext().isLookingAt(pos)) {
            Rotation hypothetical = new Rotation(entity.rotationYaw, entity.rotationPitch + 0.0001F);
            if (wouldSneak) {
                MovingObjectPosition result = RayTraceUtils.rayTraceTowards(entity, hypothetical, blockReachDistance, true);
                if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && result.getBlockPos().equals(pos)) {
                    return Optional.of(hypothetical);
                }
            } else {
                return Optional.of(hypothetical);
            }
        }
        Optional<Rotation> possibleRotation = reachableCenter(entity, pos, blockReachDistance, wouldSneak);
        if (possibleRotation.isPresent()) {
            return possibleRotation;
        }

        IBlockState state = entity.worldObj.getBlockState(pos);
        AxisAlignedBB shape = state.getBlock().getCollisionBoundingBox(entity.worldObj, pos, state);
        if (shape == null) {
            shape = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
        }
        for (Vec3 sideOffset : BLOCK_SIDE_MULTIPLIERS) {
            double xDiff = shape.minX * sideOffset.xCoord + shape.maxX * (1 - sideOffset.xCoord);
            double yDiff = shape.minY * sideOffset.yCoord + shape.maxY * (1 - sideOffset.yCoord);
            double zDiff = shape.minZ * sideOffset.zCoord + shape.maxZ * (1 - sideOffset.zCoord);
            possibleRotation = reachableOffset(entity, pos, new Vec3(pos.getX() + xDiff, pos.getY() + yDiff, pos.getZ() + zDiff), blockReachDistance, wouldSneak);
            if (possibleRotation.isPresent()) {
                return possibleRotation;
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if the specified entity is able to reach the specified block with
     * the given offsetted position. The return type will be {@link Optional#empty()} if
     * the entity is unable to reach the block with the offset applied.
     *
     * @param entity             The viewing entity
     * @param pos                The target block position
     * @param offsetPos          The position of the block with the offset applied.
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachableOffset(Entity entity, BlockPos pos, Vec3 offsetPos, double blockReachDistance, boolean wouldSneak) {
        Vec3 eyes = wouldSneak ? RayTraceUtils.inferSneakingEyePosition(entity) : new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        Rotation rotation = calcRotationFromVec3(eyes, offsetPos, new Rotation(entity.rotationYaw, entity.rotationPitch));
        MovingObjectPosition result = RayTraceUtils.rayTraceTowards(entity, rotation, blockReachDistance, wouldSneak);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (result.getBlockPos().equals(pos)) {
                return Optional.of(rotation);
            }
            if (entity.worldObj.getBlockState(pos).getBlock() instanceof BlockFire && result.getBlockPos().equals(pos.down())) {
                return Optional.of(rotation);
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if the specified entity is able to reach the specified block where it is
     * looking at the direct center of it's hitbox.
     *
     * @param entity             The viewing entity
     * @param pos                The target block position
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachableCenter(Entity entity, BlockPos pos, double blockReachDistance, boolean wouldSneak) {
        return reachableOffset(entity, pos, VecUtils.calculateBlockCenter(entity.worldObj, pos), blockReachDistance, wouldSneak);
    }
}
