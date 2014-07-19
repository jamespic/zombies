package zombie;

public class Shoot implements Action {
    private final PlayerId target;

    public Shoot(PlayerId target) {
        this.target = target;
    }

    public PlayerId getTarget() {
        return target;
    }
}
