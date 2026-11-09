package de.skycracker;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Der SkyCracker: läuft dem nächsten Spieler hinterher und schreit ihn an.
 * Die Sprüche passen zur Situation (Nacht, Regen, wenig Leben, Hunger, Nether ...).
 * Mit Schießpulver in der Hand kann man ihn füttern. Er ist völlig harmlos.
 */
public class SkyCrackerEntity extends PathfinderMob {

    /** Mit /skycracker ruhe ein- und ausschaltbar. */
    public static boolean muted = false;

    private static final String[] NORMAL = {
            "HEY! WARTE AUF MICH!",
            "BOOOOM! DAS WAR ICH!",
            "LAUF NICHT SO SCHNELL, ICH HAB KURZE BEINE!",
            "ZÜNDE MICH AN! ICH HALTE ES NICHT MEHR AUS!",
            "WIE LANGE NOCH?! MIR WIRD LANGWEILIG!",
            "HÖRST DU MIR ÜBERHAUPT ZU?!",
            "DU TRÖDELST SCHON WIEDER!",
            "SCHAU MICH AN, WENN ICH MIT DIR REDE!",
            "AAAAAAAAAAAAAH! ... NUR SO ZUM SPASS.",
            "IST DAS ALLES, WAS DU KANNST?!",
            "FEUER FREI! ... ACH NEIN, DOCH NICHT.",
            "ICH BIN DEIN SKYCRACKER UND ICH BLEIBE BEI DIR!",
            "PSSST... PSSSSST... BOOOOOM!",
            "MACH ENDLICH WAS ZUM BAUEN, ICH LANGWEILE MICH!"
    };
    private static final String[] NIGHT = {
            "ES IST DUNKEL! ICH HAB ANGST! ... NEIN, GAR NICHT!",
            "GEH SCHLAFEN, DU ZOMBIE-FUTTER!",
            "DA DRAUSSEN IST WAS! ICH HÖRE ES!"
    };
    private static final String[] RAIN = {
            "ICH WERDE NASS! MEINE LUNTE GEHT AUS!",
            "REGEN?! DAS IST MEIN ERZFEIND!"
    };
    private static final String[] LOW_HEALTH = {
            "DU STIRBST GLEICH! ESS WAS!",
            "PASS AUF DICH AUF, DU HELD!"
    };
    private static final String[] HUNGRY = {
            "DEIN MAGEN KNURRT SO LAUT, DAS HÖRT MAN BIS HIERHER!",
            "ESS ENDLICH WAS!"
    };
    private static final String[] NETHER = {
            "ES IST HEISS HIER! ICH EXPLODIERE GLEICH VOR FREUDE!",
            "DAS IST DER NETHER! DAS IST GEFÄHRLICH!"
    };
    private static final String[] UNDERWATER = {
            "BLUB BLUB! DU ERTRINKST!",
            "WASSER! WASSER! HILFE!"
    };
    private static final String[] HAPPY = {
            "MMMH, SCHIESSPULVER! DU BIST DER BESTE!",
            "LECKER! NOCH MEHR DAVON!",
            "ICH LIEBE DICH! ... ABER ZÜNDE MICH TROTZDEM NICHT AN!"
    };

    private int yellCooldown;

