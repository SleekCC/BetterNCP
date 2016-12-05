/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.neatmonster.nocheatplus.checks.moving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.combined.BedLeave;
import fr.neatmonster.nocheatplus.checks.combined.Combined;
import fr.neatmonster.nocheatplus.checks.combined.CombinedConfig;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.checks.moving.location.LocUtil;
import fr.neatmonster.nocheatplus.checks.moving.magic.Magic;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveInfo;
import fr.neatmonster.nocheatplus.checks.moving.player.CreativeFly;
import fr.neatmonster.nocheatplus.checks.moving.player.MorePackets;
import fr.neatmonster.nocheatplus.checks.moving.player.NoFall;
import fr.neatmonster.nocheatplus.checks.moving.player.Passable;
import fr.neatmonster.nocheatplus.checks.moving.player.SurvivalFly;
import fr.neatmonster.nocheatplus.checks.moving.util.AuxMoving;
import fr.neatmonster.nocheatplus.checks.moving.util.MovingUtil;
import fr.neatmonster.nocheatplus.checks.moving.vehicle.VehicleChecks;
import fr.neatmonster.nocheatplus.checks.moving.velocity.AccountEntry;
import fr.neatmonster.nocheatplus.checks.moving.velocity.SimpleEntry;
import fr.neatmonster.nocheatplus.checks.net.NetConfig;
import fr.neatmonster.nocheatplus.checks.net.NetData;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.BridgeHealth;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.location.SimplePositionWithLook;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.components.registry.feature.IHaveCheckType;
import fr.neatmonster.nocheatplus.components.registry.feature.INeedConfig;
import fr.neatmonster.nocheatplus.components.registry.feature.INotifyReload;
import fr.neatmonster.nocheatplus.components.registry.feature.IRemoveData;
import fr.neatmonster.nocheatplus.components.registry.feature.JoinLeaveListener;
import fr.neatmonster.nocheatplus.components.registry.feature.TickListener;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.logging.debug.DebugUtil;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.PotionUtil;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MapUtil;

/**
 * Central location to listen to events that are relevant for the moving checks.
 * 
 * @see MovingEvent
 */
public class MovingListener extends CheckListener implements TickListener, IRemoveData, IHaveCheckType, INotifyReload, INeedConfig, JoinLeaveListener{

    /** The no fall check. **/
    public final NoFall                 noFall              = addCheck(new NoFall());

    /** The creative fly check. */
    private final CreativeFly           creativeFly         = addCheck(new CreativeFly());

    /** The more packets check. */
    private final MorePackets           morePackets         = addCheck(new MorePackets());

    private final VehicleChecks         vehicleChecks       = new VehicleChecks();

    /** The survival fly check. */
    private final SurvivalFly        survivalFly            = addCheck(new SurvivalFly());

    /** The Passable (simple no-clip) check.*/
    private final Passable               passable           = addCheck(new Passable());

    /** Combined check but handled here (subject to change!) */
    private final BedLeave               bedLeave           = addCheck(new BedLeave());

    /**
     * Store events by player name, in order to invalidate moving processing on higher priority level in case of teleports.
     */
    private final Map<String, PlayerMoveEvent> processingEvents = new HashMap<String, PlayerMoveEvent>();

    /** Player names to check hover for, case insensitive. */
    private final Set<String> hoverTicks = new LinkedHashSet<String>(30); // TODO: Rename

    /** Player names to check enforcing the location for in onTick, case insensitive. */
    private final Set<String> playersEnforce = new LinkedHashSet<String>(30);

    private int hoverTicksStep = 5;

    /** Location for temporary use with getLocation(useLoc). Always call setWorld(null) after use. Use LocUtil.clone before passing to other API. */
    final Location useLoc = new Location(null, 0, 0, 0); // TODO: Put to use...

    /** Auxiliary functionality. */
    private final AuxMoving aux = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(AuxMoving.class);

