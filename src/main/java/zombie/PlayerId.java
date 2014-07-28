
package zombie;

import java.util.Objects;

public class PlayerId {
    private final String name;
    private final int number;

    public PlayerId(String name, int number) {
        this.name = name;
        this.number = number;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.getName());
        hash = 19 * hash + this.getNumber();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlayerId other = (PlayerId) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.getNumber() != other.getNumber()) {
            return false;
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "PlayerId{" +
                "name='" + name + '\'' +
                ", number=" + number +
                '}';
    }
}
