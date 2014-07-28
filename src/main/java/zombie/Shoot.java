package zombie;

public class Shoot implements Action {
    private final PlayerId target;

    public Shoot(PlayerId target) {
        this.target = target;
    }

    public PlayerId getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Shoot shoot = (Shoot) o;

        if (!target.equals(shoot.target)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return "Shoot{" +
                "target=" + target +
                '}';
    }
}