    private IGenericInstanceHandle<IAttributeAccess> attributeAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IAttributeAccess.class);

    private final BlockChangeTracker blockChangeTracker;

    /** Statistics / debugging counters. */
    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);
    private final int idMoveHandled = counters.registerKey("event.player.move.handled");
    private final int idMoveHandledPos = counters.registerKey("event.player.move.handled.pos");
    private final int idMoveHandledLook = counters.registerKey("event.player.move.handled.look");
    private final int idMoveHandledPosAndLook = counters.registerKey("event.player.move.handled.pos_look");


    public MovingListener() {
        super(CheckType.MOVING);
        // Register vehicleChecks.
        NCPAPIProvider.getNoCheatPlusAPI().addComponent(vehicleChecks);
        blockChangeTracker = NCPAPIProvider.getNoCheatPlusAPI().getBlockChangeTracker();
    }

    /**
     * A workaround for players placing blocks below them getting pushed off the block by NoCheatPlus.
     * 
     * It essentially moves the "setbackpoint" to the top of the newly placed block, therefore tricking NoCheatPlus into
     * thinking the player was already on top of that block and should be allowed to stay there.
     * 
     * It also prevent players from placing a block on a liquid (which is impossible without a modified version of
     * Minecraft).
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();

        // Ignore players inside a vehicle.
        if (player.isInsideVehicle())
            return;

        final org.bukkit.block.Block block = event.getBlock();
        if (block == null) {
            return;
        }
        final int blockY = block.getY();

        final Material mat = block.getType();

        final MovingData data = MovingData.getData(player);
        final MovingConfig cc = MovingConfig.getConfig(player);
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final Location loc = player.getLocation(useLoc);
        moveInfo.set(player, loc, null, cc.yOnGround);
        if (!MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, data, cc)) {
            aux.returnPlayerMoveInfo(moveInfo);
            useLoc.setWorld(null);
            return;
        }
        aux.returnPlayerMoveInfo(moveInfo);

        if (!data.hasSetBack() || blockY + 1D < data.getSetBackY()) {
            useLoc.setWorld(null);
            return;
        }


        if (Math.abs(loc.getX() - 0.5 - block.getX()) <= 1D
                && Math.abs(loc.getZ() - 0.5 - block.getZ()) <= 1D
                && loc.getY() - blockY > 0D && loc.getY() - blockY < 2D
                && (MovingUtil.canJumpOffTop(mat) || BlockProperties.isLiquid(mat))) {
            // The creative fly and/or survival fly check is enabled, the
            // block was placed below the player and is
            // solid, so do what we have to do.
            data.setSetBackY(blockY + 1D);
            data.sfJumpPhase = 0;
        }
        useLoc.setWorld(null);
    }

    /**
     * We listen to this event to prevent player from flying by sending bed leaving packets.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            priority = EventPriority.MONITOR)
    public void onPlayerBedEnter(final PlayerBedEnterEvent event) {
        CombinedData.getData(event.getPlayer()).wasInBed = true;
    }

    /**
     * We listen to this event to prevent player from flying by sending bed leaving packets.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            priority = EventPriority.MONITOR)
    public void onPlayerBedLeave(final PlayerBedLeaveEvent event) {
        final Player player = event.getPlayer();
        if (bedLeave.isEnabled(player) && bedLeave.checkBed(player)) {
            final MovingConfig cc = MovingConfig.getConfig(player);
            // Check if the player has to be reset.
            // To "cancel" the event, we teleport the player.
            final Location loc = player.getLocation(useLoc);
            final MovingData data = MovingData.getData(player);
            Location target = null;
            final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
            moveInfo.set(player, loc, null, cc.yOnGround);
            final boolean sfCheck = MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, data, cc);
            aux.returnPlayerMoveInfo(moveInfo);
            if (sfCheck) {
                target = data.getSetBack(loc);
            }
            if (target == null) {
                // TODO: Add something to guess the best set back location (possibly data.guessSetBack(Location)).
                target = LocUtil.clone(loc);
            }
            if (sfCheck && cc.sfSetBackPolicyFallDamage && noFall.isEnabled(player, cc)) {
                // Check if to deal damage.
                double y = loc.getY();
                if (data.hasSetBack()) {
                    y = Math.min(y, data.getSetBackY());
                }
                noFall.checkDamage(player, data, y);
            }
            // Cleanup
            useLoc.setWorld(null);
            // Teleport.
            data.prepareSetBack(target); // Should be enough. | new Location(target.getWorld(), target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
            player.teleport(target, TeleportCause.PLUGIN);// TODO: schedule / other measures ?
        }
        else {
            // Reset bed ...
            CombinedData.getData(player).wasInBed = false;
        }
    }

    /**
     * Just for security, if a player switches between worlds, reset the fly and more packets checks data, because it is
     * definitely invalid now.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        // Maybe this helps with people teleporting through Multiverse portals having problems?
        final Player player = event.getPlayer();
        final MovingData data = MovingData.getData(player);
        final MovingConfig cc = MovingConfig.getConfig(player);
        data.clearMostMovingCheckData();
        // TODO: Might omit this if neither check is activated.
        final Location loc = player.getLocation(useLoc);
        data.setSetBack(loc);
        if (cc.loadChunksOnWorldChange) {
            MovingUtil.ensureChunksLoaded(player, loc, "world change", data, cc);
        }
        aux.resetPositionsAndMediumProperties(player, loc, data, cc);
        data.resetTrace(player, loc, TickTask.getTick(), mcAccess.getHandle(), cc);
        if (cc.enforceLocation) {
            // Just in case.
            playersEnforce.add(player.getName());
        }
        useLoc.setWorld(null);
    }

    /**
     * When a player changes their gamemode, all information related to the moving checks becomes invalid.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerGameModeChange(final PlayerGameModeChangeEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.CREATIVE) {
            final MovingData data = MovingData.getData(event.getPlayer());
            data.clearFlyData();
            data.clearPlayerMorePacketsData();
            // TODO: Set new set-back if any fly check is activated.
            // (Keep vehicle data as is.)
        }
    }

    /**
     * When a player moves, they will be checked for various suspicious behaviors.<br>
     * (lowest priority)
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        // Store the event for monitor level checks.
        processingEvents.put(player.getName(), event);

        final MovingData data = MovingData.getData(player);

        final Location from = event.getFrom();
        final Location to = event.getTo();
        Location newTo = null;

        //		// Check problematic yaw/pitch values.
        //		if (LocUtil.needsDirectionCorrection(from.getYaw(), from.getPitch())
        //				|| LocUtil.needsDirectionCorrection(to.getYaw(), to.getPitch())) {
        //			DataManager.getPlayerData(player).task.correctDirection();
        //		}

        // TODO: Check illegal moves here anyway (!).
        // TODO: Check if vehicle move logs correctly (fake).

        // Early return checks (no full processing).
        final boolean earlyReturn;
        if (player.isInsideVehicle()) {
            // No full processing for players in vehicles.
            newTo = vehicleChecks.onPlayerMoveVehicle(player, from, to, data);
            earlyReturn = true;
        }
        else if (player.isDead() || player.isSleeping()) {
            // Ignore dead players.
            data.sfHoverTicks = -1;
            earlyReturn = true;
        }
        else if (player.isSleeping()) {
            // Ignore sleeping playerrs.
            // TODO: sleeping: (which cb!) debug(player, "isSleepingIgnored=" + player.isSleepingIgnored());
            data.sfHoverTicks = -1;
            earlyReturn = true;
        }
        else if (!from.getWorld().equals(to.getWorld())) {
            // Keep hover ticks.
            // Ignore changing worlds.
            earlyReturn = true;
        }
        else {
            earlyReturn = false;
        }

        // TODO: Might log base parts here (+extras).
        if (earlyReturn) {
            // TODO: Remove player from enforceLocation ?
            // TODO: Log "early return: " + tags.
            if (data.debug) {
                debug(player, "Early return on PlayerMoveEvent: from: " + from + " , to: " + to);
            }
            if (newTo != null) {
                // Illegal Yaw/Pitch.
                if (LocUtil.needsYawCorrection(newTo.getYaw())) {
                    newTo.setYaw(LocUtil.correctYaw(newTo.getYaw()));
                }
                if (LocUtil.needsPitchCorrection(newTo.getPitch())) {
                    newTo.setPitch(LocUtil.correctPitch(newTo.getPitch()));
                }
                // Set.
                // TODO: Reset positions? enforceLocation?
                event.setTo(newTo);
                if (data.debug) {
                    debug(player, "Early return on PlayerMoveEvent, set back to: " + newTo);
                }
            }
            data.joinOrRespawn = false;
            return;
        }
        // newTo should be null here.

        // Fire one or two moves here.
        final MovingConfig cc = MovingConfig.getConfig(player);
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final Location loc = player.getLocation(moveInfo.useLoc);
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (cc.loadChunksOnMove) {
            MovingUtil.ensureChunksLoaded(player, from, to, lastMove, "move", data, cc);
        }
        // TODO: On pistons pulling the player back: -1.15 yDistance for split move 1 (untracked position > 0.5 yDistance!).
        if (
                // Handling split moves has been disabled.
                !cc.splitMoves ||
                // The usual case: no micro move happened.
                TrigUtil.isSamePos(from, loc)
                // Special case / bug? TODO: Which/why, which version of MC/spigot?
                || lastMove.valid && TrigUtil.isSamePos(loc, lastMove.from.getX(), lastMove.from.getY(), lastMove.from.getZ())
                // Could also be other envelopes (0.9 velocity upwards), too tedious to research.
                //&& data.lastYDist < -SurvivalFly.GRAVITY_MIN && data.lastYDist > -SurvivalFly.GRAVITY_MAX - SurvivalFly.GRAVITY_MIN
                ) {
            // Fire move from -> to
            // (Special case: Location has not been updated last moving event.)
            moveInfo.set(player, from, to, cc.yOnGround);
            checkPlayerMove(player, from, to, false, moveInfo, data, cc, event);
        }
        else {
            // Split into two moves.
            // 1. Process from -> loc.
            if (data.debug) {
                debug(player, "Split move 1 (from -> loc):");
            }
            moveInfo.set(player, from, loc, cc.yOnGround);
            if (!checkPlayerMove(player, from, loc, true, moveInfo, data, cc, event) && processingEvents.containsKey(player.getName())) {
                // Between -> set data accordingly (compare: onPlayerMoveMonitor).
                onMoveMonitorNotCancelled(player, from, loc, System.currentTimeMillis(), TickTask.getTick(), CombinedData.getData(player), data, cc);
                data.joinOrRespawn = false;
                // 2. Process loc -> to.
                if (data.debug) {
                    debug(player, "Split move 2 (loc -> to):");
                }
                moveInfo.set(player, loc, to, cc.yOnGround);
                checkPlayerMove(player, loc, to, false, moveInfo, data, cc, event);
            }
        }
        // Cleanup.
        data.joinOrRespawn = false;
        aux.returnPlayerMoveInfo(moveInfo);
    }

    /**
     * 
     * @param player
     * @param from
     * @param to
     * @param mightBeMultipleMoves
     * @param moveInfo
     * @param data
     * @param cc
     * @param event
     * @return If cancelled/done, i.e. not to process further split moves.
     */
    private boolean checkPlayerMove(final Player player, final Location from, final Location to, 
            final boolean mightBeMultipleMoves, final PlayerMoveInfo moveInfo, final MovingData data, final MovingConfig cc, 
            final PlayerMoveEvent event) {

        Location newTo = null;

        // TODO: Order this to above "early return"?
        // Set up data / caching.

        // TODO: Data resetting above ?
        data.resetTeleported();
        // Debug.
        if (data.debug) {
            outputMoveDebug(player, moveInfo.from, moveInfo.to, Math.max(cc.noFallyOnGround, cc.yOnGround), mcAccess.getHandle());
        }
        // Check for illegal move and bounding box etc.
        if ((moveInfo.from.hasIllegalCoords() || moveInfo.to.hasIllegalCoords()) ||
                !cc.ignoreStance && (moveInfo.from.hasIllegalStance() || moveInfo.to.hasIllegalStance())) {
            MovingUtil.handleIllegalMove(event, player, data, cc);
            return true;
        }

        { // TODO: Consider to remove ?
            // Debugging statistics, rather light weight.
            final boolean hasPos = !moveInfo.from.isSamePos(moveInfo.to);
            final boolean hasLook = from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();
            counters.addPrimaryThread(idMoveHandled, 1);
            final int counterId;
            if (hasPos && hasLook) {
                counterId = idMoveHandledPosAndLook;
            }
            else if (hasPos) {
                counterId = idMoveHandledPos;
            }
            else if (hasLook) {
                counterId = idMoveHandledLook;
            }
            else {
                counterId = -1;
            }
            if (counterId != -1) {
                counters.addPrimaryThread(counterId, 1);
            }
        }

        final String playerName = player.getName(); // TODO: Could switch to UUID here (needs more changes).

        // Check for location consistency.
        if (cc.enforceLocation && playersEnforce.contains(playerName)) {
            // NOTE: The setback should not be set before this, even if not yet set.
            // Last to vs. from.
            newTo = enforceLocation(player, from, data);
            // TODO: Remove anyway ? 
            playersEnforce.remove(playerName);
        }

        final long time = System.currentTimeMillis();
        if (player.isSprinting() || cc.assumeSprint) {
            // Hard to confine assumesprint further (some logics change with hdist or sprinting).
            // TODO: Collect all these properties within a context object (abstraction + avoid re-fetching). 
            if (player.getFoodLevel() > 5 || player.getAllowFlight() || player.isFlying()) {
                data.timeSprinting = time;
                data.multSprinting = attributeAccess.getHandle().getSprintAttributeMultiplier(player);
                if (data.multSprinting == Double.MAX_VALUE) {
                    data.multSprinting = 1.30000002;
                }
                else if (cc.assumeSprint && data.multSprinting == 1.0) {
                    // Server side can be inconsistent, so the multiplier might be plain wrong (1.0).
                    // TODO: Could be more/less than actual, but "infinite" latency would not work either.
                    data.multSprinting = 1.30000002;
                }
            }
            else if (time < data.timeSprinting) {
                data.timeSprinting = 0;
            }
            else {
                // keep sprinting time.
            }
        }
        else if (time < data.timeSprinting) {
            data.timeSprinting = 0;
        }

        // Prepare locations for use.
        // TODO: Block flags might not be needed if neither sf nor passable get checked.
        final PlayerLocation pFrom, pTo;
        pFrom = moveInfo.from;
        pTo = moveInfo.to;

        // HOT FIX - for VehicleLeaveEvent missing.
        if (data.wasInVehicle) {
            vehicleChecks.onVehicleLeaveMiss(player, data, cc);
        }

        // Set some data for this move.
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        thisMove.set(pFrom, pTo);
        if (mightBeMultipleMoves) {
            thisMove.mightBeMultipleMoves = true;
        }
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();

        // Potion effect "Jump".
        final double jumpAmplifier = aux.getJumpAmplifier(player);
        if (jumpAmplifier > data.jumpAmplifier) {
            data.jumpAmplifier = jumpAmplifier;
        }
        // TODO: same for speed (once medium is introduced).

        // Velocity tick (decrease + invalidation).
        // TODO: Rework to generic (?) queued velocity entries: activation + invalidation
        final int tick = TickTask.getTick();
        data.velocityTick(tick - cc.velocityActivationTicks);

        // Check which fly check to use.
        final boolean checkCf;
        final boolean checkSf;
        if (MovingUtil.shouldCheckSurvivalFly(player, pFrom, data, cc)) {
            checkCf = false;
            checkSf = true;
            data.adjustWalkSpeed(player.getWalkSpeed(), tick, cc.speedGrace);
            thisMove.flyCheck = CheckType.MOVING_SURVIVALFLY;
        }
        else if (cc.creativeFlyCheck && !NCPExemptionManager.isExempted(player, CheckType.MOVING_CREATIVEFLY) && !player.hasPermission(Permissions.MOVING_CREATIVEFLY)) {
            checkCf = true;
            checkSf = false;
            data.adjustFlySpeed(player.getFlySpeed(), tick, cc.speedGrace);
            data.adjustWalkSpeed(player.getWalkSpeed(), tick, cc.speedGrace);
            thisMove.flyCheck = CheckType.MOVING_CREATIVEFLY;
        }
        else {
            checkCf = checkSf = false;
            // (thisMove.flyCheck stays UNKNOWN.)
        }

        // Pre-check checks (hum), either for cf or for sf.
        boolean checkNf = true;
        boolean verticalBounce = false;
        if (checkSf || checkCf) {
            // Ensure we have a set-back set.
            MovingUtil.checkSetBack(player, pFrom, data, this);

            // Check for special cross world teleportation issues with the end.
            if (data.crossWorldFrom != null) {
                if (!TrigUtil.isSamePosAndLook(pFrom, pTo) // Safety check.
                        && TrigUtil.isSamePosAndLook(pTo, data.crossWorldFrom)) {
                    // Assume to (and possibly the player location) to be set to the location the player teleported from within the other world.
                    newTo = data.getSetBack(from);
                    checkNf = false;
                    NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.STATUS, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING) + " Player move end point seems to be set wrongly.");
                }
                // Always reset.
                data.crossWorldFrom = null;
            }

            // Extreme move check (sf or cf is precondition, should have their own config/actions later).
            if (newTo == null && 
                    ((Math.abs(thisMove.yDistance) > Magic.EXTREME_MOVE_DIST_VERTICAL) 
                            || thisMove.hDistance > Magic.EXTREME_MOVE_DIST_HORIZONTAL)) {
                // Test for friction and velocity.
                newTo = checkExtremeMove(player, pFrom, pTo, data, cc);
            }

            // Check jumping on things like slime blocks.
            // Detect potential bounce.
            if (newTo == null) {
                if (to.getY() < from.getY()) {
                    if ((BlockProperties.getBlockFlags(pTo.getTypeIdBelow()) & BlockProperties.F_BOUNCE25) != 0L
                            && !survivalFly.isReallySneaking(player) && checkBounceEnvelope(player, pFrom, pTo, data, cc)
                            ) {
                        // Prepare bounce: The center of the player must be above the block.
                        // TODO: Check other side conditions (fluids, web, max. distance to the block top (!))
                        verticalBounce = true;
                        // Skip NoFall.
                        checkNf = false;
                    }
                }
                else if (data.verticalBounce != null) {
                    // Prepared bounce support.
                    if (onPreparedBounceSupport(player, from, to, lastMove, tick, data)) {
                        checkNf = false;
                    }
                }
            }
        }
        
        // TODO: More adaptive margin / method (bouunding boxes).
        final boolean useBlockChangeTracker = cc.trackBlockMove && (cc.passableCheck || cc.survivalFlyCheck)
                && blockChangeTracker.hasActivity(from.getWorld().getUID(), pFrom, pTo, 3.0);

        // Check passable first to prevent set-back override.
        // TODO: Redesign to set set-backs later (queue + invalidate).
        boolean mightSkipNoFall = false; // If to skip nofall check (mainly on violation of other checks).
        if (newTo == null && cc.passableCheck && player.getGameMode() != BridgeMisc.GAME_MODE_SPECTATOR && !NCPExemptionManager.isExempted(player, CheckType.MOVING_PASSABLE) && !player.hasPermission(Permissions.MOVING_PASSABLE)) {
            // Passable is checked first to get the original set-back locations from the other checks, if needed. 
            newTo = passable.check(player, pFrom, pTo, data, cc, tick, useBlockChangeTracker);
            if (newTo != null) {
                // Check if to skip the nofall check.
                mightSkipNoFall = true;
            }
        }

        // Flying checks.
        if (checkSf) {
            // SurvivalFly

            // Prepare from, to, thisMove for full checking.
            // TODO: Could further differentiate if really needed to (newTo / NoFall).
            MovingUtil.prepareFullCheck(pFrom, pTo, thisMove, Math.max(cc.noFallyOnGround, cc.yOnGround));

            // Hack: Add velocity for transitions between creativefly and survivalfly.
            if (lastMove.toIsValid && lastMove.flyCheck == CheckType.MOVING_CREATIVEFLY) {
                workaroundFlyNoFlyTransition(player, tick, data);
            }

            // Actual check.
            if (newTo == null) {
                // Only check if passable has not already set back.
                newTo = survivalFly.check(player, pFrom, pTo, mightBeMultipleMoves, data, cc, tick, time, useBlockChangeTracker);
            }
            // Only check NoFall, if not already vetoed.
            if (checkNf) {
                checkNf = noFall.isEnabled(player, cc);
            }
            if (newTo == null) {
                // Hover.
                // TODO: Could reset for from-on-ground as well, for not too big moves.
                if (cc.sfHoverCheck 
                        && !(lastMove.toIsValid && lastMove.to.extraPropertiesValid && lastMove.to.onGroundOrResetCond) 
                        && !pTo.isOnGround()) {
                    // Start counting ticks.
                    hoverTicks.add(playerName);
                    data.sfHoverTicks = 0;
                }
                else {
                    data.sfHoverTicks = -1;
                }
                // NoFall.
                if (checkNf) {
                    noFall.check(player, pFrom, pTo, data, cc);
                }
            }
            else {
                if (checkNf && cc.sfSetBackPolicyFallDamage) {
                    if (noFall.estimateDamage(player, from.getY(), data) < 1.0) {
                        // TODO: Consider making this / damage amount configurable.
                        mightSkipNoFall = true;
                    }
                    else if (mightSkipNoFall) {
                        // Check if to really skip.
                        if (!pFrom.isOnGround() && !pFrom.isResetCond()) {
                            mightSkipNoFall = false;
                        }
                    }
                    if (!mightSkipNoFall && (!pTo.isResetCond() || !pFrom.isResetCond())) {
                        // (Don't deal damage where no fall damage is possible.)
                        noFall.checkDamage(player, data, Math.min(from.getY(), to.getY()));
                    }
                }
            }
        }
        else if (checkCf) {
            // CreativeFly
            if (newTo == null) {
                newTo = creativeFly.check(player, pFrom, pTo, data, cc, time, useBlockChangeTracker);
            }
            data.sfHoverTicks = -1;
            data.sfLowJump = false;
        }
        else {
            // No fly checking :(.
            data.clearFlyData();
        }

        // Morepackets.
        if (cc.morePacketsCheck && (newTo == null || data.isMorePacketsSetBackOldest())
                && !NCPExemptionManager.isExempted(player, CheckType.MOVING_MOREPACKETS) 
                && !player.hasPermission(Permissions.MOVING_MOREPACKETS)) {
            /* (Always check morepackets, if there is a chance that setting/overriding newTo is appropriate,
            to avoid packet speeding using micro-violations.) */
            final Location mpNewTo = morePackets.check(player, pFrom, pTo, newTo == null, data, cc);
            if (mpNewTo != null) {
                // Only override set-back, if the morepackets set-back location is older/-est. 
                if (newTo != null && data.debug) {
                    debug(player, "Override set-back by the older morepackets set-back.");
                }
                newTo = mpNewTo;
            }
        }
        else {
            // Otherwise we need to clear their data.
            data.clearPlayerMorePacketsData();
        }

        // Reset jump amplifier if needed.
        if ((checkSf || checkCf) && jumpAmplifier != data.jumpAmplifier) {
            // TODO: General cool-down for latency?
            if (thisMove.touchedGround || !checkSf && (pFrom.isOnGround() || pTo.isOnGround())) {
                // (No need to check from/to for onGround, if SurvivalFly is to be checked.)
                data.jumpAmplifier = jumpAmplifier;
            }
        }

        // Update BlockChangeTracker
        /*
         * TODO: Set an activation flag earlier on, based on query for
         * regions/chunks before running checks (and pass the activation flag to
         * those).
         */
        if (cc.trackBlockMove && data.blockChangeRef.firstSpanEntry != null) {
            if (data.debug) {
                debug(player, "BlockChangeReference: " + data.blockChangeRef.firstSpanEntry.tick + " .. " + data.blockChangeRef.lastSpanEntry.tick + " / " + tick);
            }
            data.blockChangeRef.updateFinal(pTo);
        }

        if (newTo == null) {
            // Allowed move.
            // Bounce effects.
            if (verticalBounce) {
                processBounce(player, pFrom.getY(), pTo.getY(), data, cc);
            }
            // Finished move processing.
            if (processingEvents.containsKey(playerName)) {
                // Normal processing.
                // TODO: More simple: UUID keys or a data flag instead?
                data.playerMoves.finishCurrentMove();
            }
            else {
                // Teleport during violation processing, just invalidate thisMove.
                thisMove.invalidate();
            }
            // Increase time since set-back.
            data.timeSinceSetBack ++;
            return false;
        }
        else {
            // Set-back handling.
            onSetBack(player, event, newTo, data, cc);
            return true;
        }
    }

    /**
     * A slime block is underneath, the player isn't really sneaking.
     * 
     * @param player
     * @param from
     * @param to
     * @param data
     * @param cc
     * @return
     */
    private boolean checkBounceEnvelope(Player player, PlayerLocation from, PlayerLocation to, MovingData data, MovingConfig cc) {
        return 
                // 0: Normal envelope (forestall NoFall).
                (
                        // 1: Ordinary.
                        to.getY() - to.getBlockY() <= Math.max(cc.yOnGround, cc.noFallyOnGround) 
                        // 1: With carpet. TODO: Magic block id.
                        || to.getTypeId() == 171 && to.getY() - to.getBlockY() <= 0.9
                        )
                && MovingUtil.getRealisticFallDistance(player, from.getY(), to.getY(), data) > 1.0
                // 0: Within wobble-distance.
                || to.getY() - to.getBlockY() < 0.286 && to.getY() - from.getY() > -0.5
                && to.getY() - from.getY() < -Magic.GRAVITY_MIN
                && !to.isOnGround()
                ;
    }

    /**
     * Handle a prepare bounce.
     * 
     * @param player
     * @param from
     * @param to
     * @param lastMove
     * @param tick
     * @param data
     * @return True, if bounce has been used, i.e. to do without fall damage.
     */
    private boolean onPreparedBounceSupport(Player player, Location from, Location to, PlayerMoveData lastMove, int tick, MovingData data) {
        if (to.getY() > from.getY() || to.getY() == from.getY() && data.verticalBounce.value < 0.13) {
            // Apply bounce.
            if (to.getY() == from.getY()) {
                // Fake use velocity here.
                data.prependVerticalVelocity(new SimpleEntry(tick, 0.0, 1));
                data.getOrUseVerticalVelocity(0.0);
                if (lastMove.toIsValid && lastMove.yDistance < 0.0) {
                    // Renew the bounce effect.
                    data.verticalBounce = new SimpleEntry(tick, data.verticalBounce.value, 1);
                }
            }
            else {
                data.useVerticalBounce(player);
            }
            return true;
            // TODO: Find % of verticalBounce.value or abs. value for X: yDistance > 0, deviation from effect < X -> set sfNoLowJump
        }
        else {
            data.verticalBounce = null;
            return false;
        }
    }

    /**
     * Check for extremely large moves. Initial intention is to prevent cheaters
     * from creating extreme load. SurvivalFly or CreativeFly is needed.
     * 
     * @param player
     * @param from
     * @param to
     * @param data
     * @param cc
     * @return
     */
    @SuppressWarnings("unused")
    private Location checkExtremeMove(final Player player, final PlayerLocation from, final PlayerLocation to, final MovingData data, final MovingConfig cc) {
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        // TODO: Latency effects.
        double violation = 0.0; // h + v violation (full move).
        // Vertical move.
        final boolean allowVerticalVelocity = false; // TODO: Configurable
        if (Math.abs(thisMove.yDistance) > Magic.EXTREME_MOVE_DIST_VERTICAL) {
            // Exclude valid moves first.
            // About 3.9 seems to be the positive maximum for velocity use in survival mode, regardless jump effect.
            // About -1.85 seems to be the negative maximum for velocity use in survival mode. Falling can result in slightly less than -3.
            if (lastMove.toIsValid && Math.abs(thisMove.yDistance) < Math.abs(lastMove.yDistance)
                    && (thisMove.yDistance > 0.0 && lastMove.yDistance > 0.0 
                            || thisMove.yDistance < 0.0 && lastMove.yDistance < 0.0) 
                    || allowVerticalVelocity && data.getOrUseVerticalVelocity(thisMove.yDistance) != null) {
                // Speed decreased or velocity is present.
            }
            else {
                // Violation.
                violation += thisMove.yDistance; // Could subtract lastMove.yDistance.
            }
        }
        // Horizontal move.
        if (thisMove.hDistance > Magic.EXTREME_MOVE_DIST_HORIZONTAL) {
            // Exclude valid moves first.
            // TODO: Attributes might allow unhealthy moves as well.
            // Observed maximum use so far: 5.515
            // TODO: Velocity flag too (if combined with configurable distances)?
            final double amount = thisMove.hDistance - data.getHorizontalFreedom(); // Will change with model change.
            if (amount < 0.0 || lastMove.toIsValid && thisMove.hDistance - lastMove.hDistance <= 0.0 
                    || data.useHorizontalVelocity(amount) >= amount) {
                // Speed decreased or velocity is present.
            }
            else {
                // Violation.
                violation += thisMove.hDistance; // Could subtract lastMove.yDistance.
            }
        }
        if (violation > 0.0) {
            // Ensure a set-back location is present.
            if (!data.hasSetBack()) {
                data.setSetBack(from);
            }
            // Process violation as sub check of the appropriate fly check.
            violation *= 100.0;
            final Check check;
            final ActionList actions;
            final double vL;
            if (thisMove.flyCheck == CheckType.MOVING_SURVIVALFLY) {
                check = survivalFly;
                actions = cc.survivalFlyActions;
                data.survivalFlyVL += violation;
                vL = data.survivalFlyVL;
            }
            else {
                check = creativeFly;
                actions = cc.creativeFlyActions;
                data.creativeFlyVL += violation;
                vL = data.creativeFlyVL;
            }
            final ViolationData vd = new ViolationData(check, player, vL, violation, actions);
            // TODO: Reduce copy and paste (method to fill in locations, once using exact coords and latering default actions).
            if (vd.needsParameters()) {
                vd.setParameter(ParameterName.LOCATION_FROM, String.format(Locale.US, "%.2f, %.2f, %.2f", from.getX(), from.getY(), from.getZ()));
                vd.setParameter(ParameterName.LOCATION_TO, String.format(Locale.US, "%.2f, %.2f, %.2f", to.getX(), to.getY(), to.getZ()));
                vd.setParameter(ParameterName.DISTANCE, String.format(Locale.US, "%.2f", TrigUtil.distance(from, to)));
                vd.setParameter(ParameterName.TAGS, "EXTREME_MOVE");
            }
            // Some resetting is done in MovingListener.
            if (check.executeActions(vd).willCancel()) {
                // Set-back + view direction of to (more smooth).
                return data.getSetBack(to);
            }
        }
        // No cancel intended.
        return null;

    }

    /**
     * Add velocity, in order to work around issues with turning off flying,
     * triggering SurvivalFly. Asserts last distances are set.
     * 
     * @param tick
     * @param data
     */
    private void workaroundFlyNoFlyTransition(final Player player, final int tick, final MovingData data) {
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final double amount = guessFlyNoFlyVelocity(player, data.playerMoves.getCurrentMove(), lastMove, data);
        data.clearActiveHorVel(); // Clear active velocity due to adding actual speed here.
        data.addHorizontalVelocity(new AccountEntry(tick, amount, 1, MovingData.getHorVelValCount(amount)));
        data.addVerticalVelocity(new SimpleEntry(lastMove.yDistance, 2));
        data.addVerticalVelocity(new SimpleEntry(0.0, 2));
        data.setFrictionJumpPhase();
        // Reset fall height.
        // TODO: Later (e.g. 1.9) check for the ModelFlying, if fall damage is intended.
        data.clearNoFallData();
        player.setFallDistance(0f); // TODO: Might do without this in case of elytra, needs ensure NoFall doesn't kill the player (...).
        if (data.debug) {
            debug(player, "Fly-nofly transition: Add velocity.");
        }
    }

    private static double guessFlyNoFlyVelocity(final Player player, final PlayerMoveData thisMove, final PlayerMoveData lastMove, final MovingData data) {
        // Default margin: Allow slightly less than the previous speed.
        final double defaultAmount = lastMove.hDistance * (1.0 + Magic.FRICTION_MEDIUM_AIR) / 2.0;
        // Test for exceptions.
        if (thisMove.hDistance > defaultAmount && Bridge1_9.isGlidingWithElytra(player)) {
            // Allowing the same speed won't always work on elytra (still increasing, differing modeling on client side with motXYZ).
            // (Doesn't seem to be overly effective.)
            final PlayerMoveData secondPastMove = data.playerMoves.getSecondPastMove();
            if (lastMove.modelFlying != null && secondPastMove.modelFlying != null && Magic.glideEnvelopeWithHorizontalGain(thisMove, lastMove, secondPastMove)) {
                return thisMove.hDistance + Magic.GLIDE_HORIZONTAL_GAIN_MAX;
            }
        }
        return defaultAmount;
    }

    /**
     * Adjust data to allow bouncing back and/or removing fall damage.<br>
     * yDistance is < 0, the middle of the player is above a slime block (to) +
     * on ground. This might be a micro-move onto ground.
     * 
     * @param player
     * @param from
     * @param to
     * @param data
     * @param cc
     */
    private void processBounce(final Player player,final double fromY, final double toY, final MovingData data, final MovingConfig cc) {
        // Prepare velocity.
        final double fallDistance = MovingUtil.getRealisticFallDistance(player, fromY, toY, data);
        final double base =  Math.sqrt(fallDistance) / 3.3;
        double effect = Math.min(3.5, base + Math.min(base / 10.0, Magic.GRAVITY_MAX)); // Ancient Greek technology with gravity added.
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (effect > 0.42 && lastMove.toIsValid) {
            // Extra cap by last y distance(s).
            final double max_gain = Math.abs(lastMove.yDistance < 0.0 ? Math.min(lastMove.yDistance, toY - fromY) : (toY - fromY)) - Magic.GRAVITY_SPAN;
            if (max_gain < effect) {
                effect = max_gain;
                if (data.debug) {
                    debug(player, "Cap bounce effect by recent y-distances.");
                }
            }
        }
        // (Actually observed max. is near 3.5.) TODO: Why 3.14 then?
        if (data.debug) {
            debug(player, "Bounce effect (dY=" + fallDistance + "): " + effect); 
        }
        data.noFallSkipAirCheck = true;
        data.verticalBounce =  new SimpleEntry(effect, 1);
    }

    /**
     * 
     * @param player
     * @param event
     * @param newTo Must be a cloned or new Location instance, free for whatever other plugins do with it.
     * @param data
     * @param cc
     */
    private void onSetBack(final Player player, final PlayerMoveEvent event, final Location newTo, final MovingData data, final MovingConfig cc) {
        // Illegal Yaw/Pitch.
        if (LocUtil.needsYawCorrection(newTo.getYaw())) {
            newTo.setYaw(LocUtil.correctYaw(newTo.getYaw()));
        }
        if (LocUtil.needsPitchCorrection(newTo.getPitch())) {
            newTo.setPitch(LocUtil.correctPitch(newTo.getPitch()));
        }

        // Reset some data.
        data.prepareSetBack(newTo);
        aux.resetPositionsAndMediumProperties(player, newTo, data, cc); // TODO: Might move into prepareSetBack, experimental here.

        // Set new to-location.
        event.setTo(newTo);

        // Debug.
        if (data.debug) {
            debug(player, "Set back to: " + newTo.getWorld() + StringUtil.fdec3.format(newTo.getX()) + ", " + StringUtil.fdec3.format(newTo.getY()) + ", " + StringUtil.fdec3.format(newTo.getZ()));
        }
    }

    /**
     * Monitor level PlayerMoveEvent. Uses useLoc.
     * @param event
     */
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerMoveMonitor(final PlayerMoveEvent event) {

        // TODO: Use stored move data to verify if from/to have changed (thus a teleport will result, possibly a minor issue due to the teleport).

        final long now = System.currentTimeMillis();
        final Player player = event.getPlayer();

        // TODO: Consider to store event.getFrom() from LOWEST priority in processingEvents.
        if (processingEvents.remove(player.getName()) == null) {
            // This means moving data has been reset by a teleport.
            // TODO: vehicles, cancelled, ...
            return;
        }

        if (player.isDead() || player.isSleeping()) {
            return;
        }

        // Feed combined check.
        final CombinedData data = CombinedData.getData(player);


        final Location from = event.getFrom();

        // Feed yawrate and reset moving data positions if necessary.
        final MovingData mData = MovingData.getData(player);
        final int tick = TickTask.getTick();
        final MovingConfig mCc = MovingConfig.getConfig(player);
        if (!event.isCancelled()) {
            final Location pLoc = player.getLocation(useLoc);
            onMoveMonitorNotCancelled(player, TrigUtil.isSamePosAndLook(pLoc, from) ? from : pLoc, event.getTo(), now, tick, data, mData, mCc);
            useLoc.setWorld(null);
        }
        else {
            onCancelledMove(player, from, tick, now, mData, mCc, data);
        }
    }

    /**
     * Adjust data for a cancelled move. No teleport event will fire, but an
     * outgoing position is sent. Note that event.getFrom() may be overridden by
     * a plugin, which the server will ignore, but can lead to confusion.
     * 
     * @param player
     * @param from
     * @param tick
     * @param now
     * @param mData
     * @param data
     */
    private void onCancelledMove(final Player player, final Location from, final int tick, final long now, final MovingData mData, final MovingConfig mCc, final CombinedData data) {
        // TODO: Revise handling of cancelled events.
        data.lastMoveTime = now; // TODO: Move to MovingData?
        // TODO: teleported + other resetting ?
        Combined.feedYawRate(player, from.getYaw(), now, from.getWorld().getName(), data);
        aux.resetPositionsAndMediumProperties(player, from, mData, mCc);
        mData.resetTrace(player, from, tick, mcAccess.getHandle(), mCc); // TODO: Should probably leave this to the teleport event!
        if (((NetConfig) CheckType.NET_FLYINGFREQUENCY.getConfigFactory().getConfig(player)).flyingFrequencyActive) {
            ((NetData) CheckType.NET_FLYINGFREQUENCY.getDataFactory().getData(player)).teleportQueue.onTeleportEvent(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        }
    }

    /**
     * Uses useLoc if in vehicle.
     * @param player
     * @param from Might use useLoc, but will reset it, if in vehicle.
     * @param to Do not use useLoc for this.
     * @param now
     * @param tick
     * @param data
     * @param mData
     */
    private void onMoveMonitorNotCancelled(final Player player, final Location from, final Location to, final long now, final long tick, final CombinedData data, final MovingData mData, final MovingConfig mCc) {
        data.lastMoveTime = now; // TODO: Move to MovingData ?
        final String toWorldName = to.getWorld().getName();
        Combined.feedYawRate(player, to.getYaw(), now, toWorldName, data);
        // TODO: maybe even not count vehicles at all ?
        if (player.isInsideVehicle()) {
            // TODO: refine (!).
            final Location ref = player.getVehicle().getLocation(useLoc);
            aux.resetPositionsAndMediumProperties(player, ref, mData, mCc); // TODO: Consider using to and intercept cheat attempts in another way.
            useLoc.setWorld(null);
            mData.updateTrace(player, to, tick, mcAccess.getHandle()); // TODO: Can you become invincible by sending special moves?
        }
        else if (!from.getWorld().getName().equals(toWorldName)) {
            // A teleport event should follow.
            aux.resetPositionsAndMediumProperties(player, to, mData, mCc);
            mData.resetTrace(player, to, tick, mcAccess.getHandle(), mCc);
        }
        else {
            // TODO: Detect differing location (a teleport event would follow).
            final PlayerMoveData lastMove = mData.playerMoves.getFirstPastMove();
            if (!lastMove.toIsValid || !TrigUtil.isSamePos(to, lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ())) {
                // Something odd happened.
                aux.resetPositionsAndMediumProperties(player, to, mData, mCc);
            }
            else {
                // Normal move, nothing to do.
            }
            mData.updateTrace(player, to, tick, mcAccess.getHandle());
        }
    }

    /**
     * When a player uses a portal, all information related to the moving checks becomes invalid.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerPortal(final PlayerPortalEvent event) {
        final Location to = event.getTo();
        final MovingData data = MovingData.getData(event.getPlayer());
        if (data.debug) {
            debug(event.getPlayer(), "[PORTAL] to=" + to);
        }
        if (to != null) {
            // TODO: This should be redundant, might remove anyway.
            // TODO: Rather add something like setLatencyImmunity(...ms / conditions).
            data.clearMostMovingCheckData();
        }
    }

    /**
     * Clear fly data on death.
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final MovingData data = MovingData.getData(player);
        //final MovingConfig cc = MovingConfig.getConfig(player);
        data.clearMostMovingCheckData();
        data.setSetBack(player.getLocation(useLoc)); // TODO: Monitor this change (!).
        if (data.debug) {
            // Log location.
            debug(player, "Death: " + player.getLocation(useLoc));
        }
        useLoc.setWorld(null);
    }

    /**
     * LOWEST: Checks, indicate cancel processing player move.
     * 
     * @param event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerTeleportLowest(final PlayerTeleportEvent event) {
        final Player player = event.getPlayer();

        // Prevent further moving processing for nested events.
        processingEvents.remove(player.getName());

        // Various early return conditions.
        if (event.isCancelled()) {
            return;
        }
        final TeleportCause cause = event.getCause();
        switch(cause) {
            case COMMAND:
            case ENDER_PEARL:
                break;
            default:
                return;
        }
        final Location to = event.getTo();
        if (to == null) {
            // Better cancel this one.
            //            if (!event.isCancelled()) {
            event.setCancelled(true);
            //            }
            return;
        }
        final MovingData data = MovingData.getData(player);
        if (data.isTeleported(to)) {
            return;
        }

        // Run checks.
        final MovingConfig cc = MovingConfig.getConfig(player);
        boolean cancel = false;
        // Ender pearl into blocks.
        if (cause == TeleportCause.ENDER_PEARL) {
            if (CombinedConfig.getConfig(player).enderPearlCheck && !BlockProperties.isPassable(to)) { // || !BlockProperties.isOnGroundOrResetCond(player, to, 1.0)) {
                // Not check on-ground: Check the second throw.
                // TODO: Bounding box check or onGround as replacement?
                cancel = true;
            }
        }
        // Teleport to untracked locations.
        else if (cause == TeleportCause.COMMAND) { // TODO: TeleportCause.PLUGIN?
            // Attempt to prevent teleporting to players inside of blocks at untracked coordinates.
            if (cc.passableUntrackedTeleportCheck) {
                if (cc.loadChunksOnTeleport) {
                    MovingUtil.ensureChunksLoaded(player, to, "teleport", data, cc);
                }
                if (cc.passableUntrackedTeleportCheck && MovingUtil.shouldCheckUntrackedLocation(player, to)) {
                    final Location newTo = MovingUtil.checkUntrackedLocation(to);
                    if (newTo != null) {
                        // Adjust the teleport to go to the last tracked to-location of the other player.
                        event.setTo(newTo);
                        // TODO: Consider console, consider data.debug.
                        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.TRACE_FILE, player.getName() + " correct untracked teleport destination (" + to + " corrected to " + newTo + ").");
                    }
                }
            }
        }
        // (Here event.setTo might've been called, unless cancel is set.)

        // Handle cancel.
        if (cancel) {
            // NCP actively prevents this teleport.
            event.setCancelled(true);
            // Log.
            if (data.debug) {
                debug(player, "TP " + cause + " (cancel): " + to);
            }
        }
    }

    /**
     * HIGHEST: Revert cancel on set-back.
     * 
     * @param event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        // Only check cancelled events.
        if (!event.isCancelled()) {
            return;
        }
        final Player player = event.getPlayer();
        final MovingData data = MovingData.getData(player);
        Location to = event.getTo();
        // Revert cancel on set-back.
        if (data.isTeleported(to)) {
            // Teleport by NCP.
            final Location teleported = data.getTeleported();
            // Prevent cheaters getting rid of flying data (morepackets, other).
            // TODO: even more strict enforcing ?
            event.setCancelled(false);
            event.setTo(teleported); // ?
            event.setFrom(teleported);
            if (data.debug) {
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.TRACE_FILE, player.getName() + " TP " + event.getCause() + " (revert cancel on set-back): " + to);
            }
            return;
        }
    }

    /**
     * MONITOR: Adjust data to what happened.
     * 
     * @param event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onPlayerTeleportMonitor(final PlayerTeleportEvent event) {
        // Evaluate result and adjust data.
        final Player player = event.getPlayer();
        final MovingData data = MovingData.getData(player);

        // Invalidate first-move thing.
        // TODO: Might conflict with 'moved wrongly' on join.
        data.joinOrRespawn = false;

        // Special cases.
        final Location to = event.getTo();
        if (event.isCancelled()) {
            if (data.isTeleported(to)) {
                // TODO: Schedule a teleport to set-back with PlayerData (+ failure count)?
                // TODO: Log once per player always?
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.TRACE_FILE, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING) +  " TP " + event.getCause() + " (set-back was prevented): " + to);
            }
            else {
                if (data.debug) {
                    debug(player, "TP " + event.getCause() + " (cancelled): " + to);
                }
            }
            data.resetTeleported();
            return;
        }
        else if (to == null) {
            // Weird event.
            // TODO: Log?
            if (!event.isCancelled()) {
                // TODO: Log!
                // TODO: Reset yaw rate, such as to accept any yaw to start with (!).
            }
            data.resetTeleported();
            return;
        }
        final MovingConfig cc = MovingConfig.getConfig(player);
        // Detect our own player set-backs.
        if (data.hasTeleported()) {
            if (data.isTeleported(to)) {
                // Set-back.
                final Location teleported = data.getTeleported();
                final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
                moveInfo.set(player, teleported, null, cc.yOnGround);
                if (cc.loadChunksOnTeleport) {
                    MovingUtil.ensureChunksLoaded(player, teleported, "teleport", data, cc);
                }
                data.onSetBack(moveInfo.from);
                aux.returnPlayerMoveInfo(moveInfo);

                // Reset stuff.
                Combined.resetYawRate(player, teleported.getYaw(), System.currentTimeMillis(), true); // TODO: Not sure.
                data.resetTeleported();

                // Log.
                if (data.debug) {
                    debug(player, "TP " + event.getCause() + " (set back): " + to);
                }
                return;
            }
            else {
                // Another plugin overrides the set-back location.
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.TRACE_FILE, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING) + " TP " + event.getCause() + " (set-back was overridden): " + to);
            }
        }

        boolean skipExtras = false; // Skip extra data adjustments during special teleport, e.g. vehicle set back.
        // Detect our own vehicle set-backs (...).
        if (data.isVehicleSetBack) {
            // Uncertain if this is vehicle leave or vehicle enter.
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
                // TODO: Unexpected, what now?
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.STATUS, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING_VEHICLE) + "Unexpected teleport cause on vehicle set back: " + event.getCause());
            }
            // TODO: Consider to verify, if this is somewhere near the vehicle as expected (might need storing more data for a set back).
            skipExtras = true;
        }

        // Normal teleport
        double fallDistance = data.noFallFallDistance;
        //        final LiftOffEnvelope oldEnv = data.liftOffEnvelope; // Remember for workarounds.
        data.clearFlyData();
        data.clearPlayerMorePacketsData();
        data.setSetBack(to);
        data.sfHoverTicks = -1; // Important against concurrent modification exception.
        if (cc.loadChunksOnTeleport) {
            MovingUtil.ensureChunksLoaded(player, to, "teleport", data, cc);
        }
        aux.resetPositionsAndMediumProperties(player, to, data, cc);
        // TODO: Decide to remove the LiftOffEnvelope thing completely.
        //        if (TrigUtil.maxDistance(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ())  <= 12.0) {
        //            // TODO: Might happen with bigger distances (mainly ender pearl thrown at others).
        //            // Keep old lift-off envelope.
        //            data.liftOffEnvelope = oldEnv;
        //        }

        // Reset stuff.
        Combined.resetYawRate(player, to.getYaw(), System.currentTimeMillis(), true); // TODO: Not sure.
        data.resetTeleported();

        if (!skipExtras) {
            // Adjust fall distance, if set so.
            // TODO: How to account for plugins that reset the fall distance here?
            // TODO: Detect transition from valid flying that needs resetting the fall distance.
            if (fallDistance > 1.0 && fallDistance - player.getFallDistance() > 0.0) {
                // Reset fall distance if set so in the config.
                if (!cc.noFallTpReset) {
                    // (Set fall distance if set to not reset.)
                    player.setFallDistance((float) fallDistance);
                }
                else if (fallDistance >= 3.0) {
                    data.noFallSkipAirCheck = true;
                }
            }
            if (event.getCause() == TeleportCause.ENDER_PEARL) {
                // Prevent NoFall violations for ender-pearls.
                data.noFallSkipAirCheck = true;
            }
        }

        // Cross world teleportation issues with the end.
        final Location from = event.getFrom();
        if (from != null 
                && event.getCause() == TeleportCause.END_PORTAL // Currently only related to this.
                &&!from.getWorld().getName().equals(to.getWorld().getName())) { // Less java, though.
            // Remember the position teleported from.
            data.crossWorldFrom = new SimplePositionWithLook(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        }
        else {
            // Reset last cross world position.
            data.crossWorldFrom = null;
        }

        // Log.
        if (data.debug) {
            debug(player, "TP " + event.getCause() + " (normal): " + to);
        }
    }

    /**
     * Player got a velocity packet. The server can't keep track of actual velocity values (by design), so we have to
     * try and do that ourselves. Very rough estimates.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerVelocity(final PlayerVelocityEvent event) {
        final Player player = event.getPlayer();
        final MovingData data = MovingData.getData(player);
        // Ignore players who are in vehicles.
        if (player.isInsideVehicle()) {
            data.removeAllVelocity();
            return;
        }
        // Process velocity.
        final Vector velocity = event.getVelocity();
        final MovingConfig cc = MovingConfig.getConfig(player);
        data.addVelocity(player, cc, velocity.getX(), velocity.getY(), velocity.getZ());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getCause() != DamageCause.FALL) {
            return;
        }
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        final MovingData data = MovingData.getData(player);
        if (player.isInsideVehicle()) {
            // Ignore vehicles (noFallFallDistance will be inaccurate anyway).
            data.clearNoFallData();
            return;
        }
        final MovingConfig cc = MovingConfig.getConfig(player);
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final double yOnGround = Math.max(cc.noFallyOnGround, cc.yOnGround);
        final Location loc = player.getLocation(useLoc);
        moveInfo.set(player, loc, null, yOnGround);
        final PlayerLocation pLoc = moveInfo.from;
        pLoc.collectBlockFlags(yOnGround);
        if (event.isCancelled() || !MovingUtil.shouldCheckSurvivalFly(player, pLoc, data, cc) || !noFall.isEnabled(player, cc)) {
            data.clearNoFallData();
            useLoc.setWorld(null);
            aux.returnPlayerMoveInfo(moveInfo);
            return;
        }
        boolean allowReset = true;
        if (!data.noFallSkipAirCheck) {
            // Be sure not to lose that block.
            data.noFallFallDistance += 1.0;
            // TODO: Accound for liquid too?
            if (!pLoc.isOnGround(1.0, 0.3, 0.1) && !pLoc.isResetCond() && !pLoc.isAboveLadder() && !pLoc.isAboveStairs()) {
                // Likely a new style no-fall bypass (damage in mid-air).
                data.noFallVL += 1.0;
                if (noFall.executeActions(player, data.noFallVL, 1.0, cc.noFallActions).willCancel() && data.hasSetBack()) {
                    // Cancel the event and restore fall distance.
                    // NoFall data will not be reset 
                    allowReset = false;
                }
            }
            else {
                // Legitimate damage: clear accounting data.
                data.vDistAcc.clear();
                // TODO: Also reset other properties.
                // TODO: Also reset in other cases (moved too quickly)?
            }
        }
        aux.returnPlayerMoveInfo(moveInfo);
        final float fallDistance = player.getFallDistance();
        final double damage = BridgeHealth.getDamage(event);
        final float yDiff = (float) (data.noFallMaxY - loc.getY());
        if (data.debug) {
            debug(player, "Damage(FALL): " + damage + " / dist=" + player.getFallDistance() + " nf=" + data.noFallFallDistance + " yDiff=" + yDiff);
        }
        // Fall-back check.
        final double maxD = NoFall.getDamage(Math.max(yDiff, Math.max(data.noFallFallDistance, fallDistance))) + (allowReset ? 0.0 : 3.0);
        if (maxD > damage) {
            // TODO: respect dealDamage ?
            BridgeHealth.setDamage(event, maxD);
            if (data.debug) {
                debug(player, "Adjust fall damage to: " + maxD);
            }
        }
        if (allowReset) {
            // Normal fall damage, reset data.
            data.clearNoFallData();
        }
        else {
            // Minecraft/NCP bug or cheating.
            // (Do not cancel the event, otherwise: "moved too quickly exploit".)
            if (cc.noFallViolationReset) {
                data.clearNoFallData();
            }
            // Add player to hover checks.
            if (cc.sfHoverCheck && data.sfHoverTicks < 0) {
                data.sfHoverTicks = 0;
                hoverTicks.add(player.getName());
            }
        }
        // Entity fall-distance should be reset elsewhere.
        // Cleanup.
        useLoc.setWorld(null);
    }

    /**
     * When a player respawns, all information related to the moving checks
     * becomes invalid.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final MovingData data = MovingData.getData(player);
        // TODO: Prevent/cancel scheduled teleport (use PlayerData/task for teleport, or a sequence count).
        data.clearMostMovingCheckData();
        data.resetSetBack(); // To force dataOnJoin to set it to loc.
        // Handle respawn like join.
        dataOnJoin(player, event.getRespawnLocation(), data, MovingConfig.getConfig(player), true);
    }

    @Override
    public void playerJoins(final Player player) {
        dataOnJoin(player, player.getLocation(useLoc), MovingData.getData(player), MovingConfig.getConfig(player), false);
        // Cleanup.
        useLoc.setWorld(null);
    }

    /**
     * Alter data for players joining (join, respawn).<br>
     * Do before, if necessary:<br>
     * <li>data.clearFlyData()</li>
     * <li>data.setSetBack(...)</li>
     * @param player
     * @param loc Can be useLoc (!).
     * @param data
     * @param cc
     */
    private void dataOnJoin(Player player, Location loc, MovingData data, MovingConfig cc, boolean isRespawn) {

        final int tick = TickTask.getTick();
        final String tag = isRespawn ? "Respawn" : "Join";
        // Check loaded chunks.
        if (cc.loadChunksOnJoin) {
            // (Don't use past-move heuristic for skipping here.)
            final int loaded = MapUtil.ensureChunksLoaded(loc.getWorld(), loc.getX(), loc.getZ(), Magic.CHUNK_LOAD_MARGIN_MIN);
            if (loaded > 0 && data.debug) {
                StaticLog.logInfo("Player " + tag + ": Loaded " + loaded + " chunk" + (loaded == 1 ? "" : "s") + " for the world " + loc.getWorld().getName() +  " for player: " + player.getName());
            }
        }

        // Correct set-back on join.
        if (!data.hasSetBack() || data.hasSetBackWorldChanged(loc)) {
            data.clearFlyData();
            data.setSetBack(loc);
            // (resetPositions is called below)
            data.joinOrRespawn = true; // TODO: Review if to always set (!).
        }
        else {
            // TODO: Check consistency/distance.
            //final Location setBack = data.getSetBack(loc);
            //final double d = loc.distanceSquared(setBack);
            // TODO: If to reset positions: relate to previous ones and set-back.
            // (resetPositions is called below)
        }
        // (Note: resetPositions resets lastFlyCheck and other.)
        data.clearVehicleData(); // TODO: Uncertain here, what to check.
        data.clearAllMorePacketsData();
        data.removeAllVelocity();
        data.resetTrace(player, loc, tick, mcAccess.getHandle(), cc); // Might reset to loc instead of set-back ?

        // More resetting.
        data.vDistAcc.clear();
        aux.resetPositionsAndMediumProperties(player, loc, data, cc);

        // Enforcing the location.
        if (cc.enforceLocation) {
            playersEnforce.add(player.getName());
        }

        // Hover.
        initHover(player, data, cc, data.playerMoves.getFirstPastMove().from.onGroundOrResetCond); // isOnGroundOrResetCond

        //		// Bad pitch/yaw, just in case.
        //		if (LocUtil.needsDirectionCorrection(useLoc.getYaw(), useLoc.getPitch())) {
        //			DataManager.getPlayerData(player).task.correctDirection();
        //		}

        // Check for vehicles.
        // TODO: Order / exclusion of items.
        if (player.isInsideVehicle()) {
            vehicleChecks.onPlayerVehicleEnter(player, player.getVehicle());
        }

        if (data.debug) {
            // Log location.
            debug(player, tag + ": " + loc);
        }
    }

    /**
     * Initialize the hover check for a player (login, respawn). 
     * @param player
     * @param data
     * @param cc
     * @param isOnGroundOrResetCond 
     */
    private void initHover(final Player player, final MovingData data, final MovingConfig cc, final boolean isOnGroundOrResetCond) {
        // Reset hover ticks until a better method is used.
        if (!isOnGroundOrResetCond && cc.sfHoverCheck) {
            // Start as if hovering already.
            // Could check shouldCheckSurvivalFly(player, data, cc), but this should be more sharp (gets checked on violation).
            data.sfHoverTicks = 0;
            data.sfHoverLoginTicks = cc.sfHoverLoginTicks;
            hoverTicks.add(player.getName());
        }
        else {
            data.sfHoverLoginTicks = 0;
            data.sfHoverTicks = -1;
        }
    }

    @Override
    public void playerLeaves(final Player player) {
        final MovingData data = MovingData.getData(player);
        final Location loc = player.getLocation(useLoc);
        // Debug logout.
        if (data.debug) {
            StaticLog.logInfo("Player " + player.getName() + " leaves at location: " + loc.toString());
        }
        if (!player.isSleeping() && !player.isDead()) {
            // Check for missed moves.
            // TODO: Force-load chunks [log if (!)] ?
            // TODO: Consider to catch all, at least (debug-) logging-wise.
            if (!BlockProperties.isPassable(loc)) {
                final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
                if (lastMove.toIsValid) {
                    final Location refLoc = new Location(loc.getWorld(), lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ());
                    final double d = refLoc.distanceSquared(loc);
                    if (d > 0.0) {
                        // TODO: Consider to always set back here. Might skip on big distances.
                        if (TrigUtil.manhattan(loc, refLoc) > 0 || BlockProperties.isPassable(refLoc)) {
                            if (passable.isEnabled(player)) {
                                StaticLog.logWarning("Potential exploit: Player " + player.getName() + " leaves, having moved into a block (not tracked by moving checks): " + player.getWorld().getName() + " / " + DebugUtil.formatMove(refLoc, loc));
                                // TODO: Actually trigger a passable violation (+tag).
                                if (d > 1.25) {
                                    StaticLog.logWarning("SKIP set-back for " + player.getName() + ", because distance is too high (risk of false positives): " + d);
                                }
                                else {
                                    StaticLog.logInfo("Set back player " + player.getName() + ": " + LocUtil.simpleFormat(refLoc));
                                    data.prepareSetBack(refLoc);
                                    if (!player.teleport(refLoc)) {
                                        StaticLog.logWarning("FAILED to set back player " + player.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        useLoc.setWorld(null);
        // Adjust data.
        survivalFly.setReallySneaking(player, false);
        noFall.onLeave(player);
        // TODO: Add a method for ordinary presence-change resetting (use in join + leave).
        data.onPlayerLeave();
        if (data.vehicleSetBackTaskId != -1) {
            // Reset the id, assume the task will still teleport the vehicle.
            // TODO: Should rather force teleport (needs storing the task + data).
            data.vehicleSetBackTaskId = -1;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldunload(final WorldUnloadEvent event) {
        // TODO: Consider removing the world-related data anyway (even if the event is cancelled).
        MovingData.onWorldUnload(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        survivalFly.setReallySneaking(event.getPlayer(), event.isSneaking());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSprint(final PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) {
            MovingData.getData(event.getPlayer()).timeSprinting = 0;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerToggleFlight(final PlayerToggleFlightEvent event) {
        // (ignoreCancelled = false: we track the bit of vertical extra momentum/thing).
        final Player player = event.getPlayer();
        if (player.isFlying() || event.isFlying() && !event.isCancelled()) {
            return;
        }
        final MovingData data = MovingData.getData(player);
        final MovingConfig cc = MovingConfig.getConfig(player);
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final Location loc = player.getLocation(useLoc);
        moveInfo.set(player, loc, null, cc.yOnGround);
        // TODO: data.isVelocityJumpPhase() might be too harsh, but prevents too easy abuse.
        if (!MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, data, cc) || data.isVelocityJumpPhase() ||
                BlockProperties.isOnGroundOrResetCond(player, loc, cc.yOnGround)) {
            useLoc.setWorld(null);
            aux.returnPlayerMoveInfo(moveInfo);
            return;
        }
        aux.returnPlayerMoveInfo(moveInfo);
        useLoc.setWorld(null);
        // TODO: Configurable.
        // TODO: Confine to minimum activation ticks.
        data.addVelocity(player, cc, 0.0, 0.3, 0.0);
    }

    @Override
    public void onTick(final int tick, final long timeLast) {
        final List<String> rem = new ArrayList<String>(hoverTicks.size()); // Pessimistic.
        // TODO: Change to per world checking (as long as configs are per world).

        // Enforcing location check.
        for (final String playerName : playersEnforce) {
            final Player player = DataManager.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                rem.add(playerName);
                continue;
            }
            else if (player.isDead() || player.isSleeping() || player.isInsideVehicle()) {
                // Don't remove but also don't check [subject to change].
                continue;
            }
            final MovingData data = MovingData.getData(player);
            final Location newTo = enforceLocation(player, player.getLocation(useLoc), data);
            if (newTo != null) {
                data.prepareSetBack(newTo);
                player.teleport(newTo, TeleportCause.PLUGIN);
            }
        }
        if (!rem.isEmpty()) {
            playersEnforce.removeAll(rem);
        }
        // Hover check (survivalfly).
        rem.clear();
        if (tick % hoverTicksStep != 0) {
            // Only check every so and so ticks.
            return;
        }
        final PlayerMoveInfo info = aux.usePlayerMoveInfo();
        for (final String playerName : hoverTicks) {
            // TODO: put players into the set (+- one tick would not matter ?)
            // TODO: might add an online flag to data !
            final Player player = DataManager.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                rem.add(playerName);
                continue;
            }
            final MovingData data = MovingData.getData(player);
            if (player.isDead() || player.isSleeping() || player.isInsideVehicle()) {
                data.sfHoverTicks = -1;
                // (Removed below.)
            }
            if (data.sfHoverTicks < 0) {
                data.sfHoverLoginTicks = 0;
                rem.add(playerName);
                continue;
            }
            else if (data.sfHoverLoginTicks > 0) {
                // Additional "grace period".
                data.sfHoverLoginTicks --;
                continue;
            }
            final MovingConfig cc = MovingConfig.getConfig(player);
            // Check if enabled at all.
            if (!cc.sfHoverCheck) {
                rem.add(playerName);
                data.sfHoverTicks = -1;
                continue;
            }
            // Increase ticks here.
            data.sfHoverTicks += hoverTicksStep;
            if (data.sfHoverTicks < cc.sfHoverTicks) {
                // Don't do the heavier checking here, let moving checks reset these.
                continue;
            }
            if (checkHover(player, data, cc, info)) {
                rem.add(playerName);
            }
        }
        hoverTicks.removeAll(rem);
        rem.clear();
        aux.returnPlayerMoveInfo(info);
        useLoc.setWorld(null);
    }

    private Location enforceLocation(final Player player, final Location loc, final MovingData data) {
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (lastMove.toIsValid && TrigUtil.distanceSquared(lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ(), loc.getX(), loc.getY(), loc.getZ()) > 1.0 / 256.0) {
            // Teleport back. 
            // TODO: Add history / alert?
            //player.sendMessage(ChatColor.RED + "NCP: enforce location !"); // TODO: DEBUG - REMOVE.
            if (data.hasSetBack()) {
                // Might have to re-check all context with playerJoins and keeping old set-backs...
                // Could use a flexible set-back policy (switch to in-air on login). 
                return data.getSetBack(loc);
            }
            else {
                return new Location(player.getWorld(), lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ(), loc.getYaw(), loc.getPitch());
            }
        }
        else {
            return null;
        }
    }

    /**
     * The heavier checking including on.ground etc., check if enabled/valid to check before this. 
     * @param player
     * @param data
     * @param cc
     * @param info
     * @return
     */
    private boolean checkHover(final Player player, final MovingData data, final MovingConfig cc, final PlayerMoveInfo info) {
        // Check if player is on ground.
        final Location loc = player.getLocation(useLoc); // useLoc.setWorld(null) is done in onTick.
        info.set(player, loc, null, cc.yOnGround);
        // (Could use useLoc of MoveInfo here. Note orderm though.)
        final boolean res;
        // TODO: Collect flags, more margin ?
        final int loaded = info.from.ensureChunksLoaded();
        if (loaded > 0 && data.debug) {
            // DEBUG
            StaticLog.logInfo("Hover check: Needed to load " + loaded + " chunk" + (loaded == 1 ? "" : "s") + " for the world " + loc.getWorld().getName() +  " around " + loc.getBlockX() + "," + loc.getBlockZ() + " in order to check player: " + player.getName());
        }
        if (info.from.isOnGroundOrResetCond() || info.from.isAboveLadder() || info.from.isAboveStairs()) {
            res = true;
            data.sfHoverTicks = 0;
        }
        else {
            if (data.sfHoverTicks > cc.sfHoverTicks) {
                // Re-Check if survivalfly can apply at all.
                final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
                moveInfo.set(player, loc, null, cc.yOnGround);
                if (MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, data, cc)) {
                    handleHoverViolation(player, loc, cc, data);
                    // Assume the player might still be hovering.
                    res = false;
                    data.sfHoverTicks = 0;
                }
                else {
                    // Reset hover ticks and check next period.
                    res = false;
                    data.sfHoverTicks = 0;
                }
                aux.returnPlayerMoveInfo(moveInfo);
            }
            else res = false;
        }
        info.cleanup();
        return res;
    }

    private void handleHoverViolation(final Player player, final Location loc, final MovingConfig cc, final MovingData data) {
        // Check nofall damage (!).
        if (cc.sfHoverFallDamage && noFall.isEnabled(player, cc)) {
            // Consider adding 3/3.5 to fall distance if fall distance > 0?
            noFall.checkDamage(player, data, loc.getY());
        }
        // Delegate violation handling.
        survivalFly.handleHoverViolation(player, loc, cc, data);
    }

    @Override
    public CheckType getCheckType() {
        // TODO: this is for the hover check only...
        return CheckType.MOVING_SURVIVALFLY;
    }

    @Override
    public IData removeData(String playerName) {
        hoverTicks.remove(playerName);
        playersEnforce.remove(playerName);
        return null;
    }

    @Override
    public void removeAllData() {
        hoverTicks.clear();
        playersEnforce.clear();
        aux.clear();
    }

    @Override
    public void onReload() {
        aux.clear();
        hoverTicksStep = Math.max(1, ConfigManager.getConfigFile().getInt(ConfPaths.MOVING_SURVIVALFLY_HOVER_STEP));
        MovingData.onReload();
    }

    /**
     * Output information specific to player-move events.
     * @param player
     * @param from
     * @param to
     * @param mcAccess
     */
    private void outputMoveDebug(final Player player, final PlayerLocation from, final PlayerLocation to, final double maxYOnGround, final MCAccess mcAccess) {
        final StringBuilder builder = new StringBuilder(250);
        final Location loc = player.getLocation();
        builder.append(CheckUtils.getLogMessagePrefix(player, checkType));
        builder.append("MOVE in world " + from.getWorld().getName() + ":\n");
        DebugUtil.addMove(from, to, loc, builder);
        final double jump = mcAccess.getJumpAmplifier(player);
        final double speed = mcAccess.getFasterMovementAmplifier(player);
        final double strider = BridgeEnchant.getDepthStriderLevel(player);
        if (BuildParameters.debugLevel > 0) {
            try{
                // TODO: Check backwards compatibility (1.4.2). Remove try-catch
                builder.append("\n(walkspeed=" + player.getWalkSpeed() + " flyspeed=" + player.getFlySpeed() + ")");
            } catch (Throwable t) {}
            if (player.isSprinting()) {
                builder.append("(sprinting)");
            }
            if (player.isSneaking()) {
                builder.append("(sneaking)");
            }
            if (player.isBlocking()) {
                builder.append("(blocking)");
            }
            final Vector v = player.getVelocity();
            if (v.lengthSquared() > 0.0) {
                builder.append("(svel=" + v.getX() + "," + v.getY() + "," + v.getZ() + ")");
            }
        }
        if (speed != Double.NEGATIVE_INFINITY) {
            builder.append("(e_speed=" + (speed + 1) + ")");
        }
        final double slow = PotionUtil.getPotionEffectAmplifier(player, PotionEffectType.SLOW);
        if (slow != Double.NEGATIVE_INFINITY) {
            builder.append("(e_slow=" + (slow + 1) + ")");
        }
        if (jump != Double.NEGATIVE_INFINITY) {
            builder.append("(e_jump=" + (jump + 1) + ")");
        }
        if (strider != 0) {
            builder.append("(e_depth_strider=" + strider + ")");
        }
        if (Bridge1_9.isGliding(player)) {
            builder.append("(gliding)");
        }
        if (player.getAllowFlight()) {
            builder.append("(allow_flight)");
        }
        if (player.isFlying()) {
            builder.append("(flying)");
        }
        // Print basic info first in order
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
        // Extended info.
        if (BuildParameters.debugLevel > 0) {
            builder.setLength(0);
            // Note: the block flags are for normal on-ground checking, not with yOnGrond set to 0.5.
            from.collectBlockFlags(maxYOnGround);
            if (from.getBlockFlags() != 0) {
                builder.append("\nfrom flags: " + StringUtil.join(BlockProperties.getFlagNames(from.getBlockFlags()), "+"));
            }
            if (from.getTypeId() != 0) {
                DebugUtil.addBlockInfo(builder, from, "\nfrom");
            }
            if (from.getTypeIdBelow() != 0) {
                DebugUtil.addBlockBelowInfo(builder, from, "\nfrom");
            }
            if (!from.isOnGround() && from.isOnGround(0.5)) {
                builder.append(" (ground within 0.5)");
            }
            to.collectBlockFlags(maxYOnGround);
            if (to.getBlockFlags() != 0) {
                builder.append("\nto flags: " + StringUtil.join(BlockProperties.getFlagNames(to.getBlockFlags()), "+"));
            }
            if (to.getTypeId() != 0) {
                DebugUtil.addBlockInfo(builder, to, "\nto");
            }
            if (to.getTypeIdBelow() != 0) {
                DebugUtil.addBlockBelowInfo(builder, to, "\nto");
            }
            if (!to.isOnGround() && to.isOnGround(0.5)) {
                builder.append(" (ground within 0.5)");
            }
            NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
        }
    }

}
