package modoru.main.data.user.cosmetics;

public enum Particle {

    CHERRY(2, 1, org.bukkit.Particle.CHERRY_LEAVES),
    PALE(2, 1, org.bukkit.Particle.PALE_OAK_LEAVES);

    public final int count, thirdPersonCount;
    public final org.bukkit.Particle particleType;

    /**
     * @param count amount of particles shown per spawn tick (half of second) to the player itself
     * @param thirdPersonCount amount of particles shown per spawn tick (half of second) to the players who see the player
     */
    Particle(int count, int thirdPersonCount, org.bukkit.Particle particleType) {
        this.count = count;
        this.thirdPersonCount = thirdPersonCount;
        this.particleType = particleType;
    }

}