    public SkyCrackerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.yellCooldown = 100 + this.random.nextInt(200);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createSkyCrackerAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.15));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.yellCooldown > 0) {
            this.yellCooldown--;
            return;
        }
        if (muted || this.level().getNearestPlayer(this, 40.0) == null) {
            this.yellCooldown = 100;
            return;
        }
        this.yell(null);
        this.yellCooldown = 200 + this.random.nextInt(400); // alle 10 bis 30 Sekunden
    }

    /** Füttern mit Schießpulver: der SkyCracker freut sich. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.GUNPOWDER)) {
            if (!this.level().isClientSide()) {
                stack.consume(1, player);
                this.heal(4.0f);
                this.yell(HAPPY);
                this.yellCooldown = 200 + this.random.nextInt(200);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Lässt den SkyCracker sofort schreien (für /skycracker schrei). */
    public void shoutNow() {
        this.yell(null);
    }

    private String[] pickPool(Player nearest) {
        if (nearest != null) {
            if (nearest.getHealth() <= 6.0f && this.random.nextInt(2) == 0) return LOW_HEALTH;
            if (nearest.getFoodData().getFoodLevel() <= 6 && this.random.nextInt(2) == 0) return HUNGRY;
            if (nearest.isUnderWater() && this.random.nextInt(2) == 0) return UNDERWATER;
            if (this.level().dimension() == Level.NETHER && this.random.nextInt(2) == 0) return NETHER;
        }
        if (this.level().isRaining() && this.random.nextInt(3) == 0) return RAIN;
        if (this.level().isDarkOutside() && this.random.nextInt(3) == 0) return NIGHT;
        return NORMAL;
    }

    private void yell(String[] forcedPool) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Player nearest = serverLevel.getNearestPlayer(this, 40.0);
        String[] pool = forcedPool != null ? forcedPool : pickPool(nearest);
        String message = pool[this.random.nextInt(pool.length)];

        Component text = Component.literal("<SkyCracker> ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        for (ServerPlayer player : serverLevel.players()) {
            if (this.distanceToSqr(player) < 40.0 * 40.0) {
                player.sendSystemMessage(text);
            }
        }

        this.playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.9f + this.random.nextFloat() * 0.4f);
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                this.getX(), this.getEyeY() + 0.3, this.getZ(), 25, 0.3, 0.3, 0.3, 0.08);
        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                this.getX(), this.getEyeY() + 0.5, this.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
    }

    // ---- Hilfsfunktionen für die Befehle ----

      public static List<? extends SkyCrackerEntity> all(ServerLevel level) {
        return level.getEntities(SkyCrackerMod.SKYCRACKER, e -> true);
    }

    public static int countIn(ServerLevel level) {
        return all(level).size();
    }

    public static int removeAll(ServerLevel level) {
               List<? extends SkyCrackerEntity> list = all(level);
        for (SkyCrackerEntity e : list) {
            e.discard();
        }
        return list.size();
    }

    public static int shoutAll(ServerLevel level) {
        int count = 0;
        for (SkyCrackerEntity e : all(level)) {
            e.shoutNow();
            count++;
        }
        return count;
    }

    /** Läuft dem nächsten Spieler hinterher und teleportiert sich bei zu großer Entfernung heran. */
    static class FollowPlayerGoal extends Goal {
        private final SkyCrackerEntity mob;
        private final double speed;
        private Player target;
        private int recalcDelay;

        FollowPlayerGoal(SkyCrackerEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.target = this.mob.level().getNearestPlayer(this.mob, 64.0);
            return this.target != null
                    && !this.target.isSpectator()
                    && this.mob.distanceToSqr(this.target) > 16.0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null
                    && this.target.isAlive()
                    && !this.target.isSpectator()
                    && this.mob.distanceToSqr(this.target) > 9.0;
        }

        @Override
        public void start() {
            this.recalcDelay = 0;
        }

        @Override
        public void stop() {
            this.target = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.mob.getLookControl().setLookAt(this.target, 30.0f, 30.0f);
            if (--this.recalcDelay > 0) {
                return;
            }
            this.recalcDelay = 10;
            if (this.mob.distanceToSqr(this.target) > 24.0 * 24.0) {
                this.tryTeleportToTarget();
            } else {
                this.mob.getNavigation().moveTo(this.target, this.speed);
            }
        }

        private void tryTeleportToTarget() {
            for (int i = 0; i < 12; i++) {
                double angle = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
                double distance = 3.0 + this.mob.getRandom().nextDouble() * 3.0;
                double x = this.target.getX() + Math.cos(angle) * distance;
                double z = this.target.getZ() + Math.sin(angle) * distance;
                if (this.mob.randomTeleport(x, this.target.getY(), z, true)) {
                    this.mob.getNavigation().stop();
                    return;
                }
            }
        }
    }
}
